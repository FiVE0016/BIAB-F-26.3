package dev.fallingcloud.betterinventory.shim;

import net.minecraft.core.HolderLookup;

/** Stand-in for NeoForge's INBTSerializable. */
public interface INBTSerializable<T> {
    T serializeNBT(HolderLookup.Provider registries);

    void deserializeNBT(HolderLookup.Provider registries, T tag);
}
