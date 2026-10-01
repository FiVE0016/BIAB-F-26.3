package dev.fallingcloud.betterinventory.compat;

import dev.fallingcloud.betterinventory.platform.AccessorySlots;
import dev.fallingcloud.betterinventory.platform.ItemStore;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric's counterpart to CuriosCompat: exposes the player's equipped Trinkets slots
 * so the overhauled inventory can host real accessory slots.
 *
 * <p>Reflection keeps Trinkets entirely optional with no compile-time dependency - the
 * same approach the original Curios integration took.
 */
public final class TrinketsCompat {
    private static boolean checked;
    private static boolean available;
    private static Method getComponent;
    private static Method getAllEquipped;

    private TrinketsCompat() {}

    private static void init() {
        if (checked) {
            return;
        }
        checked = true;
        if (!FabricLoader.getInstance().isModLoaded("trinkets")) {
            return;
        }
        try {
            Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            getComponent = api.getMethod("getTrinketComponent", LivingEntity.class);
            Class<?> component = Class.forName("dev.emi.trinkets.api.TrinketComponent");
            getAllEquipped = component.getMethod("getAllEquipped");
            available = true;
        } catch (ReflectiveOperationException e) {
            available = false;
        }
    }

    /** The player's equipped trinket slots, or {@link AccessorySlots#NONE} if absent. */
    public static AccessorySlots equipped(Player player) {
        init();
        if (!available) {
            return AccessorySlots.NONE;
        }
        try {
            Object result = getComponent.invoke(null, player);
            if (result instanceof Optional<?> opt && opt.isPresent()) {
                Object equipped = getAllEquipped.invoke(opt.get());
                if (equipped instanceof List<?> list) {
                    List<ItemStack> stacks = new ArrayList<>();
                    for (Object entry : list) {
                        if (entry instanceof Map.Entry<?, ?> pair && pair.getValue() instanceof ItemStack stack) {
                            stacks.add(stack);
                        }
                    }
                    return new TrinketSlots(stacks);
                }
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            available = false;
        }
        return AccessorySlots.NONE;
    }

    /**
     * The equipped trinkets as an {@link ItemStore}, which is what the inventory
     * screen's curio slots are built around, or {@code null} when Trinkets is absent.
     *
     * <p>Changes are written into this snapshot, not back into Trinkets. Promote
     * Trinkets to a real dependency if two-way syncing is needed.
     */
    public static ItemStore store(Player player) {
        AccessorySlots slots = equipped(player);
        if (!slots.present()) {
            return null;
        }
        ItemStore store = new ItemStackHandler(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            store.set(i, slots.get(i));
        }
        return store;
    }

    /** Flat view over the equipped trinkets. */
    private record TrinketSlots(List<ItemStack> stacks) implements AccessorySlots {
        @Override
        public int size() {
            return stacks.size();
        }

        @Override
        public ItemStack get(int index) {
            return index >= 0 && index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
        }

        @Override
        public void set(int index, ItemStack stack) {
            // Writing through reflection is unsafe; these slots stay read-only unless
            // Trinkets is promoted to a real compile-time dependency.
        }

        @Override
        public boolean isValid(int index, ItemStack stack) {
            return false;
        }
    }
}
