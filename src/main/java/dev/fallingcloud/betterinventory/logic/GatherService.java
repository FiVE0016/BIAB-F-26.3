package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import java.util.LinkedHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 pickup routing: routes picked-up items into the gather hubs (and into
 * backpack pages via the Pickup upgrade tiers) before vanilla pickup, and
 * voids the remainder via the Void upgrade tiers.
 *
 * <p>26.3 adaptation: {@code stack.is(Item)} survives but tag/lookup-free item
 * resolution goes through {@code BuiltInRegistries.ITEM.getOptional}.
 */
public final class GatherService {
    private GatherService() {}

    public static boolean onPickup(Player player, ItemEntity entity) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.isSpectator()) {
            return false;
        }
        if (entity.hasPickUpDelay()) {
            return false;
        }
        PlayerLoadout loadout = PlayerLoadout.existing(serverPlayer);
        if (loadout == null) {
            return false;
        }
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return false;
        }
        int originalCount = stack.getCount();
        route(serverPlayer, loadout, stack);
        if (stack.getCount() != originalCount) {
            loadout.dirty = true;
        }
        if (stack.isEmpty()) {
            serverPlayer.take(entity, originalCount);
            entity.discard();
            return true;
        }
        return false;
    }

    static void route(ServerPlayer serverPlayer, PlayerLoadout loadout, ItemStack stack) {
        int pickupLevel = loadout.upgradeLevel(UpgradeItem.Kind.PICKUP);
        if (pickupLevel > 0 && loadout.pages() > 0
                && BetterInventorySettings.upgradeEnabled(serverPlayer, UpgradeItem.Kind.PICKUP.id())) {
            BigStackHandler handler = packContents(loadout);
            if (handler != null && pickupMatches(serverPlayer, loadout, handler, stack, pickupLevel)) {
                for (int tab = 1; tab <= loadout.pages() && !stack.isEmpty(); tab++) {
                    if (BetterInventorySettings.gatherHub(serverPlayer, tab)) {
                        int base = PlayerLoadout.pageBase(tab);
                        insertInto(handler, BetterInventorySettings.view(serverPlayer, tab),
                                base, 27, PlayerLoadout.PAGE_SIZE, stack, loadout.storageCap());
                    }
                }
            }
        }

        for (int tab : gatherOrder(loadout)) {
            if (stack.isEmpty()) {
                break;
            }
            if (loadout.tabAvailable(tab) && BetterInventorySettings.gatherHub(serverPlayer, tab)) {
                if (tab == 0) {
                    if (loadout.gatherUnlocked()) {
                        insertIntoGatherHandler(loadout, serverPlayer, stack);
                    }
                } else {
                    BigStackHandler handler = packContents(loadout);
                    if (handler != null) {
                        insertInto(handler, BetterInventorySettings.view(serverPlayer, tab),
                                PlayerLoadout.pageBase(tab), 27, PlayerLoadout.PAGE_SIZE,
                                stack, loadout.storageCap());
                    }
                }
            }
        }

        if (!stack.isEmpty() && shouldVoid(serverPlayer, loadout, stack)) {
            stack.setCount(0);
        }
    }

    private static int[] gatherOrder(PlayerLoadout loadout) {
        LinkedHashSet<Integer> order = new LinkedHashSet<>();
        order.add(loadout.activeTab);
        order.add(0);
        for (int tab = 1; tab <= PlayerLoadout.PAGE_COUNT; tab++) {
            order.add(tab);
        }
        int[] result = new int[order.size()];
        int i = 0;
        for (int tab : order) {
            result[i++] = tab;
        }
        return result;
    }

    private static BigStackHandler packContents(PlayerLoadout loadout) {
        return loadout.pageStore();
    }

    private static boolean matchesLock(ItemStack stack, Identifier itemId) {
        return BuiltInRegistries.ITEM.getOptional(itemId).map(stack::is).orElse(false);
    }

    private static boolean pickupMatches(Player player, PlayerLoadout loadout,
            BigStackHandler handler, ItemStack stack, int level) {
        if (level >= 3) {
            return true;
        }
        for (TabSettings.LockEntry lock : BetterInventorySettings.raw(player, 1).locks()) {
            if (matchesLock(stack, lock.item())) {
                return true;
            }
        }
        if (level >= 2) {
            int limit = loadout.pages() * PlayerLoadout.PAGE_SIZE;
            for (int i = 0; i < limit; i++) {
                if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void insertIntoGatherHandler(PlayerLoadout loadout, Player player, ItemStack stack) {
        TabSettings settings = BetterInventorySettings.raw(player, 0);
        int cap = loadout.storageCap();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < loadout.gather.getSlots() && !stack.isEmpty(); i++) {
                int lockIndex = PlayerLoadout.GATHER_START + i;
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

    private static void insertInto(BigStackHandler handler, TabSettings settings,
            int base, int from, int to, ItemStack stack, int cap) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = from; i < to && !stack.isEmpty(); i++) {
                if (!settings.allows(i, stack)) {
                    continue;
                }
                ItemStack existing = handler.getStackInSlot(base + i);
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
                    handler.setStackInSlot(base + i, existing);
                } else if (existing.isEmpty()) {
                    int moved = Math.min(limit, stack.getCount());
                    handler.setStackInSlot(base + i, stack.copyWithCount(moved));
                    stack.shrink(moved);
                }
            }
        }
    }

    private static boolean shouldVoid(ServerPlayer player, PlayerLoadout loadout, ItemStack stack) {
        int level = loadout.upgradeLevel(UpgradeItem.Kind.VOID);
        if (level > 0 && BetterInventorySettings.upgradeEnabled(player, UpgradeItem.Kind.VOID.id())) {
            for (TabSettings.LockEntry lock : BetterInventorySettings.raw(player, 1).locks()) {
                if (matchesLock(stack, lock.item())) {
                    return true;
                }
            }
            BigStackHandler handler = packContents(loadout);
            if (level >= 2 && handler != null) {
                int limit = loadout.pages() * PlayerLoadout.PAGE_SIZE;
                for (int i = 0; i < limit; i++) {
                    if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), stack)) {
                        return true;
                    }
                }
            }
            if (level >= 3) {
                var inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack held = inventory.getItem(i);
                    if (ItemStack.isSameItemSameComponents(held, stack)
                            && held.getCount() >= held.getMaxStackSize()) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }
}
