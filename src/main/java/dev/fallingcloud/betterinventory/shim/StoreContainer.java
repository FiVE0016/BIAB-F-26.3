package dev.fallingcloud.betterinventory.shim;

import dev.fallingcloud.betterinventory.platform.ItemStore;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

/**
 * Minimal {@link Container} view over an {@link ItemStore}, needed because a vanilla
 * {@code Slot} requires a {@code Container} in its constructor.
 */
public class StoreContainer extends SimpleContainer {
    private final ItemStore store;

    public StoreContainer(ItemStore store) {
        super(store.size());
        this.store = store;
    }

    @Override
    public int getContainerSize() {
        return store.size();
    }

    @Override
    public ItemStack getItem(int slot) {
        return store.get(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        store.set(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return store.isValid(slot, stack);
    }
}
