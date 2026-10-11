package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.client.CreativeInventory;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.4.1 CreativeScreenMixin 的 26.3 移植：mouseReleased(mouseX,mouseY,button) →
 * {@code mouseReleased(MouseButtonEvent)}（标记点击区间），setSelectedTab →
 * 私有方法 {@code selectTab(CreativeModeTab)}（取消 mod 标签点击）。
 *
 * <p>原 1.4.1 里的 getMaxCount 重定向已由工程既有 CreativeStackSizeMixin 覆盖
 * （26.3 的 slotClicked 满叠钳制），此处不再重复。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin {
    @Unique
    private boolean betterinventory$clicking;

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void betterinventory$clickBegins(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        this.betterinventory$clicking = true;
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void betterinventory$clickEnds(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        this.betterinventory$clicking = false;
    }

    @Inject(method = "selectTab", at = @At("HEAD"), cancellable = true)
    private void betterinventory$inventoryTab(CreativeModeTab tab, CallbackInfo ci) {
        if (this.betterinventory$clicking && CreativeInventory.tabClicked(tab)) {
            ci.cancel();
        }
    }
}
