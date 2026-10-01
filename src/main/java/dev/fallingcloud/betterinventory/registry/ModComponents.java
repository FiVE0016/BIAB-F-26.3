package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.data.StoredItems;
import dev.fallingcloud.betterinventory.data.TabSettings;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import dev.fallingcloud.betterinventory.shim.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BetterInventory.MODID);

    public static final Supplier<DataComponentType<StoredItems>> BACKPACK_CONTENTS = COMPONENTS.register(
            "backpack_contents",
            () -> DataComponentType.<StoredItems>builder()
                    .persistent(StoredItems.CODEC)
                    .networkSynchronized(StoredItems.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    public static final Supplier<DataComponentType<StoredItems>> BACKPACK_UPGRADES = COMPONENTS.register(
            "backpack_upgrades",
            () -> DataComponentType.<StoredItems>builder()
                    .persistent(StoredItems.CODEC)
                    .networkSynchronized(StoredItems.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    public static final Supplier<DataComponentType<TabSettings>> BACKPACK_SETTINGS = COMPONENTS.register(
            "backpack_settings",
            () -> DataComponentType.<TabSettings>builder()
                    .persistent(TabSettings.CODEC)
                    .networkSynchronized(TabSettings.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    private ModComponents() {}
}
