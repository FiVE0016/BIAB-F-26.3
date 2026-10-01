package dev.fallingcloud.betterinventory.menu.slots;

import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.platform.ItemStore;
import dev.fallingcloud.betterinventory.shim.SlotItemHandler;

/**
 * A big-stack storage slot over an item handler, honoring per-slot item locks.
 * Used by the backpack GUI (slot index == storage index 0-44).
 */
public class LockedHandlerSlot extends SlotItemHandler {
    private final Supplier<TabSettings> settings;
    private final IntSupplier cap;
    private final Supplier<ItemStack> host;

    public LockedHandlerSlot(ItemStore handler, int index, int x, int y,
                             Supplier<TabSettings> settings, IntSupplier cap, Supplier<ItemStack> host) {
        super(handler, index, x, y);
        this.settings = settings;
        this.cap = cap;
        this.host = host;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack == host.get()) {
            return false; // never allow a backpack inside itself
        }
        return settings.get().allows(index, stack) && super.mayPlace(stack);
    }

    @Override
    public int getMaxStackSize() {
        return Math.max(1, cap.getAsInt());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return BigStackHandler.capFor(stack, cap.getAsInt());
    }
}
