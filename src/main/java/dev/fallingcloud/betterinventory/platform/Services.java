package dev.fallingcloud.betterinventory.platform;

import java.util.ServiceLoader;

/**
 * Locates the loader's {@link Platform} implementation.
 *
 * <p>ServiceLoader rather than a static setter so common code can be used from a
 * loader entry point that runs before any of our own initialisation, and so a
 * missing implementation fails loudly at class-load instead of NPE-ing later.
 */
public final class Services {
    private static Platform platform;

    private Services() {}

    public static Platform platform() {
        Platform local = platform;
        if (local == null) {
            local = ServiceLoader.load(Platform.class)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No BetterInventory Platform implementation found on the classpath. "
                                    + "Each loader module must register one in META-INF/services."));
            platform = local;
        }
        return local;
    }
}
