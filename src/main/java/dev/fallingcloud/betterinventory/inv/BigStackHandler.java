package dev.fallingcloud.betterinventory.inv;

import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import java.util.function.IntSupplier;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;

/**
 * An ItemStackHandler whose per-slot capacity is driven by the owner's stack upgrade.
 * Unstackable items still stack to 1; everything else stacks to the upgrade cap
 * (never below the item's own maximum).
 */
public class BigStackHandler extends ItemStackHandler {
    private final IntSupplier capacity;

    public BigStackHandler(int size, IntSupplier capacity) {
        super(size);
        this.capacity = capacity;
    }

    /**
     * Per-item ceiling under the given upgrade. Scaled by the item's own natural
     * size so a 16-stack item does not jump to a 64-stack item's ceiling, and
     * unstackable items stay at 1.
     */
    public static int capFor(ItemStack stack, int upgradeCap) {
        if (stack.isEmpty()) {
            return upgradeCap;
        }
        return dev.fallingcloud.betterinventory.logic.StackUpgradeService.scaledCap(stack, upgradeCap);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Math.max(1, capacity.getAsInt());
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return capFor(stack, getSlotLimit(slot));
    }

    /** The default capacity when no stack upgrade is installed. */
    public static final IntSupplier VANILLA = () -> 64;

    public static IntSupplier fromUpgrade(ItemStackHandler personal) {
        return () -> {
            ItemStack stack = personal.getStackInSlot(0);
            if (stack.getItem() instanceof StackUpgradeItem upgrade) {
                return upgrade.capacity();
            }
            return 64;
        };
    }
}
