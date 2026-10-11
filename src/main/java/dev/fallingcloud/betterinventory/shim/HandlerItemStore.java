package dev.fallingcloud.betterinventory.shim;

import dev.fallingcloud.betterinventory.platform.ItemStore;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ItemStore} view over a plain {@link IItemHandlerModifiable}, so the
 * shim's {@link SlotItemHandler} (which takes an ItemStore) can also back slots
 * with handlers that do not extend ItemStore - e.g. PlayerLoadout's nested
 * PackUpgradeHandler.
 */
public class HandlerItemStore extends ItemStore {
    private final IItemHandlerModifiable handler;

    public HandlerItemStore(IItemHandlerModifiable handler) {
        super(handler.getSlots());
        this.handler = handler;
    }

    @Override
    public ItemStack get(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    public void set(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return handler.isItemValid(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return handler.getSlotLimit(slot);
    }

    @Override
    public void onChanged() {
        // The delegate persists on write; there is no separate change signal.
    }
}
