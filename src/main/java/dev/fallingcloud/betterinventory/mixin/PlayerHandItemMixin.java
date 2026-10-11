package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.client.ClientCombatBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.1 PlayerHandItemMixin 的切换冷却重定向部分（@ModifyExpressionValue →
 * @Redirect），目标 tick() 内 Player.getMainHandItem()（常量池 owner 已用
 * javap 核验）。getItemBySlot 注入已拆到 LivingEntityHandItemMixin（继承方法
 * 需直接目标声明类）。
 *
 * <p>仅客户端：引用了 Minecraft/ClientHudState（工具架显示窗口），在专用服
 * 上不可加载，故归 client 组。
 */
@Mixin(Player.class)
public abstract class PlayerHandItemMixin {

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
