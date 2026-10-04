package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
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
 *
 * <p>In 26.3 the creative inventory is not opened directly by the E key: vanilla
 * opens an InventoryScreen and that screen swaps itself over inside init() once
 * it sees the player has infinite materials. Cancelling that first setScreen
 * kills the swap, so creative mode is handed back to vanilla untouched.
 *
 * <p>priority is 1100 instead of the default 1000 so this handler runs after
 * other mods; if one of them already cancelled setScreen this never runs.
 */
@Mixin(value = Gui.class, priority = 1100)
public class GuiSetScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void betterinventory$openLoadoutInstead(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof InventoryScreen)) {
            return;
        }
        // hasInfiniteMaterials() is the exact predicate vanilla init() uses for
        // the creative swap, so letting it through keeps the two in step.
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.hasInfiniteMaterials()) {
            return;
        }
        ci.cancel();
        PacketDistributor.sendToServer(new BetterInventoryPayloads.OpenLoadout());
    }
}
