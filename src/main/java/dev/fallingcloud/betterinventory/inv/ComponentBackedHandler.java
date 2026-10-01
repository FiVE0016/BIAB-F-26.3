package dev.fallingcloud.betterinventory.inv;

import dev.fallingcloud.betterinventory.data.StoredItems;
import java.util.function.IntSupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * A handler view over a StoredItems data component on a specific ItemStack (a backpack).
 * Every mutation is written back to the component immediately, so the item on the
 * player is always the source of truth.
 */
public class ComponentBackedHandler extends BigStackHandler {
    private final ItemStack host;
    private final DataComponentType<StoredItems> component;
    private Runnable changeListener = () -> {};
    @org.jetbrains.annotations.Nullable
    private StoredItems lastSeen;

    public ComponentBackedHandler(ItemStack host, DataComponentType<StoredItems> component, int size, IntSupplier capacity) {
        super(size, capacity);
        this.host = host;
        this.component = component;
        refresh();
    }

    public ItemStack host() {
        return host;
    }

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    /** Reload from the component (the host stack's data changed externally). */
    public void refresh() {
        this.lastSeen = host.getOrDefault(component, StoredItems.EMPTY);
        this.stacks = lastSeen.unpack(getSlots());
    }

    /**
     * Several handlers can view the same backpack (an open menu plus the gather
     * hub / refill / feeding services). Each write publishes a fresh StoredItems
     * instance to the component, so an identity check spots external edits and
     * re-reads before this view acts - otherwise a later write from a stale view
     * would erase them.
     */
    private void validate() {
        StoredItems current = host.getOrDefault(component, StoredItems.EMPTY);
        if (current != lastSeen) {
            refresh();
        }
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validate();
        return super.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validate();
        super.setStackInSlot(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validate();
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validate();
        return super.extractItem(slot, amount, simulate);
    }

    public void writeBack() {
        StoredItems stored = StoredItems.of(stacks);
        this.lastSeen = stored.isEmpty() ? StoredItems.EMPTY : stored;
        if (stored.isEmpty()) {
            host.remove(component);
        } else {
            host.set(component, stored);
        }
        changeListener.run();
    }

    @Override
    protected void onContentsChanged(int slot) {
        writeBack();
    }
}
