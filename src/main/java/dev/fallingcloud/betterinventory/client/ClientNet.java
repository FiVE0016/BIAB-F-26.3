package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** 1.4.1 client networking: registers the loadout sync receiver and the sender. */
public final class ClientNet {
    private ClientNet() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BetterInventoryPayloads.SyncLoadout.TYPE,
                (payload, context) -> ClientSync.apply(payload));
        Net.setClientSender(ClientPlayNetworking::send);
    }
}
