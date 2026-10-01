package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.StackUpgradeService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Enforces the player's stack upgrade across the entire player inventory.
 *
 * <p>{@code Container#getMaxStackSize} is an interface default hard-coded to 99 that
 * {@code Inventory} never overrides, so every slot of the real inventory is min'd
 * down to 99 no matter how large the item's own MAX_STACK_SIZE is. Mixin merges
 * these overrides into Inventory, which shadows the default for the player
 * inventory ONLY — chests, hoppers and every other Container keep vanilla's 99.
 *
 * <p>This is also what keeps things vanilla with no upgrade installed: the ceiling
 * on item defaults is global, but the limit enforced here is per-player.
 */
@Mixin(Inventory.class)
public abstract class InventoryStackSizeMixin {
    @Shadow
    @Final
    public Player player;

    public int getMaxStackSize() {
        return StackUpgradeService.capFor(this.player);
    }

    /**
     * Scaled per item, so a 16-stack item rises proportionally instead of jumping to
     * a 64-stack item's ceiling. This overload is the one {@code Inventory#add} uses,
     * so it governs pickups as well as GUI moves.
     */
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(StackUpgradeService.limitFor(this.player, stack), stack.getMaxStackSize());
    }
}
