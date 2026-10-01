package dev.fallingcloud.betterinventory.platform;

import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The seam between shared game logic and the mod loader.
 *
 * <p>Everything in {@code common/} is compiled for every target, so it may only touch
 * vanilla Minecraft. The handful of things that genuinely differ per loader live
 * behind this interface, and each loader module supplies one implementation via
 * {@link Services} (ServiceLoader).
 *
 * <p>The divergences that actually matter for this mod, and why each is here:
 * <ul>
 *   <li><b>Per-player data</b> — NeoForge data attachments vs Fabric Cardinal
 *       Components. There is no shared API.</li>
 *   <li><b>Networking</b> — 1.20.5+ payloads vs 1.20.1 SimpleChannel vs Fabric's
 *       own networking.</li>
 *   <li><b>Config</b> — ModConfigSpec is Forge/NeoForge only.</li>
 *   <li><b>Curios vs Trinkets</b> — different mods entirely, same idea.</li>
 * </ul>
 *
 * <p>Deliberately NOT here: item handlers and stack storage. Those looked
 * loader-specific only because the code used NeoForge's {@code ItemStackHandler};
 * a vanilla-only equivalent works everywhere and stays in common.
 */
public interface Platform {
    /** Loader id, e.g. "neoforge" / "fabric" / "forge". For logs and compat checks. */
    String loaderName();

    boolean isModLoaded(String modId);

    /** True on a physical client; some logic must not touch client-only classes. */
    boolean isClient();

    Path configDir();

    /** Per-player mod data, however this loader attaches it. */
    PlayerData playerData(Player player);

    /** Sends a payload the loader has registered. Payload types live in common. */
    void sendToServer(Object payload);

    void sendToPlayer(ServerPlayer player, Object payload);

    /** Equipped accessory slots: Curios on NeoForge/Forge, Trinkets on Fabric. */
    AccessorySlots accessories(Player player);
}
