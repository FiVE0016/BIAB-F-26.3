package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.4.1 ticking behaviors of the ability upgrades (magnet + feeding).
 *
 * <p>26.3 adaptation: {@code Player#eat(Level, ItemStack, FoodProperties)} is
 * gone - consumption goes through the CONSUMABLE component, whose
 * {@code onConsume} applies the food and returns the remainder (bowl/bottle).
 * The "usingConvertsTo" skip is replaced by skipping foods with consume
 * effects, and the food with a crafting remainder is skipped the same way
 * 1.4.1 did via the Item's remainder.
 */
public final class UpgradeService {
    private UpgradeService() {}

    public static void tick(ServerPlayer player) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        if (player.tickCount % BetterInventoryConfig.MAGNET_INTERVAL.get() == 0
                && enabledLevel(player, loadout, UpgradeItem.Kind.MAGNET) > 0) {
            magnetTick(player, enabledLevel(player, loadout, UpgradeItem.Kind.MAGNET));
        }
        if (player.tickCount % 20 == 0) {
            feedingTick(player, loadout);
        }
    }

    private static int enabledLevel(Player player, PlayerLoadout loadout, UpgradeItem.Kind kind) {
        int level = loadout.upgradeLevel(kind);
        return level > 0 && BetterInventorySettings.upgradeEnabled(player, kind.id()) ? level : 0;
    }

    private static void magnetTick(ServerPlayer player, int level) {
        int radius = UpgradeItem.magnetRadius(level);
        AABB box = player.getBoundingBox().inflate(radius);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, box,
                item -> !item.hasPickUpDelay() && !item.getItem().isEmpty());
        Vec3 target = player.position().add(0.0, 0.75, 0.0);

        for (ItemEntity item : items) {
            Vec3 delta = target.subtract(item.position());
            double distance = delta.length();
            if (distance < 0.75 || distance > radius) {
                continue;
            }
            Vec3 pull = delta.scale(1.0 / distance)
                    .scale((0.35 + 0.1 * Math.min(1.0, 3.0 / distance))
                            * BetterInventoryConfig.MAGNET_PULL_STRENGTH.get());
            item.setDeltaMovement(item.getDeltaMovement().scale(0.6).add(pull));
        }
    }

    private static void feedingTick(ServerPlayer player, PlayerLoadout loadout) {
        int level = enabledLevel(player, loadout, UpgradeItem.Kind.FEEDING);
        if (level > 0 && loadout.pages() > 0
                && player.getFoodData().getFoodLevel() <= UpgradeItem.feedingThreshold(level)
                && player.canEat(false)) {
            BigStackHandler handler = loadout.pageStore();
            if (handler != null) {
                int limit = loadout.pages() * PlayerLoadout.PAGE_SIZE;
                for (int i = 0; i < limit; i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    FoodProperties food = stack.get(DataComponents.FOOD);
                    Consumable consumable = stack.get(DataComponents.CONSUMABLE);
                    // Nutrition only: skip stews, effect foods and items with a
                    // crafting remainder (they carry side effects).
                    if (food == null
                            || stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)
                            || consumable == null
                            || !consumable.onConsumeEffects().isEmpty()
                            || stack.getItem().getCraftingRemainder() != null) {
                        continue;
                    }
                    if (!consumable.canConsume(player, stack.copyWithCount(1))) {
                        continue;
                    }
                    ItemStack eaten = stack.copyWithCount(1);
                    // 26.3: onConsume applies the food and returns the remainder.
                    ItemStack remainder = consumable.onConsume(player.level(), player, eaten);
                    if (stack.getCount() <= 1) {
                        handler.setStackInSlot(i, remainder);
                    } else {
                        handler.setStackInSlot(i, stack.copyWithCount(stack.getCount() - 1));
                        if (!remainder.isEmpty()) {
                            player.spawnAtLocation(player.level(), remainder.copy());
                        }
                    }
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, 1.0F);
                    loadout.dirty = true;
                    return;
                }
            }
        }
    }
}
