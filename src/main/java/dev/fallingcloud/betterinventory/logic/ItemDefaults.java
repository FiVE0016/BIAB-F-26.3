package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Fabric counterpart of NeoForge's ModifyDefaultComponentsEvent.
 *
 * <p>Raises the CEILING on stackable items to the highest tier a stack upgrade can
 * grant (and, in HUNDRED mode, rebases plain 64-stack items to 100 first). Only the
 * ceiling is raised - what a player may actually stack is enforced per-player by
 * InventoryStackSizeMixin, so with no upgrade installed the inventory behaves exactly
 * like vanilla.
 */
public final class ItemDefaults {
    private ItemDefaults() {}

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            boolean hundred = BetterInventoryConfig.STACK_MODE.get() == BetterInventoryConfig.StackMode.HUNDRED;
            int ceiling = StackUpgradeService.maxTierValue();
            for (Item item : BuiltInRegistries.ITEM) {
                int base = StackUpgradeService.baseMax(item);
                if (base <= 1) {
                    continue; // never make unstackable items stack
                }
                int rebased = hundred && base == 64 ? 100 : base;
                StackUpgradeService.recordOriginal(item, rebased);
                int max = StackUpgradeService.scale(rebased, ceiling);
                context.modify(item, builder -> builder.set(DataComponents.MAX_STACK_SIZE, max));
            }
        });
    }
}
