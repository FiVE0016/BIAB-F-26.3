package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.client.ClientCombatBridge;
import dev.fallingcloud.betterinventory.client.ClientHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.4.1 PlayerHandItemMixin 的 26.3 移植：getEquippedStack →
 * {@code getItemBySlot(EquipmentSlot)}；切换冷却重算的 getMainHandStack →
 * {@code tick()} 内的 {@code getMainHandItem()}（@ModifyExpressionValue → @Redirect）。
 *
 * <p>仅客户端：引用了 Minecraft/ClientHudState（工具架显示窗口），在专用服
 * 上不可加载，故归 client 组。
 */
@Mixin(Player.class)
public abstract class PlayerHandItemMixin {

    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void betterinventory$rackToolInDisplayWindows(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot == EquipmentSlot.MAINHAND) {
            ItemStack engaged = ClientCombatBridge.engagedWeapon();
            if (engaged != null || ClientHudState.toolDisplayWindow) {
                Minecraft minecraft = Minecraft.getInstance();
                if ((Object) this == minecraft.player) {
                    if (!minecraft.options.keyUse.isDown()) {
                        if (engaged != null) {
                            cir.setReturnValue(engaged);
                        } else if (BetterInventoryConfig.SHOW_TOOL_IN_HAND.get()) {
                            ItemStack tool = ClientHudState.activeTool;
                            if (tool != null && !tool.isEmpty()) {
                                cir.setReturnValue(tool);
                            }
                        }
                    }
                }
            }
        }
    }

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack betterinventory$realHandForSwitchCooldown(Player self) {
        if (ClientCombatBridge.engagedWeapon() == null) {
            return self.getMainHandItem();
        }
        return self == Minecraft.getInstance().player ? self.getInventory().getSelectedItem() : self.getMainHandItem();
    }
}
