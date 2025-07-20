package com.crigas.buildschematic.schematic;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SchematicLoader {

    private static final String SCHEMATICS_DIR = "schematics";

    public SchematicData loadSchematic(String filename) {
        try {
            Path schematicPath = getSchematicPath(filename);
            if (!schematicPath.toFile().exists()) {
                throw new IOException("Schematic file not found: " + filename);
            }

            return createDummySchematic();

        } catch (Exception e) {
            System.err.println("Failed to load schematic: " + filename + " - " + e.getMessage());
            return null;
        }
    }

    public boolean schematicExists(String filename) {
        Path schematicPath = getSchematicPath(filename);
        return schematicPath.toFile().exists();
    }

    private Path getSchematicPath(String filename) {
        String fullFilename = filename.endsWith(".schem") ? filename : filename + ".schem";
        return Paths.get(SCHEMATICS_DIR, fullFilename);
    }

    private SchematicData createDummySchematic() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();

        // Создаем простой куб 3x3x3 из камня
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    blocks.put(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        return new SchematicData(3, 3, 3, blocks);
    }

    public String[] getAvailableSchematics() {
        File schematicsDir = new File(SCHEMATICS_DIR);
        if (!schematicsDir.exists() || !schematicsDir.isDirectory()) {
            return new String[0];
        }

        File[] files = schematicsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".schem"));
        if (files == null) {
            return new String[0];
        }

        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName();
        }

        return names;
    }
}
