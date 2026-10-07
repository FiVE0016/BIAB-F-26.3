package dev.fallingcloud.betterinventory.inv;

import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * State of the portable furnace granted by the furnace crafting upgrade.
 *
 * <p>Ported from 1.3.0. Slots are INPUT / FUEL / OUTPUT.
 *
 * <p>The slots deliberately do NOT scale with stack upgrades: a furnace is a
 * processing block, and letting a stack upgrade smelt a thousand items per burn
 * would break the cost curve. UpgradeService feeds it one recipe at a time.
 */
public final class FurnaceState {
    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;
    public static final int SLOTS = 3;

    public int lit;
    public int litDuration;
    public int cook;
    public int cookTotal;
    public float xp;

    /** Last seen input, so a recipe lookup can be skipped while it is unchanged. */
    public ItemStack cachedInput = ItemStack.EMPTY;
    /** Last resolved recipe, cached the same way. Typed as Object: it is a
     *  registry holder whose type is only needed by FurnaceService. */
    public Object cachedSmelt;

    public final ItemStackHandler items;

    public FurnaceState(Runnable changed) {
        this.items = new ItemStackHandler(SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                changed.run();
            }
        };
    }

    public boolean isEmpty() {
        for (int i = 0; i < SLOTS; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isLit() {
        return lit > 0;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("items", items.serializeNBT(provider));
        tag.putInt("lit", lit);
        tag.putInt("lit_duration", litDuration);
        tag.putInt("cook", cook);
        tag.putInt("cook_total", cookTotal);
        tag.putFloat("xp", xp);
        return tag;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        items.deserializeNBT(provider, tag.getCompoundOrEmpty("items"));
        lit = tag.getIntOr("lit", 0);
        litDuration = tag.getIntOr("lit_duration", 0);
        cook = tag.getIntOr("cook", 0);
        cookTotal = tag.getIntOr("cook_total", 0);
        xp = tag.getFloatOr("xp", 0.0F);
        cachedInput = ItemStack.EMPTY;
        cachedSmelt = null;
    }
}