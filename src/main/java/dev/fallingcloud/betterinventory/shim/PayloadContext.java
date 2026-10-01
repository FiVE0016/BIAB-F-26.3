package dev.fallingcloud.betterinventory.shim;

import net.minecraft.world.entity.player.Player;

/** Minimal stand-in for NeoForge's IPayloadContext. */
public interface PayloadContext {
    Player player();
}
