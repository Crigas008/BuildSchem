package com.crigas.buildschematic.client.render;

import com.crigas.buildschematic.client.networking.BlockJitterEffect;
import com.crigas.buildschematic.entity.FallingSchematicBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class FallingSchematicBlockEntityRenderer extends EntityRenderer<FallingSchematicBlockEntity, EntityRenderState> {
    private final BlockRenderManager blockRenderManager;

    public FallingSchematicBlockEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.blockRenderManager = context.getBlockRenderManager();
    }

    @Override
    public EntityRenderState createRenderState() {
        return new FallingSchematicBlockRenderState();
    }

    @Override
    public void render(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!(state instanceof FallingSchematicBlockRenderState renderState)) {
            return;
        }

        BlockState blockState = renderState.blockState;
        if (blockState == null || blockState.getRenderType() != BlockRenderType.MODEL) {
            return;
        }

        matrices.push();

        BlockPos blockPos = BlockPos.ofFloored(state.x, state.y, state.z);
        if (BlockJitterEffect.isJittering(blockPos)) {
            double[] offset = BlockJitterEffect.getJitterOffset(blockPos);
            matrices.translate(offset[0], offset[1], offset[2]);
        }
        if (renderState.isLanding && renderState.blockScale < 1.0f) {
            float scale = renderState.blockScale;
            matrices.scale(scale, scale, scale);
        }

        matrices.translate(-0.5, 0.0, -0.5);

        var vertexConsumer = vertexConsumers.getBuffer(RenderLayers.getMovingBlockLayer(blockState));

        BlockPos renderPos = BlockPos.ofFloored(state.x, state.y, state.z);

        this.blockRenderManager.getModelRenderer().render(
            MinecraftClient.getInstance().world,
            this.blockRenderManager.getModel(blockState),
            blockState,
            renderPos,
            matrices,
            vertexConsumer,
            false,
            Random.create(),
            blockState.getRenderingSeed(renderPos),
            OverlayTexture.DEFAULT_UV
        );

        matrices.pop();
    }

    @Override
    public void updateRenderState(FallingSchematicBlockEntity entity, EntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);

        if (state instanceof FallingSchematicBlockRenderState renderState) {
            renderState.blockState = entity.getBlockState();
            renderState.isLanding = entity.isLanding();
            renderState.blockScale = entity.getBlockScale();
        }
    }

    public static class FallingSchematicBlockRenderState extends EntityRenderState {
        public BlockState blockState;
        public boolean isLanding;
        public float blockScale = 1.0f;
    }
}
