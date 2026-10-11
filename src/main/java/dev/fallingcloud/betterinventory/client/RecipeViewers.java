package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.platform.ModPresence;

/** 1.4.1 recipe viewer detection (JEI/EMI/Slate Crafting/REI). */
public final class RecipeViewers {
    private static final String[] MOD_IDS =
            new String[]{"jei", "emi", "slate_crafting", "roughlyenoughitems"};
    private static Boolean installed;

    private RecipeViewers() {}

    public static boolean installed() {
        Boolean cached = installed;
        if (cached == null) {
            cached = false;
            for (String id : MOD_IDS) {
                if (ModPresence.isLoaded(id)) {
                    cached = true;
                    break;
                }
            }
            installed = cached;
        }
        return cached;
    }
}
