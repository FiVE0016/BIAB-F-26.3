package dev.fallingcloud.betterinventory.client;

import net.minecraft.client.Minecraft;

/**
 * Client-side tick and render hooks.
 *
 * <p>Fabric has no MouseScrollingEvent and no cancellable ScreenEvent.Opening, so
 * those two behaviours from the original ClientEvents still need a mixin - see
 * STATUS.md. Scroll and screen-open handling are stubbed here so the rest compiles.
 */
public final class ClientEvents {
    private ClientEvents() {}

    public static void onClientTick(Minecraft client) {
        if (client.player == null) {
            return;
        }
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
