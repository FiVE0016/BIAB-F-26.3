package dev.fallingcloud.betterinventory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 背着背包渲染层，移植到 26.3 无状态渲染管线。
 *
 * <p>26.3 移除了 {@code FeatureRenderer}/{@code PlayerRenderer}：渲染层现在是
 * 无状态 {@link RenderLayer}，对 {@link AvatarRenderState} 提交；物品渲染走
 * {@link ItemStackRenderState}（{@link Minecraft#getItemModelResolver()} 解析 +
 * {@code submit}），替代旧 {@code ItemRenderer.renderItem}。
 *
 * <p>实体经 {@link AvatarRenderState#id} 从客户端世界取回，因此远程玩家的背包
 * 同样渲染（1.4.1 行为：配装数据在客户端同步的实体上）。
 */
public final class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float SCALE = 0.85F;
    private static final float BACK_FACE = 0.109375F;
    private static final float CENTRE_Y = 0.3125F;
    private static final org.slf4j.Logger BACKPACK_LAYER_LOGGER =
            org.slf4j.LoggerFactory.getLogger("betterinventory/backpack-layer");
    /** 诊断去重：结果不变时最多每 2 秒（40 tick）打一条。 */
    private static String betterinventory$lastOutcome;
    private static long betterinventory$lastLogTick;

    private static void diagnose(org.slf4j.Logger logger, AvatarRenderState state, String outcome) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (outcome.equals(betterinventory$lastOutcome) && now - betterinventory$lastLogTick < 40L) {
            return;
        }
        betterinventory$lastOutcome = outcome;
        betterinventory$lastLogTick = now;
        logger.info("[BI backpack layer] id={} tick={} {}", state.id, now, outcome);
    }

    public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isSpectator) {
            return;
        }
        if (!BetterInventoryConfig.SHOW_WORN_BACKPACK.get()) {
            diagnose(BACKPACK_LAYER_LOGGER, state, "disabled by config");
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player)) {
            diagnose(BACKPACK_LAYER_LOGGER, state, "id " + state.id + " -> not a player ("
                    + (entity == null ? "null" : entity.getClass().getSimpleName()) + ")");
            return;
        }
        PlayerLoadout loadout = PlayerLoadout.of(player);
        ItemStack pack = loadout.highestBackpack();
        if (pack.isEmpty()) {
            diagnose(BACKPACK_LAYER_LOGGER, state, "no backpack equipped for " + player.getScoreboardName()
                    + " (backpack slots hold " + loadout.backpacks.getSlots() + " slots)");
            return;
        }
        ItemStackRenderState rendered = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForLiving(rendered, pack, ItemDisplayContext.NONE, player);
        if (rendered.isEmpty()) {
            diagnose(BACKPACK_LAYER_LOGGER, state, "item " + pack.getItem() + " resolved to an empty render state");
            return;
        }
        float[] bounds = measure(rendered);
        float centreX = (bounds[0] + bounds[3]) * 0.5F;
        float centreY = (bounds[1] + bounds[4]) * 0.5F;
        float x = -SCALE * (centreX - 0.5F);
        float y = CENTRE_Y + SCALE * (centreY - 0.5F);
        float z = BACK_FACE + SCALE * (bounds[5] - 0.5F);
        pose.pushPose();
        // 躯干变换：1.4.1 是 ((PlayerEntityModel) getContextModel()).body.rotate(pose)；
        // 26.3 等价物是 ModelPart.translateAndRotate（AvatarRenderer 的模型即 PlayerModel）。
        getParentModel().body.translateAndRotate(pose);
        pose.translate(x, y, z);
        pose.scale(SCALE, SCALE, SCALE);
        // 1.4.1 用 Direction.POSITIVE_*.rotationDegrees(180)；26.3 删除该方法，
        // PoseStack 自带 rotateDegrees(Axis, degrees) 是等价物。
        pose.rotateDegrees(Axis.ZP, 180.0F);
        pose.rotateDegrees(Axis.YP, 180.0F);
        rendered.submit(pose, collector, packedLight, OverlayTexture.NO_OVERLAY, state.id);
        pose.popPose();
        diagnose(BACKPACK_LAYER_LOGGER, state, "drawn " + pack.getItem() + " at " + x + "," + y + "," + z
                + " bounds=" + bounds[0] + "," + bounds[1] + "," + bounds[2] + ".." + bounds[3] + "," + bounds[4] + "," + bounds[5]);
    }

    /**
     * 26.3 的物品模型已解析进 {@link ItemStackRenderState}，直接用
     * {@code visitExtents} 取顶点范围（1.4.1 的 BakedQuad 包围盒等价物）。
     * 渲染状态每帧重建，无法像旧版按 BakedModel 缓存，且遍历成本极低，故不缓存。
     *
     * <p><b>坐标空间</b>：{@code visitExtents} 给的是物品渲染空间——方块类模型已被
     * 居中到原点（约 -0.5..0.5），而 1.4.1 的 {@code measure()} 量的是 BakedModel
     * 原始坐标（0..1）。因此这里统一 +0.5 归一到 0..1，否则下面的居中偏移会算成
     * 半个方块（实机日志：偏移 x=0.425，实际应为 0——背包因此飘在身体外）。
     */
    private static float[] measure(ItemStackRenderState rendered) {
        float[] b = new float[]{
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        rendered.visitExtents(v -> {
            b[0] = Math.min(b[0], v.x() + 0.5F);
            b[1] = Math.min(b[1], v.y() + 0.5F);
            b[2] = Math.min(b[2], v.z() + 0.5F);
            b[3] = Math.max(b[3], v.x() + 0.5F);
            b[4] = Math.max(b[4], v.y() + 0.5F);
            b[5] = Math.max(b[5], v.z() + 0.5F);
        });
        return b[0] <= b[3] ? b : new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};
    }
}
