package dev.fallingcloud.betterinventory.block;

import dev.fallingcloud.betterinventory.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BackpackBlockEntity extends BlockEntity {
    private ItemStack stored = ItemStack.EMPTY;

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BACKPACK_BE.get(), pos, state);
    }

    public ItemStack stored() {
        return stored;
    }

    public void setStored(ItemStack stack) {
        this.stored = stack;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!stored.isEmpty()) {
            output.store("stored", ItemStack.CODEC, stored);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stored = input.read("stored", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
}
