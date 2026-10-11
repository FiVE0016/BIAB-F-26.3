package dev.fallingcloud.betterinventory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Per-tab settings. Stored on the player attachment for the default tab and as a data
 * component on each backpack for backpack tabs.
 *
 * <p>1.4.1 adds {@code pageLayout}: 0 = legacy (tabs hold 27 main slots), 1 = 5x9
 * (tabs page the whole 45-slot area). Old data migrates through
 * {@link #withLocksRebased(IntUnaryOperator)}.
 */
public record TabSettings(
        boolean gatherHub,
        boolean autoRefill,
        CombatPref combat,
        List<Identifier> pickaxe2Blocks,
        List<LockEntry> locks,
        Map<String, Boolean> upgradeToggles,
        int pageLayout) {

    public static final int PAGE_LAYOUT_LEGACY = 0;
    public static final int PAGE_LAYOUT_5X9 = 1;
    public static final TabSettings DEFAULT =
            new TabSettings(true, true, CombatPref.SWORD, List.of(), List.of(), Map.of(), 1);

    public enum CombatPref implements StringRepresentable {
        SWORD, AXE;

        public static final Codec<CombatPref> CODEC = StringRepresentable.fromEnum(CombatPref::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A slot lock: only the given item may occupy the slot. */
    public record LockEntry(int slot, Identifier item) {
        public static final Codec<LockEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(LockEntry::slot),
                Identifier.CODEC.fieldOf("item").forGetter(LockEntry::item)
        ).apply(i, LockEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, LockEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, LockEntry::slot,
                Identifier.STREAM_CODEC, LockEntry::item,
                LockEntry::new);
    }

    public static final Codec<TabSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("gather_hub", true).forGetter(TabSettings::gatherHub),
            Codec.BOOL.optionalFieldOf("auto_refill", true).forGetter(TabSettings::autoRefill),
            CombatPref.CODEC.optionalFieldOf("combat", CombatPref.SWORD).forGetter(TabSettings::combat),
            Identifier.CODEC.listOf().optionalFieldOf("pickaxe2_blocks", List.of()).forGetter(TabSettings::pickaxe2Blocks),
            LockEntry.CODEC.listOf().optionalFieldOf("locks", List.of()).forGetter(TabSettings::locks),
            Codec.unboundedMap(Codec.STRING, Codec.BOOL).optionalFieldOf("upgrade_toggles", Map.of())
                    .forGetter(TabSettings::upgradeToggles),
            Codec.INT.optionalFieldOf("page_layout", PAGE_LAYOUT_LEGACY).forGetter(TabSettings::pageLayout)
    ).apply(i, TabSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TabSettings::gatherHub,
            ByteBufCodecs.BOOL, TabSettings::autoRefill,
            ByteBufCodecs.idMapper(idx -> CombatPref.values()[idx], CombatPref::ordinal), TabSettings::combat,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), TabSettings::pickaxe2Blocks,
            LockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TabSettings::locks,
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.BOOL),
            TabSettings::upgradeToggles,
            ByteBufCodecs.VAR_INT, TabSettings::pageLayout,
            TabSettings::new);

    public TabSettings withGatherHub(boolean value) {
        return new TabSettings(value, autoRefill, combat, pickaxe2Blocks, locks, upgradeToggles, pageLayout);
    }

    public TabSettings withAutoRefill(boolean value) {
        return new TabSettings(gatherHub, value, combat, pickaxe2Blocks, locks, upgradeToggles, pageLayout);
    }

    public TabSettings withCombat(CombatPref value) {
        return new TabSettings(gatherHub, autoRefill, value, pickaxe2Blocks, locks, upgradeToggles, pageLayout);
    }

    public TabSettings withPickaxe2Blocks(List<Identifier> value) {
        return new TabSettings(gatherHub, autoRefill, combat, List.copyOf(value), locks, upgradeToggles, pageLayout);
    }

    public TabSettings withUpgradeToggle(String id, boolean value) {
        Map<String, Boolean> map = new LinkedHashMap<>(upgradeToggles);
        map.put(id, value);
        return new TabSettings(gatherHub, autoRefill, combat, pickaxe2Blocks, locks, Map.copyOf(map), pageLayout);
    }

    public TabSettings withLock(int slot, Optional<Identifier> item) {
        List<LockEntry> list = new ArrayList<>(locks);
        list.removeIf(l -> l.slot() == slot);
        item.ifPresent(rl -> list.add(new LockEntry(slot, rl)));
        return new TabSettings(gatherHub, autoRefill, combat, pickaxe2Blocks, List.copyOf(list), upgradeToggles, pageLayout);
    }

    /**
     * 1.4.1 migration: legacy lock slots (27-slot pages) are remapped to the 5x9
     * layout and the layout marker is set to {@link #PAGE_LAYOUT_5X9}.
     */
    public TabSettings withLocksRebased(IntUnaryOperator remap) {
        List<LockEntry> list = new ArrayList<>(locks.size());
        for (LockEntry l : locks) {
            list.add(new LockEntry(remap.applyAsInt(l.slot()), l.item()));
        }
        return new TabSettings(gatherHub, autoRefill, combat, pickaxe2Blocks, List.copyOf(list), upgradeToggles,
                PAGE_LAYOUT_5X9);
    }

    /** Keeps only the locks inside [base, base+size), renumbered from 0. */
    public TabSettings sliceLocks(int base, int size) {
        if (base == 0 || locks.isEmpty()) {
            return this;
        }
        List<LockEntry> sliced = new ArrayList<>();
        for (LockEntry l : locks) {
            if (l.slot() >= base && l.slot() < base + size) {
                sliced.add(new LockEntry(l.slot() - base, l.item()));
            }
        }
        return new TabSettings(gatherHub, autoRefill, combat, pickaxe2Blocks, List.copyOf(sliced), upgradeToggles,
                pageLayout);
    }

    public Optional<Item> lockFor(int slot) {
        for (LockEntry l : locks) {
            if (l.slot() == slot) {
                return BuiltInRegistries.ITEM.getOptional(l.item());
            }
        }
        return Optional.empty();
    }

    /** True if the stack may sit in the given storage slot under the lock rules. */
    public boolean allows(int slot, ItemStack stack) {
        return lockFor(slot).map(stack::is).orElse(true);
    }

    public boolean upgradeEnabled(String id) {
        return upgradeToggles.getOrDefault(id, true);
    }
}
