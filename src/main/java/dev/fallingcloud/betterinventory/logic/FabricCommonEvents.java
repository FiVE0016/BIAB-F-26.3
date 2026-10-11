package dev.fallingcloud.betterinventory.logic;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric wiring for the 1.4.1 CommonEvents set: the per-tick pipeline is
 * {@link PlayerHooks#playerTick}, plus login/logout/respawn/clone handlers.
 *
 * <p>26.3 adaptation: the dimension-change event was renamed
 * ServerEntityWorldChangeEvents → {@link ServerEntityLevelChangeEvents}
 * (AFTER_PLAYER_CHANGE_LEVEL), so the 1.4.1 dimension sync returns here. The
 * AFTER_DEATH sync below is kept from the previous port iteration - it stays
 * harmless once the death mixin lands.
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
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PlayerHooks.onLogin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PlayerHooks.onLogout(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                PlayerHooks.onRespawnOrDimensionChange(newPlayer));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
                PlayerHooks.onRespawnOrDimensionChange(player));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                PlayerHooks.onClone(oldPlayer, newPlayer, !alive));
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerHooks.playerTick(player);
        }
    }
}
