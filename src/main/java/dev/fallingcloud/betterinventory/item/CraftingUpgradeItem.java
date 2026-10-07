package dev.fallingcloud.betterinventory.item;

import net.minecraft.world.item.Item;

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

}
