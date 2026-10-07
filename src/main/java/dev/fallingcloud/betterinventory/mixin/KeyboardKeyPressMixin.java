package dev.fallingcloud.betterinventory.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fallingcloud.betterinventory.client.BetterInventoryClient;
import dev.fallingcloud.betterinventory.client.InventoryModeToggle;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the screen-mode key work while a screen is open.
 *
 * <p>KeyboardHandler.keyPress is the single door every keyboard event passes
 * through. Its body forks on whether a screen is open: when one is, the event
 * goes to Screen.keyPressed and then the method returns, never reaching the
 * branch that increments KeyMapping.clickCount. That is why a tick loop that
 * polls consumeClick() goes deaf the moment the inventory opens.
 *
 * <p>Injecting at HEAD puts us before that fork, so the key is seen either way.
 * Guards, in order: press only so release and auto-repeat do not fire it; a
 * screen must be open because with no screen the tick path already handles it
 * and we must not act twice; never inside KeyBindsScreen or the player could
 * not bind the key at all; never while the screen has captured text input.
 */
@Mixin(value = KeyboardHandler.class, priority = 1100)
public class KeyboardKeyPressMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void betterinventory$toggleScreenMode(long windowHandle, int action, KeyEvent event, CallbackInfo ci) {
        if (action != InputConstants.PRESS) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Screen open = client.gui.screen();
        if (open == null) {
            return;
        }
        if (open instanceof KeyBindsScreen) {
            return;
        }
        if (open.isInputCaptured()) {
            return;
        }
        if (!BetterInventoryClient.TOGGLE_INVENTORY_MODE.matches(event)) {
            return;
        }
        ci.cancel();
        InventoryModeToggle.toggle(client);
    }
}
