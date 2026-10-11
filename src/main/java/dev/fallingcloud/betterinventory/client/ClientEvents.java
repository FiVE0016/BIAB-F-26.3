package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

/**
 * 1.4.1 client event wiring: tick phases, screen render/click hooks, attack
 * hooks and the ClientSelfTest tick.
 */
public final class ClientEvents {
    private static final Identifier OPEN_WINDOW = BetterInventory.id("open_tool_display_window");
    private static final Identifier CLOSE_WINDOW = BetterInventory.id("close_tool_display_window");

    private ClientEvents() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.addPhaseOrdering(OPEN_WINDOW, Event.DEFAULT_PHASE);
        ClientTickEvents.END_CLIENT_TICK.addPhaseOrdering(Event.DEFAULT_PHASE, CLOSE_WINDOW);
        ClientTickEvents.END_CLIENT_TICK.register(OPEN_WINDOW, client -> ClientHooks.openToolDisplayWindow());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientHooks.onClientTick());
        ClientTickEvents.END_CLIENT_TICK.register(CLOSE_WINDOW, client -> ClientHooks.closeToolDisplayWindow());
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
                // 26.3: afterRender -> afterExtract (render pipeline is extract-based).
                ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                        ClientHooks.onScreenRendered(s, graphics, mouseX, mouseY));
                // 26.3: the callback now receives a MouseButtonEvent instead of x/y/button.
                ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
                        !ClientHooks.onScreenClicked(s, event.x(), event.y(), event.button()));
            }
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            ClientHooks.onAttack(player);
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            ClientHooks.onLeftClickBlock(player, level, pos);
            return InteractionResult.PASS;
        });
        // F-2b 接线：selftest 属性开启时跑 ClientSelfTest 每 tick 自检。
        if (ClientSelfTest.enabled()) {
            ClientTickEvents.END_CLIENT_TICK.register(ClientSelfTest::tick);
        }
    }
}
