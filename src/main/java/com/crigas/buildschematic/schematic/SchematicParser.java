package com.crigas.buildschematic.schematic;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.*;
import com.crigas.buildschematic.util.VarIntUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SchematicParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchematicParser.class);

    public static SchematicData parseSchematic(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            NbtCompound root = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());
            return parseNbtData(root);
        }
    }

    private static SchematicData parseNbtData(NbtCompound root) {
        int width = root.getShort("Width");
        int height = root.getShort("Height");
        int length = root.getShort("Length");
        
        LOGGER.info("Parsing schematic: {}x{}x{}", width, height, length);

        NbtCompound paletteNbt = root.getCompound("Palette");
        Map<Integer, BlockState> palette = new HashMap<>();

        for (String key : paletteNbt.getKeys()) {
            int id = paletteNbt.getInt(key);
            try {
                BlockState state = parseBlockState(key);
                if (state != null) {
                    palette.put(id, state);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse block state: {}", key, e);
            }
        }

        byte[] blockData = root.getByteArray("BlockData");
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        
        try {
            int[] blockIds = VarIntUtil.decodeVarIntArray(blockData);

            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        if (index < blockIds.length) {
                            int paletteId = blockIds[index];
                            BlockState state = palette.get(paletteId);

                            if (state != null && !state.isAir()) {
                                blocks.put(new BlockPos(x, y, z), state);
                            }
                            index++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to decode VarInt array", e);
            int[] blockIds = decodeVarIntArraySimple(blockData);
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        if (index < blockIds.length) {
                            int paletteId = blockIds[index];
                            BlockState state = palette.get(paletteId);

                            if (state != null && !state.isAir()) {
                                blocks.put(new BlockPos(x, y, z), state);
                            }
                            index++;
                        }
                    }
                }
            }
        }

        LOGGER.info("Parsed {} blocks from schematic", blocks.size());
        return new SchematicData(width, height, length, blocks);
    }

    private static int[] decodeVarIntArraySimple(byte[] data) {
        if (data.length == 0) return new int[0];

        int[] result = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i] & 0xFF;
        }
        return result;
    }

    private static BlockState parseBlockState(String blockString) {
        try {
            String[] parts = blockString.split("\\[");
            String blockName = parts[0];

            Identifier id = Identifier.tryParse(blockName);
            if (id == null) {
                LOGGER.warn("Invalid block identifier: {}", blockName);
                return null;
            }

            var block = Registries.BLOCK.get(id);

            BlockState state = block.getDefaultState();
            
            if (parts.length > 1) {
                String properties = parts[1].replace("]", "");
                state = parseBlockProperties(state, properties);
            }
            
            return state;
        } catch (Exception e) {
            LOGGER.warn("Failed to parse block: {}", blockString, e);
            return null;
        }
    }

    private static BlockState parseBlockProperties(BlockState state, String properties) {
        if (properties.isEmpty()) return state;
        
        String[] props = properties.split(",");
        for (String prop : props) {
            String[] keyValue = prop.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                try {
                    state = setBlockProperty(state, key, value);
                } catch (Exception e) {
                    LOGGER.debug("Failed to parse property {}={}", key, value);
                }
            }
        }

        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState setBlockProperty(BlockState state, String propertyName, String value) {
        var property = state.getBlock().getStateManager().getProperty(propertyName);
        if (property != null) {
            var parsedValue = property.parse(value);
            if (parsedValue.isPresent()) {
                return state.with((net.minecraft.state.property.Property<T>) property, (T) parsedValue.get());
            }
        }
        return state;
    }
}
