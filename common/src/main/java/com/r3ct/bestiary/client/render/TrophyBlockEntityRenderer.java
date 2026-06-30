package com.r3ct.bestiary.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.r3ct.bestiary.block.TrophyBlock;
import com.r3ct.bestiary.block.TrophyBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TrophyBlockEntityRenderer implements BlockEntityRenderer<TrophyBlockEntity, TrophyBlockEntityRenderer.TrophyRenderState> {

    private final EntityRenderDispatcher entityRenderer;
    private final Font font;

    public TrophyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
        this.font = context.font();
    }

    @Override
    public TrophyRenderState createRenderState() {
        return new TrophyRenderState();
    }

    @Override
    public void extractRenderState(TrophyBlockEntity blockEntity, TrophyRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        state.facing = blockEntity.getBlockState().getValue(TrophyBlock.FACING);
        state.isWall = blockEntity.getBlockState().getValue(TrophyBlock.FACE) == AttachFace.WALL;

        String ownerName = blockEntity.getOwnerName();
        if (ownerName != null && !ownerName.isEmpty()) {
            state.customName = Component.literal(ownerName);
        } else {
            state.customName = null;
        }

        state.time = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0) + partialTicks;

        Entity displayEntity = blockEntity.getOrCreateDisplayEntity();
        if (displayEntity != null) {

            net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
            displayEntity.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

            displayEntity.xOld = displayEntity.getX();
            displayEntity.yOld = displayEntity.getY();
            displayEntity.zOld = displayEntity.getZ();

            displayEntity.tickCount = (int) (blockEntity.getLevel().getGameTime() % 10000);

            float maxDimension = Math.max(displayEntity.getBbWidth(), displayEntity.getBbHeight());
            if (maxDimension <= 0.01F) maxDimension = 1.0F;

            float targetSize = 0.35F * (float) Math.pow(maxDimension, 0.4);
            state.entityScale = targetSize / maxDimension;

            state.entityRenderState = this.entityRenderer.extractEntity(displayEntity, partialTicks);

            if (state.entityRenderState != null) {
                state.entityRenderState.shadowRadius = 0.0F;
                state.entityRenderState.shadowPieces.clear();
            }

        } else {
            state.entityRenderState = null;
        }
    }

    @Override
    public void submit(TrophyRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.translate(0.5D, 0.0D, 0.5D);
        float rotation = -state.facing.toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        if (state.isWall) {
            poseStack.translate(0.0D, 0.0D, -0.3125D);
        }

        if (state.entityRenderState != null) {
            poseStack.pushPose();

            float offset = (float) Math.sin(state.time / 10.0F) * 0.05F;
            float spin = state.time * 3.0F;

            poseStack.translate(0.0D, 0.25D + offset, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.scale(state.entityScale, state.entityScale, state.entityScale);

            this.entityRenderer.submit(state.entityRenderState, camera, 0.0D, 0.0D, 0.0D, poseStack, submitNodeCollector);

            poseStack.popPose();
        }

        if (state.customName != null) {
            FormattedCharSequence formattedText = state.customName.getVisualOrderText();
            float textWidth = this.font.width(formattedText);
            float textX = -textWidth / 2.0F;

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.18D, 0.25D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(-0.015F, -0.015F, 0.015F);
            submitNodeCollector.submitText(poseStack, textX, 0.0F, formattedText, false, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0x00000000);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.18D, -0.25D);
            poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            poseStack.scale(-0.015F, -0.015F, 0.015F);
            submitNodeCollector.submitText(poseStack, textX, 0.0F, formattedText, false, Font.DisplayMode.NORMAL, 15728880, 0xFFFFFFFF, 0, 0x00000000);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    public static class TrophyRenderState extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean isWall = false;

        public EntityRenderState entityRenderState = null;
        public float entityScale = 0.35F;

        public Component customName = null;
        public float time = 0.0f;
    }
}