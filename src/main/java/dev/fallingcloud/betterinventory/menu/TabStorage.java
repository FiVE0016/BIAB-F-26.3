package dev.fallingcloud.betterinventory.menu;

import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 tab storage: the 72-slot view behind the 3x9 + 5x9 areas of the
 * overhauled inventory. Index 0-26 is the live player main inventory; 27-71
 * routes to the personal gather hub on the default tab and into the active
 * backpack tab's contents component otherwise (or straight to the held-pages
 * handler when the config runs pack-free).
 */
public class TabStorage implements Container {
    public static final int SIZE = 72;
    public static final int GATHER_START = 27;

    private final Player player;
    private final PlayerLoadout loadout;
    @Nullable
    private ComponentBackedHandler cache;

    public TabStorage(Player player, PlayerLoadout loadout) {
        this.player = player;
        this.loadout = loadout;
    }

    public int tab() {
        int tab = loadout.activeTab;
        return loadout.tabAvailable(tab) ? tab : 0;
    }

    /** The handler behind the active backpack tab, or the held pages pack-free. */
    @Nullable
    public BigStackHandler packHandler() {
        if (loadout.packFree) {
            cache = null;
            return loadout.heldPages;
        }
        ItemStack pack = loadout.backpack();
        if (pack.isEmpty()) {
            cache = null;
            return null;
        }
        if (cache == null || cache.host() != pack) {
            cache = new ComponentBackedHandler(pack, ModComponents.BACKPACK_CONTENTS.get(), PlayerLoadout.CONTENTS_SIZE, loadout::storageCap);
            cache.setChangeListener(() -> loadout.dirty = true);
        }
        return cache;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < GATHER_START) {
            // The main grid IS the live player inventory.
            return player.getInventory().getItem(9 + slot);
        }
        int tab = tab();
        if (tab == 0) {
            return loadout.gather.getStackInSlot(slot - GATHER_START);
        }
        BigStackHandler handler = packHandler();
        return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(PlayerLoadout.pageBase(tab) + slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        } else {
            setItem(slot, stack);
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
        if (slot < GATHER_START) {
            player.getInventory().setItem(9 + slot, stack);
            player.getInventory().setChanged();
            return;
        }
        int tab = tab();
        if (tab == 0) {
            loadout.gather.setStackInSlot(slot - GATHER_START, stack);
            return;
        }
        BigStackHandler handler = packHandler();
        if (handler != null) {
            handler.setStackInSlot(PlayerLoadout.pageBase(tab) + slot, stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return Math.max(64, loadout.storageCap());
    }

    @Override
    public void setChanged() {
        player.getInventory().setChanged();
        loadout.dirty = true;
        if (tab() != 0 && packHandler() instanceof ComponentBackedHandler backed) {
            backed.writeBack();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    /** True when the given view index sits in the real (vanilla) player inventory. */
    public boolean isVanillaRegion(int index) {
        return tab() == 0 && index < GATHER_START;
    }
}
