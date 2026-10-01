package dev.fallingcloud.betterinventory.menu;

import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.logic.TabService;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The 45-slot view behind the 3x9 + 2x9 areas of the overhauled inventory.
 * Index 0-26 = main grid, 27-44 = gather hub. The default tab routes to the real
 * player inventory + the personal gather hub; backpack tabs route into the
 * backpack's contents component.
 */
public class TabStorage implements Container {
    public static final int SIZE = 45;
    public static final int GATHER_START = 27;

    private final Player player;
    private final PlayerLoadout loadout;
    private final ComponentBackedHandler[] cache = new ComponentBackedHandler[5];

    public TabStorage(Player player, PlayerLoadout loadout) {
        this.player = player;
        this.loadout = loadout;
    }

    public int tab() {
        int tab = loadout.activeTab;
        return loadout.tabAvailable(tab) ? tab : 0;
    }

    @Nullable
    public ComponentBackedHandler packHandler(int tab) {
        ItemStack pack = loadout.backpack(tab);
        if (pack.isEmpty()) {
            cache[tab] = null;
            return null;
        }
        ComponentBackedHandler handler = cache[tab];
        if (handler == null || handler.host() != pack) {
            handler = new ComponentBackedHandler(pack, ModComponents.BACKPACK_CONTENTS.get(), SIZE, loadout::storageCap);
            handler.setChangeListener(() -> loadout.dirty = true);
            cache[tab] = handler;
        }
        return handler;
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
        // The main grid IS the live player inventory - TabService swaps a tab's
        // contents into it on switch, so there is only ever one place to look.
        if (slot < GATHER_START) {
            return player.getInventory().getNonEquipmentItems().get(TabService.INV_MAIN_START + slot);
        }
        int tab = tab();
        if (tab == 0) {
            return loadout.gather.getStackInSlot(slot - GATHER_START);
        }
        ComponentBackedHandler handler = packHandler(tab);
        return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(slot);
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
            player.getInventory().getNonEquipmentItems().set(TabService.INV_MAIN_START + slot, stack);
            player.getInventory().setChanged();
            return;
        }
        int tab = tab();
        if (tab == 0) {
            loadout.gather.setStackInSlot(slot - GATHER_START, stack);
            return;
        }
        ComponentBackedHandler handler = packHandler(tab);
        if (handler != null) {
            handler.setStackInSlot(slot, stack);
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
        int tab = tab();
        if (tab != 0) {
            ComponentBackedHandler handler = packHandler(tab);
            if (handler != null) {
                handler.writeBack();
            }
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
