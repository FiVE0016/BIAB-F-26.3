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

    /**
     * 26.3: the block's registry id has to be on the properties before the block is
     * constructed, so registration goes through registerWithId instead of a plain
     * supplier.
     */
    public static final DeferredBlock<BackpackBlock> BACKPACK = BLOCKS.registerWithId("backpack",
            id -> new BackpackBlock(BlockBehaviour.Properties.of()
                    .setId(id)
                    .strength(0.8f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final Supplier<BlockEntityType<BackpackBlockEntity>> BACKPACK_BE = BLOCK_ENTITIES.register(
            "backpack",
            () -> new BlockEntityType<>(BackpackBlockEntity::new, java.util.Set.of(BACKPACK.get())));

    private ModBlocks() {}
}
