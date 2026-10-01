package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric wiring for the server-side events CommonEvents used to subscribe to.
 *
 * <p>Where NeoForge exposes an event that Fabric has no equivalent for, the handler
 * is listed in {@code TODO} below and must be driven from a mixin instead - those are
 * the remaining pieces of work in this port.
 */
public final class FabricCommonEvents {
    private FabricCommonEvents() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FabricCommonEvents::onServerTick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                SyncService.syncNow(player);
            }
        });
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerLoadout loadout = ModAttachments.get(player);
            if (loadout == null) {
                continue;
            }
            UpgradeService.tick(player);
            RefillService.tick(player);
            SyncService.syncIfDirty(player);
        }
    }
}
