package dev.fallingcloud.betterinventory.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Widens the vanilla ItemStack save codec's count bound (1..99) so oversized
 * BetterInventory stacks (stack upgrades, HUNDRED stack mode) survive serialization.
 * The bound lives inside the CODEC record-builder lambda; widening is
 * behavior-neutral for vanilla counts.
 *
 * <p>26.3 adaptation: the compiler-generated lambda holding the intRange call was
 * renumbered by the 26.3 compiler, so the injection target below was repointed to
 * the current index. {@code ExtraCodecs#intRange(int, int)} itself is unchanged.
 */
@Mixin(ItemStack.class)
public class ItemStackCountCodecMixin {

    @ModifyArg(
            method = "lambda$static$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"),
            index = 1)
    private static int betterinventory$widenCountBound(int max) {
        return 99999;
    }
}
