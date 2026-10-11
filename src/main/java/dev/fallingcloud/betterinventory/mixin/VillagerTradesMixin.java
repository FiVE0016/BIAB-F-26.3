package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.LootTradeTables;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * G-1：1.4.1 随机村民报价的 26.3 重放。26.3 移除 TradeOfferHelper、交易
 * 数据驱动化（VillagerProfession.getTrades(level) → TradeSet 注册表），随机
 * 价格/随机物品无法用 JSON 表达，故在 updateTrades 尾部追加 1.4.1 报价。
 *
 * <p>报价集与 1.4.1 的 TradeOfferHelper 注册一致：LEATHERWORKER L3 早期
 * 升级、L5 旅人结；TOOLSMITH L4 中期；LIBRARIAN L5 中期；CARTOGRAPHER L5
 * 晚期。
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin {

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void betterinventory$randomOffers(ServerLevel level, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        VillagerData data = self.getVillagerData();
        int villagerLevel = data.level();
        RandomSource random = self.level().getRandom();
        if (data.profession().is(VillagerProfession.LEATHERWORKER)) {
            if (villagerLevel == 3) {
                addUpgrade(self, LootTradeTables.TIER_EARLY, 10, 16, 4, random);
            } else if (villagerLevel == 5) {
                self.getOffers().add(LootTradeTables.knotOffer(18, 3));
            }
        } else if (data.profession().is(VillagerProfession.TOOLSMITH) && villagerLevel == 4) {
            addUpgrade(self, LootTradeTables.TIER_MID, 20, 28, 3, random);
        } else if (data.profession().is(VillagerProfession.LIBRARIAN) && villagerLevel == 5) {
            addUpgrade(self, LootTradeTables.TIER_MID, 18, 26, 2, random);
        } else if (data.profession().is(VillagerProfession.CARTOGRAPHER) && villagerLevel == 5) {
            addUpgrade(self, LootTradeTables.TIER_LATE, 32, 45, 2, random);
        }
    }

    private static void addUpgrade(Villager self, int tier, int minEmeralds, int maxEmeralds, int uses, RandomSource random) {
        MerchantOffer offer = LootTradeTables.upgradeOffer(tier, minEmeralds, maxEmeralds, uses).create(self, random);
        if (offer != null) {
            MerchantOffers offers = self.getOffers();
            offers.add(offer);
        }
    }
}
