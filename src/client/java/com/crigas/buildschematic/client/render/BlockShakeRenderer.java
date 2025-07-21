package com.crigas.buildschematic.client.render;

import com.crigas.buildschematic.client.BlockShakeManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockShakeRenderer {
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return;

            MatrixStack matrices = context.matrixStack();
            var camera = context.camera();
            double camX = camera.getPos().x;
            double camY = camera.getPos().y;
            double camZ = camera.getPos().z;

            assert matrices != null;
            matrices.push();
            matrices.translate(-camX, -camY, -camZ);

            for (BlockPos pos : BlockShakeManager.getActivePositions()) {
                BlockState state = client.world.getBlockState(pos);
                if (!hasVisibleFace(pos)) continue;

                float tickDelta = client.getRenderTickCounter().getTickDelta(false);
                float dx = BlockShakeManager.getShakeOffset(pos, tickDelta, 0);
                float dy = BlockShakeManager.getShakeOffset(pos, tickDelta, 1);
                float dz = BlockShakeManager.getShakeOffset(pos, tickDelta, 2);

                matrices.push();
                matrices.translate(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                client.getBlockRenderManager().renderBlockAsEntity(
                        state, matrices, context.consumers(), 15728880, OverlayTexture.DEFAULT_UV
                );
                matrices.pop();
            }

            matrices.pop();
        });
    }

    private static boolean hasVisibleFace(BlockPos pos) {
        var world = MinecraftClient.getInstance().world;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            assert world != null;
            BlockState state = world.getBlockState(neighbor);
            if (state.isAir() || !state.isOpaqueFullCube()) {
                return true;
            }
        }
        return false;
    }
}
