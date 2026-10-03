package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Persists the per-player loadout.
 *
 * <p>Fabric's attachment API persists through Codecs, while PlayerLoadout is
 * NBT-based, so saving is done here instead: one NBT file per player UUID under the
 * world's data directory, read on join and written on disconnect and server stop.
 */
public final class LoadoutStorage {
    private static final Map<UUID, PlayerLoadout> LOADED = new HashMap<>();
    private static Path base;

    private LoadoutStorage() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> base = dir(server));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            CompoundTag tag = read(player.getUUID());
            if (tag != null) {
                ModAttachments.get(player).deserializeNBT(player.registryAccess(), tag);
            }
            LOADED.put(player.getUUID(), ModAttachments.get(player));
            SyncService.syncNow(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            save(player);
            LOADED.remove(player.getUUID());
        });

        // Copy the loadout onto the fresh player instance (respawn / dimension change).
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            CompoundTag tag = ModAttachments.get(oldPlayer)
                    .serializeNBT(oldPlayer.registryAccess());
            ModAttachments.get(newPlayer).deserializeNBT(newPlayer.registryAccess(), tag);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                save(player);
            }
            LOADED.clear();
        });
    }

    private static Path dir(MinecraftServer server) {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data").resolve("betterinventory");
    }

    private static void save(ServerPlayer player) {
        if (base == null) {
            return;
        }
        PlayerLoadout loadout = ModAttachments.get(player);
        if (loadout == null) {
            return;
        }
        try {
            Files.createDirectories(base);
            NbtIo.writeCompressed(loadout.serializeNBT(player.registryAccess()),
                    base.resolve(player.getUUID() + ".dat"));
        } catch (IOException e) {
            BetterInventoryLog.warn("Failed to save loadout for " + player.getUUID(), e);
        }
    }

    private static CompoundTag read(UUID uuid) {
        if (base == null) {
            return null;
        }
        Path file = base.resolve(uuid + ".dat");
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return NbtIo.readCompressed(file, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            BetterInventoryLog.warn("Failed to read loadout for " + uuid, e);
            return null;
        }
    }
}
