package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryConfig.ScreenMode;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Hotkey handler for the inventory screen switch.
 *
 * <p>Plan B: one press swaps MOD to VANILLA and back. AUTO is the shipped
 * default and is deliberately not reachable from the key, so the key can never
 * silently drop a player back into the mode they did not ask for.
 */
public final class InventoryModeToggle {

    private InventoryModeToggle() {}

    public static void toggle(Minecraft client) {
        ScreenMode current = BetterInventoryConfig.SCREEN_MODE.get();
        ScreenMode next = current == ScreenMode.VANILLA ? ScreenMode.MOD : ScreenMode.VANILLA;
        BetterInventoryConfig.SCREEN_MODE.set(next);
        BetterInventoryConfig.writeClient();
        applyNow(client, next);
        LocalPlayer player = client.player;
        if (player != null) {
            player.sendOverlayMessage(Component.translatable(next == ScreenMode.VANILLA
                    ? "message.betterinventory.screen_mode.vanilla"
                    : "message.betterinventory.screen_mode.mod"));
        }
    }
    /**
     * Swaps whichever inventory screen is open right now, so the key is
     * visible without closing and reopening.
     *
     * <p>The two directions are deliberately asymmetric. Going to vanilla is a
     * local act: build the screen. Going to the mod screen is a request, because
     * the server owns the menu and pushes it back to us.
     *
     * <p>"Am I on the mod screen" is decided by class name, not instanceof.
     * Vanilla builds an InventoryScreen for the E key and then replaces itself
     * with the creative screen inside init() once the player has infinite
     * materials, so an instanceof check against InventoryScreen quietly stops
     * matching in creative mode. That was why switching back to the mod screen
     * did nothing.
     */
    private static void applyNow(Minecraft client, ScreenMode next) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        Screen open = client.gui.screen();
        if (open == null) {
            return;
        }
        boolean onModScreen = open.getClass().getName().contains("BetterInventoryScreen");
        if (next == ScreenMode.MOD) {
            if (onModScreen) {
                return;
            }
            PacketDistributor.sendToServer(new BetterInventoryPayloads.OpenLoadout());
        } else {
            if (!onModScreen) {
                return;
            }
            client.gui.setScreen(new InventoryScreen(player));
        }
    }
}

