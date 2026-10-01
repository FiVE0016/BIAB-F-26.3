package dev.fallingcloud.betterinventory.shim;

import dev.fallingcloud.betterinventory.platform.ItemStore;

/**
 * Stand-in for NeoForge's ItemStackHandler.
 *
 * <p>The mod already ships {@link ItemStore}, a vanilla-only equivalent with the same
 * shape. This class exists so the ~36 existing {@code new ItemStackHandler(...)} and
 * {@code IItemHandler} references keep compiling unchanged on Fabric.
 */
public class ItemStackHandler extends ItemStore implements IItemHandlerModifiable {
    public ItemStackHandler(int size) {
        super(size);
    }
}
