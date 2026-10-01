package dev.fallingcloud.betterinventory.item;

import dev.fallingcloud.betterinventory.block.BackpackBlockEntity;
import dev.fallingcloud.betterinventory.data.StoredItems;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.menu.BackpackMenu;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.betterinventory.backpack.level", level, upgradeCapacity()).withStyle(ChatFormatting.GRAY));
        int count = 0;
        StoredItems contents = stack.getOrDefault(ModComponents.BACKPACK_CONTENTS.get(), StoredItems.EMPTY);
        for (StoredItems.Entry entry : contents.entries()) {
            count += entry.stack().getCount();
        }
        if (count > 0) {
            tooltip.accept(Component.translatable("item.betterinventory.backpack.contents", count).withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("item.betterinventory.backpack.howto").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
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
