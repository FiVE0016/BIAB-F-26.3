package dev.fallingcloud.betterinventory.inv;

import org.jetbrains.annotations.Nullable;

public interface LoadoutHolder {
    PlayerLoadout betterinventory$loadout();

    @Nullable
    PlayerLoadout betterinventory$existingLoadout();

    void betterinventory$setLoadout(PlayerLoadout var1);
}
