package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.client.ClientCombatBridge;
import dev.fallingcloud.betterinventory.client.ClientHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.4.1 PlayerHandItemMixin 的 getItemBySlot 注入，26.3 拆到 LivingEntity：
 * getItemBySlot 由 LivingEntity 声明、Player 仅继承，mixin 对继承方法的
 * 注入查找在本环境失败（实机日志 InvalidInjectionException），故直接目标
 * 声明类。语义不变：只对本地玩家的主手生效。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHandItemMixin {

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
}
