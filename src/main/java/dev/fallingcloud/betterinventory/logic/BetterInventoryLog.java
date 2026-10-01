package dev.fallingcloud.betterinventory.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterInventoryLog {
    public static final Logger LOG = LoggerFactory.getLogger("betterinventory");

    private BetterInventoryLog() {}

    public static void warn(String message, Throwable t) {
        LOG.warn(message, t);
    }
}
