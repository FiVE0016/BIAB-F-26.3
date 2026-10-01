package dev.fallingcloud.betterinventory.item;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;

/**
 * Raises the max stack size inside BetterInventory storage (gather hub + backpack tabs) when
 * placed in the personal stack-upgrade slot. Seven tiers; values depend on the
 * configured stack mode.
 */
public class StackUpgradeItem extends Item {
    private final int tier;

    public StackUpgradeItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    public int capacity() {
        return BetterInventoryConfig.tierValue(tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.betterinventory.stack_upgrade.desc", capacity()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.betterinventory.stack_upgrade.scope").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
