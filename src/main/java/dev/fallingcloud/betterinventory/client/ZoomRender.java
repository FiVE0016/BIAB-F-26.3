package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 zoom panel: the circular magnifier right of the main panels showing a
 * scaled-up copy of the hovered slot's item.
 *
 * <p>26.3 adaptations: {@code blit(RenderPipelines.GUI_TEXTURED, ...)} for the
 * panel and {@code pose()} (a 2D-only {@code Matrix3x2fStack}) for the scale;
 * the 1.21.1 z pre-translate is gone because 26.3 GUI rendering has no per-draw
 * depth on this path. Scaling stays uniform so diffuse-lit models keep their
 * brightness. The 1.4.1 static helpers {@code zoomArea}/{@code playerViewArea}
 * arrive with the F-2c screen ports (the 26.3 BetterInventoryScreen provides
 * the same rectangles directly).
 */
public final class ZoomRender {
    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/inventory.png");

    private ZoomRender() {}

    /** Draws the circle panel centered at (cx, cy) plus the item scaled up inside it. */
    public static void draw(GuiGraphicsExtractor graphics, int cx, int cy, ItemStack stack) {
        int half = BetterInventoryLayout.ZOOM_SIZE / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, cx - half, cy - half,
                BetterInventoryLayout.SPR_ZOOM_X, BetterInventoryLayout.SPR_ZOOM_Y,
                BetterInventoryLayout.ZOOM_SIZE, BetterInventoryLayout.ZOOM_SIZE,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
        if (stack.isEmpty()) {
            return;
        }
        float scale = BetterInventoryConfig.ZOOM_SCALE.get().floatValue();
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, -8, -8);
        graphics.pose().popMatrix();
    }
}
