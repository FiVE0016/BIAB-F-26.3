package dev.fallingcloud.betterinventory.menu.slots;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * 1.4.1 background slot: a plain {@link Slot} with a fluent
 * {@code setBackground} so menu construction can chain an empty-slot icon the
 * same way the 1.4.1 code did with its atlas+sprite pair.
 *
 * <p>26.3 adaptation: 1.21.1's {@code setBackground(Identifier, Identifier)}
 * is gone; the supported way is overriding {@link #getNoItemIcon()} (the GUI
 * sprite atlas is implicit), which this class does with a fluent setter.
 */
public class BackgroundSlot extends Slot {
    private Identifier noItemIcon;

    public BackgroundSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public BackgroundSlot setBackground(Identifier sprite) {
        this.noItemIcon = sprite;
        return this;
    }

    @Override
    public Identifier getNoItemIcon() {
        return noItemIcon;
    }
}
