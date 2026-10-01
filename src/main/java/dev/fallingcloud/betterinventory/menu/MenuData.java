package dev.fallingcloud.betterinventory.menu;

import java.util.function.Supplier;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/**
 * Bridges vanilla {@link MenuProvider} onto Fabric's {@link ExtendedMenuProvider}, which
 * is how 26.3 sends screen-opening data for an
 * {@link net.fabricmc.fabric.api.menu.v1.ExtendedMenuType}.
 */
public final class MenuData {
    private MenuData() {}

    public static <D> MenuProvider wrap(MenuProvider provider, Supplier<D> data) {
        return new ExtendedMenuProvider<D>() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int containerId, net.minecraft.world.entity.player.Inventory inventory,
                    net.minecraft.world.entity.player.Player player) {
                return provider.createMenu(containerId, inventory, player);
            }

            @Override
            public D getScreenOpeningData(ServerPlayer player) {
                return data.get();
            }
        };
    }
}
