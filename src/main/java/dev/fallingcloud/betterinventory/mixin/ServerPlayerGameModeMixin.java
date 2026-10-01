package dev.fallingcloud.betterinventory.mixin;

import dev.fallingcloud.betterinventory.logic.ToolService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes block destruction consume the auto-selected rack tool: the single
 * getMainHandItem() inside destroyBlock feeds durability (mineBlock) and the
 * drop context (playerDestroy), so redirecting it routes both to the rack tool.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Redirect(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack betterinventory$useRackTool(ServerPlayer player, BlockPos pos) {
        return ToolService.toolForDestroy(player, pos, player.getMainHandItem());
    }
}
