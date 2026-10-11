package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 creative inventory interop: parks the carried stack when the inventory
 * tab opens the mod screen, and re-routes back to the creative inventory tab.
 *
 * <p>26.3 adaptation: the creative screen's selectedTab is a PRIVATE STATIC
 * field (no mixin accessor is used; reflection reads it), creative activity is
 * {@code gameMode#hasCreativeInventory} replaced by the player's
 * {@code hasInfiniteMaterials()}, clicks go through
 * {@code AbstractContainerMenu#clicked}, and the park timeout uses the system
 * clock (Util#getMillis is gone).
 */
public final class CreativeInventory {
    private static final int MENU_HOTBAR = 36;
    private static final long PARK_MILLIS = 3000L;
    private static boolean onInventory;
    @Nullable
    private static CreativeModeTab itemTab;
    private static int parkedSlot = -1;
    private static ItemStack parkedStack = ItemStack.EMPTY;
    private static long parkedAt;
    private static boolean parkedSeen;

    private CreativeInventory() {}

    public static boolean active(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.hasInfiniteMaterials();
    }

    @Nullable
    private static CreativeModeTab selectedTab() {
        try {
            Field field = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            field.setAccessible(true);
            return (CreativeModeTab) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void setSelectedTab(CreativeModeTab tab) {
        try {
            Field field = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            field.setAccessible(true);
            field.set(null, tab);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Best effort; the tab selection is a nicety.
        }
    }

    private static boolean isInventoryTab(@Nullable CreativeModeTab tab) {
        return tab != null && tab.getType() == CreativeModeTab.Type.INVENTORY;
    }

    private static boolean rememberedInventory() {
        return isInventoryTab(selectedTab());
    }

    public static boolean vanillaInventoryShowing(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen && rememberedInventory();
    }

    public static boolean tabClicked(CreativeModeTab tab) {
        if (isInventoryTab(tab) && !InventorySwap.vanillaPreferred() && !rememberedInventory()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (!active(minecraft)) {
                return false;
            }
            ItemStack carried = minecraft.player.inventoryMenu.getCarried();
            if (!carried.isEmpty() && !park(minecraft, carried)) {
                return false;
            }
            onInventory = true;
            Net.sendToServer(new BetterInventoryPayloads.OpenLoadout());
            return true;
        }
        return false;
    }

    private static boolean park(Minecraft minecraft, ItemStack carried) {
        Inventory inventory = minecraft.player.getInventory();
        int slot = -1;
        for (int i = 0; i < inventory.getContainerSize() && slot < 0; i++) {
            if (i != inventory.getSelectedSlot() && inventory.getItem(i).isEmpty()) {
                slot = i;
            }
        }
        if (slot < 0 && inventory.getItem(inventory.getSelectedSlot()).isEmpty()) {
            slot = inventory.getSelectedSlot();
        }
        if (slot < 0) {
            return false;
        }
        ItemStack stack = carried.copy();
        inventory.setItem(slot, stack);
        minecraft.gameMode.handleCreativeModeItemAdd(stack, MENU_HOTBAR + slot);
        minecraft.player.inventoryMenu.setCarried(ItemStack.EMPTY);
        parkedSlot = slot;
        parkedStack = stack.copy();
        parkedAt = System.currentTimeMillis();
        parkedSeen = false;
        return true;
    }

    public static void tick(BetterInventoryMenu menu) {
        if (parkedSlot >= 0) {
            Minecraft minecraft = Minecraft.getInstance();
            if (System.currentTimeMillis() - parkedAt > PARK_MILLIS || minecraft.player == null) {
                parkedSlot = -1;
            } else if (!parkedSeen) {
                parkedSeen = true;
            } else {
                var slot = menu.getSlot(parkedSlot);
                if (menu.getCarried().isEmpty() && ItemStack.matches(slot.getItem(), parkedStack)) {
                    minecraft.player.containerMenu.clicked(
                            parkedSlot, 0, ContainerInput.PICKUP, minecraft.player);
                    parkedSlot = -1;
                }
            }
        }
    }

    public static boolean reopens() {
        return onInventory || rememberedInventory();
    }

    public static void cameBack() {
        onInventory = true;
    }

    private static CreativeModeTab itemsTab() {
        CreativeModeTab remembered = selectedTab();
        if (remembered != null && !isInventoryTab(remembered)) {
            return remembered;
        }
        return itemTab != null ? itemTab : CreativeModeTabs.getDefaultTab();
    }

    public static ItemStack itemsIcon() {
        return itemsTab().getIconItem();
    }

    public static void toItems(Minecraft minecraft) {
        if (minecraft.player != null) {
            onInventory = false;
            setSelectedTab(itemsTab());
            minecraft.player.closeContainer();
            minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
        }
    }

    public static void showVanillaTab() {
        CreativeModeTab remembered = selectedTab();
        if (remembered != null && !isInventoryTab(remembered)) {
            itemTab = remembered;
        }
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (isInventoryTab(tab)) {
                setSelectedTab(tab);
                return;
            }
        }
    }
}
