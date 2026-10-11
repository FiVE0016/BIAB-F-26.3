package dev.fallingcloud.betterinventory.compat;

import dev.fallingcloud.betterinventory.platform.ModPresence;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SlateBuildingCompat {
    public static final String MODID = "slate_building";
    public static final TagKey<Item> TOOLBOXES = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("slate_building", "toolboxes")
    );
    private static volatile Boolean loaded;

    private SlateBuildingCompat() {}

    public static boolean loaded() {
        Boolean cached = loaded;
        if (cached == null) {
            cached = ModPresence.isLoaded(MODID);
            loaded = cached;
        }
        return cached;
    }

    /**
     * 26.3 removed {@code ItemStack#is(TagKey)}; tag membership now goes through
     * {@code Registry#getTagOrEmpty}, which returns {@code Iterable<Holder<T>>}.
     */
    public static boolean isToolbox(ItemStack stack) {
        if (stack.isEmpty() || !loaded()) {
            return false;
        }
        Item item = stack.getItem();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TOOLBOXES)) {
            if (holder.value() == item) {
                return true;
            }
        }
        return false;
    }
}
