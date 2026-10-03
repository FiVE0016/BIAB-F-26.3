package dev.fallingcloud.betterinventory.menu.slots;

import dev.fallingcloud.betterinventory.BetterInventory;
import net.minecraft.resources.Identifier;

/** Empty-slot outline icons (vanilla where available, our own otherwise). */
public final class IconSlots {
    public static final Identifier EMPTY_HOE = Identifier.withDefaultNamespace("container/slot/hoe");
    public static final Identifier EMPTY_SHOVEL = Identifier.withDefaultNamespace("container/slot/shovel");
    public static final Identifier EMPTY_PICKAXE = Identifier.withDefaultNamespace("container/slot/pickaxe");
    public static final Identifier EMPTY_AXE = Identifier.withDefaultNamespace("container/slot/axe");
    public static final Identifier EMPTY_SWORD = Identifier.withDefaultNamespace("container/slot/sword");
    public static final Identifier EMPTY_BACKPACK = BetterInventory.id("container/slot/backpack");
    public static final Identifier EMPTY_UPGRADE = BetterInventory.id("container/slot/upgrade");
    public static final Identifier EMPTY_STACK_UPGRADE = BetterInventory.id("container/slot/stack_upgrade");
    public static final Identifier EMPTY_OFFHAND = BetterInventory.id("container/slot/offhand");
    public static final Identifier EMPTY_CURIO = BetterInventory.id("container/slot/curio");

    public static final Identifier[] TOOL_ICONS = {
            EMPTY_HOE, EMPTY_SHOVEL, EMPTY_PICKAXE, EMPTY_PICKAXE, EMPTY_AXE, EMPTY_SWORD
    };

    private IconSlots() {}
}
