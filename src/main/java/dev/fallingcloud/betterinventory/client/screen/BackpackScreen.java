package dev.fallingcloud.betterinventory.client.screen;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.menu.BackpackMenu;
import dev.fallingcloud.betterinventory.menu.slots.LockedHandlerSlot;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;

public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/backpack.png");

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BackpackMenu.IMAGE_W, BackpackMenu.IMAGE_H);
                
        this.inventoryLabelY = BackpackMenu.PLAYER_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 512);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderGhostLocks(graphics);
        extractTooltip(graphics, mouseX, mouseY);
    }

    private TabSettings settings() {
        return BackpackItem.settings(menu.storageHandler.host());
    }

    private void renderGhostLocks(GuiGraphicsExtractor graphics) {
        TabSettings settings = settings();
        for (TabSettings.LockEntry lock : settings.locks()) {
            int index = lock.slot();
            if (index < 0 || index >= BackpackMenu.STORAGE_SIZE) {
                continue;
            }
            var slot = menu.slots.get(BackpackMenu.SLOT_STORAGE + index);
            if (slot.hasItem()) {
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

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return onMouseClick(event, doubleClick);
    }

    /** Body of the old mouseClicked, now driven by the 26.3 event object. */
    private boolean onMouseClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button == 3 && handleLockClick()) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleLockClick() {
        if (!(hoveredSlot instanceof LockedHandlerSlot slot) || slot.hasItem()) {
            return false;
        }
        int index = slot.index;
        ItemStack carried = menu.getCarried();
        TabSettings settings = settings();
        Optional<Item> lock = settings.lockFor(index);
        if (!carried.isEmpty() && lock.isEmpty()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(carried.getItem());
            BackpackItem.setSettings(menu.storageHandler.host(), settings.withLock(index, Optional.of(id)));
            PacketDistributor.sendToServer(new BetterInventoryPayloads.SetLock(-1, index, Optional.of(id)));
            return true;
        }
        if (carried.isEmpty() && lock.isPresent()) {
            BackpackItem.setSettings(menu.storageHandler.host(), settings.withLock(index, Optional.empty()));
            PacketDistributor.sendToServer(new BetterInventoryPayloads.SetLock(-1, index, Optional.empty()));
            return true;
        }
        return false;
    }
}

