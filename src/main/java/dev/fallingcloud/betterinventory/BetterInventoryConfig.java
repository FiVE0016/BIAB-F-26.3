package dev.fallingcloud.betterinventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric replacement for NeoForge's ModConfigSpec.
 *
 * <p>Fabric ships no config system, so this reads and writes plain JSON. The
 * {@code .get()} call style of ModConfigSpec is kept so every existing call site
 * compiles unchanged.
 */
public final class BetterInventoryConfig {
    public enum StackMode { VANILLA, HUNDRED }

    /** Stack upgrade tier values, vanilla mode. */
    public static final int[] VANILLA_TIERS = {96, 128, 192, 256, 384, 512, 1024};
    /** Stack upgrade tier values, hundred mode (normal stacks become 100). */
    public static final int[] HUNDRED_TIERS = {200, 300, 400, 500, 750, 999, 999};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ------------------------------------------------------------------ common
    public static final EnumValue<StackMode> STACK_MODE = new EnumValue<>(StackMode.VANILLA);
    public static final IntValue MAGNET_RADIUS = new IntValue(6, 2, 16);
    public static final IntValue FEEDING_THRESHOLD = new IntValue(14, 1, 19);

    // ------------------------------------------------------------------ client
    public static final BooleanValue ZOOM_ENABLED = new BooleanValue(true);
    public static final BooleanValue ZOOM_ALL_SCREENS = new BooleanValue(true);
    public static final DoubleValue ZOOM_SCALE = new DoubleValue(5.0, 2.0, 8.0);
    public static final BooleanValue SHOW_TOOL_IN_HAND = new BooleanValue(true);
    public static final IntValue TOOL_LINGER_TICKS = new IntValue(20, 0, 200);

    /** Small holders mimicking ModConfigSpec's config values. */
    public static class Value<T> {
        private T value;

        protected Value(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }

        protected void read(JsonObject root, String key, Class<T> type) {
            if (root.has(key)) {
                value = new Gson().fromJson(root.get(key), type);
            }
        }

        protected void write(JsonObject root, String key) {
            root.add(key, GSON.toJsonTree(value));
        }
    }

    public static final class BooleanValue extends Value<Boolean> {
        public BooleanValue(boolean def) {
            super(def);
        }
    }

    public static final class IntValue extends Value<Integer> {
        private final int min;
        private final int max;

        public IntValue(int def, int min, int max) {
            super(def);
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Integer value) {
            super.set(Math.max(min, Math.min(max, value)));
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private final double min;
        private final double max;

        public DoubleValue(double def, double min, double max) {
            super(def);
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Double value) {
            super.set(Math.max(min, Math.min(max, value)));
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends Value<E> {
        public EnumValue(E def) {
            super(def);
        }
    }

    public static int[] tierValues() {
        return STACK_MODE.get() == StackMode.HUNDRED ? HUNDRED_TIERS : VANILLA_TIERS;
    }

    public static int tierValue(int tier) {
        int[] values = tierValues();
        return values[Math.max(0, Math.min(values.length - 1, tier))];
    }

    /** Which inventory screen the E key opens. */
    public enum ScreenMode {
        /** Creative stays vanilla, survival uses the mod screen. */
        AUTO,
        /** Always the mod screen. */
        MOD,
        /** Always the vanilla screen. */
        VANILLA
    }

    /**
     * Client-side: only {@code GuiSetScreenMixin} reads it, and that mixin is
     * registered under the client mixin set. AUTO is the shipped default; the
     * toggle key swaps MOD and VANILLA only, so AUTO can be restored just by
     * editing betterinventory-client.json.
     */
    public static final EnumValue<ScreenMode> SCREEN_MODE = new EnumValue<>(ScreenMode.AUTO);

    // ------------------------------------------------------------------ io

    public static void load() {
        readCommon();
        readClient();
    }

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    private static void readCommon() {
        Path file = dir().resolve("betterinventory.json");
        JsonObject root = readJson(file);
        STACK_MODE.read(root, "stackMode", StackMode.class);
        MAGNET_RADIUS.read(root, "magnetRadius", Integer.class);
        FEEDING_THRESHOLD.read(root, "feedingThreshold", Integer.class);
        writeJson(file, root);
    }

    public static void readClient() {
        Path file = dir().resolve("betterinventory-client.json");
        JsonObject root = readJson(file);
        ZOOM_ENABLED.read(root, "zoomEnabled", Boolean.class);
        ZOOM_ALL_SCREENS.read(root, "zoomAllScreens", Boolean.class);
        ZOOM_SCALE.read(root, "zoomScale", Double.class);
        SHOW_TOOL_IN_HAND.read(root, "showToolInHand", Boolean.class);
        TOOL_LINGER_TICKS.read(root, "toolLingerTicks", Integer.class);
        SCREEN_MODE.read(root, "screenMode", ScreenMode.class);
        writeJson(file, root);
    }

    /** Persists the screen mode so the toggle survives a restart. */
    public static void writeClient() {
        Path file = dir().resolve("betterinventory-client.json");
        JsonObject root = readJson(file);
        root.addProperty("screenMode", SCREEN_MODE.get().name());
        writeJson(file, root);
    }

    private static JsonObject readJson(Path file) {
        if (!Files.exists(file)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            return obj == null ? new JsonObject() : obj;
        } catch (IOException e) {
            return new JsonObject();
        }
    }

    private static void writeJson(Path file, JsonObject root) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ignored) {
            // Config writing is best-effort; defaults are already in memory.
        }
    }

    private BetterInventoryConfig() {}
}

