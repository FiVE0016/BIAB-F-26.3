package dev.fallingcloud.betterinventory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import dev.fallingcloud.betterinventory.registry.ModRecipes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * 1.4.1 backpack upgrade recipe: crafting the next backpack tier keeps the old
 * backpack's contents, installed upgrades, tab settings and custom name.
 *
 * <p>26.3 adaptation: {@link RecipeSerializer} is now a record of (MapCodec,
 * StreamCodec), {@link ShapedRecipe} takes CommonInfo/CraftingBookInfo/Pattern/
 * ItemStackTemplate, and {@code assemble} lost its RegistryAccess parameter.
 * Pattern/result copies are stored because the parent's fields are private.
 */
public class BackpackUpgradeRecipe extends ShapedRecipe {
    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public BackpackUpgradeRecipe(Recipe.CommonInfo common, CraftingRecipe.CraftingBookInfo book,
            ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(common, book, pattern, result);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack upgraded = super.assemble(input);
        for (int i = 0; i < input.size(); i++) {
            ItemStack old = input.getItem(i);
            if (old.getItem() instanceof BackpackItem) {
                carryOver(old, upgraded);
                break;
            }
        }
        return upgraded;
    }

    public static void carryOver(ItemStack from, ItemStack to) {
        copy(from, to, ModComponents.BACKPACK_CONTENTS.get());
        copy(from, to, ModComponents.BACKPACK_UPGRADES.get());
        copy(from, to, ModComponents.BACKPACK_SETTINGS.get());
        copy(from, to, DataComponents.CUSTOM_NAME);
    }

    private static <T> void copy(ItemStack from, ItemStack to, DataComponentType<T> type) {
        T value = from.get(type);
        if (value != null) {
            to.set(type, value);
        }
    }

    public static final MapCodec<BackpackUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("show_notification", true)
                            .forGetter(BackpackUpgradeRecipe::showNotification),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC)
                            .forGetter(BackpackUpgradeRecipe::category),
                    Codec.STRING.optionalFieldOf("group", "").forGetter(BackpackUpgradeRecipe::group),
                    ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                    ItemStackTemplate.MAP_CODEC.fieldOf("result").forGetter(r -> r.result)
            ).apply(instance, (showNotification, category, group, pattern, result) ->
                    new BackpackUpgradeRecipe(
                            new Recipe.CommonInfo(showNotification),
                            new CraftingRecipe.CraftingBookInfo(category, group),
                            pattern, result))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, BackpackUpgradeRecipe::showNotification,
                    CraftingBookCategory.STREAM_CODEC, BackpackUpgradeRecipe::category,
                    ByteBufCodecs.STRING_UTF8, BackpackUpgradeRecipe::group,
                    ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
                    ItemStackTemplate.STREAM_CODEC, r -> r.result,
                    (showNotification, category, group, pattern, result) ->
                            new BackpackUpgradeRecipe(
                                    new Recipe.CommonInfo(showNotification),
                                    new CraftingRecipe.CraftingBookInfo(category, group),
                                    pattern, result)
            );

    /**
     * 26.3: RecipeSerializer is a final record, so the generic return type cannot be
     * covariant the way it was in 1.21.1 - the parent's exact signature is kept and
     * the concrete serializer is smuggled through an erasure cast.
     */
    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRecipes.BACKPACK_UPGRADE.get();
    }
}
