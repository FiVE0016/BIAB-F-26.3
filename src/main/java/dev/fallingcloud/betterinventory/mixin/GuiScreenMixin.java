package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.client.ClientHooks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.1 MinecraftScreenMixin 的 26.3 重定向：setScreen 从 MinecraftClient 移到
 * {@link Gui#setScreen(Screen)}。替换物品栏屏幕的判定全部在
 * ClientHooks.onScreenOpening（1.4.1 官方逻辑，含创造栏与 Shift 分支），
 * 同时取代工程早前移植的 GuiSetScreenMixin。
 */
@Mixin(Gui.class)
public abstract class GuiScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void betterinventory$replaceInventoryScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (screen != null && ClientHooks.onScreenOpening(screen)) {
            ci.cancel();
        }
    }
}
