package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.PlayerHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.4.1 PlayerToolChoiceMixin 的 26.3 移植：getBlockBreakingSpeed →
 * {@code getDestroySpeed}、canHarvest → {@code hasCorrectToolForDrops}，
 * 把工具架上的工具计入挖掘速度与可采判定。
 */
@Mixin(Player.class)
public abstract class PlayerToolChoiceMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void betterinventory$rackDigSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        float speed = PlayerHooks.rackDigSpeed(self, state, null, cir.getReturnValueF());
        cir.setReturnValue(PlayerHooks.wornToolFloor(self, state, null, speed));
    }

    @Inject(method = "hasCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
    private void betterinventory$rackHarvestCheck(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(PlayerHooks.harvestCheck((Player) (Object) this, state, cir.getReturnValueZ()));
    }
}
