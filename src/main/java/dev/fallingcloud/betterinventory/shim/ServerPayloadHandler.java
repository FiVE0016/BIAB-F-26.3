package dev.fallingcloud.betterinventory.shim;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@FunctionalInterface
public interface ServerPayloadHandler<T extends CustomPacketPayload> {
    void accept(T payload, PayloadContext context);
}
