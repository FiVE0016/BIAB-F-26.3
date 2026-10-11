package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Mirrors the whole loadout attachment to the owning client (HUD + screens). */
public final class SyncService {
    private SyncService() {}

    public static void syncNow(ServerPlayer player) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        loadout.dirty = false;
        Net.sendToPlayer(player, new BetterInventoryPayloads.SyncLoadout(loadout.serializeNBT(player.registryAccess())));
    }

    public static void syncIfDirty(ServerPlayer player) {
        if (PlayerLoadout.of(player).dirty) {
            syncNow(player);
        }
    }

    public static void applyClientSync(Player clientPlayer, CompoundTag data) {
        PlayerLoadout loadout = PlayerLoadout.of(clientPlayer);
        loadout.deserializeNBT(clientPlayer.registryAccess(), data);
        loadout.dirty = false;
    }
}
