package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.tags.EnchantmentTags;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 player hooks: the per-tick pipeline and lifecycle handlers that the
 * NeoForge events used to drive.
 *
 * <p>26.3 adaptation: keepInventory is read via
 * {@code level.getGameRules().get(GameRules.KEEP_INVENTORY)} (the gamerules are
 * registry-driven now); curse checks use {@code EnchantmentTags.CURSE}; and the
 * fabric dimension-change event (ServerEntityWorldChangeEvents) no longer
 * exists, so the respawn/dimension sync is covered by AFTER_RESPAWN only until
 * a dedicated hook lands with the new mixins.
 */
public final class PlayerHooks {
    private PlayerHooks() {}

    public static float rackDigSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed) {
        return ToolService.forBlock(player, state, pos)
                .map(choice -> ToolService.digSpeedWith(player, choice.stack(), state))
                .orElse(speed);
    }

    public static float wornToolFloor(Player player, BlockState state, @Nullable BlockPos pos, float speed) {
        ItemStack acting = ToolService.forBlock(player, state, pos)
                .map(ToolService.ToolChoice::stack)
                .orElseGet(player::getMainHandItem);
        float floor = WornToolFloor.floorFor(player, acting, state);
        return floor > 0.0F && speed < floor ? floor : speed;
    }

    public static boolean harvestCheck(Player player, BlockState state, boolean canHarvest) {
        return canHarvest
                || ToolService.forBlock(player, state)
                        .map(choice -> choice.stack().isCorrectToolForDrops(state))
                        .orElse(false);
    }

    public static void playerTick(ServerPlayer player) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        TabRules.apply(player, loadout);
        TabService.recoverIfOrphaned(player);
        mirrorOffhand(player, loadout);
        normalizeBrokenTools(loadout);
        LoadoutReturnService.sweep(player, loadout);
        RefillService.tick(player);
        UpgradeService.tick(player);
        FurnaceService.tick(player, loadout);
        SyncService.syncIfDirty(player);
    }

    private static void mirrorOffhand(ServerPlayer player, PlayerLoadout loadout) {
        ItemStack real = player.getInventory().getItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
        ItemStack stored = loadout.offhandStore.getStackInSlot(loadout.activeOffhand);
        if (real != stored) {
            loadout.offhandStore.setStackInSlot(loadout.activeOffhand, real);
        }
    }

    private static void normalizeBrokenTools(PlayerLoadout loadout) {
        for (int i = 0; i < loadout.tools.getSlots(); i++) {
            ItemStack stack = loadout.tools.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getCount() <= 0) {
                loadout.tools.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public static void onLogin(ServerPlayer player) {
        SyncService.syncNow(player);
        SelfTest.onLogin(player);
    }

    public static void onLogout(Player player) {
        RefillService.forget(player.getUUID());
    }

    public static void onRespawnOrDimensionChange(ServerPlayer player) {
        SyncService.syncNow(player);
    }

    public static void onClone(Player original, Player player, boolean wasDeath) {
        PlayerLoadout old = PlayerLoadout.existing(original);
        if (old != null) {
            PlayerLoadout copy = new PlayerLoadout();
            copy.deserializeNBT(player.registryAccess(), old.serializeNBT(original.registryAccess()));
            player.setAttached(ModAttachments.LOADOUT, copy);
        }
        if (wasDeath) {
            PlayerLoadout loadout = PlayerLoadout.of(player);
            boolean keepInventory = ((ServerLevel) player.level()).getGameRules().get(GameRules.KEEP_INVENTORY);
            if (keepInventory) {
                loadout.offhandStore.setStackInSlot(loadout.activeOffhand, ItemStack.EMPTY);
            }
            loadout.dirty = true;
        }
    }

    public static void onDeath(ServerPlayer player) {
        TabService.resetToDefault(player);
    }

    public static void collectDrops(ServerPlayer player, Consumer<ItemEntity> drops) {
        if (player.isSpectator()) {
            return;
        }
        if (!((ServerLevel) player.level()).getGameRules().get(GameRules.KEEP_INVENTORY)) {
            PlayerLoadout loadout = PlayerLoadout.of(player);
            loadout.offhandStore.setStackInSlot(loadout.activeOffhand, ItemStack.EMPTY);
            dropAll(drops, player, loadout.tools, LoadoutReturnService.SECTION_TOOLS);
            dropAll(drops, player, loadout.offhandStore, LoadoutReturnService.SECTION_OFFHAND);
            dropAll(drops, player, loadout.armorStore, LoadoutReturnService.SECTION_ARMOR);
            dropAll(drops, player, loadout.gather, LoadoutReturnService.SECTION_GATHER);
            dropAll(drops, player, loadout.backpacks, LoadoutReturnService.SECTION_BACKPACKS);
            dropAll(drops, player, loadout.personal, LoadoutReturnService.SECTION_PERSONAL);
            dropAll(drops, player, loadout.toolbox, LoadoutReturnService.SECTION_TOOLBOX);
            dropAll(drops, player, loadout.furnace.items, LoadoutReturnService.SECTION_FURNACE);
            ItemStack held = LoadoutReturnService.packHeld(player, loadout);
            if (held != null) {
                ItemEntity entity = new ItemEntity(
                        player.level(), player.getX(), player.getY() - 0.3, player.getZ(), held);
                entity.setDefaultPickUpDelay();
                entity.setThrower(player);
                drops.accept(entity);
            }
            loadout.dirty = true;
        }
    }

    private static void dropAll(Consumer<ItemEntity> drops, ServerPlayer player,
            ItemStackHandler handler, String section) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            handler.setStackInSlot(i, ItemStack.EMPTY);
            // 26.3: the curse check is tag-based (CURSE covers binding + vanishing).
            if (EnchantmentHelper.hasTag(stack, EnchantmentTags.CURSE)) {
                continue;
            }
            int chunk = Math.max(1, StackUpgradeService.originalMax(stack));
            while (!stack.isEmpty()) {
                int take = Math.min(chunk, stack.getCount());
                ItemStack part = stack.copyWithCount(take);
                LoadoutReturnService.tag(part, player, section, i);
                stack.shrink(take);
                ItemEntity entity = new ItemEntity(
                        player.level(), player.getX(), player.getY() - 0.3, player.getZ(), part);
                entity.setDefaultPickUpDelay();
                entity.setThrower(player);
                drops.accept(entity);
            }
        }
    }
}
