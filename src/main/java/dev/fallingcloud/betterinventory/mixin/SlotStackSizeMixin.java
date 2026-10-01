package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.StackUpgradeService;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Holds every slot to the right ceiling.
 *
 * <p>Raising item defaults lifts {@code ItemStack#getMaxStackSize} globally, which
 * would otherwise let ANY container stack past vanilla — a chest slot would take 99
 * of a 64-stack item with no upgrade installed at all. This clamps each slot back
 * to the item's original size unless the slot belongs to the player's own inventory,
 * where the player's installed upgrade applies instead.
 *
 * <p>BetterInventory's own slots override {@code getMaxStackSize(ItemStack)} themselves and
 * so never reach this.
 */
@Mixin(Slot.class)
public abstract class SlotStackSizeMixin {
    @Shadow
    @Final
    public Container container;

    @Shadow
    public abstract int getMaxStackSize();

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void betterinventory$clampToOwner(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.min(this.getMaxStackSize(), StackUpgradeService.effectiveMax(this.container, stack)));
    }
}
