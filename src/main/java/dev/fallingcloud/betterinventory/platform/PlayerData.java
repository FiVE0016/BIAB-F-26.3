package dev.fallingcloud.betterinventory.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Per-player mod data, as the loader stores it.
 *
 * <p>NeoForge has data attachments; Fabric needs Cardinal Components or a custom
 * persistent-state hook. Both can express "a CompoundTag that travels with the
 * player and survives death/dimension change", which is all this mod needs — the
 * actual loadout object is deserialised from it in common code.
 */
public interface PlayerData {
    CompoundTag read(HolderLookup.Provider registries);

    void write(CompoundTag tag, HolderLookup.Provider registries);

    /** Marks the data dirty so the loader re-syncs it to the owning client. */
    void setDirty();
}
