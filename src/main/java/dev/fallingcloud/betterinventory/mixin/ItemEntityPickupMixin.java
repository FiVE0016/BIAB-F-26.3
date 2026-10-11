package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.GatherService;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.1 ItemEntityPickupMixin 的 26.3 移植：onPlayerCollision →
 * {@code playerTouch(Player)}，把拾取改道进采集枢纽（gather hubs）。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void betterinventory$routeToGatherHubs(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide()) {
            if (GatherService.onPickup(player, self)) {
                ci.cancel();
            }
        }
    }
}
