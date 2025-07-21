package com.crigas.buildschematic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BlockShakeManager {

    private static final Map<BlockPos, ShakeData> active = new HashMap<>();

    public static void startShake(BlockPos pos, int durationTicks) {
        assert MinecraftClient.getInstance().world != null;
        long start = MinecraftClient.getInstance().world.getTime();
        active.put(pos, new ShakeData(start, durationTicks, pos));
    }

    public static Set<BlockPos> getActivePositions() {
        assert MinecraftClient.getInstance().world != null;
        long now = MinecraftClient.getInstance().world.getTime();
        Set<BlockPos> current = new HashSet<>();
        for (var entry : active.entrySet()) {
            ShakeData data = entry.getValue();
            if (now - data.startTick <= data.durationTicks) {
                current.add(entry.getKey());
            }
        }
        return current;
    }

    public static float getShakeOffset(BlockPos pos, float tickDelta, int axis) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return 0f;

        ShakeData data = active.get(pos);
        if (data == null) return 0f;

        long worldTime = client.world.getTime();
        float elapsed = (worldTime - data.startTick) + tickDelta;
        if (elapsed >= data.durationTicks) {
            active.remove(pos);
            return 0f;
        }

        float progress = elapsed / data.durationTicks;
        float fade = (float) Math.pow(1.0f - progress, 2); // затухание

        float totalTime = worldTime + tickDelta;
        float freq = 0.15f + axis * 0.05f;
        float phase = data.randomOffset[axis];
        float amp = 0.04f;

        return (float) Math.sin((totalTime + phase) * freq) * amp * fade;
    }

    private static class ShakeData {
        public final long startTick;
        public final int durationTicks;
        public final float[] randomOffset = new float[3];

        public ShakeData(long startTick, int durationTicks, BlockPos pos) {
            this.startTick = startTick;
            this.durationTicks = durationTicks;
            Random r = new Random(pos.asLong());
            for (int i = 0; i < 3; i++) {
                randomOffset[i] = r.nextFloat() * 100f;
            }
        }
    }
}
