package dev.fallingcloud.betterinventory.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Oversized stack counts (>999, from stack upgrades) render as a compact "1.5k"
 * instead of overflowing the slot.
 *
 * <p>Rewrites the incoming {@code text} parameter rather than the count local:
 * parameters are always present in the local variable table, so this matches far
 * more reliably than an {@code @At("STORE")} capture. Vanilla only formats the
 * count itself when {@code text} is null, and the {@code count != 1} guard is
 * already true for any oversized stack, so supplying the string here is
 * equivalent. Purely cosmetic, hence {@code require = 0} - a mapping change may
 * cost the nicety but must never break the mod.
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsCountMixin {
    @ModifyVariable(
            method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0)
    private String betterinventory$compactCount(String value, Font font, ItemStack stack, int x, int y, String text) {
        if (value == null && stack.getCount() > 999) {
            String formatted = String.format(java.util.Locale.ROOT, "%.1fk", stack.getCount() / 1000.0);
            return formatted.replace(".0k", "k");
        }
        return value;
    }
}
