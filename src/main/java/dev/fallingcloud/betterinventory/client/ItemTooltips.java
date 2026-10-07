package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.data.StoredItems;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.CraftingUpgradeItem;
import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip text for BetterInventory items, kept in one place.
 *
 * <p>Mojang deprecated {@code Item#appendHoverText} in 1.21.5. Fabric's
 * {@code ItemTooltipCallback} is the documented replacement, so the text lives here
 * instead of inside each item class. Keeping it in a single file keeps upstream
 * changes to the item classes from colliding with our tooltip text.
 */
public final class ItemTooltips {

    private ItemTooltips() {
    }

    /** Registers the tooltip callback. Call from the client entry point. */
    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (stack.getItem() instanceof CraftingUpgradeItem upgrade) {
                lines.add(Component.translatable(
                        "item.betterinventory.crafting_upgrade.desc",
                        upgrade.cols() + "x" + upgrade.rows())
                        .withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.betterinventory.crafting_upgrade.howto")
                        .withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            if (stack.getItem() instanceof UpgradeItem upgrade) {
                lines.add(Component.translatable(
                        "item.betterinventory.upgrade_" + upgrade.kind().id() + ".desc")
                        .withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.betterinventory.upgrade.howto")
                        .withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            if (stack.getItem() instanceof BackpackItem backpack) {
                lines.add(Component.translatable(
                        "item.betterinventory.backpack.level",
                        backpack.level(), backpack.upgradeCapacity())
                        .withStyle(ChatFormatting.GRAY));
                addContentsLine(stack, lines);
                lines.add(Component.translatable("item.betterinventory.backpack.howto")
                        .withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            if (stack.getItem() instanceof StackUpgradeItem upgrade) {
                lines.add(Component.translatable(
                        "item.betterinventory.stack_upgrade.desc", upgrade.capacity())
                        .withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.betterinventory.stack_upgrade.scope")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }

    private static void addContentsLine(ItemStack stack, java.util.List<Component> lines) {
        int count = 0;
        StoredItems contents = stack.getOrDefault(
                ModComponents.BACKPACK_CONTENTS.get(), StoredItems.EMPTY);
        for (StoredItems.Entry entry : contents.entries()) {
            count += entry.stack().getCount();
        }
        if (count > 0) {
            lines.add(Component.translatable("item.betterinventory.backpack.contents", count)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}