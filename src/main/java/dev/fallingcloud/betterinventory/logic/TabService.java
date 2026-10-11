package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 tab switching: the active tab's 27 main slots live in the real player
 * inventory; switching stows them into the tab's store (72-slot page layout)
 * and draws the new tab.
 *
 * <p>26.3 adaptation: {@code Inventory.items} is private (use getItem/setItem)
 * and {@code Player#drop(ItemStack, boolean)} is gone (use spawnAtLocation).
 */
public final class TabService {
    public static final int MAIN_SIZE = 27;
    public static final int INV_MAIN_START = 9;

    private TabService() {}

    public static boolean switchTo(Player player, int requested) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        int tab = Math.floorMod(requested, PlayerLoadout.TAB_COUNT);
        if (tab == loadout.activeTab) {
            return true;
        }
        if (!loadout.tabAvailable(tab)) {
            return false;
        }
        if (!loadout.extraOnly) {
            stow(player, loadout, loadout.activeTab);
        }
        loadout.activeTab = tab;
        if (!loadout.extraOnly) {
            draw(player, loadout, tab);
        } else if (player instanceof ServerPlayer serverPlayer) {
            TabRules.evacuateMain(serverPlayer, loadout, tab);
        }
        loadout.dirty = true;
        return true;
    }

    public static void resetToDefault(Player player) {
        switchTo(player, 0);
    }

    public static void recoverIfOrphaned(Player player) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        if (loadout.activeTab != 0 && !loadout.tabAvailable(loadout.activeTab)) {
            loadout.activeTab = 0;
            if (loadout.extraOnly) {
                loadout.dirty = true;
            } else {
                for (int i = 0; i < MAIN_SIZE; i++) {
                    ItemStack stashed = loadout.mainStash.getStackInSlot(i);
                    if (!stashed.isEmpty()) {
                        loadout.mainStash.setStackInSlot(i, ItemStack.EMPTY);
                        if (!player.getInventory().add(stashed)
                                && player.level() instanceof ServerLevel serverLevel) {
                            player.spawnAtLocation(serverLevel, stashed);
                        }
                    }
                }
                loadout.dirty = true;
            }
        }
    }

    private static void stow(Player player, PlayerLoadout loadout, int tab) {
        Inventory inventory = player.getInventory();
        if (tab == 0) {
            for (int i = 0; i < MAIN_SIZE; i++) {
                loadout.mainStash.setStackInSlot(i, inventory.getItem(INV_MAIN_START + i));
            }
        } else {
            BigStackHandler handler = contents(loadout);
            if (handler != null) {
                int base = PlayerLoadout.pageBase(tab);
                for (int i = 0; i < MAIN_SIZE; i++) {
                    handler.setStackInSlot(base + i, inventory.getItem(INV_MAIN_START + i));
                }
            }
        }
    }

    private static void draw(Player player, PlayerLoadout loadout, int tab) {
        Inventory inventory = player.getInventory();
        if (tab == 0) {
            for (int i = 0; i < MAIN_SIZE; i++) {
                inventory.setItem(INV_MAIN_START + i, loadout.mainStash.getStackInSlot(i));
                loadout.mainStash.setStackInSlot(i, ItemStack.EMPTY);
            }
        } else {
            BigStackHandler handler = contents(loadout);
            if (handler == null) {
                return;
            }
            int base = PlayerLoadout.pageBase(tab);
            for (int i = 0; i < MAIN_SIZE; i++) {
                inventory.setItem(INV_MAIN_START + i, handler.getStackInSlot(base + i));
                handler.setStackInSlot(base + i, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
    }

    @Nullable
    private static BigStackHandler contents(PlayerLoadout loadout) {
        return loadout.pageStore();
    }
}
