package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.menu.BackpackMenu;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.menu.SettingsMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取消 26.3 原版的槽位悬停高亮（16x16 精灵，只覆盖凹槽），改由 mod 自己的屏幕
 * 绘制 1.4.1 风格的 18x18 填充（{@code ClientHooks.paintSlotHighlight}）——
 * 1.21.1 原版高亮原本就是 18x18 填充，缩到 16x16 后在 1.4.1 的美术下会看起来
 * 缩向左上。仅对本模组的三个菜单生效，原版界面不受影响。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotHighlightMixin {

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void betterinventory$skipVanillaHighlightBack(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (betterinventory$isModScreen()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void betterinventory$skipVanillaHighlightFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (betterinventory$isModScreen()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean betterinventory$isModScreen() {
        Object menu = ((AbstractContainerScreen<?>) (Object) this).getMenu();
        return menu instanceof BetterInventoryMenu
                || menu instanceof BackpackMenu
                || menu instanceof SettingsMenu;
    }
}
