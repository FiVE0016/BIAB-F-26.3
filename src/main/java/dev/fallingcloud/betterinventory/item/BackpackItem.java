package dev.fallingcloud.betterinventory.item;

import dev.fallingcloud.betterinventory.block.BackpackBlockEntity;
import dev.fallingcloud.betterinventory.data.StoredItems;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.menu.BackpackMenu;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A backpack. Right click opens it, shift right click places it as a block (when
 * aiming at one) or equips it into a free backpack slot (when aiming at air).
 */
public class BackpackItem extends BlockItem {
    private final int level; // 1..5

    public BackpackItem(int level, Block block, Properties properties) {
        super(block, properties);
        this.level = level;
    }

    public int level() {
        return level;
    }
    
    public static int levelOf(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem b ? b.level() : 0;
    }
    
    /** Pages this backpack level unlocks: level 1 has none, level 5 has 4. */
    public int pages() {
        return Math.max(0, level - 1);
    }

    /** Number of ability upgrade slots for this backpack level: 2/4/6/8/10. */
    public int upgradeCapacity() {
        return level * 2;
    }

    public static int upgradeCapacityOf(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem b ? b.upgradeCapacity() : 0;
    }

    public static NonNullList<ItemStack> upgrades(ItemStack stack) {
        return stack.getOrDefault(ModComponents.BACKPACK_UPGRADES.get(), StoredItems.EMPTY).unpack(10);
    }

    public static TabSettings settings(ItemStack stack) {
        return stack.getOrDefault(ModComponents.BACKPACK_SETTINGS.get(), TabSettings.DEFAULT);
    }

    public static void setSettings(ItemStack stack, TabSettings settings) {
        stack.set(ModComponents.BACKPACK_SETTINGS.get(), settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            InteractionResult placed = super.useOn(context);
            // 26.3 made BlockItem#updateCustomBlockEntityTag static, so the stored
            // contents can no longer be restored by overriding it; do it here instead.
            if (placed.consumesAction()) {
                restoreStored(context);
            }
            return placed;
        }
        if (player != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                BackpackMenu.openHand(serverPlayer, context.getHand());
            }
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            // Equip into the first free backpack slot.
            PlayerLoadout loadout = ModAttachments.get(player);
            for (int i = 0; i < loadout.backpacks.getSlots(); i++) {
                if (loadout.backpacks.getStackInSlot(i).isEmpty()) {
                    if (!level.isClientSide()) {
                        loadout.backpacks.setStackInSlot(i, stack.copy());
                        player.setItemInHand(hand, ItemStack.EMPTY);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
                }
            }
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BackpackMenu.openHand(serverPlayer, hand);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }



    private static void restoreStored(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        for (BlockPos candidate : new BlockPos[]{
                context.getClickedPos(),
                context.getClickedPos().relative(context.getClickedFace())}) {
            if (level.getBlockEntity(candidate) instanceof BackpackBlockEntity backpack
                    && backpack.stored().isEmpty()) {
                backpack.setStored(stack.copyWithCount(1));
                return;
            }
        }
    }
}

