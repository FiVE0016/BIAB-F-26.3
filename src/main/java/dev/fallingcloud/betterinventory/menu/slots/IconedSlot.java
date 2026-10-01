package dev.fallingcloud.betterinventory.menu.slots;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A vanilla {@link Slot} that can show a placeholder sprite when empty.
 *
 * <p>26.3 removed {@code Slot#setBackground(Identifier, Identifier)} along with
 * {@code InventoryMenu.BLOCK_ATLAS}; the supported way is to override
 * {@link #getNoItemIcon()}.
 */
public class IconedSlot extends Slot {
    private Identifier noItemIcon;

    public IconedSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public void setBackground(Identifier sprite) {
        this.noItemIcon = sprite;
    }

    @Override
    public Identifier getNoItemIcon() {
        return noItemIcon;
    }

    @Override
    public ItemStack getItem() {
        return super.getItem();
    }
}
