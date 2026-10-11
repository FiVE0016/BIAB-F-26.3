package dev.fallingcloud.betterinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.compat.SlateBuildingCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/**
 * 1.4.1 offhand carousel key: Left Alt by default, Right Alt with Slate
 * Building (which holds Left Alt for its block wheel), with a one-time
 * migration of the bound key.
 */
public final class OffhandKey {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean migrationChecked;

    private OffhandKey() {}

    public static int defaultKey() {
        return SlateBuildingCompat.loaded() ? InputConstants.KEY_RALT : InputConstants.KEY_LALT;
    }

    public static void migrateOnce(Minecraft minecraft) {
        if (!migrationChecked) {
            if (BetterInventoryConfig.CLIENT_SPEC.isLoaded() && minecraft.options != null) {
                migrationChecked = true;
                if (SlateBuildingCompat.loaded() && !BetterInventoryConfig.OFFHAND_KEY_MIGRATED.get()) {
                    KeyMapping mapping = BetterInventoryClient.OFFHAND_SELECTOR;
                    InputConstants.Key bound = ClientPlatform.boundKey(mapping);
                    if (bound.getType() == InputConstants.Type.KEYBOARD
                            && bound.getValue() == InputConstants.KEY_LALT) {
                        mapping.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RALT));
                        KeyMapping.resetMapping();
                        minecraft.options.save();
                        LOGGER.info("[Better Inventory] Offhand carousel key moved from Left Alt to Right Alt"
                                + " (Slate Building uses Left Alt). Rebind it in Controls if you prefer another key.");
                    }
                    BetterInventoryConfig.OFFHAND_KEY_MIGRATED.set(true);
                    BetterInventoryConfig.CLIENT_SPEC.save();
                }
            }
        }
    }

    /**
     * 26.3 adaptation: the raw GLFW input API (InputConstants.isKeyDown /
     * glfwGetMouseButton) is no longer on the classpath, so the held state is
     * simply the mapping's own isDown() (which already covers keys and mouse
     * buttons internally).
     */
    public static boolean held() {
        return BetterInventoryClient.OFFHAND_SELECTOR.isDown();
    }
}
