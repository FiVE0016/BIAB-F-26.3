package dev.fallingcloud.betterinventory.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import dev.fallingcloud.betterinventory.client.ClientHudState;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * The HUD additions: the active-tool slot right of the hotbar and the offhand
 * carousel selector on the left.
 *
 * <p>Cells come from our own {@code hud.png}, drawn at vanilla hotbar proportions
 * (22x22, item inset +3,+3 as in Gui) so they still read as real hotbar slots. Using
 * our own texture rather than slicing the vanilla {@code hud/hotbar} sprite keeps the
 * slot tintable, which the slide/fade animation relies on.
 */
public final class BetterInventoryHud {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("betterinventory", "textures/gui/hud.png");
    private static final int TEX = 64;
    private static final int CELL = 22;
    private static final int ITEM_INSET = 3;
    /** Sprite origins within hud.png. */
    private static final int CELL_IDLE_V = 0;
    private static final int CELL_ACTIVE_V = 24;
    private static final int SEL_U = 24;
    private static final int SEL_W = 24;
    private static final int SEL_H = 23;

    private BetterInventoryHud() {}

    private static boolean hidden(Minecraft minecraft) {
        return minecraft.player == null
                || minecraft.level == null
                || minecraft.showOnlyReducedInfo()
                || minecraft.player.isSpectator();
    }

    /** One hotbar-proportioned cell; {@code active} brightens the border. */
    private static void cell(GuiGraphicsExtractor graphics, int x, int y, boolean active) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, active ? CELL_ACTIVE_V : CELL_IDLE_V, CELL, CELL, TEX, TEX);
    }

    private static void selection(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x - 1, y - 1, SEL_U, 0, SEL_W, SEL_H, TEX, TEX);
    }

    private static void item(GuiGraphicsExtractor graphics, Minecraft minecraft, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.item(stack, x + ITEM_INSET, y + ITEM_INSET);
        graphics.itemDecorations(minecraft.font, stack, x + ITEM_INSET, y + ITEM_INSET);
    }

    public static void renderToolSlot(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        // The slot exists only for auto-selected tools - it never mirrors a plain
        // hotbar item, and slides away when no tool is engaged.
        float show = ClientHudState.lerpToolShow(delta.getGameTimeDeltaPartialTick(false));
        if (show <= 0.01f) {
            return;
        }
        ItemStack tool = ClientHudState.activeTool;
        if (tool == null || tool.isEmpty()) {
            // Mid-retract: keep drawing the last tool until the slide finishes.
            tool = lastTool;
            if (tool == null || tool.isEmpty()) {
                return;
            }
        } else {
            lastTool = tool;
        }

        int centre = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - CELL;
        int x = centre + 91 + 4;
        // Left-handed players get vanilla's offhand box on this side; step clear of it.
        if (minecraft.player.getMainArm() == HumanoidArm.LEFT && !minecraft.player.getOffhandItem().isEmpty()) {
            x += 29;
        }

        // Ease-out rise from below the screen edge, fading as it goes.
        float eased = 1.0f - (1.0f - show) * (1.0f - show);
        int rise = Math.round((1.0f - eased) * (CELL + 6));

        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0f, rise);
        cell(graphics, x, y, true);
        selection(graphics, x, y);
        // 26.3: colour is part of the render state, not an immediate GL call
        if (eased > 0.35f) {
            item(graphics, minecraft, tool, x, y);
        }
        graphics.pose().popMatrix();
    }

    /** Held so the slot can finish its retract animation after the tool clears. */
    @Nullable
    private static ItemStack lastTool;

    public static void renderOffhandSelector(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hidden(minecraft)) {
            return;
        }
        float progress = ClientHudState.lerpProgress(delta.getGameTimeDeltaPartialTick(false));
        if (progress <= 0.01f) {
            return;
        }
        PlayerLoadout loadout = ClientHudState.loadout(minecraft);
        int centre = graphics.guiWidth() / 2;
        // Aligned with vanilla's offhand box position so the column grows out of it.
        int x = centre - 91 - 29;
        int baseY = graphics.guiHeight() - CELL;

        for (int i = 0; i < 4; i++) {
            int y = baseY - Math.round((CELL + 2) * (i + 1) * progress);
            boolean selected = i == ClientHudState.pendingIndex;
            cell(graphics, x, y, selected);
            if (selected) {
                selection(graphics, x, y);
            }
            // 26.3: colour is part of the render state, not an immediate GL call
            if (progress > 0.6f) {
                ItemStack stack = i == loadout.activeOffhand
                        ? minecraft.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
                        : loadout.offhandStore.getStackInSlot(i);
                item(graphics, minecraft, stack, x, y);
            }
        }
    }
}
