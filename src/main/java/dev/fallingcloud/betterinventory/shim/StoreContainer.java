package dev.fallingcloud.betterinventory.shim;

import dev.fallingcloud.betterinventory.platform.ItemStore;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

/**
 * Minimal {@link Container} view over an {@link ItemStore}, needed because a vanilla
 * {@code Slot} requires a {@code Container} in its constructor.
 */
public class StoreContainer extends SimpleContainer {
    private final ItemStore store;

    public StoreContainer(ItemStore store) {
        super(store.size());
        this.store = store;
    }

    @Override
    public int getContainerSize() {
        return store.size();
    }

    @Override
    public ItemStack getItem(int slot) {
        return store.get(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        store.set(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return store.isValid(slot, stack);
    }

    /**
     * 26.3 的 {@code Slot#remove(int)} 改为委托 {@code Container#removeItem(int, int)}
     * （不再走 getItem/setItem）。若沿用 {@link SimpleContainer} 的默认实现，它只会操作
     * SimpleContainer 自己的内部列表——本模组的 {@link ItemStore} 从不写那个列表，
     * 于是工具架/升级槽等槽位"左键拿不出东西"（背包格用的是真正的 Container 所以正常）。
     */
    @Override
    public ItemStack removeItem(int slot, int amount) {
        return store.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return store.extractItem(slot, Integer.MAX_VALUE, false);
    }

    @Override
    public void setChanged() {
        store.onChanged();
    }
}
