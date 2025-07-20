package com.crigas.buildschematic.client.networking;

import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class BlockJitterEffect {
    private static final Map<BlockPos, Integer> jitterBlocks = new HashMap<>();
    private static final Random random = new Random();

    public static void init() {
        jitterBlocks.clear();
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void addJitter(BlockPos pos, int ticks) {
        jitterBlocks.put(pos, ticks);
    }

    public static boolean isJittering(BlockPos pos) {
        boolean result = jitterBlocks.containsKey(pos);
        return result;
    }

    public static double[] getJitterOffset(BlockPos pos) {
        if (!isJittering(pos)) return new double[] {0, 0, 0};
        long time = System.currentTimeMillis();
        double x = Math.sin((time / 60.0) + pos.getX() * 13.37) * 0.12;
        double y = Math.cos((time / 70.0) + pos.getY() * 7.77) * 0.12;
        double z = Math.sin((time / 80.0) + pos.getZ() * 3.33) * 0.12;
        return new double[] {x, y, z};
    }

    private static void tick() {
        Iterator<Map.Entry<BlockPos, Integer>> it = jitterBlocks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            int left = entry.getValue() - 1;
            if (left <= 0) {
                it.remove();
            } else {
                entry.setValue(left);
            }
        }
    }
}
