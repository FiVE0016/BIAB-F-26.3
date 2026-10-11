package dev.fallingcloud.betterinventory.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fallingcloud.betterinventory.client.ClientHudState;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.1 ItemInHandRendererMixin 包裹部分的 26.3 重定向：renderItem →
 * {@code FirstPersonHandsAndItemsRenderer.submitHandsWithItems}
 * （@WrapMethod → @Inject HEAD+RETURN 拆包）。手部渲染全程标记
 * toolDisplayWindow，让 getItemBySlot 在此期间返回工具架武器。
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsRendererMixin {

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void betterinventory$markHandPassBegin(float partialTick, PoseStack pose, SubmitNodeCollector collector,
                                                   PlayerRenderState player, FirstPersonHandsAndItemsRenderState state,
                                                   CallbackInfo ci) {
        ClientHudState.toolDisplayWindow = true;
    }

    @Inject(method = "submitHandsWithItems", at = @At("RETURN"))
    private void betterinventory$markHandPassEnd(float partialTick, PoseStack pose, SubmitNodeCollector collector,
                                                 PlayerRenderState player, FirstPersonHandsAndItemsRenderState state,
                                                 CallbackInfo ci) {
        ClientHudState.toolDisplayWindow = false;
    }
}
