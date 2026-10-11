package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.client.screen.BetterInventoryScreen;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.4.1 Polymorph client compat: adds the recipe choice button beside the
 * crafting result via Polymorph's widget factory (all reflective).
 */
public final class PolymorphClientCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("betterinventory-polymorph");
    private static final int BUTTON_DX = 22;
    private static final int POLYMORPH_RAISES = 22;

    private PolymorphClientCompat() {}

    private static Slot besideTheResult(Slot result) {
        return new Slot(result.container, result.getContainerSlot(), result.x + BUTTON_DX, result.y + POLYMORPH_RAISES);
    }

    public static void register() {
        if (FabricLoader.getInstance().isModLoaded("polymorph")) {
            try {
                Class<?> widgets = Class.forName("com.illusivesoulworks.polymorph.api.client.PolymorphWidgets");
                Class<?> factory = Class.forName("com.illusivesoulworks.polymorph.api.client.PolymorphWidgets$IRecipesWidgetFactory");
                Class<?> playerWidget = Class.forName("com.illusivesoulworks.polymorph.api.client.widgets.PlayerRecipesWidget");
                Object registry = widgets.getMethod("getInstance").invoke(null);
                Method register = widgets.getMethod("registerWidget", factory);
                Constructor<?> create = playerWidget.getConstructor(AbstractContainerScreen.class, Slot.class);
                Object onScreen = Proxy.newProxyInstance(factory.getClassLoader(), new Class[]{factory}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "createWidget" -> {
                            if (args != null && args.length == 1 && args[0] instanceof BetterInventoryScreen screen) {
                                return create.newInstance(screen, besideTheResult(screen.getMenu().getSlot(114)));
                            }
                            return null;
                        }
                        case "hashCode" -> {
                            return System.identityHashCode(proxy);
                        }
                        case "equals" -> {
                            return args != null && args.length == 1 && proxy == args[0];
                        }
                        case "toString" -> {
                            return "BetterInventory Polymorph widget";
                        }
                        default -> {
                            return null;
                        }
                    }
                });
                register.invoke(registry, onScreen);
                LOGGER.info("Polymorph detected - recipe choice button added to the BetterInventory crafting grid");
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.warn("Polymorph detected but its client API did not match; no recipe choice button", e);
            }
        }
    }
}
