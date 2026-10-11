package dev.fallingcloud.betterinventory.logic;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/**
 * 1.4.1 loot/trade event wiring.
 *
 * <p>26.3 adaptation: fabric-loot-api moved to the {@code v3} package and the
 * Modify callback gained a {@code HolderLookup.Provider} parameter. The
 * villager-trade registrations moved into VillagerTradesMixin /
 * WanderingTraderTradesMixin (26.3 trades are data-driven, and the 1.4.1
 * offers are random-price/random-item, which static data cannot express).
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
    }
}
