package dev.fallingcloud.betterinventory.net;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 1.4.1 Fabric networking layer: client sender indirection + payload registrar.
 * Kept as its own class (instead of routing through the shim) because the 1.4.1
 * codebase calls {@code Net.sendToServer / sendToPlayer / Net.Registrar} directly.
 */
public final class Net {
    private static Consumer<CustomPacketPayload> clientSender = payload -> {
        throw new IllegalStateException("No client networking on this side for " + payload.type().id());
    };

    private Net() {}

    public static void setClientSender(Consumer<CustomPacketPayload> sender) {
        clientSender = sender;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        clientSender.accept(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public record Context(Player player) {}

    @FunctionalInterface
    public interface Handler<T extends CustomPacketPayload> {
        void handle(T payload, Net.Context context);
    }

    public static final class Registrar {
        public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Net.Handler<T> handler) {
            PayloadTypeRegistry.serverboundPlay().register(type, codec);
            ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                    handler.handle(payload, new Net.Context(context.player())));
        }

        public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            PayloadTypeRegistry.clientboundPlay().register(type, codec);
        }
    }
}
