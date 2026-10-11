package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.floats.ConstantValue;

/**
 * 1.4.1 loot/trade tables: chest loot pools for the upgrade tiers and the
 * traveler's knot, plus the villager trade factories.
 *
 * <p>26.3 adaptation: {@code LootTables} is {@link BuiltInLootTables} (same
 * field names), {@code ItemEntry} is {@link LootItem}, and the loot pool
 * providers are the ints/floats {@link ConstantValue} records. The villager
 * trade factories are kept but not registered yet - 26.3 made villager trades
 * fully data-driven (no TradeOfferHelper), and the random-price/random-item
 * offers cannot be expressed as static data; they need a mixin into the 26.3
 * trade-rolling code (unit G follow-up).
 */
public final class LootTradeTables {
    public static final int TIER_EARLY = 0;
    public static final int TIER_MID = 1;
    public static final int TIER_LATE = 2;
    private static List<List<Item>> tiers;
    private static final Set<ResourceKey<LootTable>> EARLY_CHESTS = Set.of(
            BuiltInLootTables.VILLAGE_TANNERY,
            BuiltInLootTables.VILLAGE_TOOLSMITH,
            BuiltInLootTables.VILLAGE_WEAPONSMITH,
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.SHIPWRECK_SUPPLY,
            BuiltInLootTables.IGLOO_CHEST,
            BuiltInLootTables.RUINED_PORTAL
    );
    private static final Set<ResourceKey<LootTable>> MID_CHESTS = Set.of(
            BuiltInLootTables.STRONGHOLD_CORRIDOR,
            BuiltInLootTables.STRONGHOLD_CROSSING,
            BuiltInLootTables.DESERT_PYRAMID,
            BuiltInLootTables.JUNGLE_TEMPLE,
            BuiltInLootTables.PILLAGER_OUTPOST,
            BuiltInLootTables.BURIED_TREASURE,
            BuiltInLootTables.UNDERWATER_RUIN_BIG,
            BuiltInLootTables.NETHER_BRIDGE,
            BuiltInLootTables.BASTION_OTHER
    );
    private static final Set<ResourceKey<LootTable>> LATE_CHESTS = Set.of(
            BuiltInLootTables.END_CITY_TREASURE,
            BuiltInLootTables.ANCIENT_CITY,
            BuiltInLootTables.BASTION_TREASURE,
            BuiltInLootTables.WOODLAND_MANSION,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD
    );
    private static final Set<ResourceKey<LootTable>> KNOT_CHESTS = Set.of(
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.STRONGHOLD_CORRIDOR,
            BuiltInLootTables.DESERT_PYRAMID,
            BuiltInLootTables.JUNGLE_TEMPLE,
            BuiltInLootTables.IGLOO_CHEST,
            BuiltInLootTables.PILLAGER_OUTPOST,
            BuiltInLootTables.SHIPWRECK_SUPPLY,
            BuiltInLootTables.WOODLAND_MANSION,
            BuiltInLootTables.VILLAGE_TANNERY
    );
    private static final float[] CHEST_CHANCE = new float[]{0.25F, 0.22F, 0.3F};

    private LootTradeTables() {}

    private static synchronized List<List<Item>> tiers() {
        if (tiers != null) {
            return tiers;
        }
        List<Item> early = new ArrayList<>();
        List<Item> mid = new ArrayList<>();
        List<Item> late = new ArrayList<>();
        for (Item item : ModItems.all()) {
            String id = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (id.equals("crafting_upgrade")) {
                early.add(item);
            } else if (id.equals("advanced_crafting_upgrade")) {
                mid.add(item);
            } else if (id.startsWith("stack_upgrade_")) {
                int n = level(id, 1);
                (n <= 2 ? early : (n <= 5 ? mid : late)).add(item);
            } else if (id.startsWith("upgrade_")) {
                int n = level(id, 1);
                (n <= 1 ? early : (n <= 3 ? mid : late)).add(item);
            }
        }
        tiers = List.of(List.copyOf(early), List.copyOf(mid), List.copyOf(late));
        return tiers;
    }

    private static int level(String id, int fallback) {
        int cut = id.lastIndexOf('_');
        if (cut >= 0 && cut != id.length() - 1) {
            try {
                return Integer.parseInt(id.substring(cut + 1));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    public static List<Item> tier(int which) {
        return tiers().get(which);
    }

    public static int chestTier(ResourceKey<LootTable> table) {
        if (EARLY_CHESTS.contains(table)) {
            return TIER_EARLY;
        }
        if (MID_CHESTS.contains(table)) {
            return TIER_MID;
        }
        return LATE_CHESTS.contains(table) ? TIER_LATE : -1;
    }

    public static boolean isKnotChest(ResourceKey<LootTable> table) {
        return KNOT_CHESTS.contains(table);
    }

    public static LootPool.Builder knotPool() {
        return LootPool.lootPool()
                .setRolls(Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1)))
                .setBonusRolls(Holder.direct(new ConstantValue(0.18F)))
                .add(LootItem.lootTableItem(ModItems.TRAVELERS_KNOT.get()));
    }

    public static LootPool.Builder upgradePool(int tier) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1)))
                .setBonusRolls(Holder.direct(new ConstantValue(CHEST_CHANCE[tier])));
        for (Item item : tier(tier)) {
            pool.add(LootItem.lootTableItem(item));
        }
        return pool;
    }

    public static boolean hasTier(int tier) {
        return tier >= 0 && !tier(tier).isEmpty();
    }

    // -- villager trades (factories kept for the unit-G mixin follow-up) --------

    public static MerchantOffer knotOffer(int emeralds, int uses) {
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds),
                new ItemStack(ModItems.TRAVELERS_KNOT.get()),
                uses, 15, 0.05F);
    }

    public static UpgradeForEmeralds upgradeOffer(int tier, int minEmeralds, int maxEmeralds, int uses) {
        return new UpgradeForEmeralds(tier, minEmeralds, maxEmeralds, uses);
    }

    /** Random upgrade tier offer; needs the 26.3 trade-rolling mixin to be used. */
    public record UpgradeForEmeralds(int tier, int minEmeralds, int maxEmeralds, int uses) {
        public MerchantOffer create(Entity trader, RandomSource random) {
            List<Item> pool = LootTradeTables.tier(this.tier);
            if (pool.isEmpty()) {
                return null;
            }
            Item item = pool.get(random.nextInt(pool.size()));
            int price = Mth.nextInt(random, this.minEmeralds, this.maxEmeralds + 1);
            return new MerchantOffer(new ItemCost(Items.EMERALD, price), new ItemStack(item), this.uses, 12, 0.05F);
        }
    }
}
