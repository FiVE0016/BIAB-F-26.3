package dev.fallingcloud.betterinventory.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;

/**
 * Installed in the personal upgrade slot beside the stack upgrade, and grows the
 * crafting tab's grid. Tiers share the one slot, so a higher tier simply replaces
 * a lower one.
 *
 * <p>Ungated the grid is 2x2; these take it to 2x3 and then 3x3.
 */
public class CraftingUpgradeItem extends Item {
    private final int cols;
    private final int rows;

    public CraftingUpgradeItem(int cols, int rows, Properties properties) {
        super(properties);
        this.cols = cols;
        this.rows = rows;
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.betterinventory.crafting_upgrade.desc", cols + "x" + rows)
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.betterinventory.crafting_upgrade.howto")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, net.minecraft.world.item.component.TooltipDisplay.DEFAULT, tooltip::accept, flag);
    }
}
