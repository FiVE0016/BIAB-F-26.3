package dev.fallingcloud.betterinventory.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Widens the vanilla ItemStack save codec's count bound (1..99) so oversized
 * BetterInventory stacks (stack upgrades, HUNDRED stack mode) survive serialization.
 * The bound lives inside the CODEC record-builder lambda (lambda$static$3 in
 * 21.1.219 bytecode); widening is behavior-neutral for vanilla counts.
 */
@Mixin(ItemStack.class)
public class ItemStackCountCodecMixin {
    @ModifyArg(
            method = "lambda$static$3",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"),
            index = 1)
    private static int betterinventory$widenCountBound(int max) {
        return 99999;
    }
}
