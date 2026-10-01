package dev.fallingcloud.betterinventory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable sparse item list used as an item data component (backpack contents and
 * installed upgrades). Counts may exceed vanilla stack sizes (stack upgrades); the
 * save codec is widened accordingly by ItemStackCountCodecMixin.
 */
public record StoredItems(int size, List<Entry> entries) {
    public static final StoredItems EMPTY = new StoredItems(0, List.of());

    public record Entry(int slot, ItemStack stack) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(Entry::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::stack)
        ).apply(i, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot,
                ItemStack.STREAM_CODEC, Entry::stack,
                Entry::new);

        @Override
        public boolean equals(Object o) {
            return o instanceof Entry e && e.slot == slot && ItemStack.matches(e.stack, stack);
        }

        @Override
        public int hashCode() {
            return 31 * slot + ItemStack.hashItemAndComponents(stack) * 31 + stack.getCount();
        }
    }

    public static final Codec<StoredItems> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("size").forGetter(StoredItems::size),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(StoredItems::entries)
    ).apply(i, StoredItems::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredItems> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoredItems::size,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), StoredItems::entries,
            StoredItems::new);

    public static StoredItems of(NonNullList<ItemStack> stacks) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) {
                entries.add(new Entry(i, stack.copy()));
            }
        }
        return new StoredItems(stacks.size(), List.copyOf(entries));
    }

    public NonNullList<ItemStack> unpack(int minSize) {
        NonNullList<ItemStack> list = NonNullList.withSize(Math.max(size, minSize), ItemStack.EMPTY);
        for (Entry e : entries) {
            if (e.slot() >= 0 && e.slot() < list.size() && !e.stack().isEmpty()) {
                list.set(e.slot(), e.stack().copy());
            }
        }
        return list;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
