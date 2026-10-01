package dev.fallingcloud.betterinventory.menu;

import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import dev.fallingcloud.betterinventory.compat.TrinketsCompat;
import dev.fallingcloud.betterinventory.platform.ItemStore;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import dev.fallingcloud.betterinventory.menu.slots.ArmorGridSlot;
import dev.fallingcloud.betterinventory.menu.slots.IconedSlot;
import dev.fallingcloud.betterinventory.menu.slots.IconSlots;
import dev.fallingcloud.betterinventory.menu.slots.OffhandRouteContainer;
import dev.fallingcloud.betterinventory.platform.AccessorySlots;
import dev.fallingcloud.betterinventory.platform.Services;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModMenus;
import dev.fallingcloud.betterinventory.registry.ModTags;
import java.util.Optional;
import dev.fallingcloud.betterinventory.shim.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import dev.fallingcloud.betterinventory.shim.IItemHandlerModifiable;
import dev.fallingcloud.betterinventory.shim.SlotItemHandler;

/**
 * The replacement for the vanilla survival inventory: hotbar, tabbed 3x9 + 2x9
 * storage, armor 2x2, offhand carousel 2x2, tool rack 2x3, backpacks + personal
 * upgrades 2x3, and up to ten curio slots.
 */
public class BetterInventoryMenu extends AbstractContainerMenu {
    public static final int SLOT_HOTBAR = 0;
    public static final int SLOT_MAIN = 9;
    public static final int SLOT_GATHER = 36;
    public static final int SLOT_ARMOR = 54;
    public static final int SLOT_OFFHAND = 58;
    public static final int SLOT_TOOLS = 62;
    public static final int SLOT_BACKPACKS = 68;
    public static final int SLOT_PERSONAL = 72;
    public static final int SLOT_CURIOS = 74;
    public static final int SLOT_CRAFT = 84;
    public static final int SLOT_CRAFT_RESULT = 93;   // after a full 3x3 grid
    public static final int SLOT_END = 94;

    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Identifier[] ARMOR_ICONS = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    public final Player player;
    public final PlayerLoadout loadout;
    public final TabStorage storage;
    @Nullable
    private final ItemStore curios;
    private final AccessorySlots accessories;
    private final int curioCount;
    private final dev.fallingcloud.betterinventory.shim.ItemStackHandler curioFallback =
            new dev.fallingcloud.betterinventory.shim.ItemStackHandler(10);
    /** Always 2x3; the third row is gated by the crafting upgrade, not by size. */
    private final CraftingContainer craftSlots =
            new TransientCraftingContainer(this, BetterInventoryLayout.CRAFT_COLS, BetterInventoryLayout.CRAFT_ROWS);
    private final ResultContainer resultSlots = new ResultContainer();

    public BetterInventoryMenu(int id, Inventory inventory) {
        super(ModMenus.LOADOUT, id);
        this.player = inventory.player;
        this.loadout = ModAttachments.get(player);
        this.storage = new TabStorage(player, loadout);
        // Accessory slots come through the platform seam: Curios here, Trinkets on
        // Fabric, AccessorySlots.NONE when neither is installed.
        this.accessories = Services.platform().accessories(player);
        this.curios = TrinketsCompat.store(player);
        this.curioCount = Math.min(10, accessories.size());

        // 0-8 hotbar - upgraded stack ceiling, same as the rest of the inventory
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i,
                    BetterInventoryLayout.CENTER_X + BetterInventoryLayout.SLOT_OFF + i * 18,
                    BetterInventoryLayout.HOTBAR_Y + BetterInventoryLayout.SLOT_OFF) {
                @Override
                public int getMaxStackSize() {
                    return Math.max(64, loadout.storageCap());
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return BigStackHandler.capFor(stack, loadout.storageCap());
                }
            });
        }
        // 9-35 main view
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new StorageSlot(row * 9 + col,
                        BetterInventoryLayout.CENTER_X + BetterInventoryLayout.SLOT_OFF + col * 18,
                        BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + row * 18));
            }
        }
        // 36-53 gather view
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new StorageSlot(TabStorage.GATHER_START + row * 9 + col,
                        BetterInventoryLayout.CENTER_X + BetterInventoryLayout.SLOT_OFF + col * 18,
                        BetterInventoryLayout.TOP_Y + BetterInventoryLayout.SLOT_OFF + row * 18));
            }
        }
        // 54-57 armor (helmet, chest / legs, boots)
        for (int i = 0; i < 4; i++) {
            int invIndex = 39 - i; // 39=head ... 36=feet
            addSlot(new ArmorGridSlot(inventory, player, ARMOR_ORDER[i], invIndex,
                    BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.TOP_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18,
                    ARMOR_ICONS[i]));
        }
        // 58-61 offhand carousel
        OffhandRouteContainer offhandRoute = new OffhandRouteContainer(inventory, loadout);
        for (int i = 0; i < 4; i++) {
            IconedSlot slot = new IconedSlot(offhandRoute, i,
                    BetterInventoryLayout.LEFT_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.TOP_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18);
            slot.setBackground(IconSlots.EMPTY_OFFHAND);
            addSlot(slot);
        }
        // 62-67 tool rack (hoe, shovel / pick1, pick2 / axe, sword)
        for (int i = 0; i < 6; i++) {
            SlotItemHandler slot = new SlotItemHandler(loadout.tools, i,
                    BetterInventoryLayout.LEFT_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18);
            slot.setBackground(IconSlots.TOOL_ICONS[i]);
            addSlot(slot);
        }
        // 68-71 backpacks
        for (int i = 0; i < 4; i++) {
            final int packTab = i + 1;
            SlotItemHandler slot = new SlotItemHandler(loadout.backpacks, i,
                    BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18) {
                @Override
                public boolean mayPickup(Player p) {
                    // The open tab's contents live in the live inventory, not in the
                    // backpack - removing it here would strand them. Switch away first.
                    return loadout.activeTab != packTab && super.mayPickup(p);
                }
            };
            slot.setBackground(IconSlots.EMPTY_BACKPACK);
            addSlot(slot);
        }
        // 72-73 personal upgrades (stack upgrade + reserved)
        for (int i = 0; i < 2; i++) {
            SlotItemHandler slot = new SlotItemHandler(loadout.personal, i,
                    BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF + i * 18,
                    BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + 2 * 18);
            slot.setBackground(
                    i == 0 ? IconSlots.EMPTY_STACK_UPGRADE : IconSlots.EMPTY_UPGRADE);
            addSlot(slot);
        }
        // 74-83 curios (2x2 top + 2x3 bottom of the right B column)
        for (int i = 0; i < 10; i++) {
            int x;
            int y;
            if (i < 4) {
                x = BetterInventoryLayout.RIGHT_B_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18;
                y = BetterInventoryLayout.TOP_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18;
            } else {
                int j = i - 4;
                x = BetterInventoryLayout.RIGHT_B_X + BetterInventoryLayout.SLOT_OFF + (j % 2) * 18;
                y = BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + (j / 2) * 18;
            }
            addSlot(new CurioSlot(i, x, y));
        }

        // 84-92 crafting grid, 93 result. Shares the character panel's footprint and
        // is only active while the crafting tab is open. The container is always 3x3;
        // cells outside the installed upgrade's extent stay inactive, which keeps
        // recipe matching honest (an unreachable cell is always empty, and
        // CraftingInput trims to the filled bounding box just like a crafting table).
        int craftX = BetterInventoryLayout.charX(curios != null);
        for (int i = 0; i < BetterInventoryLayout.CRAFT_COLS * BetterInventoryLayout.CRAFT_ROWS; i++) {
            int col = i % BetterInventoryLayout.CRAFT_COLS;
            int row = i / BetterInventoryLayout.CRAFT_COLS;
            addSlot(new CraftGridSlot(i,
                    craftX + BetterInventoryLayout.CRAFT_GRID_X + col * 18,
                    BetterInventoryLayout.CHAR_Y + BetterInventoryLayout.CRAFT_GRID_Y + row * 18));
        }
        addSlot(new CraftResultSlot(player, craftSlots, resultSlots, 0,
                craftX + BetterInventoryLayout.CRAFT_RESULT_X,
                BetterInventoryLayout.CHAR_Y + BetterInventoryLayout.CRAFT_RESULT_Y));

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return loadout.activeTab;
            }

            @Override
            public void set(int value) {
                loadout.activeTab = Math.floorMod(value, PlayerLoadout.TAB_COUNT);
            }
        });
        // Mirrors the crafting toggle so the client redraws the right panel without
        // waiting for a full attachment sync.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return loadout.craftingOpen ? 1 : 0;
            }

            @Override
            public void set(int value) {
                loadout.craftingOpen = value != 0;
            }
        });
    }

    public boolean craftingOpen() {
        return loadout.craftingOpen;
    }

    /** A crafting grid cell; the bottom row needs the crafting upgrade. */
    private class CraftGridSlot extends Slot {
        private final int index;

        CraftGridSlot(int index, int x, int y) {
            super(craftSlots, index, x, y);
            this.index = index;
        }

        @Override
        public boolean isActive() {
            return loadout.craftingOpen
                    && index % BetterInventoryLayout.CRAFT_COLS < loadout.craftingCols()
                    && index / BetterInventoryLayout.CRAFT_COLS < loadout.craftingRows();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive();
        }

        @Override
        public boolean mayPickup(Player p) {
            return isActive();
        }
    }

    /** The result slot, hidden with the panel. */
    private class CraftResultSlot extends ResultSlot {
        CraftResultSlot(Player player, CraftingContainer craft, ResultContainer result, int index, int x, int y) {
            super(player, craft, result, index, x, y);
        }

        @Override
        public boolean isActive() {
            return loadout.craftingOpen;
        }
    }

    /**
     * Recomputes the crafting result.
     *
     * <p>Reimplemented rather than calling {@code CraftingMenu.slotChangedCraftingGrid}:
     * that helper is protected, and it hardcodes result slot 0 for both
     * {@code setRemoteSlot} and the update packet, which is only correct for the
     * vanilla inventory menu. Ours lives at SLOT_CRAFT_RESULT.
     */
    private void refreshCraftingResult() {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Level level = player.level();
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = recipe.get();
            if (resultSlots.setRecipeUsed(serverPlayer, holder)) {
                ItemStack assembled = holder.value().assemble(input);
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(SLOT_CRAFT_RESULT, result);
        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), SLOT_CRAFT_RESULT, result));
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == craftSlots) {
            refreshCraftingResult();
        } else {
            super.slotsChanged(container);
        }
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        resultSlots.clearContent();
        if (!p.level().isClientSide()) {
            clearContainer(p, craftSlots);
        }
    }

    /** Returns the grid to the player, e.g. when the crafting panel is closed. */
    public void returnCraftingGrid(Player p) {
        if (!p.level().isClientSide()) {
            clearContainer(p, craftSlots);
            resultSlots.clearContent();
        }
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new BetterInventoryMenu(id, inv),
                Component.translatable("container.betterinventory.inventory")));
    }

    public boolean gatherAccessible() {
        return storage.tab() != 0 || loadout.hasAnyBackpack();
    }

    public int activeTab() {
        return storage.tab();
    }

    public int curioCount() {
        return curioCount;
    }

    public boolean curiosPresent() {
        return curios != null;
    }

    public TabSettings activeSettings() {
        return BetterInventorySettings.raw(player, storage.tab());
    }

    /** The storage view slot for the 45-slot tab area. */
    public class StorageSlot extends Slot {
        private final int viewIndex;

        public StorageSlot(int viewIndex, int x, int y) {
            super(storage, viewIndex, x, y);
            this.viewIndex = viewIndex;
        }

        public boolean isGather() {
            return viewIndex >= TabStorage.GATHER_START;
        }

        @Override
        public boolean isActive() {
            return !isGather() || gatherAccessible();
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive()) {
                return false;
            }
            if (stack == loadout.backpack(storage.tab())) {
                return false; // a backpack can never enter its own storage
            }
            return activeSettings().allows(viewIndex, stack);
        }

        // Stack upgrades apply to every part of the inventory, the real player
        // inventory included - not just the gather hub and backpack tabs.
        @Override
        public int getMaxStackSize() {
            return Math.max(64, loadout.storageCap());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return BigStackHandler.capFor(stack, loadout.storageCap());
        }
    }

    private class CurioSlot extends SlotItemHandler {
        private final int index;

        CurioSlot(int index, int x, int y) {
            super(curios == null ? curioFallback : curios, index, x, y);
            this.index = index;
        }

        @Override
        public boolean isActive() {
            return index < curioCount;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && super.mayPickup(player);
        }
    }

    @Override
    public void broadcastChanges() {
        // Deliberately does NOT force activeTab back to 0 here: the active tab's items
        // live in the player inventory, so a silent reset would let the stash overwrite
        // them. TabService.recoverIfOrphaned handles a vanished backpack losslessly.
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        // Crafting result: straight to the inventory, and onQuickCraft lets the caller
        // re-run us so holding shift keeps crafting while ingredients last.
        if (index == SLOT_CRAFT_RESULT) {
            if (!moveItemStackTo(stack, SLOT_MAIN, SLOT_GATHER, true)
                    && !moveItemStackTo(stack, SLOT_HOTBAR, SLOT_MAIN, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, stack);
            return copy;
        }

        // A slot locked to this item always wins, from anywhere - including the
        // hotbar into the gather hub, which shift-click otherwise never targets.
        boolean moved = moveToLockedSlots(stack, index);

        // Otherwise shift-click mirrors vanilla: hotbar <-> main inventory. Anything
        // left over after each step falls through to the next.
        if (!stack.isEmpty()) {
            if (index < SLOT_MAIN) { // hotbar
                moved |= tryEquip(stack);
                // Top up a partial stack already sitting in the gather hub - just
                // enough to fill it. The hub never takes a NEW stack this way, so
                // shift-clicking still can't be used to stash things there.
                if (!stack.isEmpty() && gatherAccessible()) {
                    moved |= topUpExisting(stack, SLOT_GATHER, SLOT_ARMOR);
                }
                if (!stack.isEmpty()) { // remainder to the main inventory
                    moved |= moveItemStackTo(stack, SLOT_MAIN, SLOT_GATHER, false);
                }
            } else if (index < SLOT_GATHER) { // main inventory -> hotbar
                moved |= tryEquip(stack);
                if (!stack.isEmpty()) {
                    moved |= moveItemStackTo(stack, SLOT_HOTBAR, SLOT_MAIN, false);
                }
            } else { // gather hub / equipment / curios -> main inventory, then hotbar
                moved |= moveItemStackTo(stack, SLOT_MAIN, SLOT_GATHER, false);
                if (!stack.isEmpty()) {
                    moved |= moveItemStackTo(stack, SLOT_HOTBAR, SLOT_MAIN, false);
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

    /**
     * Feeds the stack into any storage slot locked to that exact item, merging into
     * partially-filled ones before claiming empty ones. Reserved slots are a
     * deliberate destination, so they take priority over the normal routing.
     */
    private boolean moveToLockedSlots(ItemStack stack, int sourceIndex) {
        TabSettings settings = activeSettings();
        if (stack.isEmpty() || settings.locks().isEmpty()) {
            return false;
        }
        boolean moved = false;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = SLOT_MAIN; i < SLOT_ARMOR && !stack.isEmpty(); i++) {
                if (i == sourceIndex) {
                    continue;
                }
                Slot slot = slots.get(i);
                if (!(slot instanceof StorageSlot storageSlot) || !storageSlot.isActive()) {
                    continue;
                }
                if (settings.lockFor(storageSlot.index).filter(stack::is).isEmpty()) {
                    continue;
                }
                // pass 0 tops up existing stacks, pass 1 fills empty reserved slots
                if (slot.hasItem() == (pass == 1)) {
                    continue;
                }
                int before = stack.getCount();
                slot.safeInsert(stack);
                if (stack.getCount() != before) {
                    moved = true;
                }
            }
        }
        return moved;
    }

    /**
     * Merges into slots in the range that ALREADY hold this item, never into empty
     * ones — so it tops a partial stack up to full and no further.
     */
    private boolean topUpExisting(ItemStack stack, int from, int to) {
        boolean moved = false;
        for (int i = from; i < to && !stack.isEmpty(); i++) {
            Slot slot = slots.get(i);
            if (!slot.hasItem() || !slot.isActive() || !slot.mayPlace(stack)) {
                continue;
            }
            int before = stack.getCount();
            slot.safeInsert(stack);
            if (stack.getCount() != before) {
                moved = true;
            }
        }
        return moved;
    }

    private boolean tryEquip(ItemStack stack) {
        // Armor
        for (int i = 0; i < 4; i++) {
            Slot armor = slots.get(SLOT_ARMOR + i);
            if (!armor.hasItem() && armor.mayPlace(stack)) {
                return moveItemStackTo(stack, SLOT_ARMOR + i, SLOT_ARMOR + i + 1, false);
            }
        }
        // Tool rack
        for (int i = 0; i < 6; i++) {
            if (PlayerLoadout.isValidTool(i, stack) && !slots.get(SLOT_TOOLS + i).hasItem()) {
                return moveItemStackTo(stack, SLOT_TOOLS + i, SLOT_TOOLS + i + 1, false);
            }
        }
        // Backpacks
        if (stack.getItem() instanceof BackpackItem) {
            return moveItemStackTo(stack, SLOT_BACKPACKS, SLOT_PERSONAL, false);
        }
        // Personal upgrades
        if (stack.getItem() instanceof StackUpgradeItem && !slots.get(SLOT_PERSONAL).hasItem()) {
            return moveItemStackTo(stack, SLOT_PERSONAL, SLOT_PERSONAL + 1, false);
        }
        if (stack.is(ModTags.AUX_UPGRADES) && !slots.get(SLOT_PERSONAL + 1).hasItem()) {
            return moveItemStackTo(stack, SLOT_PERSONAL + 1, SLOT_PERSONAL + 2, false);
        }
        return false;
    }
}
