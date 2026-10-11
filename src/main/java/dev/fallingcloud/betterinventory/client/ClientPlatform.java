package dev.fallingcloud.betterinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * 1.4.1 client platform seam. 26.3 adaptation: fabric's key binding helper
 * moved to the {@code client.keymapping.v1} package as KeyMappingHelper, and
 * Screen.hasShiftDown() is gone, so the shift state is read from
 * InputConstants (GLFW itself is not on the compile classpath).
 */
public final class ClientPlatform {
    private ClientPlatform() {}

    public static InputConstants.Key boundKey(KeyMapping mapping) {
        return KeyMappingHelper.getBoundKeyOf(mapping);
    }

    public static boolean shiftAsksForVanilla() {
        return BetterInventoryClient.OPEN_VANILLA_INVENTORY.isDown()
                && (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT));
    }
}
