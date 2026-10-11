package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.4.1 worn-tool floor: a worn (not broken) tool never digs slower than the
 * midpoint between its full-durability speed and bare-hand speed.
 */
public final class WornToolFloor {
    private WornToolFloor() {}

    public static float floorFor(Player player, ItemStack tool, BlockState state) {
        if (!BetterInventoryConfig.wornToolFloorEnabled() || tool.isEmpty() || !tool.isDamageableItem()) {
            return -1.0F;
        }
        if (BrokenTools.isUnusable(tool)) {
            return -1.0F;
        }
        ItemStack fresh = tool.copy();
        fresh.set(DataComponents.DAMAGE, 0);
        float full = ToolService.digSpeedWith(player, fresh, state);
        float fist = ToolService.digSpeedWith(player, ItemStack.EMPTY, state);
        return full <= fist ? -1.0F : (full + fist) * 0.5F;
    }
}
