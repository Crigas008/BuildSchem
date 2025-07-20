package com.crigas.buildschematic.animation;

import com.crigas.buildschematic.BuildSchematic;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BuildAnimationManager {
    private static final List<BuildAnimation> activeAnimations = new ArrayList<>();
    private static final Map<BlockPos, Integer> jitterBlocks = new HashMap<>();

    public static void startAnimation(BuildAnimation animation) {
        activeAnimations.add(animation);
        BuildSchematic.LOGGER.info("Started build animation with {} blocks", animation.getTotalBlocks());
    }
    

    
    public static void stopAllAnimations() {
        for (BuildAnimation animation : activeAnimations) {
            animation.stop();
        }
        activeAnimations.clear();
    }
    
    public static void pauseAllAnimations() {
        for (BuildAnimation animation : activeAnimations) {
            animation.pause();
        }
    }
    
    public static void resumeAllAnimations() {
        for (BuildAnimation animation : activeAnimations) {
            animation.resume();
        }
    }
    


    public static void tickJitterBlocks() {
        jitterBlocks.replaceAll((pos, ticks) -> ticks - 1);
        jitterBlocks.entrySet().removeIf(e -> e.getValue() <= 0);
    }

    public static void tick(MinecraftServer server) {
        tickJitterBlocks();
        Iterator<BuildAnimation> iterator = activeAnimations.iterator();
        while (iterator.hasNext()) {
            BuildAnimation animation = iterator.next();
            
            animation.tick();
            
            if (animation.isCompleted()) {
                BuildSchematic.LOGGER.info("Build animation completed");
                iterator.remove();
            }
        }
    }
    

    public static int getActiveAnimationCount() {
        return activeAnimations.size();
    }
    
}
