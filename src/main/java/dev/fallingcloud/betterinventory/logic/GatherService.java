package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.TriState;

/**
 * Routes picked-up items into gather hubs (the 2x9 sections) before vanilla pickup,
 * plus the Pickup and Void upgrade behaviors.
 */
public final class GatherService {
    private GatherService() {}

    // Fabric has no ItemEntityPickupEvent; the caller (see ItemPickupMixin) passes the
    // player and entity directly and honours the returned decision.
    public static boolean onPickup(Player player, ItemEntity entity) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.isSpectator()) {
            return false;
        }
                if (entity == null || entity.hasPickUpDelay()) {
            return false;
        }
        PlayerLoadout loadout = ModAttachments.get(serverPlayer);
        if (loadout == null) {
            return false;
        }
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return false;
        }
        int originalCount = stack.getCount();

        // 1. Backpacks with an enabled Pickup upgrade that already know this item.
        for (int tab = 1; tab <= 4 && !stack.isEmpty(); tab++) {
            if (!loadout.upgradeInstalled(tab, UpgradeItem.Kind.PICKUP)) {
                continue;
            }
            TabSettings settings = BetterInventorySettings.raw(serverPlayer, tab);
            if (!settings.upgradeEnabled(UpgradeItem.Kind.PICKUP.id())) {
                continue;
            }
            ComponentBackedHandler handler = packContents(loadout, tab);
            if (handler != null && matchesExisting(handler, settings, stack)) {
                // Gather range only: while a tab is active its main 27 slots are empty
                // because those items live in the player inventory, and writing there
                // would be overwritten when the tab is stowed.
                insertInto(handler, settings, TabStorage45.GATHER_START, TabStorage45.GATHER_END,
                        stack, loadout.storageCap());
            }
        }

        // 2. Gather hubs, active tab first, then default, then the rest.
        for (int tab : gatherOrder(loadout)) {
            if (stack.isEmpty()) {
                break;
            }
            if (!loadout.tabAvailable(tab) || !BetterInventorySettings.gatherHub(serverPlayer, tab)) {
                continue;
            }
            if (tab == 0) {
                if (loadout.hasAnyBackpack()) {
                    insertIntoGatherHandler(loadout, serverPlayer, stack);
                }
            } else {
                ComponentBackedHandler handler = packContents(loadout, tab);
                if (handler != null) {
                    insertInto(handler, BetterInventorySettings.raw(serverPlayer, tab), TabStorage45.GATHER_START, TabStorage45.GATHER_END, stack, loadout.storageCap());
                }
            }
        }

        // 3. Void upgrade: destroy the remainder if a backpack voids this item.
        if (!stack.isEmpty() && shouldVoid(serverPlayer, loadout, stack)) {
            stack.setCount(0);
        }

        if (stack.getCount() != originalCount) {
            loadout.dirty = true;
        }
        if (stack.isEmpty()) {
            serverPlayer.take(entity, originalCount);
            entity.discard();
            return false;   // nothing left - caller should cancel the pickup
        }
        return true;
    }

    private static int[] gatherOrder(PlayerLoadout loadout) {
        LinkedHashSet<Integer> order = new LinkedHashSet<>();
        order.add(loadout.activeTab);
        order.add(0);
        for (int tab = 1; tab <= 4; tab++) {
            order.add(tab);
        }
        int[] result = new int[order.size()];
        int i = 0;
        for (int tab : order) {
            result[i++] = tab;
        }
        return result;
    }

    private static ComponentBackedHandler packContents(PlayerLoadout loadout, int tab) {
        ItemStack pack = loadout.backpack(tab);
        if (pack.isEmpty()) {
            return null;
        }
        ComponentBackedHandler handler = new ComponentBackedHandler(pack, ModComponents.BACKPACK_CONTENTS.get(), TabStorage45.SIZE, loadout::storageCap);
        handler.setChangeListener(() -> loadout.dirty = true);
        return handler;
    }

    private static boolean matchesExisting(ComponentBackedHandler handler, TabSettings settings, ItemStack stack) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), stack)) {
                return true;
            }
        }
        for (TabSettings.LockEntry lock : settings.locks()) {
            if (stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(lock.item()))) {
                return true;
            }
        }
        return false;
    }

    private static void insertIntoGatherHandler(PlayerLoadout loadout, Player player, ItemStack stack) {
        TabSettings settings = BetterInventorySettings.raw(player, 0);
        int cap = loadout.storageCap();
        // Merge pass, then empty-slot pass; lock indices for the personal hub are 27-44.
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < loadout.gather.getSlots() && !stack.isEmpty(); i++) {
                int lockIndex = TabStorage45.GATHER_START + i;
                if (!settings.allows(lockIndex, stack)) {
                    continue;
                }
                ItemStack existing = loadout.gather.getStackInSlot(i);
                int limit = BigStackHandler.capFor(stack, cap);
                if (pass == 0) {
                    if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                        continue;
                    }
                    int room = limit - existing.getCount();
                    if (room <= 0) {
                        continue;
                    }
                    int moved = Math.min(room, stack.getCount());
                    existing.grow(moved);
                    stack.shrink(moved);
                    loadout.gather.setStackInSlot(i, existing);
                } else if (existing.isEmpty()) {
                    int moved = Math.min(limit, stack.getCount());
                    loadout.gather.setStackInSlot(i, stack.copyWithCount(moved));
                    stack.shrink(moved);
                }
            }
        }
    }

    /** Inserts into a range of a backpack contents handler, honoring locks + big stacks. */
    private static void insertInto(ComponentBackedHandler handler, TabSettings settings, int from, int to, ItemStack stack, int cap) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = from; i < to && !stack.isEmpty(); i++) {
                if (!settings.allows(i, stack)) {
                    continue;
                }
                ItemStack existing = handler.getStackInSlot(i);
                int limit = BigStackHandler.capFor(stack, cap);
                if (pass == 0) {
                    if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
                        continue;
                    }
                    int room = limit - existing.getCount();
                    if (room <= 0) {
                        continue;
                    }
                    int moved = Math.min(room, stack.getCount());
                    existing.grow(moved);
                    stack.shrink(moved);
                    handler.setStackInSlot(i, existing);
                } else if (existing.isEmpty()) {
                    int moved = Math.min(limit, stack.getCount());
                    handler.setStackInSlot(i, stack.copyWithCount(moved));
                    stack.shrink(moved);
                }
            }
        }
    }

    private static boolean shouldVoid(ServerPlayer player, PlayerLoadout loadout, ItemStack stack) {
        for (int tab = 1; tab <= 4; tab++) {
            if (!loadout.upgradeInstalled(tab, UpgradeItem.Kind.VOID)) {
                continue;
            }
            TabSettings settings = BetterInventorySettings.raw(player, tab);
            if (!settings.upgradeEnabled(UpgradeItem.Kind.VOID.id())) {
                continue;
            }
            for (TabSettings.LockEntry lock : settings.locks()) {
                if (stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(lock.item()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Shared slot-range constants for the 45-slot storage layout. */
    public static final class TabStorage45 {
        public static final int SIZE = 45;
        public static final int GATHER_START = 27;
        public static final int GATHER_END = 45;
        public static final int ALL = 45;

        private TabStorage45() {}
    }
}
