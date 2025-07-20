package com.crigas.buildschematic.client.mixins;
import com.crigas.buildschematic.client.BlockShakeManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin {
    @Unique
    private boolean shouldPopMatrix = false;

    @Inject(
            method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V",
            at = @At("HEAD")
    )
    private void beforeRenderBlock(
            BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay, CallbackInfo ci
    ) {
        System.out.println("[DEBUG] BlockModelRendererMixin beforeRenderBlock called");
        float tickDelta = MinecraftClient.getInstance().world.getTickOrder();

        float offsetX = BlockShakeManager.getShakeOffset(pos, tickDelta, 0);
        float offsetY = BlockShakeManager.getShakeOffset(pos, tickDelta, 1);
        float offsetZ = BlockShakeManager.getShakeOffset(pos, tickDelta, 2);
        System.out.println("Current offset: " + offsetX + ", " + offsetY + ", " + offsetZ);
        if (offsetX != 0 || offsetY != 0 || offsetZ != 0) {
            matrices.push();
            matrices.translate(10, offsetY, offsetZ);
            shouldPopMatrix = true;
        } else {
            shouldPopMatrix = false;
        }
    }


    @Inject(
            method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V",
            at = @At("RETURN")
    )
    private void afterRenderBlock(
            BlockRenderView world, net.minecraft.client.render.model.BakedModel model, BlockState state, BlockPos pos,
            MatrixStack matrices, VertexConsumer vertexConsumer,
            boolean cull, Random random, long seed, int overlay,
            CallbackInfo ci
    ) {
        if (shouldPopMatrix) {
            matrices.pop();
        }
    }
}