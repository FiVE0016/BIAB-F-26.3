package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Renders the zoomed view of a hovered item inside the circular side panel. */
public final class ZoomRender {
    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/inventory.png");

    /** Depth GuiGraphicsExtractor#renderItem bakes in; mirrored so scaling keeps items at GUI depth. */
    private static final float ITEM_BASE_Z = 150.0f;

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
        // Scale UNIFORMLY: vanilla renders items at scale (16, -16, 16), so an extra
        // non-uniform scale skews the normal matrix and diffuse-lit block models come
        // out visibly darker than flat items. The z pre-translate cancels the depth
        // that renderItem's own 150 gains from being scaled, keeping the zoomed item
        // at normal GUI depth instead of floating in front of tooltips.
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, -8, -8);
        graphics.pose().popMatrix();
    }
}
