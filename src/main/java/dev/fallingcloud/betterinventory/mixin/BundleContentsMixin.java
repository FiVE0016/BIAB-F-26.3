package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.StackUpgradeService;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.1 BundleContentsMixin 的 26.3 移植（@WrapOperation → @Redirect）。
 *
 * <p>1.4.1 在 getOccupancy 里把 ItemStack.getMaxCount() 钳回 vanilla 上限，
 * 防止堆叠升级让束带容量权重虚高。26.3 的等价物是私有静态方法
 * {@code getWeight(ItemInstance)}，其中调用 {@code ItemInstance.getMaxStackSize()}。
 */
@Mixin(BundleContents.class)
public abstract class BundleContentsMixin {

    @Redirect(method = "getWeight",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemInstance;getMaxStackSize()I"))
    private static int betterinventory$ordinaryStackFillsABundle(ItemInstance instance) {
        return Math.min(instance.getMaxStackSize(), StackUpgradeService.originalMax(instance.typeHolder().value()));
    }
}
