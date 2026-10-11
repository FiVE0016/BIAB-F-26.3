package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.logic.ToolService;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1.4.1 client hooks: screen opening/render/click wiring, the offhand
 * selector, the safe-entity key, the tool flash window and the vanilla
 * inventory swap button.
 *
 * <p>26.3 adaptation: action-bar messages are {@code sendOverlayMessage}, the
 * hover tooltip uses {@code setTooltipForNextFrame}, and the cursor entity is
 * {@code minecraft.crosshairPickEntity}.
 */
public final class ClientHooks {
    private ClientHooks() {}

    public static boolean onScreenOpening(Screen screen) {
        if (screen instanceof InventoryScreen && !InventorySwap.vanillaPreferred()
                && !InventorySwap.openingVanillaOnce()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (ClientPlatform.shiftAsksForVanilla()) {
                if (CreativeInventory.active(minecraft)) {
                    CreativeInventory.showVanillaTab();
                }
                return false;
            }
            if (minecraft.player != null && (!CreativeInventory.active(minecraft) || CreativeInventory.reopens())) {
                Net.sendToServer(new BetterInventoryPayloads.OpenLoadout());
                return true;
            }
        }
        return false;
    }

    public static void onScreenRendered(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (CreativeInventory.vanillaInventoryShowing(screen)) {
            int bx = InventorySwap.buttonX(screen);
            int by = InventorySwap.buttonY(screen);
            boolean hovered = InventorySwap.hovering(bx, by, mouseX, mouseY);
            InventorySwap.render(graphics, bx, by, hovered);
            if (hovered) {
                graphics.setTooltipForNextFrame(InventorySwap.label(false), mouseX, mouseY);
            }
        }
    }

    public static boolean onScreenClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (button == 0 && CreativeInventory.vanillaInventoryShowing(screen)) {
            int bx = InventorySwap.buttonX(screen);
            int by = InventorySwap.buttonY(screen);
            if (InventorySwap.hovering(bx, by, mouseX, mouseY)) {
                InventorySwap.toBetter(Minecraft.getInstance());
                return true;
            }
        }
        return false;
    }

    public static boolean onMouseScroll(double delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null || minecraft.player == null) {
            return false;
        }
        if (OffhandKey.held() && delta != 0.0) {
            ClientHudState.pendingIndex = Math.floorMod(ClientHudState.pendingIndex - (int) Math.signum(delta), 4);
            return true;
        }
        return false;
    }

    public static void openToolDisplayWindow() {
        ClientHudState.toolDisplayWindow = true;
    }

    public static void closeToolDisplayWindow() {
        ClientHudState.toolDisplayWindow = false;
    }

    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        markSafeEntity(minecraft);
        while (BetterInventoryClient.OPEN_VANILLA_INVENTORY.consumeClick()) {
            InventorySwap.openVanillaOnce(minecraft);
        }
        OffhandKey.migrateOnce(minecraft);
        if (minecraft.player == null) {
            ClientHudState.selectorOpen = false;
            ClientHudState.progress = 0.0F;
            ClientHudState.progressO = 0.0F;
        } else {
            boolean down = OffhandKey.held() && minecraft.gui.screen() == null;
            PlayerLoadout loadout = PlayerLoadout.of(minecraft.player);
            if (down && !ClientHudState.selectorOpen) {
                ClientHudState.pendingIndex = loadout.activeOffhand;
            }
            if (!down && ClientHudState.selectorOpen && ClientHudState.pendingIndex != loadout.activeOffhand) {
                Net.sendToServer(new BetterInventoryPayloads.SelectOffhand(ClientHudState.pendingIndex));
                loadout.activeOffhand = ClientHudState.pendingIndex;
            }
            ClientHudState.selectorOpen = down;
            ClientHudState.tick(minecraft);
            ClientCombatBridge.tick(minecraft);
        }
    }

    private static void markSafeEntity(Minecraft minecraft) {
        boolean pressed = false;
        while (BetterInventoryClient.MARK_SAFE_ENTITY.consumeClick()) {
            pressed = true;
        }
        if (pressed && minecraft.player != null && minecraft.gui.screen() == null) {
            Entity target = minecraft.crosshairPickEntity;
            if (target == null) {
                minecraft.player.sendOverlayMessage(Component.translatable("betterinventory.safe_entity.none"));
            } else {
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                if (id != null) {
                    Net.sendToServer(new BetterInventoryPayloads.ToggleSafeEntity(id));
                }
            }
        }
    }

    public static void onAttack(Player player) {
        if (player.level().isClientSide()) {
            ClientHudState.weaponFlashTicks = 15;
        }
    }

    public static void onLeftClickBlock(Player player, Level level, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (level.isClientSide() && player == minecraft.player) {
            ToolService.forBlock(minecraft.player, level.getBlockState(pos), pos)
                    .ifPresent(choice -> ClientHudState.flashTool(choice.stack()));
        }
    }
}
