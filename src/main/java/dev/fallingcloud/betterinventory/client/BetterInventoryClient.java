package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.client.screen.BackpackScreen;
import dev.fallingcloud.betterinventory.client.screen.BetterInventoryScreen;
import dev.fallingcloud.betterinventory.client.screen.SettingsScreen;
import dev.fallingcloud.betterinventory.client.hud.BetterInventoryHud;
import dev.fallingcloud.betterinventory.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Fabric client entry point.
 *
 * <p>26.3 renamed several of these: screen registration is vanilla {@link MenuScreens}
 * again, key bindings live under {@code client.keymapping}, and HUD layers go through
 * {@link HudElementRegistry} instead of the removed {@code HudRenderCallback}.
 */
public final class BetterInventoryClient implements ClientModInitializer {
    // 26.3: KeyMapping takes a plain key code plus a Category object; the old
    // (String, Type, int, String) form and InputConstants.Type.KEYSYM are both gone.
    public static final KeyMapping OFFHAND_SELECTOR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.betterinventory.offhand_selector",
            InputConstants.KEY_LALT,
            KeyMapping.Category.register(dev.fallingcloud.betterinventory.BetterInventory.id("inventory"))));

    @Override
    public void onInitializeClient() {
        ItemTooltips.register();
        BetterInventoryConfig.readClient();

        MenuScreens.register(ModMenus.LOADOUT, BetterInventoryScreen::new);
        MenuScreens.register(ModMenus.BACKPACK, BackpackScreen::new);
        MenuScreens.register(ModMenus.SETTINGS, SettingsScreen::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.onClientTick(client));

        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                BetterInventory.id("tool_slot"),
                (graphics, delta) -> BetterInventoryHud.renderToolSlot(graphics, delta));
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                BetterInventory.id("offhand_selector"),
                (graphics, delta) -> BetterInventoryHud.renderOffhandSelector(graphics, delta));
    }

    /** Unbound by default; the player picks a key in Options > Controls. */
    public static final KeyMapping TOGGLE_INVENTORY_MODE = new KeyMapping(
            "key.betterinventory.toggle_inventory_mode",
            InputConstants.UNKNOWN.getValue(),
            OFFHAND_SELECTOR.getCategory());

    static {
        KeyMappingHelper.registerKeyMapping(TOGGLE_INVENTORY_MODE);
    }
}

