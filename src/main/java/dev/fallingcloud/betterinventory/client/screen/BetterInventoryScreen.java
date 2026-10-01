package dev.fallingcloud.betterinventory.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.menu.TabStorage;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;

public class BetterInventoryScreen extends AbstractContainerScreen<BetterInventoryMenu> {

    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/inventory.png");
    /** ~45% black: dims a slot without hiding what is in it. */
    private static final int DIM_COLOR = 0x73101014;

    public BetterInventoryScreen(BetterInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BetterInventoryLayout.imageWidth(menu.curiosPresent()), BetterInventoryLayout.IMAGE_H);
                
    }

    private void panel(GuiGraphicsExtractor graphics, int[] sprite, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y, sprite[0], sprite[1], sprite[2], sprite[3],
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
    }

    /** Set in init(): whether there is room on screen for the side panels. */
    private boolean showZoomPanel;
    private boolean showCharPanel;

    @Override
    protected void init() {
        super.init();
        // Labels are drawn by the panels themselves.
        this.titleLabelX = -10000;
        this.titleLabelY = -10000;
        this.inventoryLabelX = -10000;
        this.inventoryLabelY = -10000;

        // The zoom circle and character panel float outside imageWidth, so centring
        // only the grid pushes them off-screen on narrow windows / large GUI scales.
        // Centre the whole composition instead, and drop the side panels entirely
        // when even that will not fit.
        int leftExtra = BetterInventoryLayout.ZOOM_SIZE + BetterInventoryLayout.ZOOM_GAP;
        int rightExtra = BetterInventoryLayout.CHAR_GAP + BetterInventoryLayout.CHAR_W;
        int total = leftExtra + imageWidth + rightExtra;
        if (total + 4 <= width) {
            leftPos = (width - total) / 2 + leftExtra;
            showZoomPanel = true;
            showCharPanel = true;
        } else {
            leftPos = (width - imageWidth) / 2;
            showZoomPanel = leftPos - leftExtra >= 2;
            showCharPanel = leftPos + imageWidth + rightExtra <= width - 2;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        boolean curios = menu.curiosPresent();

        // Blending stays on for the whole background pass: the panel sprites have
        // transparent corner pixels and the dim overlays are translucent.

        // Background is assembled from panel sprites so the curio column can be
        // left out completely when Curios is not installed.
        panel(graphics, BetterInventoryLayout.PANEL_2X2, BetterInventoryLayout.LEFT_X, BetterInventoryLayout.TOP_Y);
        panel(graphics, BetterInventoryLayout.PANEL_2X3, BetterInventoryLayout.LEFT_X, BetterInventoryLayout.MAIN_Y);
        panel(graphics, BetterInventoryLayout.PANEL_9X2, BetterInventoryLayout.CENTER_X, BetterInventoryLayout.TOP_Y);
        panel(graphics, BetterInventoryLayout.PANEL_9X3, BetterInventoryLayout.CENTER_X, BetterInventoryLayout.MAIN_Y);
        panel(graphics, BetterInventoryLayout.PANEL_9X1, BetterInventoryLayout.CENTER_X, BetterInventoryLayout.HOTBAR_Y);
        panel(graphics, BetterInventoryLayout.PANEL_2X2, BetterInventoryLayout.RIGHT_A_X, BetterInventoryLayout.TOP_Y);
        panel(graphics, BetterInventoryLayout.PANEL_2X3, BetterInventoryLayout.RIGHT_A_X, BetterInventoryLayout.MAIN_Y);
        if (curios) {
            panel(graphics, BetterInventoryLayout.PANEL_2X2, BetterInventoryLayout.RIGHT_B_X, BetterInventoryLayout.TOP_Y);
            panel(graphics, BetterInventoryLayout.PANEL_2X3, BetterInventoryLayout.RIGHT_B_X, BetterInventoryLayout.MAIN_Y);
        }

        // Tabs: the default inventory first, then the four backpacks. Exactly one is
        // always selected - there is no unselected state.
        for (int tab = 0; tab < PlayerLoadout.TAB_COUNT; tab++) {
            int tx = x + BetterInventoryLayout.TAB_X0 + tab * BetterInventoryLayout.TAB_SPACING;
            boolean available = menu.loadout.tabAvailable(tab);
            boolean selected = menu.activeTab() == tab;
            int state = available ? (selected ? 1 : 0) : 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, tx, y, BetterInventoryLayout.SPR_TAB_X,
                    BetterInventoryLayout.SPR_TAB_Y + state * (BetterInventoryLayout.TAB_H + 1),
                    BetterInventoryLayout.TAB_W, BetterInventoryLayout.TAB_H,
                    BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
            if (tab == 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, tx + 4, y + 2,
                        BetterInventoryLayout.SPR_TAB_ICON_X, BetterInventoryLayout.SPR_TAB_ICON_Y, 16, 16,
                        BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
            } else {
                ItemStack pack = menu.loadout.backpack(tab);
                if (!pack.isEmpty()) {
                    graphics.item(pack, tx + 4, y + 2);
                }
            }
        }

        // Crafting tab - a panel toggle rather than an inventory tab, so it uses the
        // selected/unselected states but never the disabled one.
        int craftX = x + BetterInventoryLayout.CRAFT_TAB_X;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, craftX, y, BetterInventoryLayout.SPR_TAB_X,
                BetterInventoryLayout.SPR_TAB_Y + (menu.craftingOpen() ? 1 : 0) * (BetterInventoryLayout.TAB_H + 1),
                BetterInventoryLayout.TAB_W, BetterInventoryLayout.TAB_H,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, craftX + 4, y + 2,
                BetterInventoryLayout.SPR_CRAFT_ICON_X, BetterInventoryLayout.SPR_CRAFT_ICON_Y, 16, 16,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);

        boolean settingsHover = isHovering(BetterInventoryLayout.SETTINGS_X, BetterInventoryLayout.SETTINGS_Y,
                BetterInventoryLayout.SETTINGS_SIZE, BetterInventoryLayout.SETTINGS_SIZE, mouseX, mouseY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BetterInventoryLayout.SETTINGS_X, y + BetterInventoryLayout.SETTINGS_Y,
                BetterInventoryLayout.SPR_SETTINGS_X,
                BetterInventoryLayout.SPR_SETTINGS_Y + (settingsHover ? BetterInventoryLayout.SETTINGS_SIZE + 1 : 0),
                BetterInventoryLayout.SETTINGS_SIZE, BetterInventoryLayout.SETTINGS_SIZE,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);

        // Dim (not blank out) the gather hub while no backpack is equipped.
        if (!menu.gatherAccessible()) {
            drawRegionOverlay(graphics, x + BetterInventoryLayout.CENTER_X + BetterInventoryLayout.PAD,
                    y + BetterInventoryLayout.TOP_Y + BetterInventoryLayout.PAD, 9, 2);
        }
        if (curios) {
            greyCurios(graphics, x, y);
        }

        // Character panel
        if (!showCharPanel) {
            return;
        }
        int charX = BetterInventoryLayout.charX(curios);
        int panelY = y + BetterInventoryLayout.CHAR_Y;

        // The crafting tab replaces the character view in the same footprint.
        if (menu.craftingOpen()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + charX, panelY,
                    BetterInventoryLayout.SPR_CRAFT_X, BetterInventoryLayout.SPR_CRAFT_Y,
                    BetterInventoryLayout.CHAR_W, BetterInventoryLayout.CHAR_H,
                    BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
            // Dim every cell outside the installed upgrade's extent, so the panel
            // shows at a glance what the next upgrade unlocks.
            int cols = menu.loadout.craftingCols();
            int rows = menu.loadout.craftingRows();
            for (int row = 0; row < BetterInventoryLayout.CRAFT_ROWS; row++) {
                for (int col = 0; col < BetterInventoryLayout.CRAFT_COLS; col++) {
                    if (col < cols && row < rows) {
                        continue;
                    }
                    dimCell(graphics,
                            x + charX + BetterInventoryLayout.CRAFT_GRID_X + col * 18 - 1,
                            panelY + BetterInventoryLayout.CRAFT_GRID_Y + row * 18 - 1);
                }
            }
            return;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + charX, panelY,
                BetterInventoryLayout.SPR_CHAR_X, BetterInventoryLayout.SPR_CHAR_Y,
                BetterInventoryLayout.CHAR_W, BetterInventoryLayout.CHAR_H,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics,
                x + charX + BetterInventoryLayout.PAD + 1, panelY + BetterInventoryLayout.PAD + 1,
                x + charX + BetterInventoryLayout.CHAR_W - BetterInventoryLayout.PAD - 1,
                panelY + BetterInventoryLayout.CHAR_H - BetterInventoryLayout.PAD - 1,
                46, 0.0625f, mouseX, mouseY, minecraft.player);
    }

    /**
     * Translucent dim pass over a slot that is present but unavailable — the slot
     * underneath stays readable.
     *
     * <p>Uses fill(), not a blit of a translucent sprite: GuiGraphicsExtractor#blit does not
     * enable blending, so an alpha sprite paints 100% opaque (that is what turned
     * these regions solid black). fill() goes through RenderType.gui(), which
     * carries its own transparency state and always blends correctly.
     */
    private void dimCell(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, DIM_COLOR);
    }

    /** Dims curio cells past the count Curios actually exposes. */
    private void greyCurios(GuiGraphicsExtractor graphics, int x, int y) {
        int available = menu.curioCount();
        for (int i = available; i < 10; i++) {
            int cx;
            int cy;
            if (i < 4) {
                cx = x + BetterInventoryLayout.RIGHT_B_X + BetterInventoryLayout.PAD + (i % 2) * 18;
                cy = y + BetterInventoryLayout.TOP_Y + BetterInventoryLayout.PAD + (i / 2) * 18;
            } else {
                int j = i - 4;
                cx = x + BetterInventoryLayout.RIGHT_B_X + BetterInventoryLayout.PAD + (j % 2) * 18;
                cy = y + BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.PAD + (j / 2) * 18;
            }
            dimCell(graphics, cx, cy);
        }
    }

    private void drawRegionOverlay(GuiGraphicsExtractor graphics, int x, int y, int cols, int rows) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                dimCell(graphics, x + c * 18, y + r * 18);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderGhostLocks(graphics);
        renderZoom(graphics);
        renderTabTooltips(graphics, mouseX, mouseY);
        extractTooltip(graphics, mouseX, mouseY);
    }

    private void renderGhostLocks(GuiGraphicsExtractor graphics) {
        TabSettings settings = menu.activeSettings();
        if (settings.locks().isEmpty()) {
            return;
        }
        for (TabSettings.LockEntry lock : settings.locks()) {
            int viewIndex = lock.slot();
            Slot slot = slotForView(viewIndex);
            if (slot == null || slot.hasItem() || !slot.isActive()) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(lock.item());
            ItemStack ghost = new ItemStack(item);
            if (ghost.isEmpty()) {
                continue;
            }
            int gx = leftPos + slot.x;
            int gy = topPos + slot.y;
            graphics.fakeItem(ghost, gx, gy);
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, 0);
            graphics.fill(gx, gy, gx + 16, gy + 16, 0x9E8B8B8B);
            graphics.pose().popMatrix();
        }
    }

    private Slot slotForView(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= TabStorage.SIZE) {
            return null;
        }
        int slotIndex = viewIndex < TabStorage.GATHER_START
                ? BetterInventoryMenu.SLOT_MAIN + viewIndex
                : BetterInventoryMenu.SLOT_GATHER + (viewIndex - TabStorage.GATHER_START);
        return menu.slots.get(slotIndex);
    }

    private void renderZoom(GuiGraphicsExtractor graphics) {
        if (!BetterInventoryConfig.ZOOM_ENABLED.get() || !showZoomPanel) {
            return;
        }
        ItemStack stack = hoveredSlot != null && hoveredSlot.hasItem() ? hoveredSlot.getItem() : ItemStack.EMPTY;
        int cx = leftPos + BetterInventoryLayout.ZOOM_X + BetterInventoryLayout.ZOOM_SIZE / 2;
        int cy = topPos + BetterInventoryLayout.ZOOM_Y + BetterInventoryLayout.ZOOM_SIZE / 2;
        dev.fallingcloud.betterinventory.client.ZoomRender.draw(graphics, cx, cy, stack);
    }

    private void renderTabTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int tab = 0; tab < PlayerLoadout.TAB_COUNT; tab++) {
            if (isHovering(BetterInventoryLayout.TAB_X0 + tab * BetterInventoryLayout.TAB_SPACING, 0,
                    BetterInventoryLayout.TAB_W, BetterInventoryLayout.TAB_H, mouseX, mouseY)) {
                Component text;
                if (tab == 0) {
                    text = Component.translatable("screen.betterinventory.tab.inventory");
                } else {
                    ItemStack pack = menu.loadout.backpack(tab);
                    text = pack.isEmpty()
                            ? Component.translatable("screen.betterinventory.tab.empty", tab)
                            : pack.getHoverName();
                }
                graphics.setTooltipForNextFrame(font, text, mouseX, mouseY);
            }
        }
        if (isHovering(BetterInventoryLayout.CRAFT_TAB_X, 0,
                BetterInventoryLayout.TAB_W, BetterInventoryLayout.TAB_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("screen.betterinventory.tab.crafting",
                    menu.loadout.craftingCols() + "x" + menu.loadout.craftingRows()), mouseX, mouseY);
        }
        if (isHovering(BetterInventoryLayout.SETTINGS_X, BetterInventoryLayout.SETTINGS_Y,
                BetterInventoryLayout.SETTINGS_SIZE, BetterInventoryLayout.SETTINGS_SIZE, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("screen.betterinventory.settings"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return onMouseClick(event, doubleClick);
    }

    /** Body of the old mouseClicked, now driven by the 26.3 event object. */
    private boolean onMouseClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            for (int tab = 0; tab < PlayerLoadout.TAB_COUNT; tab++) {
                if (isHovering(BetterInventoryLayout.TAB_X0 + tab * BetterInventoryLayout.TAB_SPACING, 0,
                        BetterInventoryLayout.TAB_W, BetterInventoryLayout.TAB_H, mouseX, mouseY)) {
                    // Clicking the open tab does nothing - a tab is always selected,
                    // so there is no "deselect" that would leave no inventory shown.
                    if (!menu.loadout.tabAvailable(tab) || menu.activeTab() == tab) {
                        return true;
                    }
                    // Do NOT set activeTab locally: switching physically swaps the
                    // inventory server-side, so the client must wait for that to come
                    // back or the grid and the tab highlight disagree for a frame.
                    PacketDistributor.sendToServer(new BetterInventoryPayloads.SwitchTab(tab));
                    return true;
                }
            }
            if (isHovering(BetterInventoryLayout.CRAFT_TAB_X, 0,
                    BetterInventoryLayout.TAB_W, BetterInventoryLayout.TAB_H, mouseX, mouseY)) {
                PacketDistributor.sendToServer(new BetterInventoryPayloads.ToggleCrafting());
                return true;
            }
            if (isHovering(BetterInventoryLayout.SETTINGS_X, BetterInventoryLayout.SETTINGS_Y,
                    BetterInventoryLayout.SETTINGS_SIZE, BetterInventoryLayout.SETTINGS_SIZE, mouseX, mouseY)) {
                PacketDistributor.sendToServer(new BetterInventoryPayloads.OpenSettings(menu.activeTab()));
                return true;
            }
        }
        if (button == 2 && handleLockClick()) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Middle-click slot locking: cursor item + empty slot locks, empty + empty clears. */
    private boolean handleLockClick() {
        if (!(hoveredSlot instanceof BetterInventoryMenu.StorageSlot slot) || slot.hasItem() || !slot.isActive()) {
            return false;
        }
        int viewIndex = slot.index;
        ItemStack carried = menu.getCarried();
        TabSettings settings = menu.activeSettings();
        Optional<Item> lock = settings.lockFor(viewIndex);
        int tab = menu.activeTab();
        if (!carried.isEmpty() && lock.isEmpty()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(carried.getItem());
            applyLockLocal(tab, viewIndex, Optional.of(id));
            PacketDistributor.sendToServer(new BetterInventoryPayloads.SetLock(tab, viewIndex, Optional.of(id)));
            return true;
        }
        if (carried.isEmpty() && lock.isPresent()) {
            applyLockLocal(tab, viewIndex, Optional.empty());
            PacketDistributor.sendToServer(new BetterInventoryPayloads.SetLock(tab, viewIndex, Optional.empty()));
            return true;
        }
        return false;
    }

    private void applyLockLocal(int tab, int viewIndex, Optional<Identifier> item) {
        TabSettings settings = BetterInventorySettings.raw(minecraft.player, tab);
        BetterInventorySettings.setRaw(minecraft.player, tab, settings.withLock(viewIndex, item));
    }
}
