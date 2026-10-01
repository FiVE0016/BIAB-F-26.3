package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;

/** Resolves the rack weapon used for melee attacks (axe-or-sword setting). */
public final class CombatService {
    private CombatService() {}

    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem;
    }

    /**
     * The rack weapon to attack with, or EMPTY when the held item should be used
     * (already a weapon, or the rack has none).
     */
    public static ItemStack resolve(Player player) {
        if (player.isSpectator()) {
            return ItemStack.EMPTY;
        }
        PlayerLoadout loadout = ModAttachments.get(player);
        if (loadout == null) {
            return ItemStack.EMPTY;
        }
        if (isWeapon(player.getMainHandItem())) {
            return ItemStack.EMPTY;
        }
        TabSettings.CombatPref pref = BetterInventorySettings.combat(player, loadout.activeTab);
        int first = pref == TabSettings.CombatPref.AXE ? PlayerLoadout.TOOL_AXE : PlayerLoadout.TOOL_SWORD;
        int second = pref == TabSettings.CombatPref.AXE ? PlayerLoadout.TOOL_SWORD : PlayerLoadout.TOOL_AXE;
        ItemStack weapon = loadout.tools.getStackInSlot(first);
        if (weapon.isEmpty()) {
            weapon = loadout.tools.getStackInSlot(second);
        }
        return weapon;
    }

    /** Called after an attack that used a rack weapon; clears the slot if it broke. */
    public static void postAttackCleanup(Player player) {
        PlayerLoadout loadout = ModAttachments.get(player);
        if (loadout == null) {
            return;
        }
        for (int slot : new int[]{PlayerLoadout.TOOL_AXE, PlayerLoadout.TOOL_SWORD}) {
            ItemStack stack = loadout.tools.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getCount() <= 0) {
                loadout.tools.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        loadout.dirty = true;
    }

    /** Clears the given rack weapon (it broke mid-attack). */
    public static void clearBrokenWeapon(Player player, ItemStack weapon) {
        PlayerLoadout loadout = ModAttachments.get(player);
        if (loadout == null) {
            return;
        }
        for (int i = 0; i < loadout.tools.getSlots(); i++) {
            if (loadout.tools.getStackInSlot(i) == weapon) {
                loadout.tools.setStackInSlot(i, ItemStack.EMPTY);
                return;
            }
        }
    }
}
