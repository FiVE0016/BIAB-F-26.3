package dev.fallingcloud.betterinventory.shim;

import dev.fallingcloud.betterinventory.platform.ItemStore;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Stand-in for NeoForge's SlotItemHandler: a vanilla {@link Slot} backed by an
 * {@link ItemStore} instead of a {@code Container}.
 */
public class SlotItemHandler extends Slot {
    private final ItemStore store;

    public SlotItemHandler(ItemStore store, int index, int x, int y) {
        super(new StoreContainer(store), index, x, y);
        this.store = store;
    }

    private Identifier noItemIcon;

    /** 26.3: replaces Slot#setBackground(atlas, sprite); see IconedSlot. */
    public void setBackground(Identifier sprite) {
        this.noItemIcon = sprite;
    }

    @Override
    public Identifier getNoItemIcon() {
        return noItemIcon;
    }

    public ItemStore handler() {
        return store;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return store.isValid(getContainerSlot(), stack);
    }

    @Override
    public ItemStack getItem() {
        return store.get(getContainerSlot());
    }

    @Override
    public void set(ItemStack stack) {
        store.set(getContainerSlot(), stack);
    }

    @Override
    public void setChanged() {
        store.onChanged();
    }

    @Override
    public int getMaxStackSize() {
        return store.getSlotLimit(getContainerSlot());
    }
}
