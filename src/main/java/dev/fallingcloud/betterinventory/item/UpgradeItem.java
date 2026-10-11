package dev.fallingcloud.betterinventory.item;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class UpgradeItem extends Item {
    private final Kind kind;
    private final int level;

    public UpgradeItem(Kind kind, int level, Properties properties) {
        super(properties);
        this.kind = kind;
        this.level = level;
    }

    public Kind kind() {
        return this.kind;
    }

    public int level() {
        return this.level;
    }

    public static int magnetRadius(int level) {
        int max = BetterInventoryConfig.MAGNET_RADIUS.get();
        return Math.max(2, Math.round(max * (0.35F + 0.1625F * (level - 1))));
    }

    public static int feedingThreshold(int level) {
        return Math.max(1, BetterInventoryConfig.FEEDING_THRESHOLD.get() - 3 * (5 - level));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip, TooltipFlag flag) {
        MutableComponent desc = switch (this.kind) {
            case MAGNET -> Component.translatable("item.betterinventory.upgrade_magnet.desc", magnetRadius(this.level));
            case FEEDING -> Component.translatable("item.betterinventory.upgrade_feeding.desc", feedingThreshold(this.level));
            default -> Component.translatable("item.betterinventory.upgrade_" + this.kind.id() + ".desc." + this.level);
        };
        tooltip.accept(desc.withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.betterinventory.upgrade.level", this.level, this.kind.maxLevel())
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.betterinventory.upgrade.howto").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
    }

    public enum Kind {
        MAGNET("magnet", 5),
        FEEDING("feeding", 5),
        REFILL("refill", 3),
        VOID("void", 3),
        PICKUP("pickup", 3);

        private final String id;
        private final int maxLevel;

        Kind(String id, int maxLevel) {
            this.id = id;
            this.maxLevel = maxLevel;
        }

        public String id() {
            return this.id;
        }

        public int maxLevel() {
            return this.maxLevel;
        }
    }
}
