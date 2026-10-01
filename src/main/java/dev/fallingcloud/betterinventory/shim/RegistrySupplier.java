package dev.fallingcloud.betterinventory.shim;

import java.util.function.Supplier;

/** Immediate-registration stand-in for NeoForge's DeferredHolder. */
public class RegistrySupplier<I> implements Supplier<I> {
    private final I value;

    protected RegistrySupplier(I value) {
        this.value = value;
    }

    @Override
    public I get() {
        return value;
    }
}
