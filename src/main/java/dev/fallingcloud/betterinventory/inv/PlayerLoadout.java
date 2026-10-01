package dev.fallingcloud.betterinventory.inv;

import dev.fallingcloud.betterinventory.data.SettingKey;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.item.StackUpgradeItem;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.registry.ModTags;
import java.util.EnumSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import dev.fallingcloud.betterinventory.shim.Nullable;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.INBTSerializable;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;

/**
 * All per-player BetterInventory state: the tool rack, the offhand carousel, the personal
 * gather hub, equipped backpacks, personal upgrades, and settings.
 */
public class PlayerLoadout implements INBTSerializable<CompoundTag> {
    public static final int TOOL_HOE = 0;
    public static final int TOOL_SHOVEL = 1;
    public static final int TOOL_PICKAXE_1 = 2;
    public static final int TOOL_PICKAXE_2 = 3;
    public static final int TOOL_AXE = 4;
    public static final int TOOL_SWORD = 5;

    public static final int TAB_COUNT = 5; // 0 = default, 1..4 = backpacks

    /** Set when anything HUD-relevant changed and a client sync should be sent. */
    public boolean dirty = true;

    public final ItemStackHandler tools = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidTool(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
        }
    };

    public final ItemStackHandler offhandStore = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
        }
    };

    public final BigStackHandler gather;

    public final ItemStackHandler backpacks = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof BackpackItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
        }
    };

    public final ItemStackHandler personal = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? stack.getItem() instanceof StackUpgradeItem : stack.is(ModTags.AUX_UPGRADES);
        }

        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
        }
    };

    /**
     * Holds the DEFAULT tab's 27 main slots while a backpack tab is active.
     *
     * <p>The active tab's contents always live in the real player inventory, so that
     * every screen - chests, other mods' GUIs, anything reading the inventory - sees
     * the tab you actually have open. Exactly one copy of any item exists: whichever
     * store is not currently active holds it, and the live inventory holds the rest.
     */
    public final ItemStackHandler mainStash = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
        }
    };

    public int activeTab = 0;
    public int activeOffhand = 0;
    /** Crafting tab: shows the vertical crafting grid in place of the character view. */
    public boolean craftingOpen = false;

    public TabSettings defaultTab = TabSettings.DEFAULT;
    public TabSettings shared = TabSettings.DEFAULT;
    public final EnumSet<SettingKey> sharedKeys = EnumSet.noneOf(SettingKey.class);

    public PlayerLoadout() {
        this.gather = new BigStackHandler(18, BigStackHandler.fromUpgrade(personal)) {
            @Override
            protected void onContentsChanged(int slot) {
                dirty = true;
            }
        };
    }

    public static boolean isValidTool(int slot, ItemStack stack) {
        return switch (slot) {
            case TOOL_HOE -> stack.is(ItemTags.HOES);
            case TOOL_SHOVEL -> stack.is(ItemTags.SHOVELS);
            case TOOL_PICKAXE_1, TOOL_PICKAXE_2 -> stack.is(ItemTags.PICKAXES);
            case TOOL_AXE -> stack.is(ItemTags.AXES);
            case TOOL_SWORD -> stack.is(ItemTags.SWORDS);
            default -> false;
        };
    }

    public boolean hasAnyBackpack() {
        for (int i = 0; i < backpacks.getSlots(); i++) {
            if (!backpacks.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack backpack(int tab) {
        if (tab < 1 || tab > 4) {
            return ItemStack.EMPTY;
        }
        return backpacks.getStackInSlot(tab - 1);
    }

    public boolean tabAvailable(int tab) {
        return tab == 0 || !backpack(tab).isEmpty();
    }

    /** The crafting upgrade in the personal aux slot, if any. */
    @Nullable
    private dev.fallingcloud.betterinventory.item.CraftingUpgradeItem craftingUpgrade() {
        return personal.getStackInSlot(1).getItem()
                instanceof dev.fallingcloud.betterinventory.item.CraftingUpgradeItem upgrade ? upgrade : null;
    }

    public boolean hasCraftingUpgrade() {
        return craftingUpgrade() != null;
    }

    /** Columns of the crafting grid available: 2 normally, 3 with the advanced upgrade. */
    public int craftingCols() {
        var upgrade = craftingUpgrade();
        return upgrade == null ? 2 : upgrade.cols();
    }

    /** Rows of the crafting grid available: 2 normally, 3 once upgraded. */
    public int craftingRows() {
        var upgrade = craftingUpgrade();
        return upgrade == null ? 2 : upgrade.rows();
    }

    public int storageCap() {
        ItemStack stack = personal.getStackInSlot(0);
        if (stack.getItem() instanceof StackUpgradeItem upgrade) {
            return upgrade.capacity();
        }
        return 64;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("tools", tools.serializeNBT(provider));
        tag.put("offhand_store", offhandStore.serializeNBT(provider));
        tag.put("gather", gather.serializeNBT(provider));
        tag.put("backpacks", backpacks.serializeNBT(provider));
        tag.put("personal", personal.serializeNBT(provider));
        tag.put("main_stash", mainStash.serializeNBT(provider));
        tag.putInt("active_tab", activeTab);
        tag.putBoolean("crafting_open", craftingOpen);
        tag.putInt("active_offhand", activeOffhand);
        TabSettings.CODEC.encodeStart(NbtOps.INSTANCE, defaultTab).result().ifPresent(t -> tag.put("default_tab", t));
        TabSettings.CODEC.encodeStart(NbtOps.INSTANCE, shared).result().ifPresent(t -> tag.put("shared", t));
        int mask = 0;
        for (SettingKey key : sharedKeys) {
            mask |= 1 << key.ordinal();
        }
        tag.putInt("shared_keys", mask);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        tools.deserializeNBT(provider, tag.getCompoundOrEmpty("tools"));
        offhandStore.deserializeNBT(provider, tag.getCompoundOrEmpty("offhand_store"));
        gather.deserializeNBT(provider, tag.getCompoundOrEmpty("gather"));
        backpacks.deserializeNBT(provider, tag.getCompoundOrEmpty("backpacks"));
        personal.deserializeNBT(provider, tag.getCompoundOrEmpty("personal"));
        mainStash.deserializeNBT(provider, tag.getCompoundOrEmpty("main_stash"));
        activeTab = Math.floorMod(tag.getIntOr("active_tab", 0), TAB_COUNT);
        craftingOpen = tag.getBooleanOr("crafting_open", false);
        activeOffhand = Math.floorMod(tag.getIntOr("active_offhand", 0), 4);
        if (!tag.getCompoundOrEmpty("default_tab").isEmpty()) {
            TabSettings.CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("default_tab")).result().ifPresent(s -> defaultTab = s);
        }
        if (!tag.getCompoundOrEmpty("shared").isEmpty()) {
            TabSettings.CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("shared")).result().ifPresent(s -> shared = s);
        }
        sharedKeys.clear();
        int mask = tag.getIntOr("shared_keys", 0);
        for (SettingKey key : SettingKey.values()) {
            if ((mask & (1 << key.ordinal())) != 0) {
                sharedKeys.add(key);
            }
        }
        // Keep handler sizes sane even if older data had different sizes.
        ensureSize(tools, 6);
        ensureSize(offhandStore, 4);
        ensureSize(gather, 18);
        ensureSize(backpacks, 4);
        ensureSize(personal, 2);
        ensureSize(mainStash, 27);
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

    /** Marks every ability upgrade installed in the given equipped backpack. */
    public boolean upgradeInstalled(int tab, UpgradeItem.Kind kind) {
        ItemStack pack = backpack(tab);
        if (pack.isEmpty()) {
            return false;
        }
        return BackpackItem.upgrades(pack).stream()
                .anyMatch(s -> s.getItem() instanceof UpgradeItem u && u.kind() == kind);
    }
}
