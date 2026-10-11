package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.PlayerHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.1 ServerPlayerDeathMixin 的 26.3 移植：onDeath → {@code die(DamageSource)}。
 * 死亡前复位分页、死亡后把配装掉落物交给 PlayerHooks.collectDrops。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void betterinventory$resetTabBeforeDrops(DamageSource source, CallbackInfo ci) {
        PlayerHooks.onDeath((ServerPlayer) (Object) this);
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void betterinventory$dropLoadout(DamageSource source, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        PlayerHooks.collectDrops(self, self.level()::addFreshEntity);
    }
}
