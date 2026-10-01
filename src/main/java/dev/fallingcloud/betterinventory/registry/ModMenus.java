package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.menu.BackpackMenu;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.menu.SettingsMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Fabric menu registration.
 *
 * <p>In 26.3 the Fabric screen-handler API was renamed to {@code fabric-menu-api-v1}:
 * {@code ExtendedScreenHandlerType} became {@link ExtendedMenuType}, which is generic in
 * the screen-opening data type and needs an explicit {@code StreamCodec}. A menu with no
 * extra data uses the plain vanilla {@code MenuType}, which now also takes a feature-flag
 * set.
 */
public final class ModMenus {
    public static final MenuType<BetterInventoryMenu> LOADOUT =
            Registry.register(BuiltInRegistries.MENU, BetterInventory.id("loadout"),
                    new MenuType<>(BetterInventoryMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<BackpackMenu> BACKPACK =
            Registry.register(BuiltInRegistries.MENU, BetterInventory.id("backpack"),
                    new ExtendedMenuType<BackpackMenu, BackpackMenu.OpenData>(
                            BackpackMenu::create, BackpackMenu.OpenData.CODEC));

    public static final MenuType<SettingsMenu> SETTINGS =
            Registry.register(BuiltInRegistries.MENU, BetterInventory.id("settings"),
                    new ExtendedMenuType<SettingsMenu, Integer>(
                            (id, inv, tab) -> new SettingsMenu(id, inv, tab), ByteBufCodecs.VAR_INT));

    private ModMenus() {}
}
