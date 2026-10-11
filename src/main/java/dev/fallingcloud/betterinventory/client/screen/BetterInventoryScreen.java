package dev.fallingcloud.betterinventory.client.screen;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import dev.fallingcloud.betterinventory.client.ClientTheme;
import dev.fallingcloud.betterinventory.client.CompatButtons;
import dev.fallingcloud.betterinventory.client.CreativeInventory;
import dev.fallingcloud.betterinventory.client.EffectRow;
import dev.fallingcloud.betterinventory.client.FtbSidebarCompat;
import dev.fallingcloud.betterinventory.client.InventorySwap;
import dev.fallingcloud.betterinventory.client.RecipeViewers;
import dev.fallingcloud.betterinventory.client.ZoomRender;
import dev.fallingcloud.betterinventory.compat.SlateBuildingCompat;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.FurnaceState;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 overhauled inventory screen: 72-slot tabbed storage, armor grid plus
 * armor store, offhand carousel, tool rack, backpack/pack-upgrade slots,
 * crafting or furnace panel, trash, toolbox, effect row, compat buttons, the
 * vanilla swap button and the zoom magnifier.
 *
 * <p>26.3 adaptations: extractBackground/extractRenderState/containerTick
 * replace drawBackground/render/handledScreenTick, {@code isHovering} replaces
 * isPointWithinBounds, right-click is SDL button code 3, the player preview is
 * {@code InventoryScreen.extractEntityInInventoryFollowsMouse}, GUI sprites
 * (furnace flame) use {@code blitSprite}, borders are four fills, and the
 * per-draw shader colour of the 1.4.1 furnace flame is dropped (26.3 has no
 * immediate colour API on the GUI extractor). The zoom panel width is chosen
 * in the constructor because 26.3's image size is final.
 */
public class BetterInventoryScreen extends AbstractContainerScreen<BetterInventoryMenu> {
    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/inventory.png");
    private static final int LOCKED_MARK = -1644822;
    private static final int LOCKED_MARK_SHADOW = -1072952304;
    private static final int BOARD_COLOR = -15263973;
    private static final int BOARD_EDGE = -14013903;
    private static final int GLYPH_COLOR = -13421773;
    private static final int LOCK_BODY = -10724250;
    private static final int LOCK_HI = -8158321;
    private static final int LOCK_HOLE = -14277076;
    private static final int BAR_EDGE = -15065821;
    private static final int BAR_FACE = -9537914;
    private static final int BAR_FACE_HOVER = -7432280;
    private static final int BAR_LIGHT = -6642764;
    private static final int BAR_LIGHT_HOVER = -4800562;
    private static final int BAR_GLYPH = -854535;
    private static final int TRASH_ARMED = -4179669;
    private final boolean showZoomPanel;
    private static final Identifier FURNACE_FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final ItemStack FURNACE_ICON = new ItemStack(Items.FURNACE);

    public BetterInventoryScreen(BetterInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title,
                BetterInventoryConfig.ZOOM_ENABLED.get()
                        && BetterInventoryLayout.zoomFits(Minecraft.getInstance().getWindow().getGuiScaledWidth(), RecipeViewers.installed())
                        ? BetterInventoryLayout.IMAGE_W_ZOOM
                        : BetterInventoryLayout.IMAGE_W,
                BetterInventoryLayout.IMAGE_H);
        this.showZoomPanel = imageWidth == BetterInventoryLayout.IMAGE_W_ZOOM;
    }

    private void panel(GuiGraphicsExtractor graphics, int[] sprite, int x, int y) {
        int dx = sprite.length > 4 ? sprite[4] : 0;
        int dy = sprite.length > 5 ? sprite[5] : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x + dx, topPos + y + dy,
                sprite[0], sprite[1], sprite[2], sprite[3], BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -10000;
        this.titleLabelY = -10000;
        this.inventoryLabelX = -10000;
        this.inventoryLabelY = -10000;
        this.leftPos = BetterInventoryLayout.leftPos(width);
        AbstractWidget ftbButtons = FtbSidebarCompat.createIfMissing(children());
        if (ftbButtons != null) {
            addRenderableWidget(ftbButtons);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        panel(graphics, BetterInventoryLayout.PANEL_2X2, 82, 20);
        panel(graphics, BetterInventoryLayout.PANEL_2X2, 82, 74);
        panel(graphics, BetterInventoryLayout.PANEL_2X3, 82, 125);
        panel(graphics, BetterInventoryLayout.CENTER_STRIP, 132, 20);
        panel(graphics, BetterInventoryLayout.PANEL_2X2, 308, 20);
        panel(graphics, BetterInventoryLayout.PANEL_2X2, 308, 74);
        panel(graphics, BetterInventoryLayout.PANEL_2X3, 308, 125);
        panel(graphics, BetterInventoryLayout.PANEL_1X1, 317, 192);
        if (menu.trashLoaded()) {
            border(graphics, x + 317 + 3, y + 192 + 3, 18, 18, TRASH_ARMED);
        }
        if (SlateBuildingCompat.loaded()) {
            panel(graphics, BetterInventoryLayout.PANEL_1X1, 91, 192);
        }

        for (int tab = 0; tab < 5; tab++) {
            int tx = x + 134 + tab * 24;
            boolean available = menu.loadout.tabAvailable(tab);
            boolean selected = menu.activeTab() == tab;
            int state = available ? (selected ? 1 : 0) : 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, tx, y, 220.0F, 0 + state * 21, 24, 20,
                    BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
            if (available) {
                String label = String.valueOf(tab);
                graphics.text(font, label, tx + (24 - font.width(label)) / 2, y + 7, GLYPH_COLOR, false);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, tx + 4, y + 3, 220.0F, 168.0F, 16, 16,
                        BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
            }
        }

        int craftX = x + 254;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, craftX, y, 220.0F,
                0 + (!menu.craftingOpen() && !menu.furnaceOpen() ? 0 : 1) * 21, 24, 20,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
        if (menu.furnaceOpen()) {
            graphics.item(FURNACE_ICON, craftX + 4, y + 3);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, craftX + 4, y + 3, 220.0F, 150.0F, 16, 16,
                    BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
        }

        if (CreativeInventory.active(minecraft)) {
            int itemsX = x + 91;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, itemsX, y, 220.0F, 0.0F, 24, 20,
                    BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
            graphics.item(CreativeInventory.itemsIcon(), itemsX + 4, y + 3);
        }

        boolean settingsHover = isHovering(280, 0, 20, 20, mouseX, mouseY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 280, y + 0, 220.0F, 66 + (settingsHover ? 21 : 0), 20, 20,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
        if (!menu.gatherAccessible()) {
            int bx = x + 132 + 3;
            int by = y + 20 + 3;
            int bh = 90;
            graphics.fill(bx, by, bx + 162, by + bh, BOARD_COLOR);
            border(graphics, bx, by, 162, bh, BOARD_EDGE);
            renderLockIcon(graphics, bx + 81 - 5, by + bh / 2 - 6);
        }
        if (!menu.loadout.upgradesUnlocked()) {
            int ux = x + 308 + 3 + 18;
            int uy = y + 125 + 3;
            graphics.fill(ux, uy, ux + 18, uy + 54, BOARD_COLOR);
            border(graphics, ux, uy, 18, 54, BOARD_EDGE);
            renderLockIcon(graphics, ux + 4, uy + 27 - 6);
        }

        renderArmorSwapBar(graphics, mouseX, mouseY);
        CompatButtons.render(graphics, x + 82, y + 20, mouseX, mouseY);
        EffectRow.render(graphics, font, x, y);
        int panelX = x + 0;
        int panelY = y + 37;
        if (!menu.craftingOpen()) {
            if (menu.furnaceOpen()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, panelX, panelY, 256.0F, 116.0F, 78, 130,
                        BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
                renderFurnace(graphics, panelX, panelY);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, panelX, panelY, 352.0F, 0.0F, 78, 130,
                        BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                        graphics, panelX + 3 + 1, panelY + 3 + 1, panelX + 78 - 3 - 1, panelY + 130 - 3 - 1,
                        46, 0.0625F, mouseX, mouseY, minecraft.player);
            }
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, panelX, panelY, 432.0F, 0.0F, 78, 130,
                    BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
            int cols = menu.loadout.craftingCols();
            int rows = menu.loadout.craftingRows();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (col >= cols || row >= rows) {
                        lockCraftCell(graphics, panelX + 12 + col * 18 - 1, panelY + 12 + row * 18 - 1);
                    }
                }
            }
        }
    }

    private void renderFurnace(GuiGraphicsExtractor graphics, int panelX, int panelY) {
        FurnaceState furnace = menu.loadout.furnace;
        int flameX = panelX + 31;
        int flameY = panelY + 31;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FURNACE_FLAME, flameX, flameY, 14, 14);
        if (furnace.lit > 0 && furnace.litDuration > 0) {
            int lit = Mth.clamp((int) Math.ceil(13.0F * furnace.lit / furnace.litDuration) + 1, 1, 14);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FURNACE_FLAME, flameX, flameY + 14 - lit, 14, lit, 0, 14 - lit, 14, lit);
        }
        if (furnace.cook > 0 && furnace.cookTotal > 0) {
            int cooked = Mth.clamp((int) Math.ceil(18.0F * furnace.cook / furnace.cookTotal), 0, 18);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, panelX + 31, panelY + 66, 336.0F, 116.0F, 16, cooked,
                    BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
        }
    }

    private boolean creativeTabHovered(double mouseX, double mouseY) {
        return CreativeInventory.active(minecraft) && isHovering(91, 0, 24, 20, mouseX, mouseY);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        CreativeInventory.tick(menu);
    }

    private boolean armorSwapHovered(double mouseX, double mouseY) {
        return isHovering(308, 63, 42, 10, mouseX, mouseY);
    }

    private void renderArmorSwapBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int bx = leftPos + 308;
        int by = topPos + 63;
        int w = 42;
        int h = 10;
        boolean hovered = armorSwapHovered(mouseX, mouseY);
        graphics.fill(bx + 1, by, bx + w - 1, by + h, BAR_EDGE);
        graphics.fill(bx, by + 1, bx + w, by + h - 1, BAR_EDGE);
        graphics.fill(bx + 2, by + 2, bx + w - 2, by + h - 2, hovered ? BAR_FACE_HOVER : BAR_FACE);
        graphics.fill(bx + 2, by + 2, bx + w - 2, by + 3, hovered ? BAR_LIGHT_HOVER : BAR_LIGHT);
        int top = by + 2;
        int up = bx + w / 2 - 4;
        int down = bx + w / 2 + 3;
        graphics.fill(up, top, up + 1, top + 1, BAR_GLYPH);
        graphics.fill(up - 1, top + 1, up + 2, top + 2, BAR_GLYPH);
        graphics.fill(up, top + 2, up + 1, top + 5, BAR_GLYPH);
        graphics.fill(down, top, down + 1, top + 3, BAR_GLYPH);
        graphics.fill(down - 1, top + 3, down + 2, top + 4, BAR_GLYPH);
        graphics.fill(down, top + 4, down + 1, top + 5, BAR_GLYPH);
    }

    private void renderLockIcon(GuiGraphicsExtractor graphics, int ix, int iy) {
        graphics.fill(ix + 3, iy, ix + 7, iy + 1, LOCK_HI);
        graphics.fill(ix + 2, iy + 1, ix + 4, iy + 5, LOCK_HI);
        graphics.fill(ix + 6, iy + 1, ix + 8, iy + 5, LOCK_HI);
        graphics.fill(ix + 1, iy + 5, ix + 9, iy + 12, LOCK_BODY);
        graphics.fill(ix, iy + 6, ix + 10, iy + 11, LOCK_BODY);
        graphics.fill(ix + 1, iy + 5, ix + 9, iy + 6, LOCK_HI);
        graphics.fill(ix + 4, iy + 7, ix + 6, iy + 9, LOCK_HOLE);
        graphics.fill(ix + 4, iy + 9, ix + 6, iy + 11, LOCK_HOLE);
    }

    private void dimCell(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 220.0F, 112.0F, 18, 18,
                BetterInventoryLayout.TEX_W, BetterInventoryLayout.TEX_H);
    }

    private void lockCraftCell(GuiGraphicsExtractor graphics, int x, int y) {
        if (!BetterInventoryConfig.STRONG_LOCKED_SLOTS.get()) {
            dimCell(graphics, x, y);
        } else {
            cross(graphics, x - 1, y, LOCKED_MARK_SHADOW);
            cross(graphics, x + 1, y, LOCKED_MARK_SHADOW);
            cross(graphics, x, y - 1, LOCKED_MARK_SHADOW);
            cross(graphics, x, y + 1, LOCKED_MARK_SHADOW);
            cross(graphics, x, y, LOCKED_MARK);
        }
    }

    private void cross(GuiGraphicsExtractor graphics, int x, int y, int color) {
        for (int i = 0; i < 12; i++) {
            graphics.fill(x + 3 + i, y + 3 + i, x + 4 + i, y + 4 + i, color);
            graphics.fill(x + 14 - i, y + 3 + i, x + 15 - i, y + 4 + i, color);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderGhostLocks(graphics);
        renderSwapButton(graphics, mouseX, mouseY);
        renderZoom(graphics);
        renderTabTooltips(graphics, mouseX, mouseY);
        renderControlTooltips(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty()) {
            EffectRow.renderTooltip(graphics, font, leftPos, topPos, mouseX, mouseY);
        }
        extractTooltip(graphics, mouseX, mouseY);
    }

    private void renderZoom(GuiGraphicsExtractor graphics) {
        if (showZoomPanel) {
            ItemStack stack = hoveredSlot != null && hoveredSlot.hasItem() ? hoveredSlot.getItem() : ItemStack.EMPTY;
            ZoomRender.draw(graphics, leftPos + 366 + 50, topPos + 58 + 50, stack);
        }
    }

    @Nullable
    public int[] zoomArea() {
        return showZoomPanel
                ? new int[]{leftPos + 366, topPos + 58, 100, 100}
                : null;
    }

    public int[] playerViewArea() {
        return new int[]{leftPos + 0, topPos + 37, 78, 130};
    }

    private void renderSwapButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int bx = InventorySwap.buttonX(this);
        int by = InventorySwap.buttonY(this);
        boolean hovered = InventorySwap.hovering(bx, by, mouseX, mouseY);
        InventorySwap.render(graphics, bx, by, hovered);
        if (hovered) {
            graphics.setTooltipForNextFrame(font, InventorySwap.label(true), mouseX, mouseY);
        }
    }

    private void renderGhostLocks(GuiGraphicsExtractor graphics) {
        TabSettings settings = menu.activeSettings();
        if (settings.locks().isEmpty()) {
            return;
        }
        int wash = ClientTheme.ghostWash();
        for (TabSettings.LockEntry lock : settings.locks()) {
            int viewIndex = lock.slot();
            Slot slot = slotForView(viewIndex);
            if (slot != null && !slot.hasItem() && slot.isActive()) {
                Item item = BuiltInRegistries.ITEM.getValue(lock.item());
                ItemStack ghost = new ItemStack(item);
                if (!ghost.isEmpty()) {
                    int gx = leftPos + slot.x;
                    int gy = topPos + slot.y;
                    graphics.fakeItem(ghost, gx, gy);
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(0, 0);
                    graphics.fill(gx, gy, gx + 16, gy + 16, wash);
                    graphics.pose().popMatrix();
                }
            }
        }
    }

    private Slot slotForView(int viewIndex) {
        if (viewIndex >= 0 && viewIndex < 72) {
            int slotIndex = viewIndex < 27 ? 9 + viewIndex : 36 + (viewIndex - 27);
            return menu.slots.get(slotIndex);
        }
        return null;
    }

    private void renderTabTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int tab = 0; tab < 5; tab++) {
            if (isHovering(134 + tab * 24, 0, 24, 20, mouseX, mouseY)) {
                Component text;
                if (tab == 0) {
                    text = Component.translatable("screen.betterinventory.tab.inventory");
                } else if (menu.loadout.tabAvailable(tab)) {
                    text = Component.translatable("screen.betterinventory.tab.page", tab);
                } else {
                    text = Component.translatable("screen.betterinventory.tab.locked", tab + 1);
                }
                graphics.setTooltipForNextFrame(font, text, mouseX, mouseY);
            }
        }

        if (isHovering(254, 0, 24, 20, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(menu.furnaceOpen()
                    ? Component.translatable("screen.betterinventory.tab.furnace")
                    : Component.translatable("screen.betterinventory.tab.crafting",
                            menu.loadout.craftingCols() + "x" + menu.loadout.craftingRows()));
            if (menu.loadout.hasFurnace() && !menu.furnaceOpen()) {
                lines.add(Component.translatable("screen.betterinventory.tab.furnace.hint").withStyle(ChatFormatting.GRAY));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }

        if (creativeTabHovered(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("screen.betterinventory.tab.creative"), mouseX, mouseY);
        }

        if (isHovering(280, 0, 20, 20, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("screen.betterinventory.settings"), mouseX, mouseY);
        }
    }

    private void renderControlTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (armorSwapHovered(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("screen.betterinventory.armor_swap"), mouseX, mouseY);
        } else if (!menu.trashLoaded() && isHovering(321, 196, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("screen.betterinventory.trash"), mouseX, mouseY);
        } else if (SlateBuildingCompat.loaded()
                && !menu.slots.get(116).hasItem()
                && isHovering(95, 196, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("screen.betterinventory.toolbox"), mouseX, mouseY);
        } else {
            CompatButtons.Entry button = CompatButtons.hovered(leftPos + 82, topPos + 20, mouseX, mouseY);
            if (button != null) {
                graphics.setTooltipForNextFrame(font, button.label(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 1) {
            if (InventorySwap.hovering(InventorySwap.buttonX(this), InventorySwap.buttonY(this), mouseX, mouseY)) {
                InventorySwap.toVanilla(minecraft);
                return true;
            }

            for (int tab = 0; tab < 5; tab++) {
                if (isHovering(134 + tab * 24, 0, 24, 20, mouseX, mouseY)) {
                    if (menu.loadout.tabAvailable(tab) && menu.activeTab() != tab) {
                        Net.sendToServer(new BetterInventoryPayloads.SwitchTab(tab));
                        return true;
                    }
                    return true;
                }
            }

            if (isHovering(254, 0, 24, 20, mouseX, mouseY)) {
                Net.sendToServer(new BetterInventoryPayloads.ToggleCrafting());
                return true;
            }

            if (isHovering(280, 0, 20, 20, mouseX, mouseY)) {
                Net.sendToServer(new BetterInventoryPayloads.OpenSettings(menu.activeTab()));
                return true;
            }

            if (creativeTabHovered(mouseX, mouseY)) {
                CreativeInventory.toItems(minecraft);
                return true;
            }

            if (armorSwapHovered(mouseX, mouseY)) {
                Net.sendToServer(new BetterInventoryPayloads.SwapArmor());
                return true;
            }

            if (CompatButtons.click(leftPos + 82, topPos + 20, mouseX, mouseY)) {
                return true;
            }
        }
        return button == 3 && handleLockClick() || super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop)
                && !EffectRow.covers(guiLeft, guiTop, mouseX, mouseY);
    }

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
            Net.sendToServer(new BetterInventoryPayloads.SetLock(tab, viewIndex, Optional.of(id)));
            return true;
        }
        if (carried.isEmpty() && lock.isPresent()) {
            applyLockLocal(tab, viewIndex, Optional.empty());
            Net.sendToServer(new BetterInventoryPayloads.SetLock(tab, viewIndex, Optional.empty()));
            return true;
        }
        return false;
    }

    private void applyLockLocal(int tab, int viewIndex, Optional<Identifier> item) {
        BetterInventorySettings.setLockAt(minecraft.player, tab, viewIndex, item);
    }

    /** 26.3 adaptation: drawBorder is gone; a border is four 1px fills. */
    private static void border(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
