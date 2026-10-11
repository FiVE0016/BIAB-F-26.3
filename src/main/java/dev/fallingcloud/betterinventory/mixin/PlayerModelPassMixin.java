package dev.fallingcloud.betterinventory.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fallingcloud.betterinventory.client.ClientHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.1 PlayerRendererMixin 的 26.3 重定向（@WrapMethod → @Inject HEAD+RETURN
 * 拆包）。渲染自身模型的包裹点：PlayerEntityRenderer 已移除，且 submit 由
 * LivingEntityRenderer 声明、AvatarRenderer 仅继承——实机证实本环境 mixin 对
 * 继承方法的注入查找失败，故直接目标声明类，处理器内以
 * {@code instanceof AvatarRenderer + state instanceof AvatarRenderState} 守卫，
 * 只对玩家渲染路径生效。
 *
 * <p>渲染本地玩家（无屏幕打开、state.id 与本地玩家一致）时把
 * ClientHudState.toolDisplayWindow 置真，让 getItemBySlot 返回工具架武器；
 * 返回时恢复原值。带屏幕的物品栏玩家预览不包裹，与 1.4.1 行为一致。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerModelPassMixin {
    @Unique
    private boolean betterinventory$wrapped;
    @Unique
    private boolean betterinventory$previous;

    @Inject(method = "submit", at = @At("HEAD"))
    private void betterinventory$markOwnModelPassBegin(LivingEntityRenderState state, PoseStack pose,
                                                       SubmitNodeCollector collector, CameraRenderState camera,
                                                       CallbackInfo ci) {
        if (!((Object) this instanceof AvatarRenderer) || !(state instanceof AvatarRenderState avatar)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean own = minecraft.player != null && minecraft.gui.screen() == null
                && avatar.id == minecraft.player.getId();
        this.betterinventory$wrapped = own;
        if (own) {
            this.betterinventory$previous = ClientHudState.toolDisplayWindow;
            ClientHudState.toolDisplayWindow = true;
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void betterinventory$markOwnModelPassEnd(LivingEntityRenderState state, PoseStack pose,
                                                     SubmitNodeCollector collector, CameraRenderState camera,
                                                     CallbackInfo ci) {
        if (this.betterinventory$wrapped) {
            ClientHudState.toolDisplayWindow = this.betterinventory$previous;
            this.betterinventory$wrapped = false;
        }
    }
}
