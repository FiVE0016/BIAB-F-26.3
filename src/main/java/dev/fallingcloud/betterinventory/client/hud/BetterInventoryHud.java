package dev.fallingcloud.betterinventory.client.hud;

import dev.fallingcloud.betterinventory.client.ClientHudState;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 HUD additions: the active-tool slot right of the hotbar and the
 * offhand carousel selector column on the left, built from the vanilla
 * {@code hud/hotbar} and {@code hud/hotbar_selection} sprites.
 *
 * <p>26.3 adaptations: sprite slicing uses
 * {@code blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h, u, v, uW, vH)}
 * (drawGuiTexture is gone), the slide transform goes through the 2D
 * {@code pose()} stack, and the 1.21.1 {@code options.hudHidden} flag no longer
 * exists - {@code Minecraft.showOnlyReducedInfo()} is the closest equivalent
 * and is used here. The 1.4.1 alpha fade tint is dropped: 26.3 has no per-draw
 * colour API on the GUI extractor.
 */
public final class BetterInventoryHud {
    private static final Identifier HOTBAR = Identifier.fromNamespaceAndPath("minecraft", "hud/hotbar");
    private static final Identifier HOTBAR_SELECTION = Identifier.fromNamespaceAndPath("minecraft", "hud/hotbar_selection");
    private static final int HOTBAR_W = 182;
    private static final int HOTBAR_H = 22;
    private static final int CELL = 22;
    private static final int HALF = 11;
    private static final int ITEM_INSET = 3;
    private static final int SEL_W = 24;
    private static final int SEL_H = 23;
    @Nullable
    private static ItemStack lastTool;

    private BetterInventoryHud() {}

    private static boolean hidden(Minecraft minecraft) {
        return minecraft.player == null
                || minecraft.level == null
                || minecraft.showOnlyReducedInfo()
                || minecraft.player.isSpectator();
    }

    /** One hotbar-proportioned cell, spliced from the vanilla hotbar sprite's halves. */
    private static void cell(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, x, y, HALF, HOTBAR_H, 0, 0, HALF, HOTBAR_H);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, x + HALF, y, HALF, HOTBAR_H, 171, 0, HALF, HOTBAR_H);
    }

    private static void selection(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, x - 1, y - 1, SEL_W, SEL_H);
    }

    private static void item(GuiGraphicsExtractor graphics, Minecraft minecraft, ItemStack stack, int x, int y) {
        if (!stack.isEmpty()) {
            graphics.item(stack, x + ITEM_INSET, y + ITEM_INSET);
            graphics.itemDecorations(minecraft.font, stack, x + ITEM_INSET, y + ITEM_INSET);
        }
    }

    public static void renderToolSlot(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        float show = ClientHudState.lerpToolShow(delta.getGameTimeDeltaPartialTick(false));
        if (show <= 0.01F) {
            return;
        }
        ItemStack tool = ClientHudState.activeTool;
        if (tool != null && !tool.isEmpty()) {
            lastTool = tool;
        } else {
            tool = lastTool;
            if (tool == null || tool.isEmpty()) {
                return;
            }
        }

        int centre = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - CELL;
        int x = centre + 91 + 4;
        // Left-handed players get vanilla's offhand box on this side; step clear of it.
        if (minecraft.player.getMainArm() == HumanoidArm.LEFT && !minecraft.player.getOffhandItem().isEmpty()) {
            x += 29;
        }

        float eased = 1.0F - (1.0F - show) * (1.0F - show);
        int rise = Math.round((1.0F - eased) * 28.0F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, rise);
        cell(graphics, x, y);
        selection(graphics, x, y);
        if (eased > 0.35F) {
            item(graphics, minecraft, tool, x, y);
        }
        graphics.pose().popMatrix();
    }

    public static void renderOffhandSelector(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        float progress = ClientHudState.lerpProgress(delta.getGameTimeDeltaPartialTick(false));
        if (progress <= 0.01F) {
            return;
        }
        PlayerLoadout loadout = ClientHudState.loadout(minecraft);
        int centre = graphics.guiWidth() / 2;
        int x = centre - 91 - 29;
        int baseY = graphics.guiHeight() - CELL;

        for (int i = 0; i < 4; i++) {
            int y = baseY - Math.round(24 * (i + 1) * progress);
            boolean selected = i == ClientHudState.pendingIndex;
            cell(graphics, x, y);
            if (selected) {
                selection(graphics, x, y);
            }
            if (progress > 0.6F) {
                ItemStack stack = i == loadout.activeOffhand
                        ? minecraft.player.getItemBySlot(EquipmentSlot.OFFHAND)
                        : loadout.offhandStore.getStackInSlot(i);
                item(graphics, minecraft, stack, x, y);
            }
        }
    }
}
