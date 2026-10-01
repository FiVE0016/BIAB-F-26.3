package dev.fallingcloud.betterinventory.menu.slots;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A four-slot view of the offhand carousel. The active index routes to the real
 * offhand slot (which mirrors the stored item); the rest route to the store.
 * This keeps the GUI dupe-safe: taking the active item empties the real offhand,
 * and the per-tick mirror logic reconciles the store.
 */
public class OffhandRouteContainer implements Container {
    public static final int OFFHAND_INV_INDEX = 40;

    private final Inventory inventory;
    private final PlayerLoadout loadout;

    public OffhandRouteContainer(Inventory inventory, PlayerLoadout loadout) {
        this.inventory = inventory;
        this.loadout = loadout;
    }

    private boolean isActive(int slot) {
        return slot == loadout.activeOffhand;
    }

    @Override
    public int getContainerSize() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < 4; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isActive(slot) ? inventory.getItem(OFFHAND_INV_INDEX) : loadout.offhandStore.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (isActive(slot)) {
            ItemStack result = inventory.removeItem(OFFHAND_INV_INDEX, amount);
            syncActiveFromInventory();
            return result;
        }
        ItemStack stack = loadout.offhandStore.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            loadout.offhandStore.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            loadout.offhandStore.setStackInSlot(slot, stack);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isActive(slot)) {
            inventory.setItem(OFFHAND_INV_INDEX, stack);
            syncActiveFromInventory();
        } else {
            loadout.offhandStore.setStackInSlot(slot, stack);
        }
    }

    /** Keeps the store's active entry identical to the real offhand instance. */
    private void syncActiveFromInventory() {
        loadout.offhandStore.setStackInSlot(loadout.activeOffhand, inventory.getItem(OFFHAND_INV_INDEX));
    }

    @Override
    public void setChanged() {
        syncActiveFromInventory();
        inventory.setChanged();
        loadout.dirty = true;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < 4; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }
}
