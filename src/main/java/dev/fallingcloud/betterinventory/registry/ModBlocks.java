package dev.fallingcloud.betterinventory.registry;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.block.BackpackBlock;
import dev.fallingcloud.betterinventory.block.BackpackBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import dev.fallingcloud.betterinventory.shim.DeferredBlock;
import dev.fallingcloud.betterinventory.shim.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BetterInventory.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BetterInventory.MODID);

    public static final DeferredBlock<BackpackBlock> BACKPACK = BLOCKS.register("backpack",
            () -> new BackpackBlock(BlockBehaviour.Properties.of()
                    .strength(0.8f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final Supplier<BlockEntityType<BackpackBlockEntity>> BACKPACK_BE = BLOCK_ENTITIES.register(
            "backpack",
            () -> new BlockEntityType<>(BackpackBlockEntity::new, java.util.Set.of(BACKPACK.get())));

    private ModBlocks() {}
}
