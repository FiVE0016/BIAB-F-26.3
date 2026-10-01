package dev.fallingcloud.betterinventory.data;

/** The three per-tab settings that can optionally be shared across all tabs. */
public enum SettingKey {
    GATHER_HUB,
    AUTO_REFILL,
    COMBAT_WEAPON;

    public static SettingKey byId(int id) {
        SettingKey[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
