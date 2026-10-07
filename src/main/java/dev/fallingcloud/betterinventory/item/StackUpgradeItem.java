package dev.fallingcloud.betterinventory.item;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import net.minecraft.world.item.Item;

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

}
