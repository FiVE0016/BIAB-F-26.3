package dev.fallingcloud.betterinventory.logic;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/**
 * 1.4.1 loot/trade event wiring.
 *
 * <p>26.3 adaptation: fabric-loot-api moved to the {@code v3} package and the
 * Modify callback gained a {@code HolderLookup.Provider} parameter. The
 * villager-trade registrations are absent: 26.3 removed TradeOfferHelper and
 * made trades data-driven, and the 1.4.1 offers are random-price/random-item,
 * which static data cannot express - they return as a mixin in unit G.
 */
public final class LootTradeEvents {
    private LootTradeEvents() {}

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (LootTradeTables.isKnotChest(key)) {
                tableBuilder.withPool(LootTradeTables.knotPool());
            }
            int tier = LootTradeTables.chestTier(key);
            if (LootTradeTables.hasTier(tier)) {
                tableBuilder.withPool(LootTradeTables.upgradePool(tier));
            }
        });
        // TODO(G): 26.3 villager trades are data-driven (VillagerTrade/TradeSet
        // registries); re-add the 1.4.1 offers by mixing into the trade-rolling
        // logic, since random-price/random-item offers cannot be JSON data.
    }
}
