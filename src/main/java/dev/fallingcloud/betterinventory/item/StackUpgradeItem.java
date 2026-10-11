package dev.fallingcloud.betterinventory.item;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class StackUpgradeItem extends Item {
    private final int tier;

    public StackUpgradeItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return this.tier;
    }

    public int capacity() {
        return BetterInventoryConfig.tierValue(this.tier);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.betterinventory.stack_upgrade.desc", this.capacity())
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.betterinventory.stack_upgrade.scope")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
    }
}
