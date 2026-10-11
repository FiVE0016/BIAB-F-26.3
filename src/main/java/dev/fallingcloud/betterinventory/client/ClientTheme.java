package dev.fallingcloud.betterinventory.client;

import net.minecraft.client.Minecraft;

/** 1.4.1 theme detection: the vanilla_style built-in pack restyles the GUI. */
public final class ClientTheme {
    private static final int GHOST_DARK = -1642456550;
    private static final int GHOST_VANILLA = -1635021941;

    private ClientTheme() {}

    public static boolean vanillaStyle() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourcePackRepository() == null) {
            return false;
        }
        for (String id : minecraft.getResourcePackRepository().getSelectedIds()) {
            if (id.contains("vanilla_style")) {
                return true;
            }
        }
        return false;
    }

    public static int ghostWash() {
        return vanillaStyle() ? GHOST_VANILLA : GHOST_DARK;
    }
}
