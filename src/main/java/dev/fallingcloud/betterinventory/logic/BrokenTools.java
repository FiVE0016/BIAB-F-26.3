package dev.fallingcloud.betterinventory.logic;

import java.lang.reflect.Method;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1: soft bridge to the "improved_damage" mod's broken-tool verdict, so the
 * worn-tool floor does not fight its rules. Reflection-based; absent mod = false.
 */
public final class BrokenTools {
    private static final String IMPROVED_DAMAGE = "net.cloud.improved_damage.ImprovedDamage";
    private static Lookup lookup = Lookup.UNTRIED;
    private static Method isBroken;

    private BrokenTools() {}

    public static boolean isUnusable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (lookup == Lookup.UNTRIED) {
            resolve();
        }
        if (lookup != Lookup.OK) {
            return false;
        }
        try {
            return (Boolean) isBroken.invoke(null, stack);
        } catch (Throwable t) {
            lookup = Lookup.ABSENT;
            return false;
        }
    }

    private static void resolve() {
        try {
            isBroken = Class.forName(IMPROVED_DAMAGE).getMethod("isBroken", ItemStack.class);
            lookup = Lookup.OK;
        } catch (Throwable notInstalled) {
            lookup = Lookup.ABSENT;
        }
    }

    private enum Lookup {
        UNTRIED,
        OK,
        ABSENT;
    }
}
