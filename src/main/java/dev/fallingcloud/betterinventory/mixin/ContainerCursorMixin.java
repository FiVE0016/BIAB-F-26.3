package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.StackUpgradeService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the cursor honest about stack size.
 *
 * <p>Two spots in {@code doClick} bypass the slot's own limit:
 * <ul>
 *   <li>Picking a slot up whole passes {@code Integer.MAX_VALUE} as the limit, so an
 *       oversized stack would come onto the cursor entire. Clamping it means that
 *       when the ceiling drops (upgrade removed or downgraded), grabbing a too-large
 *       stack takes only what you may now hold and leaves the remainder behind.</li>
 *   <li>Cursor merging, creative middle-click clone and quick-craft all read the raw
 *       {@code ItemStack#getMaxStackSize}, which our raised global ceiling would
 *       otherwise hand a full 1024.</li>
 * </ul>
 *
 * <p>26.3 adaptation: {@code AbstractContainerMenu#doClick} gained a
 * {@link ContainerInput} parameter (an enum that replaces the old {@code ClickType})
 * in front of the {@code Player}. {@code cursorMax} is a {@code @Redirect}, whose
 * handler signature is the target invocation's arguments followed by the enclosing
 * method's arguments, so the new parameter has to be mirrored here.
 * {@code clampPickup} hooks {@code Slot#tryRemove(int, int, Player)}, whose signature
 * is unchanged, so it needs no adaptation.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerCursorMixin {

    @Redirect(
            method = "doClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private int betterinventory$cursorMax(ItemStack stack, int slotId, int button, ContainerInput input, Player player) {
        return Math.min(stack.getMaxStackSize(), StackUpgradeService.limitFor(player, stack));
    }

    @ModifyArg(
            method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;tryRemove(IILnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"),
            index = 1)
    private int betterinventory$clampPickup(int count, int limit, Player player) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack carried = self.getCarried();
        int cap = carried.isEmpty()
                ? StackUpgradeService.capFor(player)
                : StackUpgradeService.limitFor(player, carried);
        return Math.min(limit, Math.max(0, cap - carried.getCount()));
    }
}
