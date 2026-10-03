package dev.fallingcloud.betterinventory.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import dev.fallingcloud.betterinventory.shim.IItemHandlerModifiable;
import dev.fallingcloud.betterinventory.shim.INBTSerializable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.NbtOps;

/**
 * A fixed-size stack store with per-slot validation and NBT round-tripping.
 *
 * <p>Replaces NeoForge's {@code ItemStackHandler}, which the mod leaned on in ~28
 * files and which made large parts of the codebase look loader-specific when they
 * are not. This uses only vanilla types, so it compiles for every target and the
 * tool rack / offhand carousel / gather hub / backpack slots become shared code.
 *
 * <p>Counts are written as ints rather than through the vanilla stack codec, because
 * stack upgrades allow counts far above the vanilla cap.
 */
public class ItemStore implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    protected NonNullList<ItemStack> stacks;
    private Runnable onChanged = () -> {};

    public ItemStore(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public void setChangeListener(Runnable listener) {
        this.onChanged = listener;
    }

    public int size() {
        return stacks.size();
    }

    public ItemStack get(int slot) {
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
    }

    public void set(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.size()) {
            return;
        }
        stacks.set(slot, stack);
        onContentsChanged(slot);
        onChanged();
    }

    /** Override to restrict what a slot accepts (tool rack, upgrade slots, ...). */
    public boolean isValid(int slot, ItemStack stack) {
        return isItemValid(slot, stack);
    }

    /** Hook for subclasses, matching NeoForge's {@code ItemStackHandler#onContentsChanged}. */
    protected void onContentsChanged(int slot) {}

    /** Per-slot stack ceiling hook, matching NeoForge's {@code ItemStackHandler#getStackLimit}. */
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    public void onChanged() {
        onChanged.run();
    }

    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Resizes in place, preserving what still fits. */
    public void resize(int size) {
        if (size == stacks.size()) {
            return;
        }
        NonNullList<ItemStack> resized = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(size, stacks.size()); i++) {
            resized.set(i, stacks.get(i));
        }
        stacks = resized;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            // Count stored separately: oversized stacks exceed the vanilla codec bound.
            entry.putInt("Count", stack.getCount());
            entry.put("Item", ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack.copyWithCount(1)).getOrThrow());
            list.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", list);
        tag.putInt("Size", stacks.size());
        return tag;
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        resize(Math.max(1, tag.getIntOr("Size", stacks.size())));
        stacks.replaceAll(ignored -> ItemStack.EMPTY);
        ListTag list = tag.getListOrEmpty("Items");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            int slot = entry.getIntOr("Slot", -1);
            if (slot < 0 || slot >= stacks.size()) {
                continue;
            }
            ItemStack stack = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), entry.getCompoundOrEmpty("Item")).result().orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                stack.setCount(Math.max(1, entry.getIntOr("Count", 1)));
                stacks.set(slot, stack);
            }
        }
    }

    // ------------------------------------------------------------------ shim bridge
    // NeoForge's ItemStackHandler surface, so existing call sites compile unchanged.

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return get(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        set(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 99;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= stacks.size() || !isValid(slot, stack)) {
            return stack;
        }
        ItemStack existing = stacks.get(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        int room = limit - existing.getCount();
        if (room <= 0) {
            return stack;
        }
        int moved = Math.min(room, stack.getCount());
        if (!simulate) {
            ItemStack merged = existing.isEmpty() ? stack.copy() : existing.copy();
            merged.setCount(existing.getCount() + moved);
            set(slot, merged);
        }
        return moved >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= stacks.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, existing.getCount());
        ItemStack out = existing.copyWithCount(taken);
        if (!simulate) {
            if (taken >= existing.getCount()) {
                set(slot, ItemStack.EMPTY);
            } else {
                ItemStack left = existing.copy();
                left.setCount(existing.getCount() - taken);
                set(slot, left);
            }
        }
        return out;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        return save(registries);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        load(registries, tag);
    }
}
