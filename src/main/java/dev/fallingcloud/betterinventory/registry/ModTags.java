package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    /** Items accepted by the reserved second personal upgrade slot. Empty for now. */
    public static final TagKey<Item> AUX_UPGRADES =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BetterInventory.MODID, "aux_upgrades"));

    private ModTags() {}
}
