package dev.fallingcloud.betterinventory.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.4.1 Polymorph compat: asks Polymorph for the player's chosen crafting
 * recipe in the BetterInventory grid (all reflective).
 */
public final class PolymorphCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("betterinventory-polymorph");
    private static boolean checked;
    private static boolean available;
    private static Method getInstance;
    private static Method getRecipeManager;
    private static Method getPlayerRecipe;

    private PolymorphCompat() {}

    public static boolean loaded() {
        init();
        return available;
    }

    private static void init() {
        if (!checked) {
            checked = true;
            if (FabricLoader.getInstance().isModLoaded("polymorph")) {
                try {
                    Class<?> api = Class.forName("com.illusivesoulworks.polymorph.api.PolymorphApi");
                    Class<?> manager = Class.forName("com.illusivesoulworks.polymorph.api.common.base.IPolymorphRecipeManager");
                    getInstance = api.getMethod("getInstance");
                    getRecipeManager = api.getMethod("getRecipeManager");
                    getPlayerRecipe = manager.getMethod("getPlayerRecipe",
                            AbstractContainerMenu.class, RecipeType.class, RecipeInput.class, Level.class, Player.class);
                    available = true;
                    LOGGER.info("Polymorph detected - recipe choice enabled in the BetterInventory crafting grid");
                } catch (ReflectiveOperationException e) {
                    LOGGER.warn("Polymorph detected but its API did not match; recipe choice disabled", e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<RecipeHolder<CraftingRecipe>> craftingRecipe(
            AbstractContainerMenu menu, CraftingInput input, Level level, Player player) {
        init();
        if (!available) {
            return null;
        }
        try {
            Object manager = getRecipeManager.invoke(getInstance.invoke(null));
            return (Optional<RecipeHolder<CraftingRecipe>>) getPlayerRecipe.invoke(
                    manager, menu, RecipeType.CRAFTING, input, level, player);
        } catch (InvocationTargetException | IllegalAccessException | RuntimeException e) {
            available = false;
            LOGGER.warn("Polymorph's recipe choice failed in the BetterInventory crafting grid; disabled", e);
            return null;
        }
    }
}
