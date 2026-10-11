package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.client.ClientHudState;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.1 ItemInHandRendererMixin 重定向部分的 26.3 重定向：HeldItemRenderer
 * 已移除，updateHeldItems → 客户端类 {@code FirstPersonHandsAndItems.tick(LocalPlayer)}，
 * 其中读取主手物品以驱动第一人称手部渲染。
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack betterinventory$showRackToolInHand(LocalPlayer player) {
        if (BetterInventoryConfig.SHOW_TOOL_IN_HAND.get()) {
            ItemStack tool = ClientHudState.activeTool;
            if (tool != null && !tool.isEmpty()) {
                return tool;
            }
        }
        return player.getMainHandItem();
    }
}
