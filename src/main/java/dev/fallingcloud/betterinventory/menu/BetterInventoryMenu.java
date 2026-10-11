package dev.fallingcloud.betterinventory.menu;

import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import dev.fallingcloud.betterinventory.compat.PolymorphCompat;
import dev.fallingcloud.betterinventory.compat.SlateBuildingCompat;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.CraftingUpgradeItem;
import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.logic.FurnaceService;
import dev.fallingcloud.betterinventory.menu.slots.ArmorGridSlot;
import dev.fallingcloud.betterinventory.menu.slots.BackgroundSlot;
import dev.fallingcloud.betterinventory.menu.slots.IconSlots;
import dev.fallingcloud.betterinventory.menu.slots.OffhandRouteContainer;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModMenus;
import dev.fallingcloud.betterinventory.shim.HandlerItemStore;
import dev.fallingcloud.betterinventory.shim.SlotItemHandler;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ContainerInput;
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

/**
 * 1.4.1 replacement for the vanilla survival inventory: hotbar, tabbed 3x9 +
 * 5x9 storage (72 slots), armor grid + armor store 2x2, offhand carousel 2x2,
 * tool rack 2x3, backpack slot, three pack-upgrade slots, 3x3 crafting grid
 * with result, trash, toolbox and the 3-slot furnace panel.
 *
 * <p>26.3 adaptations: clicks override {@code clicked(int, int, ContainerInput,
 * Player)} (ClickType/SlotActionType are gone), crafting result assembly uses
 * {@code assemble(input)} without the registry access and
 * {@code RecipeManager.getRecipeFor}, empty-slot icons use the single
 * {@code getNoItemIcon()} override, and the seven synced ints are
 * {@code DataSlot}s.
 */
public class BetterInventoryMenu extends AbstractContainerMenu {
    public static final int SLOT_HOTBAR = 0;
    public static final int SLOT_MAIN = 9;
    public static final int SLOT_GATHER = 36;
    public static final int SLOT_ARMOR = 81;
    public static final int SLOT_ARMOR_STORE = 85;
    public static final int SLOT_OFFHAND = 89;
    public static final int SLOT_TOOLS = 93;
    public static final int SLOT_PERSONAL = 99;
    public static final int SLOT_BACKPACK = 101;
    public static final int SLOT_PACK_UPGRADES = 102;
    public static final int SLOT_CRAFT = 105;
    public static final int SLOT_CRAFT_RESULT = 114;
    public static final int SLOT_TRASH = 115;
    public static final int SLOT_TOOLBOX = 116;
    public static final int SLOT_FURNACE = 117;
    public static final int SLOT_END = 120;

    private static final Identifier[] ARMOR_ICONS = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    public final Player player;
    public final PlayerLoadout loadout;
    public final TabStorage storage;
    /** Always 3x3; cells outside the installed upgrade's extent stay inactive. */
    private final CraftingContainer craftSlots =
            new TransientCraftingContainer(this, BetterInventoryLayout.CRAFT_COLS, BetterInventoryLayout.CRAFT_ROWS);
    private final ResultContainer resultSlots = new ResultContainer();
    private final SimpleContainer trash = new SimpleContainer(1);

    public BetterInventoryMenu(int id, Inventory inventory) {
        super(ModMenus.LOADOUT, id);
        this.player = inventory.player;
        this.loadout = ModAttachments.get(player);
        this.storage = new TabStorage(player, loadout);

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
        // 36-80 gather view (5 rows in 1.4.1)
        for (int row = 0; row < BetterInventoryLayout.GATHER_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new StorageSlot(TabStorage.GATHER_START + row * 9 + col,
                        BetterInventoryLayout.CENTER_X + BetterInventoryLayout.SLOT_OFF + col * 18,
                        BetterInventoryLayout.TOP_Y + BetterInventoryLayout.SLOT_OFF + row * 18));
            }
        }
        // 81-84 armor (helmet, chest / legs, boots)
        for (int i = 0; i < 4; i++) {
            int invIndex = 39 - i; // 39=head ... 36=feet
            addSlot(new ArmorGridSlot(inventory, player, PlayerLoadout.ARMOR_ORDER[i], invIndex,
                    BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.SIDE_TOP_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18,
                    ARMOR_ICONS[i]));
        }
        // 85-88 stored armor (swapped in/out by the armor swap bar)
        for (int i = 0; i < 4; i++) {
            addSlot(new ArmorStoreSlot(i,
                    BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.TOP_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18));
        }
        // 89-92 offhand carousel
        OffhandRouteContainer offhandRoute = new OffhandRouteContainer(inventory, loadout);
        for (int i = 0; i < 4; i++) {
            BackgroundSlot slot = new BackgroundSlot(offhandRoute, i,
                    BetterInventoryLayout.LEFT_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.SIDE_TOP_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18);
            slot.setBackground(IconSlots.EMPTY_OFFHAND);
            addSlot(slot);
        }
        // 93-98 tool rack (hoe, shovel / pick1, pick2 / axe, sword)
        for (int i = 0; i < 6; i++) {
            SlotItemHandler slot = new SlotItemHandler(loadout.tools, i,
                    BetterInventoryLayout.LEFT_X + BetterInventoryLayout.SLOT_OFF + (i % 2) * 18,
                    BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + (i / 2) * 18);
            slot.setBackground(IconSlots.TOOL_ICONS[i]);
            addSlot(slot);
        }
        // 99-100 personal upgrades (stack upgrade, crafting upgrade)
        for (int i = 0; i < 2; i++) {
            SlotItemHandler slot = new SlotItemHandler(loadout.personal, i,
                    BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF,
                    BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + i * 18);
            slot.setBackground(i == 0 ? IconSlots.EMPTY_STACK_UPGRADE : IconSlots.EMPTY_CRAFTING_UPGRADE);
            addSlot(slot);
        }
        // 101 the worn backpack
        SlotItemHandler backpackSlot = new SlotItemHandler(loadout.backpacks, 0,
                BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF,
                BetterInventoryLayout.HOTBAR_Y - 27) {
            @Override
            public boolean mayPickup(Player p) {
                return (loadout.activeTab == 0 || loadout.packFree) && super.mayPickup(p);
            }
        };
        backpackSlot.setBackground(IconSlots.EMPTY_BACKPACK);
        addSlot(backpackSlot);
        // 102-104 pack ability upgrades
        for (int i = 0; i < 3; i++) {
            SlotItemHandler slot = new SlotItemHandler(new HandlerItemStore(loadout.packUpgrades), i,
                    // 1.4.1 硬编码 330（= 右侧面板 312 + 18）；曾误写 +22 导致图标与框错位 4px。
                    BetterInventoryLayout.RIGHT_A_X + BetterInventoryLayout.SLOT_OFF + 18,
                    BetterInventoryLayout.MAIN_Y + BetterInventoryLayout.SLOT_OFF + i * 18) {
                @Override
                public boolean isActive() {
                    return loadout.upgradesUnlocked();
                }

                @Override
                public boolean mayPickup(Player p) {
                    return isActive() && super.mayPickup(p);
                }
            };
            slot.setBackground(IconSlots.EMPTY_UPGRADE);
            addSlot(slot);
        }
        // 105-113 crafting grid, 114 result
        for (int i = 0; i < BetterInventoryLayout.CRAFT_COLS * BetterInventoryLayout.CRAFT_ROWS; i++) {
            int col = i % BetterInventoryLayout.CRAFT_COLS;
            int row = i / BetterInventoryLayout.CRAFT_COLS;
            addSlot(new CraftGridSlot(i,
                    BetterInventoryLayout.CHAR_X + BetterInventoryLayout.CRAFT_GRID_X + col * 18,
                    BetterInventoryLayout.CHAR_Y + BetterInventoryLayout.CRAFT_GRID_Y + row * 18));
        }
        addSlot(new CraftResultSlot(player, craftSlots, resultSlots, 0,
                BetterInventoryLayout.CHAR_X + BetterInventoryLayout.CRAFT_RESULT_X,
                BetterInventoryLayout.CHAR_Y + BetterInventoryLayout.CRAFT_RESULT_Y));
        // 115 trash, 116 toolbox, 117-119 furnace
        BackgroundSlot trashSlot = new BackgroundSlot(trash, 0,
                BetterInventoryLayout.TRASH_X + BetterInventoryLayout.SLOT_OFF,
                BetterInventoryLayout.HOTBAR_Y + BetterInventoryLayout.SLOT_OFF);
        trashSlot.setBackground(IconSlots.EMPTY_TRASH);
        addSlot(trashSlot);
        addSlot(new ToolboxSlot(
                BetterInventoryLayout.TOOLBOX_X + BetterInventoryLayout.SLOT_OFF,
                BetterInventoryLayout.HOTBAR_Y + BetterInventoryLayout.SLOT_OFF));
        addSlot(new FurnaceSlot(0,
                BetterInventoryLayout.CHAR_X + BetterInventoryLayout.FURNACE_SLOT_X,
                BetterInventoryLayout.CHAR_Y + BetterInventoryLayout.FURNACE_INPUT_Y));
        addSlot(new FurnaceSlot(1,
                BetterInventoryLayout.CHAR_X + BetterInventoryLayout.FURNACE_SLOT_X,
                BetterInventoryLayout.CHAR_Y + BetterInventoryLayout.FURNACE_FUEL_Y));
        addSlot(new FurnaceSlot(2,
                BetterInventoryLayout.CHAR_X + BetterInventoryLayout.CRAFT_RESULT_X,
                BetterInventoryLayout.CHAR_Y + BetterInventoryLayout.CRAFT_RESULT_Y));

        // Seven synced ints: tab, panel toggles and furnace progress.
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
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return loadout.furnaceOpen ? 1 : 0;
            }

            @Override
            public void set(int value) {
                loadout.furnaceOpen = value != 0;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return loadout.furnace.lit;
            }

            @Override
            public void set(int value) {
                loadout.furnace.lit = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return loadout.furnace.litDuration;
            }

            @Override
            public void set(int value) {
                loadout.furnace.litDuration = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return loadout.furnace.cook;
            }

            @Override
            public void set(int value) {
                loadout.furnace.cook = value;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return loadout.furnace.cookTotal;
            }

            @Override
            public void set(int value) {
                loadout.furnace.cookTotal = value;
            }
        });
    }

    public boolean craftingOpen() {
        return loadout.craftingOpen;
    }

    public boolean furnaceOpen() {
        return loadout.furnaceOpen && loadout.hasFurnace();
    }

    /** 1.4.1 panel cycle: crafting -> furnace -> hidden -> crafting. */
    public static void cyclePanel(ServerPlayer player, PlayerLoadout loadout) {
        if (loadout.craftingOpen) {
            loadout.craftingOpen = false;
            if (player.containerMenu instanceof BetterInventoryMenu menu) {
                menu.returnCraftingGrid(player);
            }
            loadout.furnaceOpen = loadout.hasFurnace();
        } else if (loadout.furnaceOpen) {
            loadout.furnaceOpen = false;
        } else {
            loadout.craftingOpen = true;
        }
        loadout.dirty = true;
    }

    private boolean toFurnace(ItemStack stack) {
        if (FurnaceService.smelting(player.level(), stack) != null) {
            return moveItemStackTo(stack, SLOT_FURNACE, SLOT_FURNACE + 1, false);
        }
        return FurnaceService.isFuel(stack) && moveItemStackTo(stack, SLOT_FURNACE + 1, SLOT_FURNACE + 2, false);
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
        Optional<RecipeHolder<CraftingRecipe>> recipe = PolymorphCompat.craftingRecipe(this, input, level, player);
        if (recipe == null) {
            recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        }
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
            if (container == player.getInventory()) {
                refreshCraftingResult();
            }
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
        trash.clearContent();
    }

    public boolean trashLoaded() {
        return !trash.getItem(0).isEmpty();
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player p) {
        if (slotId == SLOT_TRASH && !trash.getItem(0).isEmpty() && incomingStack(input, button, p)) {
            trash.setItem(0, ItemStack.EMPTY);
        }
        super.clicked(slotId, button, input, p);
    }

    private boolean incomingStack(ContainerInput input, int button, Player p) {
        return switch (input) {
            case PICKUP -> !getCarried().isEmpty();
            case SWAP -> button == 40
                    ? !p.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()
                    : button >= 0 && button < 9 && !p.getInventory().getItem(button).isEmpty();
            default -> false;
        };
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
        return storage.tab() != 0 || loadout.gatherUnlocked();
    }

    public int activeTab() {
        return storage.tab();
    }

    public TabSettings activeSettings() {
        return BetterInventorySettings.view(player, storage.tab());
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
        boolean intoFurnace = !stack.isEmpty() && index < SLOT_ARMOR && furnaceOpen() && toFurnace(stack);
        moved |= intoFurnace;

        if (!stack.isEmpty() && !intoFurnace) {
            if (index < SLOT_MAIN) { // hotbar
                moved |= tryEquip(stack);
                // Top up a partial stack already sitting in the gather hub - just
                // enough to fill it. The hub never takes a NEW stack this way.
                if (!stack.isEmpty() && gatherAccessible()) {
                    moved |= topUpExisting(stack, SLOT_GATHER, SLOT_ARMOR);
                }
                if (!stack.isEmpty()) { // remainder to the main inventory
                    moved |= moveItemStackTo(stack, SLOT_MAIN, SLOT_GATHER, false);
                }
            } else if (index < SLOT_GATHER) { // main inventory
                moved |= tryEquip(stack);
                if (!stack.isEmpty() && gatherAccessible()) {
                    moved |= moveItemStackTo(stack, SLOT_GATHER, SLOT_ARMOR, false);
                }
                if (!stack.isEmpty()) {
                    moved |= moveItemStackTo(stack, SLOT_HOTBAR, SLOT_MAIN, false);
                }
            } else { // gather hub / equipment / ... -> main inventory, then hotbar
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
     * ones - so it tops a partial stack up to full and no further.
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
        // Backpack
        if (stack.getItem() instanceof BackpackItem && !slots.get(SLOT_BACKPACK).hasItem()) {
            return moveItemStackTo(stack, SLOT_BACKPACK, SLOT_BACKPACK + 1, false);
        }
        // Toolbox, then personal upgrades, then pack upgrades
        Slot toolbox = slots.get(SLOT_TOOLBOX);
        if (!toolbox.hasItem() && toolbox.mayPlace(stack)) {
            return moveItemStackTo(stack, SLOT_TOOLBOX, SLOT_TOOLBOX + 1, false);
        }
        if (stack.getItem() instanceof StackUpgradeItem && !slots.get(SLOT_PERSONAL).hasItem()) {
            return moveItemStackTo(stack, SLOT_PERSONAL, SLOT_PERSONAL + 1, false);
        }
        if (stack.getItem() instanceof CraftingUpgradeItem && !slots.get(SLOT_PERSONAL + 1).hasItem()) {
            return moveItemStackTo(stack, SLOT_PERSONAL + 1, SLOT_PERSONAL + 2, false);
        }
        return stack.getItem() instanceof UpgradeItem
                && moveItemStackTo(stack, SLOT_PACK_UPGRADES, SLOT_CRAFT, false);
    }

    private class ArmorStoreSlot extends SlotItemHandler {
        private final EquipmentSlot type;

        ArmorStoreSlot(int index, int x, int y) {
            super(loadout.armorStore, index, x, y);
            this.type = PlayerLoadout.ARMOR_ORDER[index];
            setBackground(ARMOR_ICONS[index]);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            var equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
            return equippable != null && equippable.slot() == type && super.mayPlace(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** A crafting grid cell; cells outside the upgrade's extent stay inactive. */
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

    private class FurnaceSlot extends SlotItemHandler {
        private final int kind;

        FurnaceSlot(int kind, int x, int y) {
            super(loadout.furnace.items, kind, x, y);
            this.kind = kind;
        }

        @Override
        public boolean isActive() {
            return furnaceOpen();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive() || kind == 2) {
                return false;
            }
            return (kind != 1 || FurnaceService.isFuel(stack)) && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player p) {
            return isActive() && super.mayPickup(p);
        }

        @Override
        public void onTake(Player p, ItemStack stack) {
            if (kind == 2 && p instanceof ServerPlayer serverPlayer) {
                FurnaceService.popExperience(serverPlayer, loadout.furnace);
            }
            super.onTake(p, stack);
        }
    }

    /** The storage view slot for the 72-slot tab area. */
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

    private class ToolboxSlot extends SlotItemHandler {
        ToolboxSlot(int x, int y) {
            super(loadout.toolbox, 0, x, y);
            setBackground(IconSlots.EMPTY_TOOLBOX);
        }

        @Override
        public boolean isActive() {
            return SlateBuildingCompat.loaded();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player p) {
            return isActive() && super.mayPickup(p);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
