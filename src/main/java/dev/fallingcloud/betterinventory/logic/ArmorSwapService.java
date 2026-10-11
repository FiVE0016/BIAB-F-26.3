package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * 1.4.1 armor swap: exchanges the worn armor set with the loadout's armor store.
 *
 * <p>26.3 adaptation: the curse check uses {@code EnchantmentTags#CURSE} via
 * {@code EnchantmentHelper#hasTag} (26.3 removed the per-enchantment has()
 * overloads; CURSE covers binding and vanishing), and {@code Platform#canEquip}
 * is the vanilla {@code getEquipmentSlotForItem} equality directly.
 */
public final class ArmorSwapService {
    private ArmorSwapService() {}

    public static boolean swap(ServerPlayer player) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        Inventory inventory = player.getInventory();
        boolean moved = false;

        for (int i = 0; i < PlayerLoadout.ARMOR_ORDER.length; i++) {
            EquipmentSlot type = PlayerLoadout.ARMOR_ORDER[i];
            ItemStack worn = player.getItemBySlot(type);
            ItemStack stored = loadout.armorStore.getStackInSlot(i);
            if ((!worn.isEmpty() || !stored.isEmpty())
                    && (worn.isEmpty() || player.isCreative() || !EnchantmentHelper.hasTag(worn, EnchantmentTags.CURSE))
                    && (stored.isEmpty() || player.getEquipmentSlotForItem(stored) == type)) {
                player.setItemSlot(type, stored);
                loadout.armorStore.setStackInSlot(i, worn);
                player.onEquipItem(type, worn, stored);
                moved = true;
            }
        }

        if (moved) {
            inventory.setChanged();
            loadout.dirty = true;
        }

        return moved;
    }
}
