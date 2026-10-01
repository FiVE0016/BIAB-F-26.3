package dev.fallingcloud.betterinventory;

import dev.fallingcloud.betterinventory.logic.FabricCommonEvents;
import dev.fallingcloud.betterinventory.logic.LoadoutStorage;
import dev.fallingcloud.betterinventory.logic.ItemDefaults;
import dev.fallingcloud.betterinventory.shim.RegisterPayloadHandlersEvent;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.registry.ModBlocks;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import dev.fallingcloud.betterinventory.registry.ModCreativeTab;
import dev.fallingcloud.betterinventory.registry.ModItems;
import dev.fallingcloud.betterinventory.registry.ModMenus;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/**
 * Fabric entry point for BetterInventory (Minecraft 26.3).
 *
 * <p>Keeps the original class name and {@link #id(String)} helper so the rest of the
 * codebase - which was written against {@code BetterInventory.MODID} - is untouched.
 */
public final class BetterInventory implements ModInitializer {
    public static final String MODID = "betterinventory";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        BetterInventoryConfig.load();

        // Touching the registry classes triggers their static initialisers, which
        // register every object immediately (Fabric has no deferred registration).
        ModItems.ITEMS.register(null);
        ModBlocks.BLOCKS.register(null);
        ModBlocks.BLOCK_ENTITIES.register(null);
        ModComponents.COMPONENTS.register(null);
        // ModMenus registers via static final fields; touch it to force init.
        ModMenus.LOADOUT.getClass();
        ModCreativeTab.TABS.register(null);

        ItemDefaults.register();
        BetterInventoryPayloads.register(new RegisterPayloadHandlersEvent());
        FabricCommonEvents.register();
        LoadoutStorage.register();

    }
}
