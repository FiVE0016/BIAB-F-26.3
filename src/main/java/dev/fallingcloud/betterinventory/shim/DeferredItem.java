package dev.fallingcloud.betterinventory.shim;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/** Stand-in for NeoForge's DeferredItem: a registered item that is also an ItemLike. */
public class DeferredItem<I extends Item> extends RegistrySupplier<I> implements ItemLike {
    protected DeferredItem(I value) {
        super(value);
    }

    @Override
    public Item asItem() {
        return get();
    }
}
