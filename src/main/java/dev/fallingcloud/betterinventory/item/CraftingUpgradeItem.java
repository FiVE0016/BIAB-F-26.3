package dev.fallingcloud.betterinventory.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class CraftingUpgradeItem extends Item {
    private final int cols;
    private final int rows;
    private final boolean furnace;

    public CraftingUpgradeItem(int cols, int rows, Properties properties) {
        this(cols, rows, false, properties);
    }

    public CraftingUpgradeItem(int cols, int rows, boolean furnace, Properties properties) {
        super(properties);
        this.cols = cols;
        this.rows = rows;
        this.furnace = furnace;
    }

    public boolean furnace() {
        return this.furnace;
    }

    public int cols() {
        return this.cols;
    }

    public int rows() {
        return this.rows;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.betterinventory.crafting_upgrade.desc", this.cols + "x" + this.rows)
                .withStyle(ChatFormatting.DARK_PURPLE));
        if (this.furnace) {
            tooltip.accept(Component.translatable("item.betterinventory.crafting_upgrade.furnace")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        tooltip.accept(Component.translatable("item.betterinventory.crafting_upgrade.howto")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
    }
}
