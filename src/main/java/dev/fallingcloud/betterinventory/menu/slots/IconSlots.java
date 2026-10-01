package dev.fallingcloud.betterinventory.menu.slots;

import dev.fallingcloud.betterinventory.BetterInventory;
import net.minecraft.resources.Identifier;

/** Empty-slot outline icons (vanilla where available, our own otherwise). */
public final class IconSlots {
    public static final Identifier EMPTY_HOE = Identifier.withDefaultNamespace("item/empty_slot_hoe");
    public static final Identifier EMPTY_SHOVEL = Identifier.withDefaultNamespace("item/empty_slot_shovel");
    public static final Identifier EMPTY_PICKAXE = Identifier.withDefaultNamespace("item/empty_slot_pickaxe");
    public static final Identifier EMPTY_AXE = Identifier.withDefaultNamespace("item/empty_slot_axe");
    public static final Identifier EMPTY_SWORD = Identifier.withDefaultNamespace("item/empty_slot_sword");
    public static final Identifier EMPTY_BACKPACK = BetterInventory.id("item/empty_slot_backpack");
    public static final Identifier EMPTY_UPGRADE = BetterInventory.id("item/empty_slot_upgrade");
    public static final Identifier EMPTY_STACK_UPGRADE = BetterInventory.id("item/empty_slot_stack_upgrade");
    public static final Identifier EMPTY_OFFHAND = BetterInventory.id("item/empty_slot_offhand");
    public static final Identifier EMPTY_CURIO = BetterInventory.id("item/empty_slot_curio");

    public static final Identifier[] TOOL_ICONS = {
            EMPTY_HOE, EMPTY_SHOVEL, EMPTY_PICKAXE, EMPTY_PICKAXE, EMPTY_AXE, EMPTY_SWORD
    };

    private IconSlots() {}
}
