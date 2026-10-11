package dev.fallingcloud.betterinventory.client;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.jetbrains.annotations.Nullable;

/**
 * 1.4.1 effect row: the player's potion effects drawn as icon frames left of
 * the character panel, with a "+N" overflow frame and hover tooltips.
 *
 * <p>26.3 adaptations: {@code blit(RenderPipelines.GUI_TEXTURED, ...)} for the
 * frames, {@code Hud.getMobEffectSprite(effect.getEffect())} +
 * {@code blitSprite} for icons (the 1.21.1 StatusEffectSpriteManager is gone),
 * {@code text()} for labels and {@code setComponentTooltipForNextFrame} for the
 * list tooltip. The per-icon expiry blink used {@code RenderSystem.setShaderColor},
 * which 26.3 removed (colour lives in the render pipeline now), so icons draw
 * at full alpha; the countdown text still communicates expiry.
 */
public final class EffectRow {
    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/inventory.png");
    private static final int TEXT_COLOR = 16777215;
    private static final int TOOLTIP_MIN_Y = 16;

    private EffectRow() {}

    private static List<MobEffectInstance> listed(Minecraft minecraft) {
        if (minecraft.player == null || !BetterInventoryConfig.SHOW_EFFECTS.get()) {
            return List.of();
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (inInventory(effect)) {
                effects.add(effect);
            }
        }
        Collections.sort(effects);
        return effects;
    }

    private static int room(int topPos) {
        int above = Math.max(0, topPos + 9 - 2) / 27;
        return (1 + above) * 3;
    }

    private static int frameX(int leftPos, int index) {
        return leftPos + index % 3 * 27;
    }

    private static int frameY(int topPos, int index) {
        return topPos + 9 - index / 3 * 27;
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos) {
        Minecraft minecraft = Minecraft.getInstance();
        List<MobEffectInstance> effects = listed(minecraft);
        if (effects.isEmpty()) {
            return;
        }
        int frames = Math.min(effects.size(), room(topPos));
        int icons = effects.size() > frames ? frames - 1 : frames;
        int[] frame = BetterInventoryLayout.PANEL_1X1;
        for (int i = 0; i < frames; i++) {
            int x = frameX(leftPos, i);
            int y = frameY(topPos, i);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, frame[0], frame[1], frame[2], frame[3], 512, 256);
            int ix = x + 3;
            int iy = y + 3;
            if (i < icons) {
                MobEffectInstance effect = effects.get(i);
                icon(graphics, effect, x, y);
                Component level = level(effect.getAmplifier());
                if (level != null) {
                    graphics.text(font, level, ix + 18 - font.width(level), iy + 18 - 8, TEXT_COLOR, true);
                }
            } else {
                String rest = "+" + (effects.size() - icons);
                graphics.text(font, rest, ix + (18 - font.width(rest) + 1) / 2, iy + 5, TEXT_COLOR, true);
            }
        }
    }

    private static void icon(GuiGraphicsExtractor graphics, MobEffectInstance effect, int x, int y) {
        Identifier sprite = Hud.getMobEffectSprite(effect.getEffect());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 3, y + 3, 18, 18);
    }

    private static int frameAt(int leftPos, int topPos, int frames, double mouseX, double mouseY) {
        for (int i = 0; i < frames; i++) {
            int x = frameX(leftPos, i);
            int y = frameY(topPos, i);
            if (mouseX >= x && mouseX < x + 24 && mouseY >= y && mouseY < y + 24) {
                return i;
            }
        }
        return -1;
    }

    public static boolean covers(int leftPos, int topPos, double mouseX, double mouseY) {
        int count = listed(Minecraft.getInstance()).size();
        return count > 0 && frameAt(leftPos, topPos, Math.min(count, room(topPos)), mouseX, mouseY) >= 0;
    }

    public static void renderTooltip(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        List<MobEffectInstance> effects = listed(minecraft);
        if (effects.isEmpty()) {
            return;
        }
        int frames = Math.min(effects.size(), room(topPos));
        int hovered = frameAt(leftPos, topPos, frames, mouseX, mouseY);
        if (hovered < 0) {
            return;
        }
        int icons = effects.size() > frames ? frames - 1 : frames;
        List<Component> lines = new ArrayList<>();
        if (hovered < icons) {
            MobEffectInstance effect = effects.get(hovered);
            lines.add(name(effect));
            lines.add(timeLeft(minecraft, effect).copy().withStyle(ChatFormatting.GRAY));
        } else {
            for (MobEffectInstance effect : effects.subList(icons, effects.size())) {
                lines.add(name(effect).append("  ").append(timeLeft(minecraft, effect).copy().withStyle(ChatFormatting.GRAY)));
            }
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, Math.max(mouseY, TOOLTIP_MIN_Y));
    }

    private static MutableComponent name(MobEffectInstance effect) {
        MobEffect mobEffect = effect.getEffect().value();
        MutableComponent name = mobEffect.getDisplayName().copy();
        Component level = level(effect.getAmplifier());
        return level == null ? name : name.append(" ").append(level);
    }

    @Nullable
    private static Component level(int amplifier) {
        if (amplifier < 1) {
            return null;
        }
        return amplifier <= 9
                ? Component.translatable("enchantment.level." + (amplifier + 1))
                : Component.literal(String.valueOf(amplifier + 1));
    }

    private static Component timeLeft(Minecraft minecraft, MobEffectInstance effect) {
        float ticksPerSecond = minecraft.level != null ? minecraft.level.tickRateManager().tickrate() : 20.0F;
        return MobEffectUtil.formatDuration(effect, 1.0F, ticksPerSecond);
    }

    private static boolean inInventory(MobEffectInstance effect) {
        return true;
    }
}
