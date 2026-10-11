package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.LootTradeTables;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * G-1：1.4.1 流浪商人的随机报价（原 TradeOfferHelper.registerWanderingTraderOffers
 * 第 2 级）在 26.3 数据驱动交易下的重放：updateTrades 尾部追加旅人结与中期
 * 升级报价。
 */
@Mixin(WanderingTrader.class)
public abstract class WanderingTraderTradesMixin {

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void betterinventory$randomOffers(ServerLevel level, CallbackInfo ci) {
        WanderingTrader self = (WanderingTrader) (Object) this;
        MerchantOffers offers = self.getOffers();
        RandomSource random = self.level().getRandom();
        offers.add(LootTradeTables.knotOffer(22, 1));
        MerchantOffer upgrade = LootTradeTables.upgradeOffer(LootTradeTables.TIER_MID, 24, 32, 1).create(self, random);
        if (upgrade != null) {
            offers.add(upgrade);
        }
    }
}
