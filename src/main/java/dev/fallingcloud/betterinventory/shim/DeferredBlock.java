package dev.fallingcloud.betterinventory.shim;

import net.minecraft.world.level.block.Block;

/** Stand-in for NeoForge's DeferredBlock. */
public class DeferredBlock<B extends Block> extends RegistrySupplier<B> {
    protected DeferredBlock(B value) {
        super(value);
    }
}
