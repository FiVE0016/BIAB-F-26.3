package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

/** Ticking behaviors of the backpack ability upgrades (magnet + feeding). */
public final class UpgradeService {
    private UpgradeService() {}

    public static void tick(ServerPlayer player) {
        PlayerLoadout loadout = ModAttachments.get(player);
        if (player.tickCount % 2 == 0 && hasEnabled(player, loadout, UpgradeItem.Kind.MAGNET)) {
            magnetTick(player);
        }
        if (player.tickCount % 20 == 0) {
            feedingTick(player, loadout);
        }
    }

    private static boolean hasEnabled(Player player, PlayerLoadout loadout, UpgradeItem.Kind kind) {
        for (int tab = 1; tab <= 4; tab++) {
            if (loadout.upgradeInstalled(tab, kind)
                    && BetterInventorySettings.raw(player, tab).upgradeEnabled(kind.id())) {
                return true;
            }
        }
        return false;
    }

    private static void magnetTick(ServerPlayer player) {
        int radius = BetterInventoryConfig.MAGNET_RADIUS.get();
        AABB box = player.getBoundingBox().inflate(radius);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, box,
                item -> !item.hasPickUpDelay() && !item.getItem().isEmpty());
        Vec3 target = player.position().add(0, 0.75, 0);
        for (ItemEntity item : items) {
            Vec3 delta = target.subtract(item.position());
            double distance = delta.length();
            if (distance < 0.75 || distance > radius) {
                continue;
            }
            Vec3 pull = delta.scale(1.0 / distance).scale(0.35 + 0.1 * Math.min(1.0, 3.0 / distance));
            item.setDeltaMovement(item.getDeltaMovement().scale(0.6).add(pull));
        }
    }

    private static void feedingTick(ServerPlayer player, PlayerLoadout loadout) {
        if (player.getFoodData().getFoodLevel() > BetterInventoryConfig.FEEDING_THRESHOLD.get() || !player.canEat(false)) {
            return;
        }
        for (int tab = 1; tab <= 4; tab++) {
            if (!loadout.upgradeInstalled(tab, UpgradeItem.Kind.FEEDING)) {
                continue;
            }
            TabSettings settings = BetterInventorySettings.raw(player, tab);
            if (!settings.upgradeEnabled(UpgradeItem.Kind.FEEDING.id())) {
                continue;
            }
            ItemStack pack = loadout.backpack(tab);
            ComponentBackedHandler handler = new ComponentBackedHandler(pack, ModComponents.BACKPACK_CONTENTS.get(), 45, loadout::storageCap);
            handler.setChangeListener(() -> loadout.dirty = true);
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                FoodProperties food = stack.get(DataComponents.FOOD);
                // Skip anything that carries effects - the feeding upgrade is meant to be
                // nutrition only, not a way to auto-apply potion or stew effects.
                if (food == null || stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
                    continue;
                }

                ItemStack eaten = stack.copyWithCount(1);
                Consumable consumable = stack.get(DataComponents.CONSUMABLE);
                ItemStack remainder = ItemStack.EMPTY;
                if (consumable != null) {
                    if (!consumable.canConsume(player, eaten)) {
                        continue;
                    }
                    // onConsume applies the food and returns whatever the item turns into
                    // (a bowl for stew, a bottle for honey, ...) - that is the 26.3
                    // replacement for the removed ItemStack#getCraftingRemainingItem.
                    remainder = consumable.onConsume(player.level(), player, eaten);
                } else {
                    player.getFoodData().eat(food.nutrition(), food.saturation());
                }

                // Put the container back where the food was, or drop it if it won't fit.
                if (stack.getCount() <= 1) {
                    handler.setStackInSlot(i, remainder);
                    if (!remainder.isEmpty() && !handler.getStackInSlot(i).isEmpty()) {
                        player.spawnAtLocation(player.level(), remainder.copy());
                        handler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                } else {
                    handler.setStackInSlot(i, stack.copyWithCount(stack.getCount() - 1));
                    if (!remainder.isEmpty()) {
                        player.spawnAtLocation(player.level(), remainder.copy());
                    }
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 1.0f);
                loadout.dirty = true;
                return;
            }
        }
    }
}
