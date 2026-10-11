package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.platform.ModPresence;
import java.lang.reflect.Constructor;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * 1.4.1 FTB Library sidebar compat: reflects the sidebar button so the
 * BetterInventory screens render the same buttons other inventories get.
 */
public final class FtbSidebarCompat {
    private static final String BUTTON_CLASS = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton";
    private static Constructor<?> buttonCtor;
    private static boolean resolved;
    private static Class<?> buttonClass;

    private FtbSidebarCompat() {}

    private static synchronized void resolve() {
        if (!resolved) {
            resolved = true;
            if (ModPresence.isLoaded("ftblibrary")) {
                try {
                    buttonClass = Class.forName(BUTTON_CLASS);
                    buttonCtor = buttonClass.getDeclaredConstructor();
                    buttonCtor.setAccessible(true);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    buttonClass = null;
                    buttonCtor = null;
                }
            }
        }
    }

    public static boolean available() {
        resolve();
        return buttonCtor != null;
    }

    public static AbstractWidget createIfMissing(List<?> existing) {
        resolve();
        if (buttonCtor == null) {
            return null;
        }
        for (Object r : existing) {
            if (buttonClass.isInstance(r)) {
                return null;
            }
        }
        try {
            return (AbstractWidget) buttonCtor.newInstance();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }
}
