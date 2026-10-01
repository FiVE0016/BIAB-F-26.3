package dev.fallingcloud.betterinventory.shim;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Stand-in for NeoForge's PayloadRegistrar.
 *
 * <p>Registers the codec on both sides and wires the handler onto the Fabric
 * networking API, so the existing {@code playToServer} / {@code playToClient} calls
 * and their lambdas compile unchanged.
 */
public final class PayloadRegistrar {
    public <T extends CustomPacketPayload> void playToServer(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            ServerPayloadHandler<T> handler) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.accept(payload, context::player));
    }

    public <T extends CustomPacketPayload> void playToClient(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            ClientPayloadHandler<T> handler) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        ClientPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.accept(payload, context::player));
    }
}
