package dev.fallingcloud.betterinventory.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Temporarily replaces the main-hand item's attribute contributions (item modifiers +
 * enchantment attribute effects) with a virtual item's, runs an action, then restores
 * everything. Used so rack tools/weapons get correct attribute-driven behavior
 * (attack damage, mining efficiency) without ever occupying the hand.
 */
public final class AttributeJuggler {
    private AttributeJuggler() {}

    public static <T> T withVirtualMainhand(Player player, ItemStack virtual, Supplier<T> action) {
        ItemStack main = player.getMainHandItem();
        AttributeMap map = player.getAttributes();
        List<Runnable> undo = new ArrayList<>();
        try {
            main.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                AttributeInstance instance = map.getInstance(attribute);
                if (instance != null && instance.hasModifier(modifier.id())) {
                    instance.removeModifier(modifier.id());
                    undo.add(() -> {
                        if (!instance.hasModifier(modifier.id())) {
                            instance.addTransientModifier(modifier);
                        }
                    });
                }
            });
            virtual.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                AttributeInstance instance = map.getInstance(attribute);
                if (instance != null && !instance.hasModifier(modifier.id())) {
                    instance.addTransientModifier(modifier);
                    undo.add(() -> instance.removeModifier(modifier.id()));
                }
            });
            return action.get();
        } finally {
            for (int i = undo.size() - 1; i >= 0; i--) {
                undo.get(i).run();
            }
        }
    }
}
