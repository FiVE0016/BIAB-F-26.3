package dev.fallingcloud.betterinventory.menu;

import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import dev.fallingcloud.betterinventory.registry.ModMenus;
import dev.fallingcloud.betterinventory.shim.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import dev.fallingcloud.betterinventory.shim.SlotItemHandler;

/**
 * The per-tab settings screen: toggles (rendered client side, applied via payloads)
 * plus the backpack's ability-upgrade slots.
 */
public class SettingsMenu extends AbstractContainerMenu {
    public static final int UPGRADE_SLOTS = 10;
    public static final int SLOT_UPGRADES = 0;
    public static final int SLOT_PLAYER = 10;
    public static final int SLOT_HOTBAR = 37;
    public static final int SLOT_END = 46;

    public static final int IMAGE_W = 190;
    public static final int IMAGE_H = 264;
    public static final int UPGRADES_X = 50;
    public static final int UPGRADES_Y = 107;
    public static final int PLAYER_Y = 182;
    public static final int HOTBAR_Y = 240;
    public static final int INV_X = 14;
    /** Divider rules baked into the background, for the screen to lay out against. */
    public static final int DIV_TOP = 27;
    public static final int DIV_MID = 102;
    public static final int DIV_BOTTOM = 168;
    /** Row of upgrade toggle buttons, just under the bay. */
    public static final int TOGGLES_Y = UPGRADES_Y + 36 + 4;

    public final Player player;
    public final PlayerLoadout loadout;
    public final int tab;
    @Nullable
    private final ComponentBackedHandler upgrades;

    public SettingsMenu(int id, Inventory inventory, int tab) {
        super(ModMenus.SETTINGS, id);
        this.player = inventory.player;
        this.loadout = ModAttachments.get(player);
        this.tab = Math.floorMod(tab, PlayerLoadout.TAB_COUNT);

        ItemStack pack = loadout.backpack(this.tab);
        if (!pack.isEmpty()) {
            this.upgrades = new ComponentBackedHandler(pack, ModComponents.BACKPACK_UPGRADES.get(), UPGRADE_SLOTS, () -> 1);
            this.upgrades.setChangeListener(() -> loadout.dirty = true);
        } else {
            this.upgrades = null;
        }

        ItemStackHandler handler = upgrades != null ? upgrades : new ItemStackHandler(UPGRADE_SLOTS);
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            addSlot(new UpgradeSlot(handler, i,
                    UPGRADES_X + (i % 5) * 18,
                    UPGRADES_Y + (i / 5) * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, INV_X + col * 18, PLAYER_Y + row * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, INV_X + i * 18, HOTBAR_Y));
        }
    }

    public static SettingsMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new SettingsMenu(id, inventory, buf.readVarInt());
    }

    public static void open(ServerPlayer player, int tab) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.betterinventory.settings");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new SettingsMenu(id, inv, tab);
            }
        };
        player.openMenu(MenuData.wrap(provider, () -> tab));
    }

    /** How many upgrade slots this tab's backpack level unlocks (0 for the default tab). */
    public int upgradeCapacity() {
        return BackpackItem.upgradeCapacityOf(loadout.backpack(tab));
    }

    public boolean hasBackpack() {
        return upgrades != null;
    }

    private boolean kindInstalled(UpgradeItem.Kind kind, int exceptSlot) {
        if (upgrades == null) {
            return false;
        }
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (i == exceptSlot) {
                continue;
            }
            if (upgrades.getStackInSlot(i).getItem() instanceof UpgradeItem u && u.kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private class UpgradeSlot extends SlotItemHandler {
        private final int index;

        UpgradeSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
            this.index = index;
            setBackground(dev.fallingcloud.betterinventory.menu.slots.IconSlots.EMPTY_UPGRADE);
        }

        @Override
        public boolean isActive() {
            return upgrades != null && index < upgradeCapacity();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive()
                    && stack.getItem() instanceof UpgradeItem upgrade
                    && !kindInstalled(upgrade.kind(), index);
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && super.mayPickup(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return tab == 0 || !loadout.backpack(tab).isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        boolean moved;
        if (index < SLOT_PLAYER) {
            moved = moveItemStackTo(stack, SLOT_PLAYER, SLOT_END, true);
        } else {
            moved = stack.getItem() instanceof UpgradeItem
                    && moveItemStackTo(stack, SLOT_UPGRADES, SLOT_PLAYER, false);
            if (!moved) {
                if (index < SLOT_HOTBAR) {
                    moved = moveItemStackTo(stack, SLOT_HOTBAR, SLOT_END, false);
                } else {
                    moved = moveItemStackTo(stack, SLOT_PLAYER, SLOT_HOTBAR, false);
                }
            }
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
