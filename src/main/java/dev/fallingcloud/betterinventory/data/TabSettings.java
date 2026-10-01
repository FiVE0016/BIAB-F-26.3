package dev.fallingcloud.betterinventory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 */
public record TabSettings(
        boolean gatherHub,
        boolean autoRefill,
        CombatPref combat,
        List<Identifier> pickaxe2Blocks,
        List<LockEntry> locks,
        Map<String, Boolean> upgradeToggles) {

    public static final TabSettings DEFAULT = new TabSettings(true, true, CombatPref.SWORD, List.of(), List.of(), Map.of());

    public enum CombatPref implements StringRepresentable {
        SWORD, AXE;

        public static final Codec<CombatPref> CODEC = StringRepresentable.fromEnum(CombatPref::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A slot lock: only the given item may occupy the slot (0-26 main, 27-44 gather). */
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
            Codec.unboundedMap(Codec.STRING, Codec.BOOL).optionalFieldOf("upgrade_toggles", Map.of()).forGetter(TabSettings::upgradeToggles)
    ).apply(i, TabSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TabSettings::gatherHub,
            ByteBufCodecs.BOOL, TabSettings::autoRefill,
            ByteBufCodecs.idMapper(idx -> CombatPref.values()[idx], CombatPref::ordinal), TabSettings::combat,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), TabSettings::pickaxe2Blocks,
            LockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TabSettings::locks,
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.BOOL), TabSettings::upgradeToggles,
            TabSettings::new);

    public TabSettings withGatherHub(boolean value) {
        return new TabSettings(value, autoRefill, combat, pickaxe2Blocks, locks, upgradeToggles);
    }

    public TabSettings withAutoRefill(boolean value) {
        return new TabSettings(gatherHub, value, combat, pickaxe2Blocks, locks, upgradeToggles);
    }

    public TabSettings withCombat(CombatPref value) {
        return new TabSettings(gatherHub, autoRefill, value, pickaxe2Blocks, locks, upgradeToggles);
    }

    public TabSettings withPickaxe2Blocks(List<Identifier> value) {
        return new TabSettings(gatherHub, autoRefill, combat, List.copyOf(value), locks, upgradeToggles);
    }

    public TabSettings withUpgradeToggle(String id, boolean value) {
        Map<String, Boolean> map = new LinkedHashMap<>(upgradeToggles);
        map.put(id, value);
        return new TabSettings(gatherHub, autoRefill, combat, pickaxe2Blocks, locks, Map.copyOf(map));
    }

    public TabSettings withLock(int slot, Optional<Identifier> item) {
        List<LockEntry> list = new ArrayList<>(locks);
        list.removeIf(l -> l.slot() == slot);
        item.ifPresent(rl -> list.add(new LockEntry(slot, rl)));
        return new TabSettings(gatherHub, autoRefill, combat, pickaxe2Blocks, List.copyOf(list), upgradeToggles);
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
