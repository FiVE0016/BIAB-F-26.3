package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.inv.BigStackHandler;
import dev.fallingcloud.betterinventory.inv.FurnaceState;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.menu.TabStorage;
import dev.fallingcloud.betterinventory.registry.ModItems;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * 1.4.1 feature self tests (tab rules, trading, furnace, pack-free mode,
 * extra-only mode, held bundle). 26.3 adaptations: SimpleMerchant is gone
 * (ClientSideMerchant instead), ItemStack.onClicked/SlotContext are gone
 * (bundle insertion goes through BundleContents), and assemble lost its
 * RegistryAccess parameter.
 */
final class SelfTestFeatures {
    private SelfTestFeatures() {}

    private static ItemStack craft(ServerPlayer player, int width, int height, ItemStack... grid) {
        CraftingInput input = CraftingInput.of(width, height, List.of(grid));
        return player.level().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(holder -> ((CraftingRecipe) holder.value()).assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    private static MerchantOffer offer(Item price, Item result) {
        return new MerchantOffer(new ItemCost(price, 1), new ItemStack(result), 999, 0, 0.05F);
    }

    private static int count(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static void clear(Inventory inv, PlayerLoadout loadout) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            inv.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < loadout.gather.getSlots(); i++) {
            loadout.gather.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private static void clear(ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    static int rulesScene(ServerPlayer player) {
        PlayerLoadout loadout = PlayerLoadout.of(player);
        SelfTest.check(
                "both tab rules are read from the config file, and the change dropped nothing",
                !BetterInventoryConfig.tabsNeedBackpack() && !BetterInventoryConfig.tabsSwapMain()
                        && loadout.packFree && loadout.extraOnly && itemsNear(player) == 0,
                "requireBackpack=" + BetterInventoryConfig.tabsNeedBackpack()
                        + " swapMainInventory=" + BetterInventoryConfig.tabsSwapMain()
                        + " packFree=" + loadout.packFree
                        + " extraOnly=" + loadout.extraOnly
                        + " stillWaitingForRoom=" + loadout.mainUnsettled
                        + " itemsOnTheGround=" + itemsNear(player));
        loadout.backpacks.setStackInSlot(0, ItemStack.EMPTY);
        loadout.heldPages.setStackInSlot(PlayerLoadout.pageBase(2) + 27, new ItemStack(Items.DIAMOND, 5));
        player.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 7));
        loadout.dirty = true;
        return 1;
    }

    private static int itemsNear(ServerPlayer player) {
        return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0)).size();
    }

    private static int count(BigStackHandler handler, Item item) {
        int n = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).is(item)) {
                n += handler.getStackInSlot(i).getCount();
            }
        }
        return n;
    }

    static void run(ServerPlayer player) {
        Inventory inv = player.getInventory();
        PlayerLoadout loadout = PlayerLoadout.of(player);
        ItemStack savedStackUpgrade = loadout.personal.getStackInSlot(0).copy();
        ItemStack savedCraftUpgrade = loadout.personal.getStackInSlot(1).copy();
        boolean savedPackFree = loadout.packFree;
        boolean savedExtraOnly = loadout.extraOnly;

        try {
            TabRules.apply(player, loadout, false, false, false);
            bundle(player);
            trade(player, inv, loadout);
            furnace(player, inv, loadout);
            tabsWithoutBackpack(player, inv, loadout);
            extraOnly(player, inv, loadout);
            extraOnlyWithNoRoom(player, inv, loadout);
            heldBundle(player, loadout);
        } finally {
            clear(loadout.furnace.items);
            clear(loadout.heldPages);
            clear(loadout.heldUpgrades);
            loadout.personal.setStackInSlot(0, savedStackUpgrade);
            loadout.personal.setStackInSlot(1, savedCraftUpgrade);
            loadout.packFree = savedPackFree;
            loadout.extraOnly = savedExtraOnly;
            loadout.furnaceOpen = false;
        }
    }

    private static void bundle(ServerPlayer player) {
        ItemStack out = craft(player, 1, 2, new ItemStack(Items.STRING), new ItemStack(Items.LEATHER));
        SelfTest.check(
                "string over leather crafts a bundle",
                out.is(Items.BUNDLE) && out.getCount() == 1
                        && out.isItemEnabled(player.level().enabledFeatures()),
                "result=" + out);
        Inventory inv = player.getInventory();
        ItemStack saved = inv.getItem(9);
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        inv.setItem(9, bundle);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 200);
        int expected = Math.min(StackUpgradeService.originalMax(cobble), cobble.getMaxStackSize());
        // 26.3: ItemStack.onClicked + SlotContext are gone; bundles are clicked
        // through the BUNDLE_CONTENTS component. tryInsert returns the leftover.
        BundleContents.Mutable contents =
                bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).asMutable();
        int leftover = contents.tryInsert(cobble);
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        cobble.setCount(leftover);
        inv.setItem(9, saved);
        SelfTest.check(
                "a bundle holds one ordinary stack",
                200 - cobble.getCount() == expected,
                "took " + (200 - cobble.getCount()) + " of 200, expected " + expected);
    }

    private static void trade(ServerPlayer player, Inventory inv, PlayerLoadout loadout) {
        clear(inv, loadout);
        loadout.personal.setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADES[3].get()));
        inv.setItem(9, new ItemStack(Items.EMERALD, 200));
        ClientSideMerchant merchant = new ClientSideMerchant(player);
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(Items.EMERALD, Items.BREAD));
        merchant.overrideOffers(offers);
        MerchantMenu menu = new MerchantMenu(0, inv, merchant);
        menu.tryMoveItems(0);
        int paid = menu.getSlot(0).getItem().getCount();
        int kept = count(inv, Items.EMERALD);
        menu.removed(player);
        int after = count(inv, Items.EMERALD);
        SelfTest.check(
                "clicking a trade keeps an oversized stack whole",
                paid > 0 && paid <= 64 && paid + kept == 200 && after == 200,
                "paymentSlot=" + paid + " inventory=" + kept + " afterClosing=" + after + " of 200");
    }

    private static void furnace(ServerPlayer player, Inventory inv, PlayerLoadout loadout) {
        clear(inv, loadout);
        loadout.personal.setStackInSlot(1, new ItemStack(ModItems.FURNACE_CRAFTING_UPGRADE.get()));
        FurnaceState furnace = loadout.furnace;
        clear(furnace.items);
        furnace.lit = 0;
        furnace.cook = 0;
        furnace.xp = 0.0F;
        furnace.items.setStackInSlot(FurnaceState.INPUT, new ItemStack(Items.RAW_IRON, 3));
        furnace.items.setStackInSlot(FurnaceState.FUEL, new ItemStack(Items.COAL, 1));

        for (int i = 0; i < 605; i++) {
            FurnaceService.tick(player, loadout);
        }

        ItemStack out = furnace.items.getStackInSlot(FurnaceState.OUTPUT);
        SelfTest.check(
                "the Furnace Crafting Upgrade smelts",
                loadout.hasFurnace()
                        && out.is(Items.IRON_INGOT)
                        && out.getCount() == 3
                        && furnace.items.getStackInSlot(FurnaceState.INPUT).isEmpty()
                        && furnace.items.getStackInSlot(FurnaceState.FUEL).isEmpty()
                        && furnace.lit > 0
                        && furnace.xp > 0.0F,
                "out=" + out
                        + " in=" + furnace.items.getStackInSlot(FurnaceState.INPUT)
                        + " fuel=" + furnace.items.getStackInSlot(FurnaceState.FUEL)
                        + " lit=" + furnace.lit + "/" + furnace.litDuration
                        + " xp=" + furnace.xp);
        loadout.personal.setStackInSlot(1, ItemStack.EMPTY);
        FurnaceService.tick(player, loadout);
        SelfTest.check(
                "...and hands its contents back when the upgrade is taken out",
                furnace.isEmpty() && count(inv, Items.IRON_INGOT) == 3,
                "furnaceEmpty=" + furnace.isEmpty() + " ingotsInInventory=" + count(inv, Items.IRON_INGOT));
    }

    private static void tabsWithoutBackpack(ServerPlayer player, Inventory inv, PlayerLoadout loadout) {
        clear(inv, loadout);
        loadout.backpacks.setStackInSlot(0, ItemStack.EMPTY);
        TabRules.apply(player, loadout, true, false, false);
        boolean unlocked = loadout.tabAvailable(4) && loadout.gatherUnlocked() && loadout.upgradesUnlocked();
        TabStorage storage = new TabStorage(player, loadout);
        TabService.switchTo(player, 2);
        storage.setItem(27, new ItemStack(Items.DIAMOND, 5));
        TabService.switchTo(player, 0);
        boolean ownPage = storage.getItem(27).isEmpty();
        TabService.switchTo(player, 2);
        ItemStack back = storage.getItem(27);
        TabService.switchTo(player, 0);
        SelfTest.check(
                "with requireBackpack off every tab works without a backpack",
                unlocked && ownPage && back.is(Items.DIAMOND) && back.getCount() == 5,
                "unlocked=" + unlocked + " tab0SeesIt=" + !ownPage + " tab2=" + back);
        int slot = PlayerLoadout.pageBase(2) + 27;
        TabRules.apply(player, loadout, false, false, true);
        boolean waiting = !loadout.tabAvailable(2) && loadout.heldPages.getStackInSlot(slot).getCount() == 5;
        loadout.backpacks.setStackInSlot(0, new ItemStack(ModItems.BACKPACKS[1].get()));
        TabRules.apply(player, loadout, false, false, true);
        boolean tooSmall = loadout.heldPages.getStackInSlot(slot).getCount() == 5;
        loadout.backpacks.setStackInSlot(0, new ItemStack(ModItems.BACKPACKS[4].get()));
        BigStackHandler pack = loadout.pageStore();
        pack.setStackInSlot(slot, new ItemStack(Items.REDSTONE, 3));
        TabRules.apply(player, loadout, false, false, true);
        pack = loadout.pageStore();
        int inPack = count(pack, Items.DIAMOND);
        SelfTest.check(
                "...and with it back on, the pages wait for a backpack with that tab and move into it",
                waiting
                        && tooSmall
                        && inPack == 5
                        && pack.getStackInSlot(slot).is(Items.REDSTONE)
                        && pack.getStackInSlot(slot + 1).is(Items.DIAMOND)
                        && loadout.heldPages.getStackInSlot(slot).isEmpty(),
                "waited=" + waiting
                        + " smallBackpackLeftIt=" + tooSmall
                        + " inBackpack=" + inPack
                        + " ownSlotKept=" + pack.getStackInSlot(slot)
                        + " stillHeld=" + loadout.heldPages.getStackInSlot(slot));
        loadout.backpacks.setStackInSlot(0, ItemStack.EMPTY);
    }

    private static void extraOnlyWithNoRoom(ServerPlayer player, Inventory inv, PlayerLoadout loadout) {
        clear(inv, loadout);
        loadout.backpacks.setStackInSlot(0, new ItemStack(ModItems.BACKPACKS[4].get()));
        TabRules.apply(player, loadout, false, false, false);
        int base = PlayerLoadout.pageBase(2);
        int extra = base + 27;
        BigStackHandler pack = loadout.pageStore();
        for (int i = extra; i < base + PlayerLoadout.PAGE_SIZE; i++) {
            pack.setStackInSlot(i, new ItemStack(Items.STICK, 64));
        }
        for (int i = 0; i < loadout.gather.getSlots(); i++) {
            loadout.gather.setStackInSlot(i, new ItemStack(Items.STICK, 64));
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            inv.setItem(i, new ItemStack(Items.STICK, 64));
        }
        pack.setStackInSlot(base + 3, new ItemStack(Items.IRON_INGOT, 9));
        pack.setStackInSlot(base + 4, new ItemStack(Items.GOLD_INGOT, 9));
        loadout.mainStash.setStackInSlot(0, new ItemStack(Items.EMERALD, 4));
        int onGround = itemsNear(player);
        boolean creative = player.getAbilities().instabuild;
        player.getAbilities().instabuild = true;

        try {
            TabRules.apply(player, loadout, false, true, false);
            pack = loadout.pageStore();
            boolean kept = pack.getStackInSlot(base + 3).getCount() == 9
                    && pack.getStackInSlot(base + 4).getCount() == 9
                    && loadout.mainStash.getStackInSlot(0).getCount() == 4
                    && loadout.mainUnsettled;
            pack.setStackInSlot(extra, ItemStack.EMPTY);
            TabRules.apply(player, loadout, false, true, true);
            pack = loadout.pageStore();
            boolean one = pack.getStackInSlot(extra).is(Items.IRON_INGOT)
                    && pack.getStackInSlot(base + 3).isEmpty()
                    && pack.getStackInSlot(base + 4).getCount() == 9
                    && loadout.mainUnsettled;
            loadout.gather.setStackInSlot(0, ItemStack.EMPTY);
            loadout.gather.setStackInSlot(1, ItemStack.EMPTY);
            TabRules.apply(player, loadout, false, true, true);
            pack = loadout.pageStore();
            boolean all = count(loadout.gather, Items.EMERALD) == 4
                    && count(loadout.gather, Items.GOLD_INGOT) == 9
                    && pack.getStackInSlot(base + 4).isEmpty()
                    && loadout.mainStash.getStackInSlot(0).isEmpty()
                    && !loadout.mainUnsettled;
            SelfTest.check(
                    "with no room anywhere, turning swapMainInventory off drops nothing: it waits, and settles as room appears",
                    kept && one && all && itemsNear(player) == onGround,
                    "allKeptAtFirst=" + kept + " oneSlotOfRoomTakesOne=" + one
                            + " restFollows=" + all + " newItemsOnTheGround=" + (itemsNear(player) - onGround));
            pack.setStackInSlot(base + 5, new ItemStack(Items.REDSTONE, 3));
            loadout.mainStash.setStackInSlot(2, new ItemStack(Items.LAPIS_LAZULI, 6));
            TabService.switchTo(player, 2);
            TabRules.apply(player, loadout, false, false, false);
            pack = loadout.pageStore();
            boolean back = loadout.activeTab == 0
                    && pack.getStackInSlot(base + 5).getCount() == 3
                    && loadout.mainStash.getStackInSlot(2).isEmpty()
                    && count(pack, Items.LAPIS_LAZULI) == 6;
            TabService.switchTo(player, 2);
            boolean tabTwo = inv.getItem(14).is(Items.REDSTONE);
            TabService.switchTo(player, 0);
            boolean tabZero = inv.getItem(14).is(Items.STICK);
            SelfTest.check(
                    "...and turning it back on meanwhile keeps what was still waiting",
                    back && tabTwo && tabZero && itemsNear(player) == onGround,
                    "onDefaultTab=" + (loadout.activeTab == 0)
                            + " stashEmptiedIntoAPage=" + back
                            + " tab2HasItsMainSlots=" + tabTwo
                            + " defaultTabHasItsOwn=" + tabZero
                            + " newItemsOnTheGround=" + (itemsNear(player) - onGround));
        } finally {
            player.getAbilities().instabuild = creative;
        }

        loadout.backpacks.setStackInSlot(0, ItemStack.EMPTY);
    }

    private static void extraOnly(ServerPlayer player, Inventory inv, PlayerLoadout loadout) {
        clear(inv, loadout);
        loadout.backpacks.setStackInSlot(0, new ItemStack(ModItems.BACKPACKS[4].get()));
        TabRules.apply(player, loadout, false, false, false);
        inv.setItem(9, new ItemStack(Items.GOLD_INGOT, 7));
        TabService.switchTo(player, 1);
        boolean swapped = inv.getItem(9).isEmpty();
        TabService.switchTo(player, 0);
        BigStackHandler pack = loadout.pageStore();
        int base = PlayerLoadout.pageBase(2);
        pack.setStackInSlot(base + 3, new ItemStack(Items.IRON_INGOT, 9));
        TabRules.apply(player, loadout, false, true, false);
        pack = loadout.pageStore();
        int settled = 0;
        for (int i = base + 27; i < base + PlayerLoadout.PAGE_SIZE; i++) {
            if (pack.getStackInSlot(i).is(Items.IRON_INGOT)) {
                settled += pack.getStackInSlot(i).getCount();
            }
        }
        boolean evacuated = pack.getStackInSlot(base + 3).isEmpty() && settled == 9;
        TabService.switchTo(player, 1);
        ItemStack main = inv.getItem(9);
        TabStorage storage = new TabStorage(player, loadout);
        storage.setItem(28, new ItemStack(Items.REDSTONE, 4));
        TabService.switchTo(player, 0);
        boolean paged = storage.getItem(28).isEmpty();
        SelfTest.check(
                "with swapMainInventory off tabs only page the extra area",
                swapped && main.is(Items.GOLD_INGOT) && main.getCount() == 7 && paged && evacuated,
                "swappedBefore=" + swapped + " mainStays=" + main + " extraPaged=" + paged
                        + " oldMainSlotsSettled=" + evacuated);
        TabRules.apply(player, loadout, false, false, false);
        loadout.backpacks.setStackInSlot(0, ItemStack.EMPTY);
    }

    private static void heldBundle(ServerPlayer player, PlayerLoadout loadout) {
        loadout.backpacks.setStackInSlot(0, ItemStack.EMPTY);
        TabRules.apply(player, loadout, true, false, false);
        loadout.heldPages.setStackInSlot(40, new ItemStack(Items.COBBLESTONE, 300));
        loadout.heldUpgrades.setStackInSlot(0,
                new ItemStack(ModItems.UPGRADES.get(UpgradeItem.Kind.MAGNET).get(0).get()));
        ItemStack bundle = LoadoutReturnService.packHeld(player, loadout);
        boolean packed = bundle != null
                && loadout.heldPages.getStackInSlot(40).isEmpty()
                && loadout.heldUpgrades.getStackInSlot(0).isEmpty();
        NonNullList<ItemStack> corpse = NonNullList.create();
        if (bundle != null) {
            corpse.add(bundle);
        }
        LoadoutReturnService.routeHome(player, corpse);
        SelfTest.check(
                "what is kept without a backpack leaves as one item and unpacks on the way home",
                packed && corpse.isEmpty()
                        && loadout.heldPages.getStackInSlot(40).getCount() == 300
                        && !loadout.heldUpgrades.getStackInSlot(0).isEmpty(),
                "packed=" + packed
                        + " leftInCorpse=" + corpse.size()
                        + " page=" + loadout.heldPages.getStackInSlot(40)
                        + " upgrade=" + loadout.heldUpgrades.getStackInSlot(0));
        TabRules.apply(player, loadout, false, false, false);
    }
}
