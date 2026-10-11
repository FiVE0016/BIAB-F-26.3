package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.logic.SyncService;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import net.minecraft.client.Minecraft;

/** 1.4.1 client-side loadout sync application. */
public final class ClientSync {
    private ClientSync() {}

    public static void apply(BetterInventoryPayloads.SyncLoadout payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            SyncService.applyClientSync(minecraft.player, payload.data());
        }
    }
}
