package dev.fallingcloud.betterinventory.inv;

import dev.fallingcloud.betterinventory.compat.SlateBuildingCompat;
import dev.fallingcloud.betterinventory.data.SettingKey;
import dev.fallingcloud.betterinventory.data.StoredItems;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.CraftingUpgradeItem;
import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.logic.ItemKinds;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import dev.fallingcloud.betterinventory.shim.IItemHandlerModifiable;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.NonNullList;
import org.jetbrains.annotations.Nullable;

/**
 * All per-player BetterInventory state (1.4.1 layout): tool rack, offhand carousel,
 * armor store, gather hub, equipped backpack, personal upgrades, toolbox, held
 * (pack-free) storage, furnace and per-tab settings.
 *
 * <p>26.3 adaptation: the loadout lives in a Fabric attachment
 * ({@link ModAttachments}) instead of the 1.4.1 LoadoutHolder mixin, so
 * {@link #of(Player)} / {@link #existing(Player)} read it directly.
 */
public class PlayerLoadout {
    public static final int TOOL_HOE = 0;
    public static final int TOOL_SHOVEL = 1;
    public static final int TOOL_PICKAXE_1 = 2;
    public static final int TOOL_PICKAXE_2 = 3;
    public static final int TOOL_AXE = 4;
    public static final int TOOL_SWORD = 5;
    public static final int TAB_COUNT = 5;
    public static final EquipmentSlot[] ARMOR_ORDER =
            new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public static final int MAIN_SIZE = 27;
    public static final int GATHER_ROWS = 5;
    public static final int GATHER_SIZE = 45;
    public static final int GATHER_START = 27;
    public static final int PAGE_SIZE = 72;
    public static final int PAGE_COUNT = 4;
    public static final int CONTENTS_SIZE = 288;
    public static final int LEGACY_GATHER_SIZE = 18;
    public static final int LEGACY_PAGE_SIZE = 45;
    public static final int LEGACY_CONTENTS_SIZE = 180;
    public static final int UPGRADE_SLOTS = 3;

    public boolean dirty = true;
    public final Set<Identifier> safeEntities = new HashSet<>();
    public int weaponEngagedTick = Integer.MIN_VALUE;
    public final ItemStackHandler tools = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PlayerLoadout.isValidTool(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public final ItemStackHandler offhandStore = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public final ItemStackHandler armorStore = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < PlayerLoadout.ARMOR_ORDER.length
                    && PlayerLoadout.armorSlotOf(stack) == PlayerLoadout.ARMOR_ORDER[slot];
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public final BigStackHandler gather;
    public final ItemStackHandler backpacks = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof BackpackItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public final PackUpgradeHandler packUpgrades = new PackUpgradeHandler();
    public final ItemStackHandler personal = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0
                    ? stack.getItem() instanceof StackUpgradeItem
                    : stack.getItem() instanceof CraftingUpgradeItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public final ItemStackHandler toolbox = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return SlateBuildingCompat.isToolbox(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public final ItemStackHandler mainStash = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public boolean packFree = false;
    public boolean extraOnly = false;
    public boolean mainUnsettled = false;
    public final BigStackHandler heldPages;
    public final ItemStackHandler heldUpgrades = new ItemStackHandler(3) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PlayerLoadout.this.dirty = true;
        }
    };
    public TabSettings heldSettings = TabSettings.DEFAULT;
    public final FurnaceState furnace = new FurnaceState(() -> this.dirty = true);
    public boolean furnaceOpen = false;
    public int activeTab = 0;
    public int activeOffhand = 0;
    public boolean craftingOpen = false;
    public TabSettings defaultTab = TabSettings.DEFAULT;
    public TabSettings shared = TabSettings.DEFAULT;
    public final EnumSet<SettingKey> sharedKeys = EnumSet.noneOf(SettingKey.class);

    /** 1.4.1: legacy 27-slot page slots map onto the 72-slot 5x9 layout. */
    public static int migratePageSlot(int legacySlot) {
        return legacySlot / LEGACY_PAGE_SIZE * PAGE_SIZE + legacySlot % LEGACY_PAGE_SIZE;
    }

    public static StoredItems migrateContents(StoredItems stored) {
        if (stored.size() != LEGACY_CONTENTS_SIZE) {
            return stored;
        }
        List<StoredItems.Entry> entries = new ArrayList<>(stored.entries().size());
        for (StoredItems.Entry entry : stored.entries()) {
            int slot = entry.slot();
            if (slot >= 0 && slot < LEGACY_CONTENTS_SIZE) {
                slot = migratePageSlot(slot);
            }
            entries.add(new StoredItems.Entry(slot, entry.stack()));
        }
        return new StoredItems(CONTENTS_SIZE, List.copyOf(entries));
    }

    public static int pageBase(int tab) {
        return (tab - 1) * PAGE_SIZE;
    }

    public static PlayerLoadout of(Player player) {
        return ModAttachments.get(player);
    }

    @Nullable
    public static PlayerLoadout existing(Player player) {
        return player.getAttachedOrElse(ModAttachments.LOADOUT, null);
    }

    public PlayerLoadout() {
        this.gather = new BigStackHandler(GATHER_SIZE, BigStackHandler.fromUpgrade(this.personal)) {
            @Override
            protected void onContentsChanged(int slot) {
                PlayerLoadout.this.dirty = true;
            }
        };
        this.heldPages = new BigStackHandler(CONTENTS_SIZE, BigStackHandler.fromUpgrade(this.personal)) {
            @Override
            protected void onContentsChanged(int slot) {
                PlayerLoadout.this.dirty = true;
            }
        };
    }

    public static boolean isValidTool(int slot, ItemStack stack) {
        return switch (slot) {
            case TOOL_HOE -> ItemKinds.isHoe(stack);
            case TOOL_SHOVEL -> ItemKinds.isShovel(stack);
            case TOOL_PICKAXE_1, TOOL_PICKAXE_2 -> ItemKinds.isPickaxe(stack);
            case TOOL_AXE -> ItemKinds.isAxe(stack);
            case TOOL_SWORD -> ItemKinds.isMeleeWeapon(stack);
            default -> false;
        };
    }

    @Nullable
    public static EquipmentSlot armorSlotOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().isArmor() ? equippable.slot() : null;
    }

    public ItemStack backpack() {
        return this.backpacks.getStackInSlot(0);
    }

    public int tier() {
        return this.backpack().getItem() instanceof BackpackItem b ? b.level() : 0;
    }

    public int pages() {
        return this.packFree ? PAGE_COUNT : Math.max(0, this.tier() - 1);
    }

    public boolean hasAnyBackpack() {
        return this.tier() >= 1;
    }

    public boolean gatherUnlocked() {
        return this.packFree || this.tier() >= 1;
    }

    public boolean upgradesUnlocked() {
        return this.packFree || this.hasAnyBackpack();
    }

    @Nullable
    public BigStackHandler pageStore() {
        if (this.packFree) {
            return this.heldPages;
        }
        ItemStack pack = this.backpack();
        if (pack.isEmpty()) {
            return null;
        }
        ComponentBackedHandler handler =
                new ComponentBackedHandler(pack, ModComponents.BACKPACK_CONTENTS.get(), CONTENTS_SIZE, this::storageCap);
        handler.setChangeListener(() -> this.dirty = true);
        return handler;
    }

    public boolean tabAvailable(int tab) {
        return tab == 0 || tab >= 1 && tab <= this.pages();
    }

    public ItemStack backpack(int tab) {
        return tab >= 1 && tab <= PAGE_COUNT ? this.backpack() : ItemStack.EMPTY;
    }

    public ItemStack highestBackpack() {
        return this.backpack();
    }

    @Nullable
    private CraftingUpgradeItem craftingUpgrade() {
        return this.personal.getStackInSlot(1).getItem() instanceof CraftingUpgradeItem upgrade ? upgrade : null;
    }

    public boolean hasCraftingUpgrade() {
        return this.craftingUpgrade() != null;
    }

    public boolean hasFurnace() {
        CraftingUpgradeItem upgrade = this.craftingUpgrade();
        return upgrade != null && upgrade.furnace();
    }

    public int craftingCols() {
        CraftingUpgradeItem upgrade = this.craftingUpgrade();
        return upgrade == null ? 2 : upgrade.cols();
    }

    public int craftingRows() {
        CraftingUpgradeItem upgrade = this.craftingUpgrade();
        return upgrade == null ? 2 : upgrade.rows();
    }

    public int storageCap() {
        ItemStack stack = this.personal.getStackInSlot(0);
        return stack.getItem() instanceof StackUpgradeItem upgrade ? upgrade.capacity() : 64;
    }

    public int upgradeLevel(UpgradeItem.Kind kind) {
        int best = 0;
        for (int i = 0; i < this.packUpgrades.getSlots(); i++) {
            if (this.packUpgrades.getStackInSlot(i).getItem() instanceof UpgradeItem u && u.kind() == kind) {
                best = Math.max(best, u.level());
            }
        }
        return best;
    }

    public boolean upgradeInstalled(UpgradeItem.Kind kind) {
        return this.upgradeLevel(kind) > 0;
    }

    /**
     * 1.0.0-compat alias; the old per-tab signature routed to the same
     * single-backpack upgrade storage. Remove once the services are on 1.4.1.
     */
    public boolean upgradeInstalled(int tab, UpgradeItem.Kind kind) {
        return this.upgradeInstalled(kind);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("tools", this.tools.serializeNBT(provider));
        tag.put("offhand_store", this.offhandStore.serializeNBT(provider));
        tag.put("armor_store", this.armorStore.serializeNBT(provider));
        tag.put("gather", this.gather.serializeNBT(provider));
        tag.put("backpacks", this.backpacks.serializeNBT(provider));
        tag.put("personal", this.personal.serializeNBT(provider));
        tag.put("main_stash", this.mainStash.serializeNBT(provider));
        tag.put("toolbox", this.toolbox.serializeNBT(provider));
        tag.putBoolean("pack_free", this.packFree);
        tag.putBoolean("extra_only", this.extraOnly);
        tag.putBoolean("main_unsettled", this.mainUnsettled);
        tag.put("held_pages", this.heldPages.serializeNBT(provider));
        tag.put("held_upgrades", this.heldUpgrades.serializeNBT(provider));
        TabSettings.CODEC.encodeStart(NbtOps.INSTANCE, this.heldSettings).result()
                .ifPresent(t -> tag.put("held_settings", t));
        tag.put("furnace", this.furnace.save(provider));
        tag.putBoolean("furnace_open", this.furnaceOpen);
        tag.putInt("active_tab", this.activeTab);
        tag.putBoolean("crafting_open", this.craftingOpen);
        tag.putInt("active_offhand", this.activeOffhand);
        TabSettings.CODEC.encodeStart(NbtOps.INSTANCE, this.defaultTab).result()
                .ifPresent(t -> tag.put("default_tab", t));
        TabSettings.CODEC.encodeStart(NbtOps.INSTANCE, this.shared).result()
                .ifPresent(t -> tag.put("shared", t));
        int mask = 0;
        for (SettingKey key : this.sharedKeys) {
            mask |= 1 << key.ordinal();
        }
        tag.putInt("shared_keys", mask);
        ListTag safe = new ListTag();
        for (Identifier id : this.safeEntities) {
            safe.add(StringTag.valueOf(id.toString()));
        }
        tag.put("safe_entities", safe);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.tools.deserializeNBT(provider, tag.getCompoundOrEmpty("tools"));
        this.offhandStore.deserializeNBT(provider, tag.getCompoundOrEmpty("offhand_store"));
        this.armorStore.deserializeNBT(provider, tag.getCompoundOrEmpty("armor_store"));
        this.gather.deserializeNBT(provider, tag.getCompoundOrEmpty("gather"));
        this.backpacks.deserializeNBT(provider, tag.getCompoundOrEmpty("backpacks"));
        this.personal.deserializeNBT(provider, tag.getCompoundOrEmpty("personal"));
        this.mainStash.deserializeNBT(provider, tag.getCompoundOrEmpty("main_stash"));
        this.toolbox.deserializeNBT(provider, tag.getCompoundOrEmpty("toolbox"));
        this.packFree = tag.getBooleanOr("pack_free", false);
        this.extraOnly = tag.getBooleanOr("extra_only", false);
        this.mainUnsettled = tag.getBooleanOr("main_unsettled", false);
        this.heldPages.deserializeNBT(provider, tag.getCompoundOrEmpty("held_pages"));
        this.heldUpgrades.deserializeNBT(provider, tag.getCompoundOrEmpty("held_upgrades"));
        this.heldSettings = TabSettings.DEFAULT;
        if (tag.getCompoundOrEmpty("held_settings").size() > 0) {
            TabSettings.CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("held_settings")).result()
                    .ifPresent(s -> this.heldSettings = s);
        }
        this.furnace.load(provider, tag.getCompoundOrEmpty("furnace"));
        this.furnaceOpen = tag.getBooleanOr("furnace_open", false);
        this.safeEntities.clear();
        ListTag safeList = tag.getListOrEmpty("safe_entities");
        for (Tag t : safeList) {
            Identifier id = t.asString().map(Identifier::tryParse).orElse(null);
            if (id != null) {
                this.safeEntities.add(id);
            }
        }
        this.activeTab = Math.floorMod(tag.getIntOr("active_tab", 0), TAB_COUNT);
        this.craftingOpen = tag.getBooleanOr("crafting_open", false);
        this.activeOffhand = Math.floorMod(tag.getIntOr("active_offhand", 0), 4);
        if (tag.getCompoundOrEmpty("default_tab").size() > 0) {
            TabSettings.CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("default_tab")).result()
                    .ifPresent(s -> this.defaultTab = s);
        }
        if (tag.getCompoundOrEmpty("shared").size() > 0) {
            TabSettings.CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("shared")).result()
                    .ifPresent(s -> this.shared = s);
        }
        this.sharedKeys.clear();
        int mask = tag.getIntOr("shared_keys", 0);
        for (SettingKey key : SettingKey.values()) {
            if ((mask & 1 << key.ordinal()) != 0) {
                this.sharedKeys.add(key);
            }
        }
        this.migrateLegacyBackpacks(provider, tag);
        ensureSize(this.tools, 6);
        ensureSize(this.offhandStore, 4);
        ensureSize(this.armorStore, 4);
        ensureSize(this.gather, GATHER_SIZE);
        ensureSize(this.backpacks, 1);
        ensureSize(this.personal, 2);
        ensureSize(this.mainStash, 27);
        ensureSize(this.toolbox, 1);
        ensureSize(this.heldPages, CONTENTS_SIZE);
        ensureSize(this.heldUpgrades, UPGRADE_SLOTS);
    }

    private void migrateLegacyBackpacks(HolderLookup.Provider provider, CompoundTag tag) {
        if (this.backpacks.getSlots() > 1) {
            int bestSlot = -1;
            int bestLevel = 0;
            for (int i = 0; i < this.backpacks.getSlots(); i++) {
                if (this.backpacks.getStackInSlot(i).getItem() instanceof BackpackItem b && b.level() > bestLevel) {
                    bestLevel = b.level();
                    bestSlot = i;
                }
            }
            ItemStack kept = bestSlot < 0 ? ItemStack.EMPTY : this.backpacks.getStackInSlot(bestSlot);
            for (int i = 0; i < this.backpacks.getSlots(); i++) {
                ItemStack pack = this.backpacks.getStackInSlot(i);
                this.backpacks.setStackInSlot(i, ItemStack.EMPTY);
                if (i != bestSlot && !pack.isEmpty()) {
                    this.stashItem(pack);
                }
            }
            this.backpacks.setStackInSlot(0, kept);
        }
        ItemStack pack = this.backpack();
        StoredItems upgrades = pack.getOrDefault(ModComponents.BACKPACK_UPGRADES.get(), StoredItems.EMPTY);
        if (!pack.isEmpty() && upgrades.size() > UPGRADE_SLOTS) {
            pack.remove(ModComponents.BACKPACK_UPGRADES.get());
            for (ItemStack upgrade : upgrades.unpack(0)) {
                if (!upgrade.isEmpty() && !this.insertUpgrade(upgrade)) {
                    this.stashItem(upgrade);
                }
            }
        }
        if (tag.getCompoundOrEmpty("pack_upgrades").size() > 0) {
            ItemStackHandler legacy = new ItemStackHandler(UPGRADE_SLOTS);
            legacy.deserializeNBT(provider, tag.getCompoundOrEmpty("pack_upgrades"));
            for (int i = 0; i < legacy.getSlots(); i++) {
                ItemStack upgrade = legacy.getStackInSlot(i);
                if (!upgrade.isEmpty() && !this.insertUpgrade(upgrade)) {
                    this.stashItem(upgrade);
                }
            }
        }
        this.dirty = true;
    }

    private boolean insertUpgrade(ItemStack upgrade) {
        for (int i = 0; i < this.packUpgrades.getSlots(); i++) {
            if (this.packUpgrades.getStackInSlot(i).isEmpty() && this.packUpgrades.isItemValid(i, upgrade)) {
                this.packUpgrades.setStackInSlot(i, upgrade);
                return true;
            }
        }
        return false;
    }

    private void stashItem(ItemStack stack) {
        for (int i = 0; i < this.mainStash.getSlots(); i++) {
            if (this.mainStash.getStackInSlot(i).isEmpty()) {
                this.mainStash.setStackInSlot(i, stack);
                return;
            }
        }
        for (int i = 0; i < this.gather.getSlots(); i++) {
            if (this.gather.getStackInSlot(i).isEmpty()) {
                this.gather.setStackInSlot(i, stack);
                return;
            }
        }
    }

    private static void ensureSize(ItemStackHandler handler, int size) {
        if (handler.getSlots() != size) {
            ItemStack[] copy = new ItemStack[Math.min(size, handler.getSlots())];
            for (int i = 0; i < copy.length; i++) {
                copy[i] = handler.getStackInSlot(i);
            }
            handler.resize(size);
            for (int i = 0; i < copy.length; i++) {
                handler.setStackInSlot(i, copy[i]);
            }
        }
    }

    /** 1.4.1: the three ability upgrade slots, backed by the backpack's component. */
    public class PackUpgradeHandler implements IItemHandlerModifiable {
        private NonNullList<ItemStack> read() {
            if (!PlayerLoadout.this.packFree) {
                ItemStack pack = PlayerLoadout.this.backpack();
                return pack.isEmpty()
                        ? NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY)
                        : pack.getOrDefault(ModComponents.BACKPACK_UPGRADES.get(), StoredItems.EMPTY).unpack(UPGRADE_SLOTS);
            }
            NonNullList<ItemStack> held = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);
            for (int i = 0; i < UPGRADE_SLOTS; i++) {
                held.set(i, PlayerLoadout.this.heldUpgrades.getStackInSlot(i));
            }
            return held;
        }

        private void write(NonNullList<ItemStack> stacks) {
            if (!PlayerLoadout.this.packFree) {
                ItemStack pack = PlayerLoadout.this.backpack();
                if (!pack.isEmpty()) {
                    StoredItems stored = StoredItems.of(stacks);
                    if (stored.isEmpty()) {
                        pack.remove(ModComponents.BACKPACK_UPGRADES.get());
                    } else {
                        pack.set(ModComponents.BACKPACK_UPGRADES.get(), stored);
                    }
                    PlayerLoadout.this.dirty = true;
                }
            } else {
                for (int i = 0; i < UPGRADE_SLOTS; i++) {
                    PlayerLoadout.this.heldUpgrades.setStackInSlot(i, stacks.get(i));
                }
                PlayerLoadout.this.dirty = true;
            }
        }

        @Override
        public int getSlots() {
            return UPGRADE_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            NonNullList<ItemStack> stacks = this.read();
            return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            NonNullList<ItemStack> stacks = this.read();
            if (slot >= 0 && slot < UPGRADE_SLOTS) {
                stacks.set(slot, stack);
                this.write(stacks);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stack.isEmpty() && this.isItemValid(slot, stack) && this.getStackInSlot(slot).isEmpty()) {
                if (!simulate) {
                    this.setStackInSlot(slot, stack.copyWithCount(1));
                }
                return stack.getCount() <= 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack existing = this.getStackInSlot(slot);
            if (!existing.isEmpty() && amount > 0) {
                if (!simulate) {
                    this.setStackInSlot(slot, ItemStack.EMPTY);
                }
                return existing.copy();
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (PlayerLoadout.this.upgradesUnlocked() && stack.getItem() instanceof UpgradeItem upgrade) {
                for (int i = 0; i < UPGRADE_SLOTS; i++) {
                    if (i != slot && this.getStackInSlot(i).getItem() instanceof UpgradeItem other
                            && other.kind() == upgrade.kind()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }
}
