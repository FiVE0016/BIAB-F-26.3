package dev.fallingcloud.betterinventory.shim;

/** Stand-in for NeoForge's TriState. */
public enum TriState {
    TRUE,
    FALSE,
    DEFAULT;

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public static TriState fromBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }
}
