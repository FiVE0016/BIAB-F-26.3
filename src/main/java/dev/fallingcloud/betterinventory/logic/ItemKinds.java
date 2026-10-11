package dev.fallingcloud.betterinventory.logic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 1.4.1 item classification, adapted to 26.3.
 *
 * <p>The dedicated tool item classes (SwordItem/PickaxeItem/ShovelItem/AxeItem/
 * HoeItem/DiggerItem) no longer exist in 26.3 - tool/weapon identity moved to
 * data components, so classification goes through tags plus the TOOL component.
 * Tag membership uses {@code Registry#getTagOrEmpty} iteration because 26.3
 * removed {@code ItemStack#is(TagKey)}.
 */
public final class ItemKinds {
    private static final TagKey<Item> MINING_TOOL_TOOLS = common("tools/mining_tool");
    private static final TagKey<Item> MELEE_WEAPON_TOOLS = common("tools/melee_weapon");
    private static final TagKey<Item> RANGED_WEAPON_TOOLS = common("tools/ranged_weapon");
    private static final TagKey<Item> TOOLS_SPEAR = common("tools/spear");
    private static final TagKey<Item> TOOLS_MACE = common("tools/mace");
    private static final TagKey<Item> TOOLS_SHIELD = common("tools/shield");

    private ItemKinds() {}

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    private static boolean is(ItemStack stack, TagKey<Item> tag) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            if (holder.value() == item) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHoe(ItemStack stack) {
        return is(stack, ItemTags.HOES);
    }

    public static boolean isShovel(ItemStack stack) {
        return is(stack, ItemTags.SHOVELS);
    }

    public static boolean isPickaxe(ItemStack stack) {
        return is(stack, ItemTags.PICKAXES);
    }

    public static boolean isAxe(ItemStack stack) {
        return is(stack, ItemTags.AXES);
    }

    public static boolean isMiningTool(ItemStack stack) {
        return stack.has(DataComponents.TOOL)
                || is(stack, MINING_TOOL_TOOLS)
                || isHoe(stack)
                || isShovel(stack)
                || isPickaxe(stack)
                || isAxe(stack);
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof TridentItem || item instanceof MaceItem || is(stack, ItemTags.SWORDS)) {
            return true;
        }
        if (is(stack, MELEE_WEAPON_TOOLS) || is(stack, TOOLS_SPEAR) || is(stack, TOOLS_MACE)) {
            return true;
        }
        return !isMiningTool(stack)
                && !(item instanceof BowItem)
                && !(item instanceof CrossbowItem)
                && !is(stack, RANGED_WEAPON_TOOLS)
                && !is(stack, TOOLS_SHIELD)
                && addsAttackDamage(stack);
    }

    private static boolean addsAttackDamage(ItemStack stack) {
        // getOrDefault already merges the item's default modifiers in 26.3, so no
        // separate item-default lookup is needed.
        ItemAttributeModifiers modifiers =
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        boolean[] found = new boolean[]{false};
        modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_DAMAGE.value() && modifier.amount() > 0.0) {
                found[0] = true;
            }
        });
        return found[0];
    }
}
