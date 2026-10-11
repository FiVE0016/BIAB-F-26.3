package dev.fallingcloud.betterinventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.fabricmc.loader.api.FabricLoader;

public final class BetterInventoryConfig {
    public static final int[] VANILLA_TIERS = new int[]{96, 128, 192, 256, 384, 512, 1024};
    public static final int[] HUNDRED_TIERS = new int[]{200, 300, 400, 500, 750, 999, 999};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Spec> SPECS = new ArrayList<>();
    public static final Spec STARTUP_SPEC = new Spec("startup");
    public static final Value<Boolean> BACKPACK_PLACEABLE = STARTUP_SPEC.define(
            "general",
            "backpackPlaceable",
            true,
            "Allow placing backpacks as blocks (sneak + right click on a block).",
            "Turn off if backpacks should only work equipped in the inventory."
    );
    public static final Value<Boolean> BACKPACK_OPENABLE = STARTUP_SPEC.define(
            "general",
            "backpackOpenable",
            true,
            "Allow opening a held or placed backpack with right click (the standalone backpack",
            "screen). Turn off if backpacks should only be usable through the inventory tabs."
    );
    public static final Value<StackMode> STACK_MODE = STARTUP_SPEC.defineEnum(
            "stacks",
            "stackMode",
            StackMode.VANILLA,
            "VANILLA: normal stacks stay 64 and stack upgrades grant 96/128/192/256/384/512/1024.",
            "HUNDRED: normal 64-stack items stack to 100 and stack upgrades grant 200/300/400/500/750/999.",
            "Stack upgrades only apply inside BetterInventory storage (the gather hub and backpack tabs)."
    );
    public static final Value<Integer> MAGNET_RADIUS = STARTUP_SPEC.defineInRange(
            "upgrades",
            "magnetRadius",
            12,
            2,
            24,
            "Radius in blocks of the LEVEL V Magnet upgrade. Lower levels scale down from this",
            "(level I pulls from about a third of it)."
    );
    public static final Value<Double> MAGNET_PULL_STRENGTH = STARTUP_SPEC.defineInRange(
            "upgrades", "magnetPullStrength", 1.0, 0.25, 3.0,
            "Multiplier for how strongly the Magnet upgrade pulls items toward you."
    );
    public static final Value<Integer> MAGNET_INTERVAL = STARTUP_SPEC.defineInRange(
            "upgrades", "magnetInterval", 2, 1, 20,
            "How often (in ticks) the Magnet upgrade pulls nearby items. Lower is smoother and stronger."
    );
    public static final Value<Integer> FEEDING_THRESHOLD = STARTUP_SPEC.defineInRange(
            "upgrades",
            "feedingThreshold",
            14,
            1,
            19,
            "The LEVEL V Feeding upgrade eats when your hunger is at or below this many points",
            "(out of 20). Each lower level triggers 3 points later."
    );
    public static final Spec CLIENT_SPEC = new Spec("client");
    public static final Value<Boolean> USE_VANILLA_INVENTORY = CLIENT_SPEC.define(
            "inventory",
            "useVanillaInventory",
            false,
            "Open Minecraft's own inventory screen instead of this mod's one.",
            "The small button under the player view (same spot on either screen) flips this,",
            "so there is rarely a reason to edit it here."
    );
    public static final Value<Boolean> STRONG_LOCKED_SLOTS = CLIENT_SPEC.define(
            "inventory",
            "strongLockedSlots",
            true,
            "Mark crafting cells your current crafting upgrade has not unlocked with an x,",
            "so it is obvious they are disabled. Turn off for a plain faint dim instead."
    );
    public static final Value<Boolean> SHOW_EFFECTS = CLIENT_SPEC.define(
            "inventory",
            "showEffects",
            true,
            "Show your active effects as a row of icons above the player view, with the name and",
            "the time left in each one's tooltip. Turn off to not show them on this screen."
    );
    public static final Value<Boolean> OFFHAND_KEY_MIGRATED = CLIENT_SPEC.define(
            "inventory",
            "offhandKeyMigrated",
            false,
            "Bookkeeping, not a preference. Slate Building holds Left Alt for its block wheel, so",
            "with it installed the offhand carousel defaults to Right Alt, and a carousel key still",
            "on Left Alt from before is moved to Right Alt once. This records that the one-time",
            "move has happened, so a key you set back to Left Alt yourself stays there."
    );
    public static final Value<Boolean> ZOOM_ENABLED = CLIENT_SPEC.define(
            "zoom",
            "enabled",
            true,
            "Show the hovered item enlarged in a frame to the right of the inventory.",
            "It only appears when the window is wide enough for it - and for a recipe viewer's",
            "item list beside it, if one is installed. Turn off to never show it."
    );
    public static final Value<Double> ZOOM_SCALE = CLIENT_SPEC.defineInRange(
            "zoom", "scale", 5.0, 2.0, 8.0, "How large the item in that frame is drawn."
    );
    public static final Value<Boolean> SHOW_TOOL_IN_HAND = CLIENT_SPEC.define(
            "hud", "showToolInHand", true,
            "Show the auto-selected tool in your first-person hand while it is being used."
    );
    public static final Value<Integer> TOOL_LINGER_TICKS = CLIENT_SPEC.defineInRange(
            "hud",
            "toolLingerTicks",
            20,
            0,
            200,
            "How long (in ticks, 20 = 1 second) the auto-used tool keeps showing in the HUD slot",
            "after you stop using it, before the slot falls back to your held item."
    );
    public static final Value<Boolean> SHOW_WORN_BACKPACK = CLIENT_SPEC.define(
            "backpack",
            "showWornBackpack",
            true,
            "Draw your highest equipped backpack on your back, in third person and on other players.",
            "Turn off if another mod already draws a pack there, or you would rather not see one."
    );

    /**
     * 26.3 Fabric adaptation kept from the previous port iteration: which screen the E
     * key opens. {@link dev.fallingcloud.betterinventory.mixin.GuiSetScreenMixin} reads it
     * on the client. AUTO keeps creative vanilla (its swap happens in init()), MOD/VANILLA
     * are the two states the in-game toggle flips between.
     */
    public enum ScreenMode { AUTO, MOD, VANILLA }

    public static final Value<ScreenMode> SCREEN_MODE = CLIENT_SPEC.defineEnum(
            "inventory",
            "screenMode",
            ScreenMode.AUTO,
            "Which inventory screen the E key opens: AUTO (creative vanilla, survival mod),",
            "MOD (always the mod screen) or VANILLA (always Minecraft's own screen)."
    );

    public static final Spec COMMON_SPEC = new Spec("common");
    public static final Value<Boolean> WORN_TOOL_FLOOR = COMMON_SPEC.define(
            "gameplay",
            "wornToolFloor",
            true,
            "Stop wear from making a tool worse than your bare hand.",
            "Durability mods subtract a flat amount of mining speed once a tool passes a wear",
            "threshold. That amount is the same for every tier, so it barely touches iron but",
            "halves wood - a half-worn wooden axe ends up slower than punching the block. With",
            "this on, a worn (not broken) tool never drops below the midpoint between its speed at",
            "full durability and bare-hand speed. Wear still costs up to half the tool's advantage.",
            "Broken tools are not affected - that penalty is left as the durability mod intends."
    );
    public static final Value<Boolean> REQUIRE_BACKPACK = COMMON_SPEC.define(
            "tabs",
            "requireBackpack",
            true,
            "Tabs need a backpack: the page tabs come with backpack levels 2 to 5, and the 5x9 extra",
            "area and the three ability-upgrade slots with level 1.",
            "Turn this off to have all of it without any backpack: every tab, the extra area and the",
            "upgrade slots are unlocked for everyone from the start, and what they hold is kept on the",
            "player instead of inside a backpack item. Backpacks still work as placeable storage.",
            "Changing this moves each player's stored items across the next time they are online -",
            "out of the backpack they have equipped when it is turned off, back into one when it is",
            "turned on again. Nothing is deleted either way."
    );
    public static final Value<Boolean> SWAP_MAIN_INVENTORY = COMMON_SPEC.define(
            "tabs",
            "swapMainInventory",
            true,
            "Switching tabs swaps the 27 main inventory slots along with the 5x9 extra area, so every",
            "tab is a whole second inventory.",
            "Turn this off to keep the main inventory the same on every tab: tabs then only page the",
            "extra area. Whatever a tab was still holding in its own main slots moves into that tab's",
            "extra area, or the default tab's, or the inventory. Nothing is dropped: what has no",
            "room yet is kept, and follows as soon as there is some."
    );

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("betterinventory.json");
    }

    public static void load() {
        Path path = path();
        JsonObject root = null;
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                root = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException | RuntimeException e) {
                BetterInventory.LOGGER.warn("could not read {}, using defaults", path, e);
            }
        }

        for (Spec spec : SPECS) {
            JsonObject section = root != null && root.has(spec.section) && root.get(spec.section).isJsonObject()
                    ? root.getAsJsonObject(spec.section)
                    : new JsonObject();

            for (Value<?> value : spec.values) {
                value.read(section);
            }

            spec.loaded = true;
        }

        write(path);
    }

    /**
     * Kept for call-site compatibility with the previous port iteration: the client
     * entrypoint used to read a separate betterinventory-client.json. The 1.4.1 system
     * stores every spec (including the client one) in betterinventory.json via load().
     */
    public static void readClient() {
        // All specs, client included, are read by load().
    }

    /** Persists the client spec (used by the in-game screen-mode toggle). */
    public static void writeClient() {
        CLIENT_SPEC.save();
    }

    private static synchronized void write(Path path) {
        JsonObject root = new JsonObject();

        for (Spec spec : SPECS) {
            Map<String, JsonObject> groups = new LinkedHashMap<>();

            for (Value<?> value : spec.values) {
                JsonObject group = groups.computeIfAbsent(value.group, g -> new JsonObject());
                group.addProperty("_comment_" + value.name, value.comment);
                group.add(value.name, value.json());
            }

            JsonObject section = new JsonObject();
            groups.forEach(section::add);
            root.add(spec.section, section);
        }

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException e) {
            BetterInventory.LOGGER.warn("could not write {}", path, e);
        }
    }

    public static boolean tabsNeedBackpack() {
        return !COMMON_SPEC.isLoaded() || REQUIRE_BACKPACK.get();
    }

    public static boolean tabsSwapMain() {
        return !COMMON_SPEC.isLoaded() || SWAP_MAIN_INVENTORY.get();
    }

    public static boolean wornToolFloorEnabled() {
        return !COMMON_SPEC.isLoaded() || WORN_TOOL_FLOOR.get();
    }

    public static int[] tierValues() {
        return STACK_MODE.get() == StackMode.HUNDRED ? HUNDRED_TIERS : VANILLA_TIERS;
    }

    public static int tierValue(int tier) {
        int[] values = tierValues();
        return values[Math.max(0, Math.min(values.length - 1, tier))];
    }

    private BetterInventoryConfig() {}

    public static final class Spec {
        private final String section;
        private final List<Value<?>> values = new ArrayList<>();
        private boolean loaded;

        private Spec(String section) {
            this.section = section;
            SPECS.add(this);
        }

        public boolean isLoaded() {
            return this.loaded;
        }

        public void save() {
            write(path());
        }

        private <T> Value<T> define(
                String group, String name, T def, Function<JsonElement, T> reader, UnaryOperator<T> clamp, String... comment) {
            Value<T> value = new Value<>(group, name, def, reader, clamp, String.join(" ", comment));
            this.values.add(value);
            return value;
        }

        private Value<Boolean> define(String group, String name, boolean def, String... comment) {
            return this.define(group, name, def, JsonElement::getAsBoolean, UnaryOperator.identity(), comment);
        }

        private Value<Integer> defineInRange(String group, String name, int def, int min, int max, String... comment) {
            return this.define(group, name, def, JsonElement::getAsInt, v -> Math.max(min, Math.min(max, v)), comment);
        }

        private Value<Double> defineInRange(String group, String name, double def, double min, double max, String... comment) {
            return this.define(group, name, def, JsonElement::getAsDouble, v -> Math.max(min, Math.min(max, v)), comment);
        }

        private <E extends Enum<E>> Value<E> defineEnum(String group, String name, E def, String... comment) {
            return this.define(group, name, def, json -> Enum.valueOf(def.getDeclaringClass(), json.getAsString()),
                    UnaryOperator.identity(), comment);
        }
    }

    public enum StackMode {
        VANILLA,
        HUNDRED;
    }

    public static final class Value<T> implements Supplier<T> {
        private final String group;
        private final String name;
        private final T def;
        private final Function<JsonElement, T> reader;
        private final UnaryOperator<T> clamp;
        private final String comment;
        private T value;

        private Value(String group, String name, T def, Function<JsonElement, T> reader, UnaryOperator<T> clamp, String comment) {
            this.group = group;
            this.name = name;
            this.def = def;
            this.reader = reader;
            this.clamp = clamp;
            this.comment = comment;
            this.value = def;
        }

        @Override
        public T get() {
            return this.value;
        }

        public void set(T v) {
            this.value = v;
        }

        private void read(JsonObject section) {
            JsonObject object = section.has(this.group) && section.get(this.group).isJsonObject()
                    ? section.getAsJsonObject(this.group) : null;
            if (object != null && object.has(this.name)) {
                try {
                    this.value = this.clamp.apply(this.reader.apply(object.get(this.name)));
                } catch (RuntimeException e) {
                    this.value = this.def;
                }
            } else {
                this.value = this.def;
            }
        }

        private JsonElement json() {
            if (this.value instanceof Boolean b) {
                return new JsonPrimitive(b);
            } else if (this.value instanceof Number n) {
                return new JsonPrimitive(n);
            } else {
                return this.value instanceof Enum<?> e
                        ? new JsonPrimitive(e.name())
                        : new JsonPrimitive(String.valueOf(this.value));
            }
        }
    }
}
