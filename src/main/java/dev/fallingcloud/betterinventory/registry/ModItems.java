package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.CraftingUpgradeItem;
import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import dev.fallingcloud.betterinventory.item.TravelersKnotItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.shim.DeferredItem;
import dev.fallingcloud.betterinventory.shim.DeferredRegister;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BetterInventory.MODID);

    @SuppressWarnings("unchecked")
    public static final DeferredItem<BackpackItem>[] BACKPACKS = new DeferredItem[5];
    @SuppressWarnings("unchecked")
    public static final DeferredItem<StackUpgradeItem>[] STACK_UPGRADES = new DeferredItem[7];

    /** 1.4.1: ability upgrades are multi-level (magnet/feeding 5, refill/void/pickup 3). */
    public static final Map<UpgradeItem.Kind, List<DeferredItem<UpgradeItem>>> UPGRADES =
            new EnumMap<>(UpgradeItem.Kind.class);
    public static final DeferredItem<CraftingUpgradeItem> CRAFTING_UPGRADE;
    public static final DeferredItem<CraftingUpgradeItem> ADVANCED_CRAFTING_UPGRADE;
    public static final DeferredItem<CraftingUpgradeItem> FURNACE_CRAFTING_UPGRADE;
    public static final DeferredItem<TravelersKnotItem> TRAVELERS_KNOT;

    static {
        for (int i = 0; i < 5; i++) {
            final int level = i + 1;
            BACKPACKS[i] = ITEMS.registerWithId("backpack_" + level,
                    id -> new BackpackItem(level, ModBlocks.BACKPACK.get(),
                            new Item.Properties().setId(id).stacksTo(1)));
        }
        for (int i = 0; i < 7; i++) {
            final int tier = i;
            STACK_UPGRADES[i] = ITEMS.registerWithId("stack_upgrade_" + (i + 1),
                    id -> new StackUpgradeItem(tier,
                            new Item.Properties().setId(id).stacksTo(1)));
        }
        for (UpgradeItem.Kind kind : UpgradeItem.Kind.values()) {
            List<DeferredItem<UpgradeItem>> levels = new ArrayList<>();
            for (int level = 1; level <= kind.maxLevel(); level++) {
                final int lvl = level;
                String name = "upgrade_" + kind.id() + (level == 1 ? "" : "_" + level);
                levels.add(ITEMS.registerWithId(name,
                        id -> new UpgradeItem(kind, lvl,
                                new Item.Properties().setId(id).stacksTo(1))));
            }
            UPGRADES.put(kind, List.copyOf(levels));
        }
        CRAFTING_UPGRADE = ITEMS.registerWithId(
                "crafting_upgrade",
                id -> new CraftingUpgradeItem(2, 3, new Item.Properties().setId(id).stacksTo(1)));
        ADVANCED_CRAFTING_UPGRADE = ITEMS.registerWithId(
                "advanced_crafting_upgrade",
                id -> new CraftingUpgradeItem(3, 3, new Item.Properties().setId(id).stacksTo(1)));
        FURNACE_CRAFTING_UPGRADE = ITEMS.registerWithId(
                "furnace_crafting_upgrade",
                id -> new CraftingUpgradeItem(3, 3, true, new Item.Properties().setId(id).stacksTo(1)));
        TRAVELERS_KNOT = ITEMS.registerWithId(
                "travelers_knot",
                id -> new TravelersKnotItem(new Item.Properties().setId(id).stacksTo(1)));
    }

    public static List<Item> all() {
        List<Item> list = new ArrayList<>();
        for (var b : BACKPACKS) {
            list.add(b.get());
        }
        for (var s : STACK_UPGRADES) {
            list.add(s.get());
        }
        for (UpgradeItem.Kind kind : UpgradeItem.Kind.values()) {
            for (var u : UPGRADES.get(kind)) {
                list.add(u.get());
            }
        }
        list.add(CRAFTING_UPGRADE.get());
        list.add(ADVANCED_CRAFTING_UPGRADE.get());
        list.add(FURNACE_CRAFTING_UPGRADE.get());
        list.add(TRAVELERS_KNOT.get());
        return list;
    }

    private ModItems() {}
}
