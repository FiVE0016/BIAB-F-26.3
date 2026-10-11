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

    public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isSpectator || !BetterInventoryConfig.SHOW_WORN_BACKPACK.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack pack = PlayerLoadout.of(player).highestBackpack();
        if (pack.isEmpty()) {
            return;
        }
        ItemStackRenderState rendered = new ItemStackRenderState();
        minecraft.getItemModelResolver().updateForLiving(rendered, pack, ItemDisplayContext.NONE, player);
        if (rendered.isEmpty()) {
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
    }

    /**
     * 26.3 的物品模型已解析进 {@link ItemStackRenderState}，直接用
     * {@code visitExtents} 取顶点范围（1.4.1 的 BakedQuad 包围盒等价物）。
     * 渲染状态每帧重建，无法像旧版按 BakedModel 缓存，且遍历成本极低，故不缓存。
     */
    private static float[] measure(ItemStackRenderState rendered) {
        float[] b = new float[]{
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        rendered.visitExtents(v -> {
            b[0] = Math.min(b[0], v.x());
            b[1] = Math.min(b[1], v.y());
            b[2] = Math.min(b[2], v.z());
            b[3] = Math.max(b[3], v.x());
            b[4] = Math.max(b[4], v.y());
            b[5] = Math.max(b[5], v.z());
        });
        return b[0] <= b[3] ? b : new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};
    }
}
