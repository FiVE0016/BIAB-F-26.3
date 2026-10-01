package dev.fallingcloud.betterinventory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.fallingcloud.betterinventory.logic.AttributeJuggler;
import dev.fallingcloud.betterinventory.logic.CombatService;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Melee attacks use the rack sword/axe (per the tab's combat setting) whenever the
 * held item is not itself a weapon: base damage via a temporary attribute swap,
 * enchantments/durability/sweep via getWeaponItem + the sweep-check call, and
 * weapon-break cleanup routed to the rack instead of the real hands.
 *
 * <p>26.3 adaptation: {@code Player#attack(Entity)} has been hollowed out into a
 * pipeline (createAttackSource -> isSweepAttack -> doSweepAttack ->
 * itemAttackInteraction -> damageStatsAndHearts) and no longer touches the hand
 * slots directly. The two hand reads/writes we need moved into
 * {@code isSweepAttack} and {@code itemAttackInteraction}, so the WrapOperations
 * below now target those methods instead of {@code attack}.
 */
@Mixin(Player.class)
public abstract class PlayerCombatMixin extends LivingEntity {
    @Unique
    @Nullable
    private ItemStack betterinventory$weapon;

    protected PlayerCombatMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @WrapMethod(method = "attack")
    private void betterinventory$attackWithRackWeapon(Entity target, Operation<Void> original) {
        Player self = (Player) (Object) this;
        ItemStack weapon = CombatService.resolve(self);
        if (weapon.isEmpty()) {
            original.call(target);
            return;
        }
        betterinventory$weapon = weapon;
        try {
            AttributeJuggler.withVirtualMainhand(self, weapon, () -> {
                original.call(target);
                return null;
            });
        } finally {
            betterinventory$weapon = null;
            CombatService.postAttackCleanup(self);
        }
    }

    @Inject(method = "getWeaponItem", at = @At("HEAD"), cancellable = true)
    private void betterinventory$weaponItem(CallbackInfoReturnable<ItemStack> cir) {
        if (betterinventory$weapon != null) {
            cir.setReturnValue(betterinventory$weapon);
        }
    }

    /**
     * 26.3: the sweep check lives in {@code Player#isSweepAttack(ZZZ)}, which still
     * reads the main hand via {@code getItemInHand}. Feed it the rack weapon.
     */
    @WrapOperation(
            method = "isSweepAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack betterinventory$sweepWithWeapon(Player player, InteractionHand hand, Operation<ItemStack> original) {
        if (betterinventory$weapon != null && hand == InteractionHand.MAIN_HAND) {
            return betterinventory$weapon;
        }
        return original.call(player, hand);
    }

    /**
     * 26.3: the "clear the broken weapon out of the hand" write now happens inside
     * {@code Player#itemAttackInteraction(Entity, ItemStack, DamageSource, boolean)}.
     * Route it to the rack instead of the real hands.
     */
    @WrapOperation(
            method = "itemAttackInteraction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void betterinventory$brokenWeaponCleanup(Player player, InteractionHand hand, ItemStack stack, Operation<Void> original) {
        if (betterinventory$weapon != null) {
            // The broken "weapon" is the rack stack, not a hand item - clear it there.
            CombatService.clearBrokenWeapon(player, betterinventory$weapon);
            return;
        }
        original.call(player, hand, stack);
    }
}
