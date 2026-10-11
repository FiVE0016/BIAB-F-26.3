package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 tab rules: keeps packFree / extraOnly (and the storage they imply) in
 * step with the config every tick, migrating items between the backpack and the
 * held stores as the config changes.
 *
 * <p>26.3 adaptation: {@code displayClientMessage(component, false)} is now
 * {@code sendSystemMessage(component)}.
 */
public final class TabRules {
    private TabRules() {}

    public static void apply(ServerPlayer player, PlayerLoadout loadout) {
        apply(player, loadout, !BetterInventoryConfig.tabsNeedBackpack(),
                !BetterInventoryConfig.tabsSwapMain(), player.tickCount % 20 == 0);
    }

    static void apply(ServerPlayer player, PlayerLoadout loadout, boolean packFree, boolean extraOnly, boolean checkLeftOver) {
        if (loadout.packFree != packFree) {
            TabService.recoverIfOrphaned(player);
            TabService.switchTo(player, 0);
            if (packFree || !loadout.backpack().isEmpty()) {
                move(player, loadout, packFree);
            }
            loadout.packFree = packFree;
            loadout.dirty = true;
            if (!packFree && heldLeftOver(loadout)) {
                player.sendSystemMessage(Component.translatable("message.betterinventory.pages_waiting"));
            }
        } else if (!packFree && checkLeftOver && !loadout.backpack().isEmpty() && heldLeftOver(loadout)) {
            move(player, loadout, false);
        }

        if (loadout.extraOnly != extraOnly) {
            loadout.extraOnly = extraOnly;
            if (extraOnly) {
                int waiting = settleMain(player, loadout);
                if (waiting > 0) {
                    player.sendSystemMessage(Component.translatable("message.betterinventory.main_waiting", waiting));
                }
            } else {
                unsettle(player, loadout);
            }
            loadout.dirty = true;
        } else if (extraOnly && checkLeftOver && loadout.mainUnsettled) {
            settleMain(player, loadout);
        }
    }

    private static boolean heldLeftOver(PlayerLoadout loadout) {
        for (int i = 0; i < loadout.heldPages.getSlots(); i++) {
            if (!loadout.heldPages.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        for (int i = 0; i < loadout.heldUpgrades.getSlots(); i++) {
            if (!loadout.heldUpgrades.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void move(ServerPlayer player, PlayerLoadout loadout, boolean toPlayer) {
        loadout.packFree = !toPlayer;
        BigStackHandler from = loadout.pageStore();
        TabSettings settings = BetterInventorySettings.raw(player, 1);
        loadout.packFree = toPlayer;
        BigStackHandler to = loadout.pageStore();
        if (from != null && to != null && from != to) {
            movePages(loadout, from, to);
        }
        if (BetterInventorySettings.raw(player, 1).equals(TabSettings.DEFAULT) && !settings.equals(TabSettings.DEFAULT)) {
            BetterInventorySettings.setRaw(player, 1, settings);
        }
        for (int i = 0; i < loadout.packUpgrades.getSlots(); i++) {
            loadout.packFree = !toPlayer;
            ItemStack upgrade = loadout.packUpgrades.getStackInSlot(i);
            if (!upgrade.isEmpty()) {
                loadout.packFree = toPlayer;
                int free = -1;
                for (int slot = 0; slot < loadout.packUpgrades.getSlots() && free < 0; slot++) {
                    if (loadout.packUpgrades.getStackInSlot(slot).isEmpty()
                            && loadout.packUpgrades.isItemValid(slot, upgrade)) {
                        free = slot;
                    }
                }
                if (free >= 0) {
                    loadout.packFree = !toPlayer;
                    loadout.packUpgrades.setStackInSlot(i, ItemStack.EMPTY);
                    loadout.packFree = toPlayer;
                    loadout.packUpgrades.setStackInSlot(free, upgrade);
                }
            }
        }
        loadout.packFree = toPlayer;
    }

    private static void movePages(PlayerLoadout loadout, BigStackHandler from, BigStackHandler to) {
        int limit = Math.min(from.getSlots(), to.getSlots());
        if (!loadout.packFree) {
            limit = Math.min(limit, loadout.pages() * PlayerLoadout.PAGE_SIZE);
        }
        int openFrom = -1;
        if (!loadout.extraOnly && loadout.activeTab > 0) {
            openFrom = PlayerLoadout.pageBase(loadout.activeTab);
        }
        for (int i = 0; i < limit; i++) {
            if (openFrom < 0 || i < openFrom || i >= openFrom + 27) {
                ItemStack stack = from.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    if (to.getStackInSlot(i).isEmpty()) {
                        from.setStackInSlot(i, ItemStack.EMPTY);
                        to.setStackInSlot(i, stack);
                    } else {
                        int base = i - i % PlayerLoadout.PAGE_SIZE;
                        ItemStack rest = stack.copy();
                        for (int slot = base + 27; slot < base + PlayerLoadout.PAGE_SIZE && !rest.isEmpty(); slot++) {
                            rest = to.insertItem(slot, rest, false);
                        }
                        if (rest.getCount() != stack.getCount()) {
                            from.setStackInSlot(i, rest);
                        }
                    }
                }
            }
        }
        loadout.mainUnsettled = loadout.mainUnsettled | loadout.extraOnly;
    }

    private static int settleMain(ServerPlayer player, PlayerLoadout loadout) {
        int waiting = 0;
        for (int i = 0; i < loadout.mainStash.getSlots(); i++) {
            ItemStack stack = loadout.mainStash.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemStack rest = giveBack(player, loadout, stack.copy());
                if (rest.getCount() != stack.getCount()) {
                    loadout.mainStash.setStackInSlot(i, rest);
                }
                if (!rest.isEmpty()) {
                    waiting++;
                }
            }
        }
        BigStackHandler store = loadout.pageStore();
        for (int tab = 1; store != null && tab <= PlayerLoadout.PAGE_COUNT; tab++) {
            waiting += evacuate(player, loadout, store, tab);
        }
        loadout.mainUnsettled = waiting > 0;
        return waiting;
    }

    public static void evacuateMain(ServerPlayer player, PlayerLoadout loadout, int tab) {
        BigStackHandler store = loadout.pageStore();
        if (store != null && tab >= 1 && tab <= PlayerLoadout.PAGE_COUNT) {
            if (evacuate(player, loadout, store, tab) > 0) {
                loadout.mainUnsettled = true;
            }
        }
    }

    private static int evacuate(ServerPlayer player, PlayerLoadout loadout, BigStackHandler store, int tab) {
        int base = PlayerLoadout.pageBase(tab);
        int waiting = 0;
        for (int i = 0; i < 27; i++) {
            ItemStack stack = store.getStackInSlot(base + i);
            if (!stack.isEmpty()) {
                ItemStack rest = stack.copy();
                for (int slot = base + 27; slot < base + PlayerLoadout.PAGE_SIZE && !rest.isEmpty(); slot++) {
                    rest = store.insertItem(slot, rest, false);
                }
                if (!rest.isEmpty()) {
                    rest = giveBack(player, loadout, rest);
                }
                if (rest.getCount() != stack.getCount()) {
                    store.setStackInSlot(base + i, rest);
                }
                if (!rest.isEmpty()) {
                    waiting++;
                }
            }
        }
        return waiting;
    }

    private static void unsettle(ServerPlayer player, PlayerLoadout loadout) {
        loadout.activeTab = 0;
        loadout.mainUnsettled = false;
        BigStackHandler store = loadout.pageStore();
        for (int i = 0; i < loadout.mainStash.getSlots(); i++) {
            ItemStack rest = loadout.mainStash.getStackInSlot(i);
            if (!rest.isEmpty()) {
                loadout.mainStash.setStackInSlot(i, ItemStack.EMPTY);
                rest = giveBack(player, loadout, rest);
                for (int tab = 1; store != null && tab <= loadout.pages() && !rest.isEmpty(); tab++) {
                    int base = PlayerLoadout.pageBase(tab);
                    for (int slot = base; slot < base + 27 && !rest.isEmpty(); slot++) {
                        rest = store.insertItem(slot, rest, false);
                    }
                }
                if (!rest.isEmpty()) {
                    player.spawnAtLocation(player.level(), rest);
                }
            }
        }
    }

    private static ItemStack giveBack(ServerPlayer player, PlayerLoadout loadout, ItemStack stack) {
        if (loadout.gatherUnlocked()) {
            for (int i = 0; i < loadout.gather.getSlots() && !stack.isEmpty(); i++) {
                stack = loadout.gather.insertItem(i, stack, false);
            }
        }
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Inventory inventory = player.getInventory();
        ItemStack rest = stack.copy();
        while (!rest.isEmpty()) {
            int slot = inventory.getSlotWithRemainingSpace(rest);
            if (slot < 0) {
                slot = inventory.getFreeSlot();
            }
            if (slot < 0) {
                break;
            }
            ItemStack there = inventory.getItem(slot);
            int moved = Math.min(rest.getCount(), StackUpgradeService.effectiveMax(inventory, rest) - there.getCount());
            if (moved <= 0) {
                break;
            }
            if (there.isEmpty()) {
                inventory.setItem(slot, rest.split(moved));
            } else {
                there.grow(moved);
                rest.shrink(moved);
            }
        }
        return rest;
    }
}
