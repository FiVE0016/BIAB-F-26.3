package dev.fallingcloud.betterinventory.logic;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** 1.4.1: registers the /betterinventory fill <stage> [preset] [style] command. */
public final class ModCommands {
    private ModCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(LoadoutFiller.build()));
    }
}
