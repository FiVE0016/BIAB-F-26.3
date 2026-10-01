package dev.fallingcloud.betterinventory.shim;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Stand-in for NeoForge's DeferredRegister.
 *
 * <p>Fabric registers objects immediately, so {@link #register(String, Supplier)}
 * performs the registration right away and returns a holder around the instance.
 * {@link #register(Object)} is a no-op kept only so existing calls compile.
 */
public class DeferredRegister<T> {
    protected final ResourceKey<Registry<T>> key;
    protected final String namespace;

    protected DeferredRegister(ResourceKey<Registry<T>> key, String namespace) {
        this.key = key;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<Registry<T>> key, String namespace) {
        return new DeferredRegister<>(key, namespace);
    }

    public static Items createItems(String namespace) {
        return new Items(namespace);
    }

    public static Blocks createBlocks(String namespace) {
        return new Blocks(namespace);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <I extends T> I doRegister(String name, I value) {
        // Resolve by comparing registry keys - ResourceKey#location() is gone in 26.3.
        Registry<T> registry = null;
        for (Registry<?> candidate : BuiltInRegistries.REGISTRY.stream().toList()) {
            if (key.equals(candidate.key())) {
                registry = (Registry<T>) candidate;
                break;
            }
        }
        if (registry == null) {
            throw new IllegalStateException("No built-in registry for " + key);
        }
        ResourceKey<T> id = ResourceKey.create(key, Identifier.fromNamespaceAndPath(namespace, name));
        return (I) Registry.register((Registry) registry, id, value);
    }

    public <I extends T> RegistrySupplier<I> register(String name, Supplier<I> supplier) {
        return new RegistrySupplier<>(doRegister(name, supplier.get()));
    }

    /** No-op on Fabric - registration already happened. */
    public void register(Object eventBus) {}

    public static final class Items extends DeferredRegister<Item> {
        private Items(String namespace) {
            super(Registries.ITEM, namespace);
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Supplier<I> supplier) {
            return new DeferredItem<>(doRegister(name, supplier.get()));
        }
    }

    public static final class Blocks extends DeferredRegister<Block> {
        private Blocks(String namespace) {
            super(Registries.BLOCK, namespace);
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Supplier<B> supplier) {
            return new DeferredBlock<>(doRegister(name, supplier.get()));
        }
    }
}
