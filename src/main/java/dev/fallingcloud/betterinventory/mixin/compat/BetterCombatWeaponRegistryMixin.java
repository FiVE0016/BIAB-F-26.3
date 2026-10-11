package dev.fallingcloud.betterinventory.mixin.compat;

import dev.fallingcloud.betterinventory.logic.CombatService;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 1.4.1 BetterCombatWeaponRegistryMixin 的 26.3 移植。@ModifyVariable 是核心
 * mixin 构造，仅需把描述符换成 26.3 mojang 名。工具架武器查询走
 * CombatService.betterCombatLookup（客户端在 BetterInventoryClient 接线）。
 */
@Pseudo
@Mixin(targets = "net/bettercombat/logic/WeaponRegistry", remap = false)
public abstract class BetterCombatWeaponRegistryMixin {

    @ModifyVariable(
            method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("HEAD"),
            argsOnly = true)
    private static ItemStack betterinventory$rackWeaponForLookup(ItemStack stack) {
        return CombatService.betterCombatLookup.apply(stack);
    }
}
