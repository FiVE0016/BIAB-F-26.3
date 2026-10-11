package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.platform.ModPresence;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.4.1 compat buttons: up to four mod-provided shortcut buttons rendered in
 * the side panel (Accessories' open-screen button is the only entry today),
 * with hover highlighting and reflection-guarded activation.
 *
 * <p>26.3 adaptations: {@code blit(RenderPipelines.GUI_TEXTURED, ...)} replaces
 * drawTexture, {@code ModPresence.isLoaded} is the mod-detection seam, and the
 * 1.21.1 pose stack translate-then-fill hover box is a plain {@code fill} (the
 * 26.3 pose stack is 2D-only and GUI fills need no z bump).
 */
public final class CompatButtons {
    private static final Logger LOGGER = LoggerFactory.getLogger("betterinventory-compat");
    private static final int CELL = 18;
    private static final int HOVER = -2130706433;
    private static final Identifier ACCESSORIES_ICON =
            Identifier.fromNamespaceAndPath("accessories", "textures/gui/accessories_open_icon.png");
    private static final Identifier ACCESSORIES_ICON_HOVER =
            Identifier.fromNamespaceAndPath("accessories", "textures/gui/accessories_open_icon_hovered.png");
    @Nullable
    private static List<Entry> entries;

    private CompatButtons() {}

    private static int centre(int origin, int size) {
        return origin + (16 - size) / 2;
    }

    private static Icon texture(Identifier normal, Identifier hover, int w, int h) {
        return (g, x, y, hovered) ->
                g.blit(RenderPipelines.GUI_TEXTURED, hovered ? hover : normal,
                        centre(x, w), centre(y, h), 0, 0, w, h, w, h);
    }

    private static synchronized List<Entry> entries() {
        if (entries != null) {
            return entries;
        }
        List<Entry> list = new ArrayList<>();
        if (ModPresence.isLoaded("accessories")) {
            list.add(new Entry(
                    "accessories",
                    "screen.betterinventory.compat.accessories",
                    texture(ACCESSORIES_ICON, ACCESSORIES_ICON_HOVER, 8, 8),
                    CompatButtons::openAccessories));
        }
        entries = List.copyOf(list.subList(0, Math.min(4, list.size())));
        return entries;
    }

    private static int cellX(int panelX, int index) {
        return panelX + 3 + index % 2 * 18;
    }

    private static int cellY(int panelY, int index) {
        return panelY + 3 + index / 2 * 18;
    }

    private static boolean inCell(int panelX, int panelY, int index, double mouseX, double mouseY) {
        int cx = cellX(panelX, index);
        int cy = cellY(panelY, index);
        return mouseX >= cx && mouseX < cx + 18 && mouseY >= cy && mouseY < cy + 18;
    }

    public static void render(GuiGraphicsExtractor graphics, int panelX, int panelY, double mouseX, double mouseY) {
        List<Entry> list = entries();
        for (int i = 0; i < list.size(); i++) {
            Entry entry = list.get(i);
            boolean hovered = inCell(panelX, panelY, i, mouseX, mouseY);
            int ix = cellX(panelX, i) + 1;
            int iy = cellY(panelY, i) + 1;
            if (!entry.iconBroken) {
                try {
                    entry.icon.draw(graphics, ix, iy, hovered);
                } catch (RuntimeException | LinkageError e) {
                    entry.iconBroken = true;
                    LOGGER.warn("Better Inventory: the {} button icon could not be drawn; leaving the cell blank", entry.modId, e);
                }
            }
            if (hovered) {
                graphics.fill(ix, iy, ix + 16, iy + 16, HOVER);
            }
        }
    }

    @Nullable
    public static Entry hovered(int panelX, int panelY, double mouseX, double mouseY) {
        List<Entry> list = entries();
        for (int i = 0; i < list.size(); i++) {
            if (inCell(panelX, panelY, i, mouseX, mouseY)) {
                return list.get(i);
            }
        }
        return null;
    }

    public static boolean click(int panelX, int panelY, double mouseX, double mouseY) {
        Entry entry = hovered(panelX, panelY, mouseX, mouseY);
        if (entry == null) {
            return false;
        }
        if (!entry.broken) {
            try {
                entry.action.run();
            } catch (RuntimeException | LinkageError e) {
                entry.broken = true;
                LOGGER.warn("Better Inventory: the {} button no longer matches that mod's classes and is disabled", entry.modId, e);
            }
        }
        return true;
    }

    private static void openAccessories() {
        try {
            Method open = Class.forName("io.wispforest.accessories.client.AccessoriesClient").getMethod("attemptToOpenScreen");
            open.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot call io.wispforest.accessories.client.AccessoriesClient.attemptToOpenScreen", e);
        }
    }

    public static final class Entry {
        private final String modId;
        private final Supplier<Component> label;
        private final Icon icon;
        private final Runnable action;
        private boolean broken;
        private boolean iconBroken;

        private Entry(String modId, String labelKey, Icon icon, Runnable action) {
            this.modId = modId;
            this.label = () -> Component.translatable(labelKey);
            this.icon = icon;
            this.action = action;
        }

        public Component label() {
            return this.label.get();
        }
    }

    private interface Icon {
        void draw(GuiGraphicsExtractor graphics, int x, int y, boolean hovered);
    }
}
