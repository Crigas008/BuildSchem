package com.crigas.buildschematic.manager;

import com.crigas.buildschematic.BuildSchematic;
import com.crigas.buildschematic.schematic.SchematicData;
import com.crigas.buildschematic.schematic.SchematicParser;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class SchematicManager {
    private static final Map<String, SchematicData> loadedSchematics = new HashMap<>();

    public static SchematicData loadSchematic(String fileName) throws IOException {
        if (loadedSchematics.containsKey(fileName)) {
            return loadedSchematics.get(fileName);
        }

        File schematicFile = new File("schematics/" + fileName);
        if (!schematicFile.exists()) {
            throw new IOException("Schematic file not found: " + fileName);
        }

        SchematicData data = SchematicParser.parseSchematic(schematicFile);
        loadedSchematics.put(fileName, data);

        BuildSchematic.LOGGER.info("Loaded schematic: {} ({} blocks)", fileName, data.getTotalBlocks());
        return data;
    }

}
