package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.data.LoadoutOrigin;
import dev.fallingcloud.betterinventory.data.StoredItems;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import dev.fallingcloud.betterinventory.registry.ModItems;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 loadout return service: items dropped or moved out of the loadout carry
 * a LOADOUT_ORIGIN component, and each tick they are routed back to their
 * section/slot as soon as it can accept them.
 *
 * <p>26.3 adaptation: {@code Inventory.items/offhand} are private, so slot
 * access goes through getItem/setItem (a small SlotView adapter keeps the
 * shared routing code list-agnostic). Offhand lives in the inventory at
 * {@code SLOT_OFFHAND} instead of a separate list.
 */
public final class LoadoutReturnService {
    public static final String SECTION_TOOLS = "tools";
    public static final String SECTION_OFFHAND = "offhand";
    public static final String SECTION_ARMOR = "armor";
    public static final String SECTION_GATHER = "gather";
    public static final String SECTION_BACKPACKS = "backpacks";
    public static final String SECTION_PERSONAL = "personal";
    public static final String SECTION_TOOLBOX = "toolbox";
    public static final String SECTION_FURNACE = "furnace";
    public static final String SECTION_HELD = "held";

    private LoadoutReturnService() {}

    private interface SlotView {
        ItemStack get(int index);

        void set(int index, ItemStack stack);

        int size();
    }

    public static void tag(ItemStack stack, ServerPlayer player, String section, int slot) {
        stack.set(ModComponents.LOADOUT_ORIGIN.get(), new LoadoutOrigin(player.getUUID(), section, slot));
    }

    public static void sweep(ServerPlayer player, PlayerLoadout loadout) {
        Inventory inv = player.getInventory();
        SlotView view = new SlotView() {
            @Override
            public ItemStack get(int index) {
                return inv.getItem(index);
            }

            @Override
            public void set(int index, ItemStack stack) {
                inv.setItem(index, stack);
            }

            @Override
            public int size() {
                return inv.getContainerSize();
            }
        };
        routePass(player, loadout, view, true);
        routePass(player, loadout, view, false);
        stripHandlers(loadout);
    }

    public static void routeHome(ServerPlayer player, NonNullList<ItemStack> items) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        SlotView view = new SlotView() {
            @Override
            public ItemStack get(int index) {
                return items.get(index);
            }

            @Override
            public void set(int index, ItemStack stack) {
                items.set(index, stack);
            }

            @Override
            public int size() {
                return items.size();
            }
        };
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < items.size(); i++) {
                LoadoutOrigin origin = originOf(items.get(i));
                if (origin != null && origin.owner().equals(player.getUUID())) {
                    route(player, loadout, view, i, pass == 0);
                }
            }
        }
        items.removeIf(ItemStack::isEmpty);
    }

    private static void routePass(ServerPlayer player, PlayerLoadout loadout, SlotView view, boolean structural) {
        for (int i = 0; i < view.size(); i++) {
            route(player, loadout, view, i, structural);
        }
    }

    private static boolean isStructural(String section) {
        return SECTION_BACKPACKS.equals(section) || SECTION_PERSONAL.equals(section);
    }

    @Nullable
    private static LoadoutOrigin originOf(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ModComponents.LOADOUT_ORIGIN.get());
    }

    @Nullable
    public static ItemStack packHeld(ServerPlayer player, PlayerLoadout loadout) {
        NonNullList<ItemStack> pages = NonNullList.withSize(loadout.heldPages.getSlots(), ItemStack.EMPTY);
        NonNullList<ItemStack> upgrades = NonNullList.withSize(loadout.heldUpgrades.getSlots(), ItemStack.EMPTY);
        boolean any = false;

        for (int i = 0; i < pages.size(); i++) {
            ItemStack stack = loadout.heldPages.getStackInSlot(i);
            if (!stack.isEmpty()) {
                loadout.heldPages.setStackInSlot(i, ItemStack.EMPTY);
                pages.set(i, stack);
                any = true;
            }
        }
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = loadout.heldUpgrades.getStackInSlot(i);
            if (!stack.isEmpty()) {
                loadout.heldUpgrades.setStackInSlot(i, ItemStack.EMPTY);
                upgrades.set(i, stack);
                any = true;
            }
        }
        if (!any) {
            return null;
        }
        ItemStack bundle = new ItemStack(ModItems.BACKPACKS[ModItems.BACKPACKS.length - 1].get());
        setHeld(bundle, pages, upgrades);
        tag(bundle, player, SECTION_HELD, 0);
        return bundle;
    }

    private static void setHeld(ItemStack bundle, NonNullList<ItemStack> pages, NonNullList<ItemStack> upgrades) {
        StoredItems storedPages = StoredItems.of(pages);
        if (storedPages.isEmpty()) {
            bundle.remove(ModComponents.BACKPACK_CONTENTS.get());
        } else {
            bundle.set(ModComponents.BACKPACK_CONTENTS.get(), storedPages);
        }
        StoredItems storedUpgrades = StoredItems.of(upgrades);
        if (storedUpgrades.isEmpty()) {
            bundle.remove(ModComponents.BACKPACK_UPGRADES.get());
        } else {
            bundle.set(ModComponents.BACKPACK_UPGRADES.get(), storedUpgrades);
        }
    }

    private static NonNullList<ItemStack> heldPages(ItemStack bundle) {
        return bundle.getOrDefault(ModComponents.BACKPACK_CONTENTS.get(), StoredItems.EMPTY)
                .unpack(PlayerLoadout.CONTENTS_SIZE);
    }

    private static NonNullList<ItemStack> heldUpgrades(ItemStack bundle) {
        return bundle.getOrDefault(ModComponents.BACKPACK_UPGRADES.get(), StoredItems.EMPTY)
                .unpack(PlayerLoadout.UPGRADE_SLOTS);
    }

    private static ItemStack insertAnywhere(ItemStackHandler handler, ItemStack stack) {
        for (int i = 0; i < handler.getSlots() && !stack.isEmpty(); i++) {
            stack = handler.insertItem(i, stack, false);
        }
        return stack;
    }

    private static void route(ServerPlayer player, PlayerLoadout loadout, SlotView view, int index, boolean structural) {
        ItemStack stack = view.get(index);
        if (stack.isEmpty()) {
            return;
        }
        LoadoutOrigin origin = stack.get(ModComponents.LOADOUT_ORIGIN.get());
        if (origin == null || isStructural(origin.section()) != structural) {
            return;
        }
        stack.remove(ModComponents.LOADOUT_ORIGIN.get());
        loadout.dirty = true;
        if (!origin.owner().equals(player.getUUID())) {
            return;
        }
        int slot = origin.slot();
        switch (origin.section()) {
            case SECTION_TOOLS -> {
                if (slot >= 0 && slot < loadout.tools.getSlots()
                        && loadout.tools.getStackInSlot(slot).isEmpty()
                        && PlayerLoadout.isValidTool(slot, stack)) {
                    loadout.tools.setStackInSlot(slot, stack);
                    view.set(index, ItemStack.EMPTY);
                }
            }
            case SECTION_OFFHAND -> {
                if (slot < 0 || slot >= loadout.offhandStore.getSlots()) {
                    return;
                }
                Inventory inv = player.getInventory();
                if (slot == loadout.activeOffhand) {
                    if (inv.getItem(Inventory.SLOT_OFFHAND).isEmpty()) {
                        inv.setItem(Inventory.SLOT_OFFHAND, stack);
                        loadout.offhandStore.setStackInSlot(slot, stack);
                        view.set(index, ItemStack.EMPTY);
                    }
                } else if (loadout.offhandStore.getStackInSlot(slot).isEmpty()) {
                    loadout.offhandStore.setStackInSlot(slot, stack);
                    view.set(index, ItemStack.EMPTY);
                }
            }
            case SECTION_ARMOR -> {
                if (slot >= 0 && slot < loadout.armorStore.getSlots()
                        && loadout.armorStore.getStackInSlot(slot).isEmpty()
                        && loadout.armorStore.isItemValid(slot, stack)) {
                    loadout.armorStore.setStackInSlot(slot, stack);
                    view.set(index, ItemStack.EMPTY);
                }
            }
            case SECTION_GATHER -> {
                if (slot < 0 || slot >= loadout.gather.getSlots()) {
                    return;
                }
                view.set(index, loadout.gather.insertItem(slot, stack, false));
            }
            case SECTION_BACKPACKS -> {
                if (slot >= 0 && slot < loadout.backpacks.getSlots()
                        && loadout.backpacks.getStackInSlot(slot).isEmpty()
                        && loadout.backpacks.isItemValid(slot, stack)) {
                    loadout.backpacks.setStackInSlot(slot, stack);
                    view.set(index, ItemStack.EMPTY);
                }
            }
            case SECTION_PERSONAL -> {
                if (slot >= 0 && slot < loadout.personal.getSlots()
                        && loadout.personal.getStackInSlot(slot).isEmpty()
                        && loadout.personal.isItemValid(slot, stack)) {
                    loadout.personal.setStackInSlot(slot, stack);
                    view.set(index, ItemStack.EMPTY);
                }
            }
            case SECTION_TOOLBOX -> {
                if (slot >= 0 && slot < loadout.toolbox.getSlots()
                        && loadout.toolbox.getStackInSlot(slot).isEmpty()
                        && loadout.toolbox.isItemValid(slot, stack)) {
                    loadout.toolbox.setStackInSlot(slot, stack);
                    view.set(index, ItemStack.EMPTY);
                }
            }
            case SECTION_FURNACE -> {
                if (slot >= 0 && slot < loadout.furnace.items.getSlots()) {
                    view.set(index, loadout.furnace.items.insertItem(slot, stack, false));
                }
            }
            case SECTION_HELD -> view.set(index, unpackHeld(loadout, stack));
        }
    }

    private static ItemStack unpackHeld(PlayerLoadout loadout, ItemStack bundle) {
        if (!loadout.packFree) {
            if (loadout.backpacks.getStackInSlot(0).isEmpty()) {
                loadout.backpacks.setStackInSlot(0, bundle);
                return ItemStack.EMPTY;
            }
            return bundle;
        }
        NonNullList<ItemStack> pages = heldPages(bundle);
        boolean leftOver = false;
        for (int i = 0; i < pages.size(); i++) {
            ItemStack stack = pages.get(i);
            if (!stack.isEmpty()) {
                if (i < loadout.heldPages.getSlots() && loadout.heldPages.getStackInSlot(i).isEmpty()) {
                    loadout.heldPages.setStackInSlot(i, stack);
                    stack = ItemStack.EMPTY;
                } else {
                    stack = insertAnywhere(loadout.heldPages, stack);
                }
                pages.set(i, stack);
                leftOver |= !stack.isEmpty();
            }
        }
        NonNullList<ItemStack> upgrades = heldUpgrades(bundle);
        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack stack = upgrades.get(i);
            if (!stack.isEmpty() && i < loadout.heldUpgrades.getSlots()
                    && loadout.heldUpgrades.getStackInSlot(i).isEmpty()) {
                loadout.heldUpgrades.setStackInSlot(i, stack);
                upgrades.set(i, ItemStack.EMPTY);
            }
            leftOver |= !upgrades.get(i).isEmpty();
        }
        if (!leftOver) {
            return ItemStack.EMPTY;
        }
        setHeld(bundle, pages, upgrades);
        return bundle;
    }

    private static void stripHandlers(PlayerLoadout loadout) {
        stripHandler(loadout, loadout.tools);
        stripHandler(loadout, loadout.offhandStore);
        stripHandler(loadout, loadout.armorStore);
        stripHandler(loadout, loadout.gather);
        stripHandler(loadout, loadout.backpacks);
        stripHandler(loadout, loadout.personal);
        stripHandler(loadout, loadout.mainStash);
        stripHandler(loadout, loadout.toolbox);
        stripHandler(loadout, loadout.heldPages);
        stripHandler(loadout, loadout.heldUpgrades);
        stripHandler(loadout, loadout.furnace.items);
    }

    private static void stripHandler(PlayerLoadout loadout, ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.has(ModComponents.LOADOUT_ORIGIN.get())) {
                stack.remove(ModComponents.LOADOUT_ORIGIN.get());
                loadout.dirty = true;
            }
        }
    }
}
