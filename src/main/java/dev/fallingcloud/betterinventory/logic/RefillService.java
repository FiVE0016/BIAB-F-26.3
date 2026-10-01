package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;

/**
 * Auto hotbar refill: when a hotbar stack is used up, pull a matching stack from the
 * main inventory, the gather hub, or backpacks with the Refill upgrade.
 */
public final class RefillService {
    private RefillService() {}

    private static final Map<UUID, ItemStack[]> SNAPSHOTS = new HashMap<>();

    public static void tick(ServerPlayer player) {
        PlayerLoadout loadout = ModAttachments.get(player);
        ItemStack[] previous = SNAPSHOTS.computeIfAbsent(player.getUUID(), id -> emptySnapshot());

        boolean enabled = BetterInventorySettings.autoRefill(player, loadout.activeTab)
                && player.containerMenu == player.inventoryMenu; // don't fight open GUIs

        if (enabled) {
            for (int i = 0; i < 9; i++) {
                ItemStack now = player.getInventory().getItem(i);
                ItemStack before = previous[i];
                if (now.isEmpty() && !before.isEmpty()) {
                    refillSlot(player, loadout, i, before);
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            previous[i] = player.getInventory().getItem(i).copy();
        }
    }

    public static void forget(UUID id) {
        SNAPSHOTS.remove(id);
    }

    private static ItemStack[] emptySnapshot() {
        ItemStack[] arr = new ItemStack[9];
        java.util.Arrays.fill(arr, ItemStack.EMPTY);
        return arr;
    }

    private static void refillSlot(ServerPlayer player, PlayerLoadout loadout, int hotbarSlot, ItemStack template) {
        int wanted = template.getMaxStackSize();

        // Main inventory (rows 9-35).
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
        // Personal gather hub.
        if (loadout.hasAnyBackpack() && takeFromHandler(player, loadout.gather, hotbarSlot, template, wanted)) {
            loadout.dirty = true;
            return;
        }
        // Backpacks with an enabled Refill upgrade.
        for (int tab = 1; tab <= 4; tab++) {
            if (!loadout.upgradeInstalled(tab, UpgradeItem.Kind.REFILL)) {
                continue;
            }
            if (!BetterInventorySettings.raw(player, tab).upgradeEnabled(UpgradeItem.Kind.REFILL.id())) {
                continue;
            }
            ItemStack pack = loadout.backpack(tab);
            ComponentBackedHandler handler = new ComponentBackedHandler(pack, ModComponents.BACKPACK_CONTENTS.get(), 45, loadout::storageCap);
            handler.setChangeListener(() -> loadout.dirty = true);
            if (takeFromHandler(player, handler, hotbarSlot, template, wanted)) {
                return;
            }
        }
    }

    private static boolean takeFromHandler(ServerPlayer player, ItemStackHandler handler, int hotbarSlot, ItemStack template, int wanted) {
        for (int i = 0; i < handler.getSlots(); i++) {
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
