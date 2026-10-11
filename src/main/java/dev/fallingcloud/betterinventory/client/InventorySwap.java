package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * 1.4.1 inventory swap: the small button on the vanilla inventory that flips
 * USE_VANILLA_INVENTORY, plus the one-time vanilla opening for the hotkey.
 *
 * <p>26.3 adaptation: {@code Minecraft#setScreen} is {@code minecraft.gui.setScreen}
 * and {@code MultiPlayerGameMode#releaseUsingItem} takes the player.
 */
public final class InventorySwap {
    public static final int SIZE = 18;
    private static final int ARROW = -854535;
    private static final int ARROW_SHADOW = -15065821;
    private static final int HOVER = -2130706433;
    private static boolean openingVanillaOnce;

    private InventorySwap() {}

    public static boolean vanillaPreferred() {
        return BetterInventoryConfig.USE_VANILLA_INVENTORY.get();
    }

    private static void setVanillaPreferred(boolean value) {
        BetterInventoryConfig.USE_VANILLA_INVENTORY.set(value);
        BetterInventoryConfig.CLIENT_SPEC.save();
    }

    public static int buttonX(Screen screen) {
        return BetterInventoryLayout.leftPos(screen.width) + 30;
    }

    public static int buttonY(Screen screen) {
        return (screen.height - 216) / 2 + 195;
    }

    public static boolean hovering(int bx, int by, double mouseX, double mouseY) {
        return mouseX >= bx && mouseX < bx + SIZE && mouseY >= by && mouseY < by + SIZE;
    }

    public static Component label(boolean toVanilla) {
        return Component.translatable(
                toVanilla ? "screen.betterinventory.swap.to_vanilla" : "screen.betterinventory.swap.to_better");
    }

    public static void render(GuiGraphicsExtractor graphics, int bx, int by, boolean hovered) {
        int[] frame = BetterInventoryLayout.PANEL_1X1;
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                BetterInventory.id("textures/gui/inventory.png"),
                bx - 3, by - 3, frame[0], frame[1], frame[2], frame[3], BetterInventoryLayout.TEX_W,
                BetterInventoryLayout.TEX_H);
        boolean vanilla = ClientTheme.vanillaStyle();
        int inner = vanilla ? 16 : 17;
        int width = vanilla ? 10 : 11;
        int gap = vanilla ? 5 : 4;
        int x = bx + 1 + (inner - width) / 2;
        int top = by + 1 + (inner - (gap + 3)) / 2 + 1;
        if (hovered) {
            graphics.fill(bx + 1, by + 1, bx + 1 + inner, by + 1 + inner, HOVER);
        }
        arrows(graphics, x + 1, top + 1, width, gap, ARROW_SHADOW);
        arrows(graphics, x, top, width, gap, ARROW);
    }

    private static void arrows(GuiGraphicsExtractor graphics, int x, int top, int width, int gap, int color) {
        int bottom = top + gap;
        graphics.fill(x, top, x + width, top + 1, color);
        graphics.fill(x + width - 2, top - 1, x + width - 1, top + 2, color);
        graphics.fill(x, bottom, x + width, bottom + 1, color);
        graphics.fill(x + 1, bottom - 1, x + 2, bottom + 2, color);
    }

    public static void openVanillaOnce(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.gui.screen() == null) {
            if (CreativeInventory.active(minecraft)) {
                CreativeInventory.showVanillaTab();
            }
            openingVanillaOnce = true;
            try {
                minecraft.gameMode.releaseUsingItem(minecraft.player);
                minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
            } finally {
                openingVanillaOnce = false;
            }
        }
    }

    public static boolean openingVanillaOnce() {
        return openingVanillaOnce;
    }

    public static void toVanilla(Minecraft minecraft) {
        if (minecraft.player != null) {
            setVanillaPreferred(true);
            if (CreativeInventory.active(minecraft)) {
                CreativeInventory.showVanillaTab();
            }
            minecraft.player.closeContainer();
            minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
        }
    }

    public static void toBetter(Minecraft minecraft) {
        if (minecraft.player != null) {
            setVanillaPreferred(false);
            CreativeInventory.cameBack();
            Net.sendToServer(new BetterInventoryPayloads.OpenLoadout());
        }
    }
}
