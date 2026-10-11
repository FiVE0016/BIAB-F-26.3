package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.inv.FurnaceState;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 portable furnace logic.
 *
 * <p>26.3 adaptation: {@code assemble} lost its RegistryAccess parameter,
 * fuel burn time comes from the {@link CookingFuel} component resolved
 * through a {@link LootContext} (vanilla's own furnace does the same via
 * {@code ResolvableInt#getFromItem}), and {@code FuelValues} no longer exists.
 */
public final class FurnaceService {
    private FurnaceService() {}

    @Nullable
    public static Smelt smelting(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        // 26.3: Level#getRecipeManager was replaced by RecipeAccess; the
        // RecipeManager itself lives on ServerLevel.
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        RecipeManager recipes = serverLevel.recipeAccess();
        return recipes.getRecipeFor(RecipeType.SMELTING, recipeInput, level)
                .map(holder -> {
                    SmeltingRecipe recipe = holder.value();
                    return new Smelt(recipe.assemble(recipeInput), recipe.cookingTime(), recipe.experience());
                })
                .filter(smelt -> !smelt.result().isEmpty())
                .orElse(null);
    }

    /** 26.3: burn time is resolved from the COOKING_FUEL component via a loot context. */
    public static int burnTime(ServerLevel level, ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        }
        LootContext ctx = new LootContext.Builder(
                new LootParams.Builder(level).create(LootContextParamSets.EMPTY))
                .create(java.util.Optional.empty());
        return Math.max(0, ResolvableInt.getFromItem(
                fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, ctx, 0));
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /** 26.3: an item is a fuel when it carries the COOKING_FUEL component. */
    public static boolean isFuel(ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL);
    }

    @Nullable
    private static Smelt recipeFor(FurnaceState furnace, Level level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        if (furnace.cachedInput.isEmpty() || !sameItem(furnace.cachedInput, input)) {
            furnace.cachedInput = input.copyWithCount(1);
            furnace.cachedSmelt = smelting(level, input);
        }
        return (Smelt) furnace.cachedSmelt;
    }

    private static boolean fits(ItemStack output, ItemStack result) {
        return output.isEmpty()
                || sameItem(output, result)
                && output.getCount() + result.getCount() <= StackUpgradeService.originalMax(result);
    }

    public static void tick(ServerPlayer player, PlayerLoadout loadout) {
        FurnaceState furnace = loadout.furnace;
        if (!loadout.hasFurnace()) {
            if (loadout.furnaceOpen) {
                loadout.furnaceOpen = false;
                loadout.dirty = true;
            }
            if (!furnace.isEmpty()) {
                eject(player, furnace);
            }
            furnace.lit = 0;
            furnace.cook = 0;
        } else {
            Level level = player.level();
            ItemStack input = furnace.items.getStackInSlot(FurnaceState.INPUT);
            ItemStack fuel = furnace.items.getStackInSlot(FurnaceState.FUEL);
            if (furnace.lit > 0) {
                furnace.lit--;
            }
            if (furnace.lit > 0 || !input.isEmpty() && !fuel.isEmpty()) {
                Smelt smelt = recipeFor(furnace, level, input);
                boolean canSmelt = smelt != null && fits(furnace.items.getStackInSlot(FurnaceState.OUTPUT), smelt.result());
                if (furnace.lit <= 0 && canSmelt) {
                    int burn = level instanceof ServerLevel serverLevel ? burnTime(serverLevel, fuel) : 0;
                    if (burn > 0) {
                        furnace.lit = burn;
                        furnace.litDuration = burn;
                        ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
                        ItemStack rest = fuel.copyWithCount(fuel.getCount() - 1);
                        furnace.items.setStackInSlot(FurnaceState.FUEL,
                                !rest.isEmpty() ? rest
                                        : (remainder == null ? ItemStack.EMPTY : remainder.create()));
                    }
                }
                if (furnace.lit > 0 && canSmelt) {
                    furnace.cookTotal = Math.max(1, smelt.time());
                    furnace.cook++;
                    if (furnace.cook >= furnace.cookTotal) {
                        furnace.cook = 0;
                        ItemStack output = furnace.items.getStackInSlot(FurnaceState.OUTPUT);
                        ItemStack result = smelt.result();
                        furnace.items.setStackInSlot(FurnaceState.OUTPUT,
                                output.isEmpty() ? result.copy() : output.copyWithCount(output.getCount() + result.getCount()));
                        furnace.items.setStackInSlot(FurnaceState.INPUT, input.copyWithCount(input.getCount() - 1));
                        furnace.xp = furnace.xp + smelt.experience();
                    }
                } else {
                    furnace.cook = 0;
                }
            } else if (furnace.cook > 0) {
                furnace.cook = Mth.clamp(furnace.cook - 2, 0, Math.max(furnace.cookTotal, 0));
            }
        }
    }

    public static void popExperience(ServerPlayer player, FurnaceState furnace) {
        float earned = furnace.xp;
        furnace.xp = 0.0F;
        int whole = Mth.floor(earned);
        float rest = earned - whole;
        if (rest != 0.0F && Math.random() < rest) {
            whole++;
        }
        if (whole > 0 && player.level() instanceof ServerLevel level) {
            ExperienceOrb.award(level, player.position(), whole);
        }
    }

    private static void eject(ServerPlayer player, FurnaceState furnace) {
        for (int i = 0; i < FurnaceState.SLOTS; i++) {
            ItemStack stack = furnace.items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                furnace.items.setStackInSlot(i, ItemStack.EMPTY);
                player.getInventory().add(stack);
            }
        }
        popExperience(player, furnace);
    }

    public record Smelt(ItemStack result, int time, float experience) {}
}
