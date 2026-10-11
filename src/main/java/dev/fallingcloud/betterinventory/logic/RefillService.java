package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 auto hotbar refill: when a hotbar stack is used up, pull a matching
 * stack from the main inventory, the gather hub, or backpack pages (level-
 * gated: level 1 gathers only the extra area, level 2+ the whole page).
 * Level 3 also tops up non-empty hotbar stacks every second.
 *
 * <p>26.3 adaptation: {@code Inventory.items} is private (getItem/setItem).
 */
public final class RefillService {
    private static final Map<UUID, ItemStack[]> SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, Integer> MANUAL_EDIT = new HashMap<>();
    private static final int MANUAL_EDIT_GRACE_TICKS = 40;

    private RefillService() {}

    public static void noteManualEdit(Player player) {
        MANUAL_EDIT.put(player.getUUID(), player.tickCount);
    }

    private static boolean recentlyEdited(ServerPlayer player) {
        Integer at = MANUAL_EDIT.get(player.getUUID());
        return at != null && player.tickCount - at < MANUAL_EDIT_GRACE_TICKS;
    }

    public static void tick(ServerPlayer player) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        ItemStack[] previous = SNAPSHOTS.computeIfAbsent(player.getUUID(), id -> emptySnapshot());
        boolean enabled = BetterInventorySettings.autoRefill(player, loadout.activeTab)
                && player.containerMenu == player.inventoryMenu
                && !recentlyEdited(player);
        if (enabled) {
            for (int i = 0; i < 9; i++) {
                ItemStack now = player.getInventory().getItem(i);
                ItemStack before = previous[i];
                if (now.isEmpty() && !before.isEmpty()) {
                    refillSlot(player, loadout, i, before);
                }
            }
            if (player.tickCount % 20 == 0
                    && loadout.upgradeLevel(UpgradeItem.Kind.REFILL) >= 3
                    && BetterInventorySettings.upgradeEnabled(player, UpgradeItem.Kind.REFILL.id())) {
                topUpHotbar(player, loadout);
            }
        }
        for (int i = 0; i < 9; i++) {
            previous[i] = player.getInventory().getItem(i).copy();
        }
    }

    public static void forget(UUID id) {
        SNAPSHOTS.remove(id);
        MANUAL_EDIT.remove(id);
    }

    private static ItemStack[] emptySnapshot() {
        ItemStack[] arr = new ItemStack[9];
        Arrays.fill(arr, ItemStack.EMPTY);
        return arr;
    }

    private static void refillSlot(ServerPlayer player, PlayerLoadout loadout, int hotbarSlot, ItemStack template) {
        int wanted = template.getMaxStackSize();

        for (int i = 9; i < 36; i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(candidate, template)) {
                int moved = Math.min(wanted, candidate.getCount());
                player.getInventory().setItem(hotbarSlot, candidate.copyWithCount(moved));
                candidate.shrink(moved);
                if (candidate.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                player.getInventory().setChanged();
                return;
            }
        }

        if (loadout.gatherUnlocked()
                && takeFromHandler(player, loadout.gather, 0, loadout.gather.getSlots(), hotbarSlot, template, wanted)) {
            loadout.dirty = true;
        } else {
            int level = loadout.upgradeLevel(UpgradeItem.Kind.REFILL);
            if (level > 0 && BetterInventorySettings.upgradeEnabled(player, UpgradeItem.Kind.REFILL.id())) {
                BigStackHandler handler = packContents(loadout);
                if (handler != null) {
                    for (int tab = 1; tab <= loadout.pages(); tab++) {
                        int base = PlayerLoadout.pageBase(tab);
                        int from = level >= 2 ? base : base + 27;
                        if (takeFromHandler(player, handler, from, base + PlayerLoadout.PAGE_SIZE,
                                hotbarSlot, template, wanted)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void topUpHotbar(ServerPlayer player, PlayerLoadout loadout) {
        BigStackHandler handler = packContents(loadout);
        if (handler != null) {
            int limit = loadout.pages() * PlayerLoadout.PAGE_SIZE;
            for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
                ItemStack stack = player.getInventory().getItem(hotbarSlot);
                if (!stack.isEmpty() && stack.getCount() < stack.getMaxStackSize()) {
                    int room = stack.getMaxStackSize() - stack.getCount();
                    for (int i = 0; i < limit && room > 0; i++) {
                        ItemStack candidate = handler.getStackInSlot(i);
                        if (ItemStack.isSameItemSameComponents(candidate, stack)) {
                            int moved = Math.min(room, candidate.getCount());
                            stack.grow(moved);
                            room -= moved;
                            if (candidate.getCount() <= moved) {
                                handler.setStackInSlot(i, ItemStack.EMPTY);
                            } else {
                                handler.setStackInSlot(i, candidate.copyWithCount(candidate.getCount() - moved));
                            }
                            player.getInventory().setChanged();
                            loadout.dirty = true;
                        }
                    }
                }
            }
        }
    }

    private static BigStackHandler packContents(PlayerLoadout loadout) {
        return loadout.pages() <= 0 ? null : loadout.pageStore();
    }

    private static boolean takeFromHandler(ServerPlayer player, ItemStackHandler handler,
            int from, int to, int hotbarSlot, ItemStack template, int wanted) {
        for (int i = from; i < to; i++) {
            ItemStack candidate = handler.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(candidate, template)) {
                int moved = Math.min(wanted, candidate.getCount());
                player.getInventory().setItem(hotbarSlot, candidate.copyWithCount(moved));
                if (candidate.getCount() <= moved) {
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                } else {
                    handler.setStackInSlot(i, candidate.copyWithCount(candidate.getCount() - moved));
                }
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }
}
