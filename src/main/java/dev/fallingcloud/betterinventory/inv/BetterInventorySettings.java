package dev.fallingcloud.betterinventory.inv;

import dev.fallingcloud.betterinventory.data.SettingKey;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves effective settings for a tab, honoring the "share between backpacks"
 * flags. pickaxe2 lists, locks and upgrade toggles are always per-tab.
 */
public final class BetterInventorySettings {
    private BetterInventorySettings() {}

    public static PlayerLoadout of(Player player) {
        return ModAttachments.get(player);
    }

    /** The tab's own settings storage (attachment for tab 0, backpack component otherwise). */
    public static TabSettings raw(Player player, int tab) {
        PlayerLoadout loadout = of(player);
        if (tab == 0) {
            return loadout.defaultTab;
        }
        ItemStack pack = loadout.backpack(tab);
        return pack.isEmpty() ? TabSettings.DEFAULT : BackpackItem.settings(pack);
    }

    public static void setRaw(Player player, int tab, TabSettings settings) {
        PlayerLoadout loadout = of(player);
        if (tab == 0) {
            loadout.defaultTab = settings;
        } else {
            ItemStack pack = loadout.backpack(tab);
            if (!pack.isEmpty()) {
                BackpackItem.setSettings(pack, settings);
            }
        }
        loadout.dirty = true;
    }

    public static boolean isShared(Player player, SettingKey key) {
        return of(player).sharedKeys.contains(key);
    }

    public static boolean gatherHub(Player player, int tab) {
        PlayerLoadout loadout = of(player);
        return loadout.sharedKeys.contains(SettingKey.GATHER_HUB)
                ? loadout.shared.gatherHub()
                : raw(player, tab).gatherHub();
    }

    public static boolean autoRefill(Player player, int tab) {
        PlayerLoadout loadout = of(player);
        return loadout.sharedKeys.contains(SettingKey.AUTO_REFILL)
                ? loadout.shared.autoRefill()
                : raw(player, tab).autoRefill();
    }

    public static TabSettings.CombatPref combat(Player player, int tab) {
        PlayerLoadout loadout = of(player);
        return loadout.sharedKeys.contains(SettingKey.COMBAT_WEAPON)
                ? loadout.shared.combat()
                : raw(player, tab).combat();
    }

    /** Applies one of the shareable settings to the correct store (shared or per-tab). */
    public static void applySetting(Player player, int tab, SettingKey key, int value) {
        PlayerLoadout loadout = of(player);
        boolean shared = loadout.sharedKeys.contains(key);
        TabSettings target = shared ? loadout.shared : raw(player, tab);
        TabSettings updated = switch (key) {
            case GATHER_HUB -> target.withGatherHub(value != 0);
            case AUTO_REFILL -> target.withAutoRefill(value != 0);
            case COMBAT_WEAPON -> target.withCombat(TabSettings.CombatPref.values()[Math.floorMod(value, TabSettings.CombatPref.values().length)]);
        };
        if (shared) {
            loadout.shared = updated;
            loadout.dirty = true;
        } else {
            setRaw(player, tab, updated);
        }
    }

    public static void setShared(Player player, SettingKey key, boolean shared, int sourceTab) {
        PlayerLoadout loadout = of(player);
        if (shared) {
            // Adopt the current tab's value as the shared value.
            TabSettings source = raw(player, sourceTab);
            loadout.shared = switch (key) {
                case GATHER_HUB -> loadout.shared.withGatherHub(source.gatherHub());
                case AUTO_REFILL -> loadout.shared.withAutoRefill(source.autoRefill());
                case COMBAT_WEAPON -> loadout.shared.withCombat(source.combat());
            };
            loadout.sharedKeys.add(key);
        } else {
            loadout.sharedKeys.remove(key);
        }
        loadout.dirty = true;
    }
}
