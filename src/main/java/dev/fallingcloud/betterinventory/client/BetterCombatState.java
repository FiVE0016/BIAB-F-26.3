package dev.fallingcloud.betterinventory.client;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;

/** 1.4.1 reflection bridge to Better Combat's client state. */
final class BetterCombatState {
    private static final Method COMBO;
    private static final Method SWING_PROGRESS;
    private static final Method UPSWING;

    private BetterCombatState() {}

    static boolean busy(Minecraft minecraft) {
        if (COMBO == null) {
            return false;
        }
        try {
            return (Integer) COMBO.invoke(minecraft) > 0 || (Float) SWING_PROGRESS.invoke(minecraft) < 1.0F;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    static boolean windingUp(Minecraft minecraft) {
        if (COMBO == null) {
            return false;
        }
        try {
            return (Integer) UPSWING.invoke(minecraft) > 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    static {
        Method combo = null;
        Method swing = null;
        Method upswing = null;
        try {
            Class<?> api = Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat");
            combo = api.getMethod("getComboCount");
            swing = api.getMethod("getSwingProgress");
            upswing = api.getMethod("getUpswingTicks");
        } catch (ReflectiveOperationException | LinkageError absent) {
            combo = null;
        }
        COMBO = combo;
        SWING_PROGRESS = combo == null ? null : swing;
        UPSWING = combo == null ? null : upswing;
    }
}
