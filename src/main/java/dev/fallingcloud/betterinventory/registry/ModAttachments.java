package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric binding for the per-player loadout.
 *
 * <p>Fabric's own data-attachment API covers what NeoForge's AttachmentType did, so no
 * third-party dependency is needed. Persistence is handled separately by
 * {@link dev.fallingcloud.betterinventory.logic.LoadoutStorage} because the attachment
 * API persists through Codecs while PlayerLoadout is NBT-based.
 */
public final class ModAttachments {
    public static final AttachmentType<PlayerLoadout> LOADOUT =
            AttachmentRegistry.createDefaulted(BetterInventory.id("loadout"), PlayerLoadout::new);

    public static PlayerLoadout get(Player player) {
        return player.getAttachedOrCreate(LOADOUT);
    }

    private ModAttachments() {}
}
