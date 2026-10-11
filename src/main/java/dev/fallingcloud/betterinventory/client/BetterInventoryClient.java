package dev.fallingcloud.betterinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.client.hud.BetterInventoryHud;
import dev.fallingcloud.betterinventory.client.screen.BackpackScreen;
import dev.fallingcloud.betterinventory.client.screen.BetterInventoryScreen;
import dev.fallingcloud.betterinventory.client.screen.SettingsScreen;
import dev.fallingcloud.betterinventory.logic.CombatService;
import dev.fallingcloud.betterinventory.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * 1.4.1 client entrypoint.
 *
 * <p>26.3 adaptations: key bindings live under {@code client.keymapping.v1}
 * (KeyMappingHelper) and HUD layers use {@link HudElementRegistry}. Two 1.4.1
 * registrations are deferred: the vanilla_style built-in pack (26.3
 * fabric-resource-loader dropped the API) and the worn-backpack layer (26.3
 * replaced PlayerRenderer with AvatarRenderer and rewrote layer states) -
 * both return in unit F-2/G.
 */
public final class BetterInventoryClient implements ClientModInitializer {
    public static final KeyMapping OFFHAND_SELECTOR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.betterinventory.offhand_selector",
            InputConstants.Type.KEYBOARD,
            OffhandKey.defaultKey(),
            KeyMapping.Category.register(BetterInventory.id("inventory"))));
    public static final KeyMapping MARK_SAFE_ENTITY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.betterinventory.mark_safe_entity",
            InputConstants.Type.KEYBOARD,
            59,
            KeyMapping.Category.register(BetterInventory.id("inventory"))));
    public static final KeyMapping OPEN_VANILLA_INVENTORY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.betterinventory.open_vanilla_inventory",
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.register(BetterInventory.id("inventory"))));

    @Override
    public void onInitializeClient() {
        BetterInventoryConfig.readClient();
        ClientNet.register();
        PolymorphClientCompat.register();
        CombatService.engagedClient = player -> ClientHudState.activeTool != null;
        CombatService.betterCombatLookup = ClientCombatBridge::substituteForBetterCombat;
        MenuScreens.register(ModMenus.LOADOUT, BetterInventoryScreen::new);
        MenuScreens.register(ModMenus.BACKPACK, BackpackScreen::new);
        MenuScreens.register(ModMenus.SETTINGS, SettingsScreen::new);

        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                BetterInventory.id("tool_slot"),
                (graphics, delta) -> BetterInventoryHud.renderToolSlot(graphics, delta));
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                BetterInventory.id("offhand_selector"),
                (graphics, delta) -> BetterInventoryHud.renderOffhandSelector(graphics, delta));

        ClientEvents.register();
    }

    // -- 1.0.0-compat key/toggle, used by the not-yet-replaced mixins --------

    /** Unbound by default; the player picks a key in Options > Controls. */
    public static final KeyMapping TOGGLE_INVENTORY_MODE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.betterinventory.toggle_inventory_mode",
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.register(BetterInventory.id("inventory"))));
}
