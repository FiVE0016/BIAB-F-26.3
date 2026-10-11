package dev.fallingcloud.betterinventory.mixin.compat;

import dev.fallingcloud.betterinventory.logic.CombatService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.1 BetterCombatAttackHelperMixin 的 26.3 移植
 * （@ModifyExpressionValue → @Redirect，MixinExtras 离线不可用）。
 *
 * <p>让 bettercombat 的攻击计算把工具架武器当作主手物品。
 * @Pseudo + BetterCombatMixinPlugin 门控：bettercombat 未装时静默跳过。
 */
@Pseudo
@Mixin(targets = "net/bettercombat/logic/PlayerAttackHelper", remap = false)
public abstract class BetterCombatAttackHelperMixin {

    @Redirect(method = "getCurrentAttack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack betterinventory$rackWeaponAsMainHand(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack rack = CombatService.resolve(player);
        return rack.isEmpty() ? player.getMainHandItem() : rack;
    }
}
