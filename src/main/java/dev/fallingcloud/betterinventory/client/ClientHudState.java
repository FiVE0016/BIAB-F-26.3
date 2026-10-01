package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.logic.CombatService;
import dev.fallingcloud.betterinventory.logic.ToolService;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Client-side state for the HUD tool slot and the offhand selector animation. */
public final class ClientHudState {
    private ClientHudState() {}

    // Offhand selector
    public static boolean selectorOpen;
    public static int pendingIndex;
    public static float progress;
    public static float progressO;

    // Active tool display
    @Nullable
    public static ItemStack activeTool;
    public static int weaponFlashTicks;
    /** Remaining grace ticks before the HUD slot drops back to the held item. */
    public static int lingerTicks;
    /** Slide/fade of the tool slot, 0 = hidden, 1 = fully out. */
    public static float toolShow;
    public static float toolShowO;

    public static void tick(Minecraft minecraft) {
        progressO = progress;
        progress = Mth.clamp(progress + (selectorOpen ? 0.25f : -0.25f), 0.0f, 1.0f);
        toolShowO = toolShow;

        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            activeTool = null;
            lingerTicks = 0;
            toolShow = 0.0f;
            toolShowO = 0.0f;
            return;
        }
        if (weaponFlashTicks > 0) {
            weaponFlashTicks--;
        }

        ItemStack current = null;
        boolean mining = minecraft.gameMode != null && minecraft.gameMode.isDestroying();
        boolean swinging = minecraft.options.keyAttack.isDown();
        if ((mining || swinging) && minecraft.hitResult instanceof BlockHitResult hit
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
            current = ToolService.forBlock(player, state).map(ToolService.ToolChoice::stack).orElse(null);
        }
        if (current == null && weaponFlashTicks > 0) {
            ItemStack weapon = CombatService.resolve(player);
            if (!weapon.isEmpty()) {
                current = weapon;
            }
        }

        // Hold the last tool on screen for a configurable grace period so the slot
        // does not flicker between blocks or blink out the instant you stop mining.
        if (current != null) {
            activeTool = current;
            lingerTicks = BetterInventoryConfig.TOOL_LINGER_TICKS.get();
        } else if (lingerTicks > 0) {
            lingerTicks--;
            if (lingerTicks == 0) {
                activeTool = null;
            }
        } else {
            activeTool = null;
        }

        // The slot only exists for auto-selected tools, so it slides away whenever
        // one is not engaged rather than falling back to the held item.
        toolShow = Mth.clamp(toolShow + (activeTool != null ? 0.2f : -0.2f), 0.0f, 1.0f);
    }

    public static float lerpProgress(float partialTick) {
        return Mth.lerp(partialTick, progressO, progress);
    }

    public static float lerpToolShow(float partialTick) {
        return Mth.lerp(partialTick, toolShowO, toolShow);
    }

    public static PlayerLoadout loadout(Minecraft minecraft) {
        return ModAttachments.get(minecraft.player);
    }
}
