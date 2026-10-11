package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.logic.CombatService;
import dev.fallingcloud.betterinventory.logic.ToolService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 client HUD state: offhand selector animation, active tool display and
 * the Better Combat engagement flash.
 */
public final class ClientHudState {
    public static boolean selectorOpen;
    public static int pendingIndex;
    public static float progress;
    public static float progressO;
    @Nullable
    public static ItemStack activeTool;
    public static boolean toolDisplayWindow;
    public static int weaponFlashTicks;
    public static int lingerTicks;
    public static float toolShow;
    public static float toolShowO;
    private static int lastSelected = -1;

    private ClientHudState() {}

    public static void tick(Minecraft minecraft) {
        progressO = progress;
        progress = Mth.clamp(progress + (selectorOpen ? 0.25F : -0.25F), 0.0F, 1.0F);
        toolShowO = toolShow;
        LocalPlayer player = minecraft.player;
        if (player != null && minecraft.level != null) {
            int selected = player.getInventory().getSelectedSlot();
            boolean usingHeld = minecraft.options.keyUse.isDown();
            if (selected != lastSelected || usingHeld) {
                lastSelected = selected;
                activeTool = null;
                lingerTicks = 0;
                weaponFlashTicks = 0;
                ClientCombatBridge.disengage();
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
                current = ToolService.forBlock(player, state, hit.getBlockPos())
                        .map(ToolService.ToolChoice::stack).orElse(null);
            }
            if (current == null && weaponFlashTicks > 0) {
                ItemStack weapon = CombatService.resolve(player);
                if (!weapon.isEmpty()) {
                    current = weapon;
                }
            }
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
            toolShow = Mth.clamp(toolShow + (activeTool != null ? 0.2F : -0.2F), 0.0F, 1.0F);
        } else {
            activeTool = null;
            lingerTicks = 0;
            lastSelected = -1;
            toolShow = 0.0F;
            toolShowO = 0.0F;
        }
    }

    public static void flashTool(ItemStack tool) {
        activeTool = tool;
        lingerTicks = Math.max(1, BetterInventoryConfig.TOOL_LINGER_TICKS.get());
    }

    public static float lerpProgress(float partialTick) {
        return Mth.lerp(partialTick, progressO, progress);
    }

    public static float lerpToolShow(float partialTick) {
        return Mth.lerp(partialTick, toolShowO, toolShow);
    }

    public static PlayerLoadout loadout(Minecraft minecraft) {
        return PlayerLoadout.of(minecraft.player);
    }
}
