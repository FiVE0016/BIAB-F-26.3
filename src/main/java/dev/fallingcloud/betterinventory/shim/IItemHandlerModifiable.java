package dev.fallingcloud.betterinventory.shim;

import net.minecraft.world.item.ItemStack;

/** Stand-in for NeoForge's IItemHandlerModifiable. */
public interface IItemHandlerModifiable extends IItemHandler {
    void setStackInSlot(int slot, ItemStack stack);
}
