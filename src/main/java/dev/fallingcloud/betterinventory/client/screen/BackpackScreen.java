package dev.fallingcloud.betterinventory.client.screen;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.client.ClientHooks;
import dev.fallingcloud.betterinventory.client.ClientTheme;
import dev.fallingcloud.betterinventory.client.FtbSidebarCompat;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.menu.BackpackMenu;
import dev.fallingcloud.betterinventory.menu.slots.LockedHandlerSlot;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 standalone backpack screen: 72 storage slots across one page, page
 * arrows, page-sliced ghost locks and right-click locking.
 *
 * <p>26.3 adaptations: extractBackground/extractRenderState replace
 * drawBackground/render, {@code isHovering} replaces isPointWithinBounds,
 * right-click is SDL button code 3, borders are four fills (drawBorder is
 * gone), and tooltips go through {@code setTooltipForNextFrame}.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/backpack.png");
    private static final int BOARD_COLOR = -15263973;
    private static final int BOARD_EDGE = -14013903;
    private static final int ARROW_W = 7;
    private static final int ARROW_H = 9;
    private static final int PAGER_Y = 168;
    private static final int ARROW_COLOR = -4671304;
    private static final int ARROW_OFF = -11908534;
    private static final int LABEL_COLOR = -4605502;

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BackpackMenu.IMAGE_W, BackpackMenu.IMAGE_H);
        this.inventoryLabelY = BackpackMenu.PLAYER_Y - 11;
        this.titleLabelX = -10000;
        this.titleLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        AbstractWidget ftbButtons = FtbSidebarCompat.createIfMissing(children());
        if (ftbButtons != null) {
            addRenderableWidget(ftbButtons);
        }
    }

    private int prevArrowX() {
        return imageWidth - 52;
    }

    private int nextArrowX() {
        return imageWidth - 15;
    }

    private void arrow(GuiGraphicsExtractor graphics, int x, int y, int dir, boolean enabled) {
        int color = enabled ? ARROW_COLOR : ARROW_OFF;
        for (int i = 0; i < ARROW_W; i++) {
            int half = Math.min(i, 6 - i);
            int col = dir < 0 ? x + i : x + ARROW_W - 1 - i;
            graphics.fill(col, y + 4 - half, col + 1, y + 4 + half + 1, color);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 512);
        if (!menu.mainRowsUnlocked()) {
            int x = leftPos + 7;
            int y = topPos + 112 - 1;
            graphics.fill(x, y, x + 162, y + 54, BOARD_COLOR);
            border(graphics, x, y, 162, 54, BOARD_EDGE);
        }
        ClientHooks.paintSlotHighlight(graphics, leftPos, topPos, hoveredSlot);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderGhostLocks(graphics);
        if (menu.pageCount() > 1) {
            arrow(graphics, leftPos + prevArrowX(), topPos + PAGER_Y, -1, menu.page() > 1);
            arrow(graphics, leftPos + nextArrowX(), topPos + PAGER_Y, 1, menu.page() < menu.pageCount());
            Component label = Component.translatable("screen.betterinventory.page", menu.page(), menu.pageCount());
            graphics.centeredText(font, label,
                    leftPos + (prevArrowX() + ARROW_W + nextArrowX()) / 2, topPos + PAGER_Y + 1, LABEL_COLOR);
        }
        extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    private TabSettings settings() {
        return menu.pageSettings();
    }

    private void renderGhostLocks(GuiGraphicsExtractor graphics) {
        TabSettings settings = settings();
        int wash = ClientTheme.ghostWash();
        for (TabSettings.LockEntry lock : settings.locks()) {
            int index = lock.slot();
            if (index < 0 || index >= BackpackMenu.STORAGE_SIZE) {
                continue;
            }
            Slot slot = menu.slots.get(BackpackMenu.SLOT_STORAGE + index);
            if (!slot.hasItem() && slot.isActive()) {
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 1 && menu.pageCount() > 1) {
            if (isHovering(prevArrowX(), PAGER_Y, ARROW_W, ARROW_H, mouseX, mouseY) && menu.page() > 1) {
                Net.sendToServer(new BetterInventoryPayloads.SetBackpackPage(menu.page() - 1));
                return true;
            }
            if (isHovering(nextArrowX(), PAGER_Y, ARROW_W, ARROW_H, mouseX, mouseY) && menu.page() < menu.pageCount()) {
                Net.sendToServer(new BetterInventoryPayloads.SetBackpackPage(menu.page() + 1));
                return true;
            }
        }
        return button == 3 && handleLockClick() || super.mouseClicked(event, doubleClick);
    }

    private boolean handleLockClick() {
        if (!(hoveredSlot instanceof LockedHandlerSlot slot) || slot.hasItem() || !slot.isActive()) {
            return false;
        }
        int index = slot.index;
        int absolute = menu.pageBase() + index;
        ItemStack carried = menu.getCarried();
        TabSettings settings = settings();
        Optional<Item> lock = settings.lockFor(index);
        ItemStack host = menu.storageHandler.host();
        if (!carried.isEmpty() && lock.isEmpty()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(carried.getItem());
            BackpackItem.setSettings(host, BackpackItem.settings(host).withLock(absolute, Optional.of(id)));
            Net.sendToServer(new BetterInventoryPayloads.SetLock(-1, absolute, Optional.of(id)));
            return true;
        }
        if (carried.isEmpty() && lock.isPresent()) {
            BackpackItem.setSettings(host, BackpackItem.settings(host).withLock(absolute, Optional.empty()));
            Net.sendToServer(new BetterInventoryPayloads.SetLock(-1, absolute, Optional.empty()));
            return true;
        }
        return false;
    }

    /** 26.3 adaptation: drawBorder is gone; a border is four 1px fills. */
    private static void border(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
