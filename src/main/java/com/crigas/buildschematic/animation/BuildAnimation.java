package com.crigas.buildschematic.animation;

import com.crigas.buildschematic.entity.FallingSchematicBlockEntity;
import com.crigas.buildschematic.networking.BlockJitterPayload;
import com.crigas.buildschematic.schematic.SchematicData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class BuildAnimation {
    private final SchematicData schematic;
    private final ServerWorld world;
    private final BlockPos startPos;
    private final List<Map.Entry<BlockPos, BlockState>> blockQueue;
    private final int blocksPerTick;
    private final AnimationType animationType;
    
    private final int layerPauseTicks;
    private final List<List<Map.Entry<BlockPos, BlockState>>> layers;
    private int currentLayerIndex = 0;
    private int layerPauseCounter = 0;
    private int currentLayerBlockIndex = 0;

    private int currentIndex = 0;
    private boolean isCompleted = false;
    private boolean isPaused = false;


    public enum AnimationType {
        BOTTOM_TO_TOP,
        TOP_TO_BOTTOM,
        LAYER_BY_LAYER,
        INSTANT_LAYER,
        RANDOM,
        SPIRAL,
        FALLING_BLOCKS,
        FALLING_LAYERS
    }
    
    public BuildAnimation(SchematicData schematic, ServerWorld world, BlockPos startPos,
                         int blocksPerTick, AnimationType animationType) {
        this(schematic, world, startPos, blocksPerTick, animationType, 20);
    }
    public BuildAnimation(SchematicData schematic, ServerWorld world, BlockPos startPos,
                         int blocksPerTick, AnimationType animationType, int layerPauseTicks) {
        this(schematic, world, startPos, blocksPerTick, animationType, layerPauseTicks, false);
    }
    public BuildAnimation(SchematicData schematic, ServerWorld world, BlockPos startPos,
                         int blocksPerTick, AnimationType animationType, int layerPauseTicks, boolean withJitter) {
        this.schematic = schematic;
        this.world = world;
        this.startPos = startPos;
        this.blocksPerTick = blocksPerTick;
        this.animationType = animationType;
        this.layerPauseTicks = layerPauseTicks;
        this.blockQueue = new ArrayList<>();
        this.layers = new ArrayList<>();

        initializeBlockQueue();
    }

    private void initializeBlockQueue() {
        switch (animationType) {
            case BOTTOM_TO_TOP:
                initializeBottomToTop();
                break;
            case TOP_TO_BOTTOM:
                initializeTopToBottom();
                break;
            case LAYER_BY_LAYER:
            case INSTANT_LAYER:
                initializeLayerByLayer();
                break;
            case RANDOM:
                initializeRandom();
                break;
            case SPIRAL:
                initializeSpiral();
                break;
            case FALLING_BLOCKS:
                initializeFallingBlocks();
                break;
            case FALLING_LAYERS:
                initializeLayerByLayer(); // Используем ту же инициализацию что и для слоев
                break;
        }
    }
    
    private void initializeBottomToTop() {
        for (int y = 0; y < schematic.height(); y++) {
            for (int x = 0; x < schematic.width(); x++) {
                for (int z = 0; z < schematic.length(); z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    if (schematic.hasBlockAt(relativePos)) {
                        blockQueue.add(Map.entry(relativePos, schematic.getBlockAt(relativePos)));
                    }
                }
            }
        }
    }




    
    private void initializeTopToBottom() {
        for (int y = schematic.height() - 1; y >= 0; y--) {
            for (int x = 0; x < schematic.width(); x++) {
                for (int z = 0; z < schematic.length(); z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    if (schematic.hasBlockAt(relativePos)) {
                        blockQueue.add(Map.entry(relativePos, schematic.getBlockAt(relativePos)));
                    }
                }
            }
        }
    }
    
    private void initializeLayerByLayer() {
        for (int y = 0; y < schematic.height(); y++) {
            List<Map.Entry<BlockPos, BlockState>> layer = new ArrayList<>();
            for (int x = 0; x < schematic.width(); x++) {
                for (int z = 0; z < schematic.length(); z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    if (schematic.hasBlockAt(relativePos)) {
                        layer.add(Map.entry(relativePos, schematic.getBlockAt(relativePos)));
                    }
                }
            }
            layers.add(layer);
        }
    }
    
    private void initializeRandom() {
        List<Map.Entry<BlockPos, BlockState>> blocks = new ArrayList<>(schematic.blocks().entrySet());
        java.util.Collections.shuffle(blocks);
        blockQueue.addAll(blocks);
    }
    
    private void initializeSpiral() {
        int centerX = schematic.width() / 2;
        int centerZ = schematic.length() / 2;
        
        for (int y = 0; y < schematic.height(); y++) {
            List<BlockPos> spiral = generateSpiralOrder(centerX, centerZ, schematic.width(), schematic.length(), y);
            for (BlockPos pos : spiral) {
                if (schematic.hasBlockAt(pos)) {
                    blockQueue.add(Map.entry(pos, schematic.getBlockAt(pos)));
                }
            }
        }
    }
    
    private void initializeFallingBlocks() {
        for (int y = schematic.height() - 1; y >= 0; y--) {
            for (int x = 0; x < schematic.width(); x++) {
                for (int z = 0; z < schematic.length(); z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    if (schematic.hasBlockAt(relativePos)) {
                        blockQueue.add(Map.entry(relativePos, schematic.getBlockAt(relativePos)));
                    }
                }
            }
        }
    }

    private List<BlockPos> generateSpiralOrder(int centerX, int centerZ, int width, int length, int y) {
        List<BlockPos> positions = new ArrayList<>();
        for (int radius = 0; radius < Math.max(width, length); radius++) {
            for (int x = Math.max(0, centerX - radius); x <= Math.min(width - 1, centerX + radius); x++) {
                for (int z = Math.max(0, centerZ - radius); z <= Math.min(length - 1, centerZ + radius); z++) {
                    if (Math.abs(x - centerX) == radius || Math.abs(z - centerZ) == radius) {
                        positions.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return positions;
    }
    
    public void tick() {
        if (isPaused || isCompleted) {
            return;
        }
        
        if (animationType == AnimationType.LAYER_BY_LAYER || animationType == AnimationType.INSTANT_LAYER) {
            tickLayerByLayer();
        } else if (animationType == AnimationType.FALLING_BLOCKS) {
            tickFallingBlocks();
        } else if (animationType == AnimationType.FALLING_LAYERS) {
            tickFallingLayers();
        } else {
            tickNormal();
        }
    }

    private void sendJitterPacket(BlockPos pos) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, new BlockJitterPayload(pos));
        }
    }

    private void setBlockWithJitter(BlockPos worldPos, BlockState blockState) {
        world.setBlockState(worldPos, blockState);
        spawnBlockPlaceParticles(worldPos, blockState);
        sendJitterPacket(worldPos);
    }

    private void tickNormal() {
        int blocksPlaced = 0;
        while (blocksPlaced < blocksPerTick && currentIndex < blockQueue.size()) {
            Map.Entry<BlockPos, BlockState> entry = blockQueue.get(currentIndex);
            BlockPos relativePos = entry.getKey();
            BlockState blockState = entry.getValue();
            BlockPos worldPos = startPos.add(relativePos);
            setBlockWithJitter(worldPos, blockState);
            currentIndex++;
            blocksPlaced++;
        }
        if (currentIndex >= blockQueue.size()) {
            isCompleted = true;
        }
    }

    private void tickLayerByLayer() {
        if (currentLayerIndex >= layers.size()) {
            isCompleted = true;
            return;
        }
        if (layerPauseCounter > 0) {
            layerPauseCounter--;
            return;
        }
        List<Map.Entry<BlockPos, BlockState>> currentLayer = layers.get(currentLayerIndex);
        if (animationType == AnimationType.INSTANT_LAYER) {
            for (Map.Entry<BlockPos, BlockState> entry : currentLayer) {
                BlockPos relativePos = entry.getKey();
                BlockState blockState = entry.getValue();
                BlockPos worldPos = startPos.add(relativePos);
                setBlockWithJitter(worldPos, blockState);
                currentIndex++;
            }
            currentLayerIndex++;
            layerPauseCounter = layerPauseTicks;
            currentLayerBlockIndex = 0;
        } else {
            int blocksPlaced = 0;
            while (blocksPlaced < blocksPerTick && currentLayerBlockIndex < currentLayer.size()) {
                Map.Entry<BlockPos, BlockState> entry = currentLayer.get(currentLayerBlockIndex);
                BlockPos relativePos = entry.getKey();
                BlockState blockState = entry.getValue();
                BlockPos worldPos = startPos.add(relativePos);
                setBlockWithJitter(worldPos, blockState);
                currentLayerBlockIndex++;
                currentIndex++;
                blocksPlaced++;
            }
            if (currentLayerBlockIndex >= currentLayer.size()) {
                currentLayerIndex++;
                layerPauseCounter = layerPauseTicks;
                currentLayerBlockIndex = 0;
            }
        }
    }
    
    private void tickFallingBlocks() {
        int blocksSpawned = 0;
        while (blocksSpawned < blocksPerTick && currentIndex < blockQueue.size()) {
            Map.Entry<BlockPos, BlockState> entry = blockQueue.get(currentIndex);
            BlockPos relativePos = entry.getKey();
            BlockState blockState = entry.getValue();

            BlockPos targetPos = startPos.add(relativePos);

            double spawnHeight = targetPos.getY() + 10 + world.random.nextDouble() * 10; // 10-20 blocks above
            double spawnX = targetPos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 2; // Slight random offset
            double spawnZ = targetPos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 2;

            FallingSchematicBlockEntity fallingBlock = new FallingSchematicBlockEntity(
                world, spawnX, spawnHeight, spawnZ, blockState, targetPos
            );

            fallingBlock.setFallSpeed(0.04 + world.random.nextDouble() * 0.02);

            Vec3d velocity = new Vec3d(
                (world.random.nextDouble() - 0.5) * 0.1,
                0,
                (world.random.nextDouble() - 0.5) * 0.1
            );
            fallingBlock.setVelocity(velocity);

            world.spawnEntity(fallingBlock);

            currentIndex++;
            blocksSpawned++;
        }

        if (currentIndex >= blockQueue.size()) {
            isCompleted = true;
        }
    }

    private void tickFallingLayers() {
        if (currentLayerIndex >= layers.size()) {
            isCompleted = true;
            return;
        }

        if (layerPauseCounter > 0) {
            layerPauseCounter--;
            return;
        }

        List<Map.Entry<BlockPos, BlockState>> currentLayer = layers.get(currentLayerIndex);

        for (Map.Entry<BlockPos, BlockState> entry : currentLayer) {
            BlockPos relativePos = entry.getKey();
            BlockState blockState = entry.getValue();
            BlockPos targetPos = startPos.add(relativePos);

            double layerSpawnHeight = targetPos.getY() + 20 + currentLayerIndex * 3;
            double spawnX = targetPos.getX() + 0.5;
            double spawnZ = targetPos.getZ() + 0.5;

            FallingSchematicBlockEntity fallingBlock = new FallingSchematicBlockEntity(
                world, spawnX, layerSpawnHeight, spawnZ, blockState, targetPos
            );

            fallingBlock.setFallSpeed(0.06);

            world.spawnEntity(fallingBlock);
            currentIndex++;
        }

        currentLayerIndex++;
        layerPauseCounter = layerPauseTicks;
        currentLayerBlockIndex = 0;
    }

    private void spawnBlockPlaceParticles(BlockPos pos, BlockState blockState) {
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        BlockStateParticleEffect particleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState);

        world.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, // center coordinates
                blockState.getSoundGroup().getPlaceSound(),
                SoundCategory.BLOCKS,
                15.0f,
                1.0f
        );

        for (int i = 0; i < 8; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
            double offsetY = world.random.nextDouble() * 0.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
            double velocityX = (world.random.nextDouble() - 0.5) * 0.2;
            double velocityY = world.random.nextDouble() * 0.1 + 0.05;
            double velocityZ = (world.random.nextDouble() - 0.5) * 0.2;
            world.spawnParticles(
                particleEffect,
                blockCenter.x + offsetX,
                blockCenter.y + offsetY,
                blockCenter.z + offsetZ,
                1,
                velocityX,
                velocityY,
                velocityZ,
                0.1
            );
        }
    }

    public void pause() {
        isPaused = true;
    }
    
    public void resume() {
        isPaused = false;
    }
    
    public void stop() {
        isCompleted = true;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    


    public int getTotalBlocks() {
        if (animationType == AnimationType.LAYER_BY_LAYER || animationType == AnimationType.INSTANT_LAYER) {
            return layers.stream().mapToInt(List::size).sum();
        } else {
            return blockQueue.size();
        }
    }




}
