package dev.fallingcloud.betterinventory.shim;

/** Stand-in for NeoForge's RegisterPayloadHandlersEvent. */
public final class RegisterPayloadHandlersEvent {
    public PayloadRegistrar registrar(String version) {
        return new PayloadRegistrar();
    }
}
