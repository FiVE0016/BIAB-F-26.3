package dev.fallingcloud.betterinventory.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class TravelersKnotItem extends Item {
    public TravelersKnotItem(Properties properties) {
        super(properties);
    }

    /**
     * 26.3 marks the five-arg appendHoverText as deprecated and offers no
     * non-deprecated text-tooltip hook (getTooltipImage only covers images),
     * so the 1.4.1 behaviour is kept verbatim with the warning suppressed.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.betterinventory.travelers_knot.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.betterinventory.travelers_knot.source")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
    }
}
