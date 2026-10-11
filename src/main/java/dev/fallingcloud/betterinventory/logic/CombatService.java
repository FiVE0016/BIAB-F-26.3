package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 rack weapon resolution (axe-or-sword setting), engagement state and
 * the client/BetterCombat hook points.
 */
public final class CombatService {
    private static final int ENGAGED_TICKS = 20;
    public static Predicate<Player> engagedClient = player -> false;
    public static UnaryOperator<ItemStack> betterCombatLookup = stack -> stack;

    private CombatService() {}

    public static boolean weaponEngaged(Player player) {
        if (player.level().isClientSide()) {
            return engagedClient.test(player);
        }
        PlayerLoadout loadout = PlayerLoadout.existing(player);
        return loadout != null && player.tickCount - loadout.weaponEngagedTick <= ENGAGED_TICKS;
    }

    public static void markEngaged(Player player) {
        PlayerLoadout loadout = PlayerLoadout.existing(player);
        if (loadout != null) {
            loadout.weaponEngagedTick = player.tickCount;
        }
    }

    public static boolean isWeapon(ItemStack stack) {
        return ItemKinds.isMeleeWeapon(stack) || ItemKinds.isAxe(stack);
    }

    public static ItemStack resolve(Player player) {
        return resolve(player, null);
    }

    public static ItemStack resolve(Player player, @Nullable Entity target) {
        if (player.isSpectator()) {
            return ItemStack.EMPTY;
        }
        if (target != null && isSafe(player, target)) {
            return ItemStack.EMPTY;
        }
        PlayerLoadout loadout = PlayerLoadout.existing(player);
        if (loadout == null) {
            return ItemStack.EMPTY;
        }
        // 26.3: Inventory#getSelected is now getSelectedItem().
        if (isWeapon(player.getInventory().getSelectedItem())) {
            return ItemStack.EMPTY;
        }
        TabSettings.CombatPref pref = BetterInventorySettings.combat(player, loadout.activeTab);
        int first = pref == TabSettings.CombatPref.AXE ? PlayerLoadout.TOOL_AXE : PlayerLoadout.TOOL_SWORD;
        int second = pref == TabSettings.CombatPref.AXE ? PlayerLoadout.TOOL_SWORD : PlayerLoadout.TOOL_AXE;
        ItemStack weapon = usable(loadout.tools.getStackInSlot(first));
        if (weapon.isEmpty()) {
            weapon = usable(loadout.tools.getStackInSlot(second));
        }
        return weapon;
    }

    private static ItemStack usable(ItemStack stack) {
        return BrokenTools.isUnusable(stack) ? ItemStack.EMPTY : stack;
    }

    public static boolean isSafe(Player player, Entity target) {
        PlayerLoadout loadout = PlayerLoadout.existing(player);
        return loadout != null && !loadout.safeEntities.isEmpty()
                && loadout.safeEntities.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
    }

    public static void postAttackCleanup(Player player) {
        PlayerLoadout loadout = PlayerLoadout.existing(player);
        if (loadout != null) {
            for (int slot : new int[]{PlayerLoadout.TOOL_AXE, PlayerLoadout.TOOL_SWORD}) {
                ItemStack stack = loadout.tools.getStackInSlot(slot);
                if (!stack.isEmpty() && stack.getCount() <= 0) {
                    loadout.tools.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            loadout.dirty = true;
        }
    }

    public static void clearBrokenWeapon(Player player, ItemStack weapon) {
        PlayerLoadout loadout = PlayerLoadout.existing(player);
        if (loadout != null) {
            for (int i = 0; i < loadout.tools.getSlots(); i++) {
                if (loadout.tools.getStackInSlot(i) == weapon) {
                    loadout.tools.setStackInSlot(i, ItemStack.EMPTY);
                    return;
                }
            }
        }
    }
}
