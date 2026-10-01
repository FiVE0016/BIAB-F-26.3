package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Owns the rule that the ACTIVE TAB IS THE PLAYER'S INVENTORY.
 *
 * <p>Switching tabs physically swaps the 27 main inventory slots with the tab's
 * store, rather than only changing what the BetterInventory screen draws. That is what makes
 * a chest (or any other mod's screen, or anything else reading the inventory) show
 * the tab you actually have open, and makes edits made there flow back into the tab.
 *
 * <p><b>Invariant:</b> exactly one copy of an item exists. Loading a tab CLEARS its
 * store, so items live in the live inventory while that tab is active and are written
 * back when you switch away. A crash mid-tab therefore loses nothing: the items are
 * in the player inventory, which saves with the player, and activeTab persists
 * alongside it.
 */
public final class TabService {
    public static final int MAIN_SIZE = 27;
    /** Player inventory index of the first main (non-hotbar) slot. */
    public static final int INV_MAIN_START = 9;

    private TabService() {}

    /** Switches tabs, writing the current one back first. Returns false if unavailable. */
    public static boolean switchTo(Player player, int requested) {
        PlayerLoadout loadout = ModAttachments.get(player);
        int tab = Math.floorMod(requested, PlayerLoadout.TAB_COUNT);
        if (tab == loadout.activeTab) {
            return true;
        }
        if (!loadout.tabAvailable(tab)) {
            return false;
        }
        stow(player, loadout, loadout.activeTab);
        loadout.activeTab = tab;
        draw(player, loadout, tab);
        loadout.dirty = true;
        return true;
    }

    /** Returns to the default tab, e.g. before death drops. */
    public static void resetToDefault(Player player) {
        switchTo(player, 0);
    }

    /**
     * Recovers if the backpack owning the live inventory disappeared.
     *
     * <p>Nothing is stowed in that case (there is no store left to stow into), so the
     * live items are simply kept as the player's own and the default tab's stash is
     * handed back, overflowing to the ground rather than being overwritten. Removing
     * the open tab's backpack is blocked in the GUI and death resets first, so this
     * is a backstop for commands and other mods.
     */
    public static void recoverIfOrphaned(Player player) {
        PlayerLoadout loadout = ModAttachments.get(player);
        if (loadout.activeTab == 0 || !loadout.backpack(loadout.activeTab).isEmpty()) {
            return;
        }
        loadout.activeTab = 0;
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStack stashed = loadout.mainStash.getStackInSlot(i);
            if (stashed.isEmpty()) {
                continue;
            }
            loadout.mainStash.setStackInSlot(i, ItemStack.EMPTY);
            if (!player.getInventory().add(stashed)) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.spawnAtLocation(serverPlayer.level(), stashed);
                }
            }
        }
        loadout.dirty = true;
    }

    /** Writes the live inventory back into the store that owns it. */
    private static void stow(Player player, PlayerLoadout loadout, int tab) {
        NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
        if (tab == 0) {
            for (int i = 0; i < MAIN_SIZE; i++) {
                loadout.mainStash.setStackInSlot(i, items.get(INV_MAIN_START + i));
            }
            return;
        }
        ComponentBackedHandler handler = contents(loadout, tab);
        if (handler == null) {
            return;
        }
        for (int i = 0; i < MAIN_SIZE; i++) {
            handler.setStackInSlot(i, items.get(INV_MAIN_START + i));
        }
    }

    /** Moves a tab's stored contents into the live inventory, emptying the store. */
    private static void draw(Player player, PlayerLoadout loadout, int tab) {
        NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
        if (tab == 0) {
            for (int i = 0; i < MAIN_SIZE; i++) {
                items.set(INV_MAIN_START + i, loadout.mainStash.getStackInSlot(i));
                loadout.mainStash.setStackInSlot(i, ItemStack.EMPTY);
            }
        } else {
            ComponentBackedHandler handler = contents(loadout, tab);
            if (handler == null) {
                return;
            }
            for (int i = 0; i < MAIN_SIZE; i++) {
                items.set(INV_MAIN_START + i, handler.getStackInSlot(i));
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
    }

    @Nullable
    private static ComponentBackedHandler contents(PlayerLoadout loadout, int tab) {
        ItemStack pack = loadout.backpack(tab);
        if (pack.isEmpty()) {
            return null;
        }
        ComponentBackedHandler handler = new ComponentBackedHandler(
                pack, ModComponents.BACKPACK_CONTENTS.get(), 45, loadout::storageCap);
        handler.setChangeListener(() -> loadout.dirty = true);
        return handler;
    }
}
