package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.StackUpgradeService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Holds the creative inventory to the player's real stack ceiling.
 *
 * <p>Item defaults are deliberately raised to the largest upgrade tier so that
 * upgraded stacks stay equal to ordinary ones, and the player inventory, every
 * slot and {@code doClick} are all clamped back afterwards. The creative screen
 * was missed: {@code slotClicked} reads {@code ItemStack#getMaxStackSize} at six
 * places to size a grabbed stack, and with the raised default that hands out the
 * full ceiling - a thousand-odd items from one hotkey press.
 *
 * <p>Upstream states the intended behaviour plainly: creative grabs must stay
 * exactly vanilla when no stack upgrade is installed. {@code limitFor} resolves
 * the same ceiling the rest of the mod uses, and it tolerates a null player, so
 * this cannot throw.
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeStackSizeMixin {

    @Redirect(
            method = "slotClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private int betterinventory$clampCreativeMax(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), StackUpgradeService.limitFor(Minecraft.getInstance().player, stack));
    }
}