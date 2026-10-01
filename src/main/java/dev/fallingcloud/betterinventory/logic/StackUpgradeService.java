package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Stack upgrades apply to the player's WHOLE inventory, not just BetterInventory storage.
 *
 * <p>Two separate limits gate stack size, and both have to move:
 * <ul>
 *   <li>{@code ItemStack#getMaxStackSize} — the MAX_STACK_SIZE component. Raised
 *       once globally on the item DEFAULTS (see BetterInventory#modifyDefaults) rather than
 *       patched onto individual stacks. That distinction matters: a per-stack patch
 *       would make upgraded stacks compare unequal to ordinary ones, so they would
 *       refuse to merge with items picked up, crafted, or pulled from a chest.</li>
 *   <li>{@code Container#getMaxStackSize} — a hard-coded 99 that Inventory inherits.
 *       Overridden for the player inventory only, by InventoryStackSizeMixin, and
 *       this is what actually enforces the player's current tier.</li>
 * </ul>
 *
 * <p>So the ceiling is global but the enforced limit is per-player: with no upgrade
 * installed the inventory behaves exactly like vanilla.
 */
public final class StackUpgradeService {
    private StackUpgradeService() {}

    /** The size a plain 64-stack item reaches with no upgrade installed. */
    public static int normalBase() {
        return BetterInventoryConfig.STACK_MODE.get() == BetterInventoryConfig.StackMode.HUNDRED ? 100 : 64;
    }

    /** The largest tier any upgrade can grant, used for the global default ceiling. */
    public static int maxTierValue() {
        int[] tiers = BetterInventoryConfig.tierValues();
        int max = normalBase();
        for (int tier : tiers) {
            max = Math.max(max, tier);
        }
        return max;
    }

    /** The player's current ceiling for a normally-64-stacking item. */
    public static int capFor(Player player) {
        if (player == null) {
            return normalBase();
        }
        PlayerLoadout loadout = ModAttachments.get(player);
        return loadout == null ? normalBase() : Math.max(normalBase(), loadout.storageCap());
    }

    /**
     * Each item's stack size as it was BEFORE we raised the global ceiling. Captured
     * during ModifyDefaultComponentsEvent, because afterwards {@code item.components()}
     * reports the raised value and the original is unrecoverable. Everything that is
     * not the player's own inventory is held to this, so chests, crafting results and
     * creative middle-click stay exactly vanilla.
     */
    private static final Map<Item, Integer> ORIGINAL_MAX = new IdentityHashMap<>();

    /** The item's stack size right now, including our raised ceiling. */
    public static int baseMax(Item item) {
        return item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    @ApiStatus.Internal
    public static void recordOriginal(Item item, int max) {
        ORIGINAL_MAX.putIfAbsent(item, max);
    }

    /** The item's vanilla stack size, ignoring our raised ceiling. */
    public static int originalMax(Item item) {
        Integer recorded = ORIGINAL_MAX.get(item);
        return recorded != null ? recorded : baseMax(item);
    }

    public static int originalMax(ItemStack stack) {
        return stack.isEmpty() ? 1 : originalMax(stack.getItem());
    }

    /**
     * The ceiling that applies to a stack sitting in the given container. Only the
     * player's own inventory scales with their upgrade; every other container is
     * held to vanilla, which is what keeps behaviour correct with no upgrade
     * installed.
     */
    public static int effectiveMax(Container container, ItemStack stack) {
        int original = originalMax(stack);
        if (container instanceof Inventory inventory) {
            return scale(original, capFor(inventory.player));
        }
        return original;
    }

    /**
     * Scales a ceiling by the item's own natural size, so a 16-stack item never
     * inherits a 64-stack item's ceiling and unstackable items stay at 1.
     */
    public static int scale(int base, int cap) {
        if (base <= 1) {
            return 1;
        }
        return Math.max(base, (int) ((long) base * cap / normalBase()));
    }

    /** Per-item ceiling for this stack under the given upgrade cap. */
    public static int scaledCap(ItemStack stack, int cap) {
        if (stack.isEmpty()) {
            return cap;
        }
        return scale(originalMax(stack.getItem()), cap);
    }

    /** Per-item ceiling for this stack, resolved against its holder. */
    public static int limitFor(Player player, ItemStack stack) {
        return scaledCap(stack, capFor(player));
    }
}
