package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.data.StoredItems;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import dev.fallingcloud.betterinventory.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 1.4.1 self test: opt-in (system property {@code betterinventory.selftest})
 * scenario checks run on login, covering tab routing, gather-hub merging and
 * the backpack-upgrade-keeps-contents recipe.
 *
 * <p>26.3 adaptation: {@code assemble} lost its RegistryAccess parameter,
 * recipe lookup is {@code RecipeManager#byKey(ResourceKey)}, getIngredients()
 * yields Optionals, quickMove is {@code quickMoveStack}, and Inventory slot
 * access goes through getItem/setItem.
 */
public final class SelfTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int pass;
    private static int fail;

    private SelfTest() {}

    public static void onLogin(ServerPlayer player) {
        if (Boolean.getBoolean("betterinventory.selftest")) {
            player.level().getServer().execute(() -> run(player));
        }
    }

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            LOGGER.info("[BI selftest] PASS {} ({})", name, detail);
        } else {
            fail++;
            LOGGER.error("[BI selftest] FAIL {} ({})", name, detail);
        }
    }

    public static void run(ServerPlayer player) {
        if (!"rules".equals(System.getProperty("betterinventory.selftest.only"))) {
            pass = 0;
            fail = 0;
            Inventory inv = player.getInventory();
            PlayerLoadout loadout = PlayerLoadout.of(player);
            List<ItemStack> savedItems = new ArrayList<>();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                savedItems.add(inv.getItem(i).copy());
            }
            List<ItemStack> savedGather = new ArrayList<>();
            for (int i = 0; i < loadout.gather.getSlots(); i++) {
                savedGather.add(loadout.gather.getStackInSlot(i).copy());
            }
            ItemStack savedPack = loadout.backpacks.getStackInSlot(0).copy();
            int savedTab = loadout.activeTab;
            TabSettings savedSettings = loadout.defaultTab;

            try {
                if (savedTab != 0) {
                    LOGGER.warn("[BI selftest] player was on tab {}; scenarios assume the default tab", savedTab);
                }
                loadout.activeTab = 0;
                loadout.defaultTab = TabSettings.DEFAULT;
                reset(inv, loadout, true);
                inv.setItem(9, new ItemStack(Items.COBBLESTONE, 16));
                new BetterInventoryMenu(0, inv).quickMoveStack(player, 9);
                check("main -> gather hub",
                        inv.getItem(9).isEmpty()
                                && gatherCount(loadout, Items.COBBLESTONE) == 16
                                && hotbarCount(inv, Items.COBBLESTONE) == 0,
                        "main=" + inv.getItem(9).getCount()
                                + " hub=" + gatherCount(loadout, Items.COBBLESTONE)
                                + " hotbar=" + hotbarCount(inv, Items.COBBLESTONE));
                reset(inv, loadout, true);
                loadout.gather.setStackInSlot(4, new ItemStack(Items.COBBLESTONE, 10));
                inv.setItem(9, new ItemStack(Items.COBBLESTONE, 16));
                new BetterInventoryMenu(0, inv).quickMoveStack(player, 9);
                check("main -> gather hub merges",
                        loadout.gather.getStackInSlot(4).getCount() == 26
                                && gatherCount(loadout, Items.COBBLESTONE) == 26,
                        "slot4=" + loadout.gather.getStackInSlot(4).getCount()
                                + " hubTotal=" + gatherCount(loadout, Items.COBBLESTONE));
                reset(inv, loadout, true);
                loadout.gather.setStackInSlot(0, new ItemStack(Items.DIRT, 16));
                new BetterInventoryMenu(0, inv).quickMoveStack(player, 36);
                check("gather hub -> main",
                        loadout.gather.getStackInSlot(0).isEmpty() && mainCount(inv, Items.DIRT) == 16,
                        "hub=" + loadout.gather.getStackInSlot(0).getCount()
                                + " main=" + mainCount(inv, Items.DIRT));
                reset(inv, loadout, true);
                inv.setItem(0, new ItemStack(Items.SAND, 16));
                new BetterInventoryMenu(0, inv).quickMoveStack(player, 0);
                check("hotbar -> main",
                        inv.getItem(0).isEmpty()
                                && mainCount(inv, Items.SAND) == 16
                                && gatherCount(loadout, Items.SAND) == 0,
                        "hotbar=" + inv.getItem(0).getCount()
                                + " main=" + mainCount(inv, Items.SAND)
                                + " hub=" + gatherCount(loadout, Items.SAND));
                reset(inv, loadout, false);
                inv.setItem(9, new ItemStack(Items.GRAVEL, 16));
                new BetterInventoryMenu(0, inv).quickMoveStack(player, 9);
                check("main -> hotbar when hub locked",
                        hotbarCount(inv, Items.GRAVEL) == 16 && gatherCount(loadout, Items.GRAVEL) == 0,
                        "hotbar=" + hotbarCount(inv, Items.GRAVEL)
                                + " hub=" + gatherCount(loadout, Items.GRAVEL));
                reset(inv, loadout, true);
                for (int i = 0; i < loadout.gather.getSlots(); i++) {
                    loadout.gather.setStackInSlot(i, new ItemStack(Items.STONE, 1));
                }
                inv.setItem(9, new ItemStack(Items.COBBLESTONE, 16));
                new BetterInventoryMenu(0, inv).quickMoveStack(player, 9);
                check("main -> hotbar when hub full",
                        hotbarCount(inv, Items.COBBLESTONE) == 16
                                && gatherCount(loadout, Items.COBBLESTONE) == 0,
                        "hotbar=" + hotbarCount(inv, Items.COBBLESTONE)
                                + " hub=" + gatherCount(loadout, Items.COBBLESTONE));

                for (int level = 2; level <= 5; level++) {
                    checkUpgradeKeepsContents(player, level);
                }

                SelfTestFeatures.run(player);
            } catch (RuntimeException e) {
                fail++;
                LOGGER.error("[BI selftest] FAIL threw", e);
            } finally {
                for (int i = 0; i < savedItems.size(); i++) {
                    inv.setItem(i, savedItems.get(i));
                }
                for (int i = 0; i < savedGather.size(); i++) {
                    loadout.gather.setStackInSlot(i, savedGather.get(i));
                }
                loadout.backpacks.setStackInSlot(0, savedPack);
                loadout.activeTab = savedTab;
                loadout.defaultTab = savedSettings;
                loadout.dirty = true;
                inv.setChanged();
                LOGGER.info("[BI selftest] DONE pass={} fail={}", pass, fail);
            }
        }
    }

    private static void checkUpgradeKeepsContents(ServerPlayer player, int level) {
        String name = "backpack " + (level - 1) + " -> " + level + " keeps its contents";
        ShapedRecipe recipe = player.level().recipeAccess()
                .byKey(ResourceKey.create(Registries.RECIPE, BetterInventory.id("backpack_" + level)))
                .map(holder -> holder.value() instanceof ShapedRecipe shaped ? shaped : null)
                .orElse(null);
        if (recipe == null) {
            check(name, false, "no shaped recipe betterinventory:backpack_" + level);
        } else {
            ItemStack old = new ItemStack(ModItems.BACKPACKS[level - 2].get());
            NonNullList<ItemStack> packed = NonNullList.withSize(PlayerLoadout.CONTENTS_SIZE, ItemStack.EMPTY);
            packed.set(3, new ItemStack(Items.COBBLESTONE, 40));
            old.set(ModComponents.BACKPACK_CONTENTS.get(), StoredItems.of(packed));
            NonNullList<ItemStack> installed = NonNullList.withSize(PlayerLoadout.UPGRADE_SLOTS, ItemStack.EMPTY);
            installed.set(0, new ItemStack(ModItems.UPGRADES.get(UpgradeItem.Kind.MAGNET).get(0).get()));
            old.set(ModComponents.BACKPACK_UPGRADES.get(), StoredItems.of(installed));
            List<ItemStack> grid = new ArrayList<>();
            for (Optional<Ingredient> optional : recipe.getIngredients()) {
                List<Item> options = optional.map(ing -> ing.items().map(net.minecraft.core.Holder::value).toList())
                        .orElse(List.of());
                Item pick = options.isEmpty() ? null : options.get(0);
                grid.add(pick instanceof BackpackItem ? old : (pick == null ? ItemStack.EMPTY : new ItemStack(pick)));
            }
            CraftingInput input = CraftingInput.of(recipe.getWidth(), recipe.getHeight(), grid);
            ItemStack out = recipe.matches(input, player.level()) ? recipe.assemble(input) : ItemStack.EMPTY;
            int cobblestone = 0;
            for (StoredItems.Entry entry
                    : out.getOrDefault(ModComponents.BACKPACK_CONTENTS.get(), StoredItems.EMPTY).entries()) {
                if (entry.stack().is(Items.COBBLESTONE)) {
                    cobblestone += entry.stack().getCount();
                }
            }
            int upgrades = out.getOrDefault(ModComponents.BACKPACK_UPGRADES.get(), StoredItems.EMPTY).entries().size();
            check(name,
                    BackpackItem.levelOf(out) == level && cobblestone == 40 && upgrades == 1,
                    "result=" + out + " cobblestone=" + cobblestone + " upgrades=" + upgrades);
        }
    }

    private static void reset(Inventory inv, PlayerLoadout loadout, boolean withBackpack) {
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < loadout.gather.getSlots(); i++) {
            loadout.gather.setStackInSlot(i, ItemStack.EMPTY);
        }
        loadout.backpacks.setStackInSlot(0,
                withBackpack ? new ItemStack(ModItems.BACKPACKS[0].get()) : ItemStack.EMPTY);
    }

    private static int gatherCount(PlayerLoadout loadout, Item item) {
        int n = 0;
        for (int i = 0; i < loadout.gather.getSlots(); i++) {
            ItemStack s = loadout.gather.getStackInSlot(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static int mainCount(Inventory inv, Item item) {
        return count(inv, item, 9, 36);
    }

    private static int hotbarCount(Inventory inv, Item item) {
        return count(inv, item, 0, 9);
    }

    private static int count(Inventory inv, Item item, int from, int to) {
        int n = 0;
        for (int i = from; i < to; i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }
}
