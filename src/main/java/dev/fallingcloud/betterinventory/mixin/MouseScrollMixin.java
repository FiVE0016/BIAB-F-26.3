package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.client.ClientHudState;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Alt + scroll cycles the offhand carousel selection.
 *
 * <p>Target verified by javap against minecraft-client.jar in step 38:
 * {@code public void net.minecraft.client.MouseHandler.onScroll(long, double, double)}.
 * Injecting at HEAD and cancelling is the Fabric equivalent of what the
 * original did with NeoForge's MouseScrollingEvent, so the scroll never
 * reaches hotbar selection underneath.
 *
 * <p>ClientHudState.selectorOpen is maintained once per tick by ClientEvents
 * and is true only while Alt is held with no screen open, so this handler does
 * not re-check those conditions itself. The slot count is read from the store
 * rather than hardcoded so it stays correct if the carousel ever resizes.
 */
@Mixin(MouseHandler.class)
public class MouseScrollMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void betterinventory$offhandScroll(long window, double deltaX, double deltaY, CallbackInfo ci) {
        if (!ClientHudState.selectorOpen || deltaY == 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        PlayerLoadout loadout = ModAttachments.get(player);
        int slots = loadout.offhandStore.getSlots();
        if (slots <= 0) {
            return;
        }
        ClientHudState.pendingIndex =
                Math.floorMod(ClientHudState.pendingIndex + (int) Math.signum(deltaY), slots);
        ci.cancel();
    }
}
