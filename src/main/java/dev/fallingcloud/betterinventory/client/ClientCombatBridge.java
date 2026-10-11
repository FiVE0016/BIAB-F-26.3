package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.logic.CombatService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 client combat bridge: keeps Better Combat's displayed weapon honest
 * while the rack sword/axe is engaged.
 *
 * <p>26.3 adaptation: {@code Minecraft#isSameThread} is {@code isRunning()},
 * the screen is {@code minecraft.gui.screen()} and the selected item is
 * {@code Inventory#getSelectedItem()}.
 */
public final class ClientCombatBridge {
    private static boolean answering;
    @Nullable
    private static ItemStack engaged;

    private ClientCombatBridge() {}

    @Nullable
    public static ItemStack engagedWeapon() {
        return engaged;
    }

    public static void disengage() {
        engaged = null;
    }

    private static ItemStack rackWeapon(LocalPlayer player) {
        answering = true;
        try {
            return CombatService.resolve(player);
        } finally {
            answering = false;
        }
    }

    public static void tick(Minecraft minecraft) {
        if (engaged != null) {
            LocalPlayer player = minecraft.player;
            boolean mining = minecraft.gameMode != null && minecraft.gameMode.isDestroying();
            if (player != null && minecraft.gui.screen() == null && !mining && BetterCombatState.busy(minecraft)) {
                if (!BetterCombatState.windingUp(minecraft)) {
                    ItemStack weapon = rackWeapon(player);
                    if (weapon.isEmpty()) {
                        engaged = null;
                        return;
                    }
                    engaged = weapon;
                }
                ClientHudState.flashTool(engaged);
            } else {
                engaged = null;
            }
        }
    }

    public static ItemStack substituteForBetterCombat(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!answering && minecraft.isRunning()) {
            LocalPlayer player = minecraft.player;
            if (player != null && minecraft.gui.screen() == null && stack != null) {
                ItemStack selected = player.getInventory().getSelectedItem();
                ItemStack displayed = ClientHudState.activeTool;
                if (stack == selected || displayed != null && stack == displayed) {
                    boolean keyDown = minecraft.options.keyAttack.isDown();
                    boolean onBlock = minecraft.hitResult != null
                            && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
                    boolean flashing = ClientHudState.weaponFlashTicks > 0;
                    boolean attacking = keyDown && !onBlock;
                    if (!attacking && !flashing && engaged == null) {
                        return stack;
                    }
                    ItemStack weapon = rackWeapon(player);
                    if (weapon.isEmpty()) {
                        return stack;
                    }
                    if (attacking) {
                        ClientHudState.weaponFlashTicks = 15;
                        ClientHudState.flashTool(weapon);
                        if (engaged == null) {
                            engaged = weapon;
                        }
                    }
                    return weapon;
                }
                return stack;
            }
            return stack;
        }
        return stack;
    }
}
