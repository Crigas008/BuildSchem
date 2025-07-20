package com.crigas.buildschematic.client;

import com.crigas.buildschematic.client.render.FallingSchematicBlockEntityRenderer;
import com.crigas.buildschematic.entity.SchematicEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.crigas.buildschematic.client.networking.BlockJitterEffect;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import com.crigas.buildschematic.networking.BlockJitterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class BuildSchematicClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("BuildSchematic-Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("BuildSchematic Client Initialized");
        EntityRendererRegistry.register(SchematicEntityTypes.FALLING_SCHEMATIC_BLOCK, FallingSchematicBlockEntityRenderer::new);
        PayloadTypeRegistry.playS2C().register(BlockJitterPayload.ID, BlockJitterPayload.CODEC);
        BlockJitterEffect.init();

        ClientPlayNetworking.registerGlobalReceiver(BlockJitterPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BlockPos pos = payload.pos();

                BlockShakeManager.startShake(pos, 100);

            });
        });
    }
}
