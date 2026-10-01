package dev.fallingcloud.betterinventory.shim;

import net.minecraft.world.item.ItemStack;

/** Stand-in for NeoForge's IItemHandler. */
public interface IItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);
}
