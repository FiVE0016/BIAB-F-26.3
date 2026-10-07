package dev.fallingcloud.betterinventory.client;

import net.minecraft.client.Minecraft;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;

/**
 * Client-side tick and render hooks.
 *
 * <p>Alt + scroll cycling of the offhand carousel is handled by
 * {@code mixin.MouseScrollMixin}, which injects into MouseHandler.onScroll.
 * This class only tracks the Alt key and commits the pending selection on
 * release.
 *
 * <p>Replacing the vanilla survival inventory screen is handled by
 * {@code mixin.GuiSetScreenMixin} (registered under {@code client} in
 * betterinventory.mixins.json), not by this class - there is no
 * onScreenOpening hook here, and none is needed.
 */
public final class ClientEvents {
    private ClientEvents() {}

    public static void onClientTick(Minecraft client) {
        if (BetterInventoryClient.TOGGLE_INVENTORY_MODE.consumeClick()) {
            InventoryModeToggle.toggle(client);
        }
        if (client.player == null) {
            ClientHudState.selectorOpen = false;
            ClientHudState.progress = 0;
            ClientHudState.progressO = 0;
            return;
        }
        boolean down = BetterInventoryClient.OFFHAND_SELECTOR.isDown() && client.gui.screen() == null;
        var loadout = ClientHudState.loadout(client);
        if (down && !ClientHudState.selectorOpen) {
            ClientHudState.pendingIndex = loadout.activeOffhand;
        }
        if (!down && ClientHudState.selectorOpen) {
            if (ClientHudState.pendingIndex != loadout.activeOffhand) {
                PacketDistributor.sendToServer(new BetterInventoryPayloads.SelectOffhand(ClientHudState.pendingIndex));
                loadout.activeOffhand = ClientHudState.pendingIndex;
            }
        }
        ClientHudState.selectorOpen = down;
        ClientHudState.tick(client);
    }

    /**
     * Zoomed hover-item rendering.
     *
     * <p>TODO: wire this to {@code ZoomRender.draw(graphics, cx, cy, stack)} using the
     * hovered slot resolved from the current screen. The original did this from a
     * ScreenEvent.Render.Post hook, which Fabric cannot replace - it needs a mixin.
     */
    public static void onHudRender(Minecraft client, net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        if (!dev.fallingcloud.betterinventory.BetterInventoryConfig.ZOOM_ENABLED.get()
                || client.player == null) {
            return;
        }
    }
}
