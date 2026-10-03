package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric stand-in for the original's cancellable ScreenEvent.Opening.
 *
 * <p>When vanilla is about to open its own InventoryScreen (the E key), cancel it
 * and ask the server to open BetterInventoryMenu instead. The server then pushes
 * the real menu back to the client, where MenuScreens picks BetterInventoryScreen.
 */
@Mixin(Gui.class)
public class GuiSetScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void betterinventory$openLoadoutInstead(Screen screen, CallbackInfo ci) {
        if (screen instanceof InventoryScreen) {
            ci.cancel();
            PacketDistributor.sendToServer(new BetterInventoryPayloads.OpenLoadout());
        }
    }
}
