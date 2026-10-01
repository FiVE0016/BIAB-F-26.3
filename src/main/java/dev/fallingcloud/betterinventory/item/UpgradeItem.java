package dev.fallingcloud.betterinventory.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;

/** A backpack ability upgrade. Installed via the tab settings screen. */
public class UpgradeItem extends Item {
    public enum Kind {
        MAGNET("magnet"),
        FEEDING("feeding"),
        REFILL("refill"),
        VOID("void"),
        PICKUP("pickup");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final Kind kind;

    public UpgradeItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.betterinventory.upgrade_" + kind.id() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.betterinventory.upgrade.howto").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, net.minecraft.world.item.component.TooltipDisplay.DEFAULT, tooltip::accept, flag);
    }
}
