package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.CraftingUpgradeItem;
import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import dev.fallingcloud.betterinventory.shim.DeferredItem;
import dev.fallingcloud.betterinventory.shim.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BetterInventory.MODID);

    @SuppressWarnings("unchecked")
    public static final DeferredItem<BackpackItem>[] BACKPACKS = new DeferredItem[5];
    @SuppressWarnings("unchecked")
    public static final DeferredItem<StackUpgradeItem>[] STACK_UPGRADES = new DeferredItem[7];

    static {
        for (int i = 0; i < 5; i++) {
            final int level = i + 1;
            BACKPACKS[i] = ITEMS.register("backpack_" + level,
                    () -> new BackpackItem(level, ModBlocks.BACKPACK.get(), new Item.Properties().stacksTo(1)));
        }
        for (int i = 0; i < 7; i++) {
            final int tier = i;
            STACK_UPGRADES[i] = ITEMS.register("stack_upgrade_" + (i + 1),
                    () -> new StackUpgradeItem(tier, new Item.Properties().stacksTo(1)));
        }
    }

    public static final DeferredItem<UpgradeItem> UPGRADE_MAGNET = upgrade(UpgradeItem.Kind.MAGNET);
    public static final DeferredItem<UpgradeItem> UPGRADE_FEEDING = upgrade(UpgradeItem.Kind.FEEDING);
    public static final DeferredItem<UpgradeItem> UPGRADE_REFILL = upgrade(UpgradeItem.Kind.REFILL);
    public static final DeferredItem<UpgradeItem> UPGRADE_VOID = upgrade(UpgradeItem.Kind.VOID);
    public static final DeferredItem<UpgradeItem> UPGRADE_PICKUP = upgrade(UpgradeItem.Kind.PICKUP);

    /** Personal (non-backpack) upgrades: grow the crafting tab's grid. */
    public static final DeferredItem<CraftingUpgradeItem> CRAFTING_UPGRADE = ITEMS.register(
            "crafting_upgrade",
            () -> new CraftingUpgradeItem(2, 3, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<CraftingUpgradeItem> ADVANCED_CRAFTING_UPGRADE = ITEMS.register(
            "advanced_crafting_upgrade",
            () -> new CraftingUpgradeItem(3, 3, new Item.Properties().stacksTo(1)));

    private static DeferredItem<UpgradeItem> upgrade(UpgradeItem.Kind kind) {
        return ITEMS.register("upgrade_" + kind.id(), () -> new UpgradeItem(kind, new Item.Properties().stacksTo(1)));
    }

    public static List<Item> all() {
        List<Item> list = new ArrayList<>();
        for (var b : BACKPACKS) {
            list.add(b.get());
        }
        for (var s : STACK_UPGRADES) {
            list.add(s.get());
        }
        list.add(UPGRADE_MAGNET.get());
        list.add(UPGRADE_FEEDING.get());
        list.add(UPGRADE_REFILL.get());
        list.add(UPGRADE_VOID.get());
        list.add(UPGRADE_PICKUP.get());
        list.add(CRAFTING_UPGRADE.get());
        list.add(ADVANCED_CRAFTING_UPGRADE.get());
        return list;
    }

    private ModItems() {}
}
