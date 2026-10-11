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
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;

/**
 * 1.4.1 client entrypoint.
 *
 * <p>26.3 adaptations: key bindings live under {@code client.keymapping.v1}
 * (KeyMappingHelper) and HUD layers use {@link HudElementRegistry}; the worn
 * backpack layer registers through
 * {@link LivingEntityRenderLayerRegistrationCallback} (AvatarRenderer), and the
 * vanilla_style built-in pack keeps using ResourceManagerHelper - still present
 * in the 26.3 fabric-resource-loader build.
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

        // G-2: vanilla_style 内置资源包（可见、默认关闭）。26.3 版
        // fabric-resource-loader 仍提供 ResourceManagerHelper，包体在
        // resources/resourcepacks/vanilla_style。
        FabricLoader.getInstance().getModContainer(BetterInventory.MODID).ifPresent(container ->
                ResourceManagerHelper.registerBuiltinResourcePack(
                        BetterInventory.id("vanilla_style"),
                        container,
                        Component.translatable("Better Inventory - Vanilla Style"),
                        ResourcePackActivationType.NORMAL));

        // F-2d：背着背包渲染层。26.3 用 AvatarRenderer 替代 PlayerRenderer，
        // 渲染层注册走 fabric 的 LivingEntityRenderLayerRegistrationCallback。
        LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (entityType == EntityTypes.PLAYER) {
                        helper.register(backpackLayer(renderer));
                    }
                });

        ClientEvents.register();
    }

    /**
     * fabric 回调给出的 renderer 是通配类型；玩家渲染器（AvatarRenderer）实际实现
     * {@code RenderLayerParent<AvatarRenderState, PlayerModel>}，此处收窄为具体类型。
     */
    private static BackpackLayer backpackLayer(LivingEntityRenderer<?, ?, ?> renderer) {
        @SuppressWarnings("unchecked")
        RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                (RenderLayerParent<AvatarRenderState, PlayerModel>) (RenderLayerParent<?, ?>) renderer;
        return new BackpackLayer(parent);
    }
}
