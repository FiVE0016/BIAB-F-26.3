package dev.fallingcloud.betterinventory.inv;

import dev.fallingcloud.betterinventory.data.SettingKey;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 settings access: per-tab settings live on the default tab (player
 * attachment), on the equipped backpack's component (backpack tabs) or in
 * heldSettings when pack-free storage is active.
 */
public final class BetterInventorySettings {
    private BetterInventorySettings() {}

    public static PlayerLoadout of(Player player) {
        return PlayerLoadout.of(player);
    }

    public static TabSettings raw(Player player, int tab) {
        PlayerLoadout loadout = of(player);
        if (tab == 0) {
            return loadout.defaultTab;
        }
        if (loadout.packFree) {
            return loadout.heldSettings;
        }
        ItemStack pack = loadout.backpack();
        return pack.isEmpty() ? TabSettings.DEFAULT : BackpackItem.settings(pack);
    }

    public static TabSettings view(Player player, int tab) {
        TabSettings settings = raw(player, tab);
        return tab == 0 ? settings : settings.sliceLocks(PlayerLoadout.pageBase(tab), PlayerLoadout.PAGE_SIZE + 27);
    }

    public static void setRaw(Player player, int tab, TabSettings settings) {
        PlayerLoadout loadout = of(player);
        if (tab == 0) {
            loadout.defaultTab = settings;
        } else if (loadout.packFree) {
            loadout.heldSettings = settings;
        } else {
            ItemStack pack = loadout.backpack();
            if (!pack.isEmpty()) {
                BackpackItem.setSettings(pack, settings);
            }
        }
        loadout.dirty = true;
    }

    public static void setLockAt(Player player, int tab, int viewIndex, Optional<Identifier> item) {
        int slot = tab == 0 ? viewIndex : PlayerLoadout.pageBase(tab) + viewIndex;
        setRaw(player, tab, raw(player, tab).withLock(slot, item));
    }

    public static boolean upgradeEnabled(Player player, String upgradeId) {
        return of(player).defaultTab.upgradeEnabled(upgradeId);
    }

    public static void setUpgradeEnabled(Player player, String upgradeId, boolean enabled) {
        PlayerLoadout loadout = of(player);
        loadout.defaultTab = loadout.defaultTab.withUpgradeToggle(upgradeId, enabled);
        loadout.dirty = true;
    }

    public static boolean isShared(Player player, SettingKey key) {
        return of(player).sharedKeys.contains(key);
    }

    public static boolean gatherHub(Player player, int tab) {
        return of(player).shared.gatherHub();
    }

    public static boolean autoRefill(Player player, int tab) {
        return of(player).shared.autoRefill();
    }

    public static TabSettings.CombatPref combat(Player player, int tab) {
        return of(player).shared.combat();
    }

    public static void applySetting(Player player, int tab, SettingKey key, int value) {
        PlayerLoadout loadout = of(player);
        loadout.shared = switch (key) {
            case GATHER_HUB -> loadout.shared.withGatherHub(value != 0);
            case AUTO_REFILL -> loadout.shared.withAutoRefill(value != 0);
            case COMBAT_WEAPON -> loadout.shared.withCombat(
                    TabSettings.CombatPref.values()[Math.floorMod(value, TabSettings.CombatPref.values().length)]);
        };
        loadout.dirty = true;
    }

    public static void setShared(Player player, SettingKey key, boolean shared, int sourceTab) {
        PlayerLoadout loadout = of(player);
        if (shared) {
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
