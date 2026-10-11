package dev.fallingcloud.betterinventory.logic;

import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Chooses which rack tool (if any) should virtually replace the held item for a
 * given block, and reimplements the vanilla dig-speed formula for that tool.
 */
public final class ToolService {
    /** 1.4.1: a rack tool must be at least this much faster than the hand to engage. */
    private static final float MIN_SPEED_GAIN = 2.0F;

    private ToolService() {}

    public record ToolChoice(int slot, ItemStack stack) {}

    /**
     * The rack tool to use for this block, or empty when the held item is already
     * the best option (or nothing in the rack applies). 1.4.1 also engages the
     * rack when it unlocks drops or is twice as fast, even without "correct tool".
     */
    public static Optional<ToolChoice> forBlock(Player player, BlockState state) {
        return forBlock(player, state, null);
    }

    public static Optional<ToolChoice> forBlock(Player player, BlockState state, @Nullable BlockPos pos) {
        if (player.isSpectator() || player.isCreative()) {
            return Optional.empty();
        }
        PlayerLoadout loadout = PlayerLoadout.existing(player);
        if (loadout == null) {
            return Optional.empty();
        }
        if (state.getDestroySpeed(player.level(), pos == null ? BlockPos.ZERO : pos) <= 0.0f) {
            return Optional.empty();
        }

        int bestSlot = -1;
        ItemStack bestStack = ItemStack.EMPTY;
        boolean bestCorrect = false;
        float bestSpeed = digSpeedWith(player, ItemStack.EMPTY, state);

        for (int slot : candidateSlots(player, loadout, state)) {
            ItemStack stack = loadout.tools.getStackInSlot(slot);
            if (stack.isEmpty() || BrokenTools.isUnusable(stack)) {
                continue;
            }
            boolean correct = stack.isCorrectToolForDrops(state);
            float speed = digSpeedWith(player, stack, state);
            if (isBetter(correct, speed, bestCorrect, bestSpeed)) {
                bestSlot = slot;
                bestStack = stack;
                bestCorrect = correct;
                bestSpeed = speed;
            }
        }
        if (bestSlot < 0) {
            return Optional.empty();
        }

        ItemStack main = player.getMainHandItem();
        boolean mainCorrect = main.isCorrectToolForDrops(state);
        float mainSpeed = digSpeedWith(player, main, state);
        boolean unlocksDrops = state.requiresCorrectToolForDrops() && bestCorrect && !mainCorrect;
        boolean rightToolAndFaster = bestCorrect && bestSpeed > mainSpeed;
        boolean meaningfullyFaster = bestSpeed >= mainSpeed * MIN_SPEED_GAIN;
        if (!unlocksDrops && !rightToolAndFaster && !meaningfullyFaster) {
            return Optional.empty();
        }
        return Optional.of(new ToolChoice(bestSlot, bestStack));
    }

    /** Candidate rack slots, resolving which pickaxe the active tab prefers for this block. */
    private static int[] candidateSlots(Player player, PlayerLoadout loadout, BlockState state) {
        int pick = PlayerLoadout.TOOL_PICKAXE_1;
        TabSettings settings = BetterInventorySettings.raw(player, loadout.activeTab);
        if (!settings.pickaxe2Blocks().isEmpty()) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (settings.pickaxe2Blocks().contains(id) && !loadout.tools.getStackInSlot(PlayerLoadout.TOOL_PICKAXE_2).isEmpty()) {
                pick = PlayerLoadout.TOOL_PICKAXE_2;
            }
        }
        if (loadout.tools.getStackInSlot(pick).isEmpty()) {
            pick = pick == PlayerLoadout.TOOL_PICKAXE_1 ? PlayerLoadout.TOOL_PICKAXE_2 : PlayerLoadout.TOOL_PICKAXE_1;
        }
        return new int[]{
                PlayerLoadout.TOOL_HOE,
                PlayerLoadout.TOOL_SHOVEL,
                pick,
                PlayerLoadout.TOOL_AXE,
                PlayerLoadout.TOOL_SWORD
        };
    }

    private static boolean isBetter(boolean correct, float speed, boolean thanCorrect, float thanSpeed) {
        if (correct != thanCorrect) {
            return correct;
        }
        return speed > thanSpeed;
    }

    /**
     * Vanilla {@code Player.getDigSpeed} recomputed for a virtual tool. Attribute-based
     * pieces (Efficiency's mining_efficiency) are evaluated with the hand contributions
     * swapped for the tool's.
     */
    public static float digSpeedWith(Player player, ItemStack tool, BlockState state) {
        float f = tool.getDestroySpeed(state);
        if (f > 1.0f) {
            f += miningEfficiency(player, tool);
        }
        if (MobEffectUtil.hasDigSpeed(player)) {
            f *= 1.0f + (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2f;
        }
        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            f *= switch (player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            };
        }
        f *= (float) player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (player.isEyeInFluid(FluidTags.WATER)) {
            f *= (float) player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        }
        if (!player.onGround()) {
            f /= 5.0f;
        }
        return f;
    }

    /**
     * The player's mining_efficiency with the held item's contribution swapped for
     * the rack tool's (this is where Efficiency lands in 1.21).
     *
     * <p>Computed from the stacks' own modifiers rather than by temporarily editing
     * the live AttributeMap: this runs on every BreakSpeed tick, on both sides, and
     * mutating attributes there marks them dirty every tick, which makes the server
     * re-broadcast the player's attributes continuously while mining.
     */
    private static float miningEfficiency(Player player, ItemStack tool) {
        double base = player.getAttributeValue(Attributes.MINING_EFFICIENCY);
        return (float) (base - addedEfficiency(player.getMainHandItem()) + addedEfficiency(tool));
    }

    private static double addedEfficiency(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        double[] sum = {0.0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.MINING_EFFICIENCY.value()
                    && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                sum[0] += modifier.amount();
            }
        });
        return sum[0];
    }

    /** Resolves the choice for the block at the given position (server destroy path). */
    public static ItemStack toolForDestroy(Player player, BlockPos pos, ItemStack fallback) {
        BlockState state = player.level().getBlockState(pos);
        return forBlock(player, state, pos).map(ToolChoice::stack).orElse(fallback);
    }
}
