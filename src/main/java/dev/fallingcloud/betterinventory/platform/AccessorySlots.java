package dev.fallingcloud.betterinventory.platform;

import net.minecraft.world.item.ItemStack;

/**
 * A flat view of the player's equipped accessory slots — Curios on NeoForge/Forge,
 * Trinkets on Fabric — or {@link #NONE} when neither is installed.
 *
 * <p>Deliberately narrower than either mod's API: the inventory screen only needs to
 * count slots, read them, write them, and ask whether a stack may go in one.
 */
public interface AccessorySlots {
    AccessorySlots NONE = new AccessorySlots() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public ItemStack get(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public void set(int index, ItemStack stack) {}

        @Override
        public boolean isValid(int index, ItemStack stack) {
            return false;
        }
    };

    int size();

    ItemStack get(int index);

    void set(int index, ItemStack stack);

    boolean isValid(int index, ItemStack stack);

    default boolean present() {
        return size() > 0;
    }
}
