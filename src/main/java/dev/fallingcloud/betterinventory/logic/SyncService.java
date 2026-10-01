package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;

/** Mirrors the whole loadout attachment to the owning client (HUD + screens). */
public final class SyncService {
    private SyncService() {}

    public static void syncNow(ServerPlayer player) {
        PlayerLoadout loadout = ModAttachments.get(player);
        loadout.dirty = false;
        PacketDistributor.sendToPlayer(player, new BetterInventoryPayloads.SyncLoadout(loadout.serializeNBT(player.registryAccess())));
    }

    public static void syncIfDirty(ServerPlayer player) {
        if (ModAttachments.get(player).dirty) {
            syncNow(player);
        }
    }

    public static void applyClientSync(Player clientPlayer, CompoundTag data) {
        PlayerLoadout loadout = ModAttachments.get(clientPlayer);
        loadout.deserializeNBT(clientPlayer.registryAccess(), data);
        loadout.dirty = false;
    }
}
