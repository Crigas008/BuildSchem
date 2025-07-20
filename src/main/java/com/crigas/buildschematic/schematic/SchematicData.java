package com.crigas.buildschematic.schematic;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public record SchematicData(int width, int height, int length, Map<BlockPos, BlockState> blocks) {

    public int getTotalBlocks() {
        return blocks.size();
    }

    public BlockState getBlockAt(BlockPos pos) {
        return blocks.get(pos);
    }

    public boolean hasBlockAt(BlockPos pos) {
        return blocks.containsKey(pos);
    }
}
