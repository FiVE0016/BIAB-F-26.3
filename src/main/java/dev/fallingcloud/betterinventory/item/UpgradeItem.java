package dev.fallingcloud.betterinventory.item;

import net.minecraft.world.item.Item;

/** A backpack ability upgrade. Installed via the tab settings screen. */
public class UpgradeItem extends Item {
    public enum Kind {
        MAGNET("magnet"),
        FEEDING("feeding"),
        REFILL("refill"),
        VOID("void"),
        PICKUP("pickup");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final Kind kind;

    public UpgradeItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

}
