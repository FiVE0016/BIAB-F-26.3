package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.recipe.BackpackUpgradeRecipe;
import dev.fallingcloud.betterinventory.shim.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 1.4.1: the backpack upgrade recipe serializer. Registered through the shim
 * DeferredRegister like the other registries.
 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BetterInventory.MODID);

    public static final Supplier<RecipeSerializer<BackpackUpgradeRecipe>> BACKPACK_UPGRADE = RECIPES.register(
            "backpack_upgrade",
            () -> new RecipeSerializer<>(BackpackUpgradeRecipe.CODEC, BackpackUpgradeRecipe.STREAM_CODEC));

    private ModRecipes() {}
}
