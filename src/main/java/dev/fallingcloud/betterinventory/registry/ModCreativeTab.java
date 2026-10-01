package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.DeferredRegister;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BetterInventory.MODID);

    public static final Supplier<CreativeModeTab> TAB = TABS.register("loadout", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.betterinventory"))
            .icon(() -> new ItemStack(ModItems.BACKPACKS[4].get()))
            .displayItems((params, output) -> ModItems.all().forEach(output::accept))
            .build());

    private ModCreativeTab() {}
}
