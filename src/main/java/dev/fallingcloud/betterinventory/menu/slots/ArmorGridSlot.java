package dev.fallingcloud.betterinventory.menu.slots;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** A vanilla-equivalent armor slot placed in the 2x2 armor grid. */
public class ArmorGridSlot extends IconedSlot {
    private final Player owner;
    private final EquipmentSlot type;

    public ArmorGridSlot(Inventory inventory, Player owner, EquipmentSlot type, int slot, int x, int y, Identifier emptyIcon) {
        super(inventory, slot, x, y);
        this.owner = owner;
        this.type = type;
        setBackground(emptyIcon);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        var equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        return equippable == null || equippable.slot() == type;
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack stack = getItem();
        return (stack.isEmpty() || player.isCreative() || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))
                && super.mayPickup(player);
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        owner.onEquipItem(type, oldStack, newStack);
        super.setByPlayer(newStack, oldStack);
    }
}
