package dev.fallingcloud.betterinventory.api;

import dev.fallingcloud.betterinventory.compat.SlateBuildingCompat;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.4.1 public API surface for other mods. */
public final class BetterInventoryApi {
    private BetterInventoryApi() {}

    public static boolean hasToolboxSlot() {
        return SlateBuildingCompat.loaded();
    }

    public static ItemStack toolbox(Player player) {
        return player != null && hasToolboxSlot() ? PlayerLoadout.of(player).toolbox.getStackInSlot(0) : ItemStack.EMPTY;
    }

    public static boolean setToolbox(Player player, ItemStack stack) {
        if (player != null && !player.getAbilities().instabuild && hasToolboxSlot()) {
            ItemStack value = stack == null ? ItemStack.EMPTY : stack;
            PlayerLoadout loadout = PlayerLoadout.of(player);
            if (value.isEmpty() || value.getCount() == 1 && loadout.toolbox.isItemValid(0, value)) {
                loadout.toolbox.setStackInSlot(0, value);
                loadout.dirty = true;
                return true;
            }
            return false;
        }
        return false;
    }
}
