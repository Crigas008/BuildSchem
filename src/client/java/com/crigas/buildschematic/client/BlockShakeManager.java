package com.crigas.buildschematic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockShakeManager {
    private static final Map<BlockPos, ShakeData> activeShakes = new HashMap<>();

    public static void startShake(BlockPos pos, int durationTicks) {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        activeShakes.put(pos, new ShakeData(
                world.getTime(),
                durationTicks
        ));
    }

    public static float getShakeOffset(BlockPos pos, float tickDelta, int axis) {
        ShakeData data = activeShakes.get(pos);
        if (data == null) return 0f;

        World world = MinecraftClient.getInstance().world;
        if (world == null) return 0f;

        long currentTime = world.getTime();
        float elapsedTicks = (currentTime - data.startTick) + tickDelta;

        if (elapsedTicks >= data.durationTicks) {
            activeShakes.remove(pos);
            return 0f;
        }

        float progress = elapsedTicks / data.durationTicks;

        Random random = new Random(pos.hashCode() + axis);
        float randomOffset = random.nextFloat() - 0.5f;

        return (float) Math.sin(progress * Math.PI * 8) * (1 - progress) * randomOffset * 0.3f;
    }

    private record ShakeData(long startTick, int durationTicks) {}
}