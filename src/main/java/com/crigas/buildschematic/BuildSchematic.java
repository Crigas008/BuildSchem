package com.crigas.buildschematic;

import com.crigas.buildschematic.command.SchematicCommands;
import com.crigas.buildschematic.entity.SchematicEntityTypes;
import com.crigas.buildschematic.manager.SchematicManager;
import com.crigas.buildschematic.animation.BuildAnimationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildSchematic implements ModInitializer {
    public static final String MOD_ID = "buidlshematic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing BuildSchematic Mod");

        SchematicEntityTypes.register();

        SchematicCommands.register();

        ServerTickEvents.END_SERVER_TICK.register(BuildAnimationManager::tick);

        LOGGER.info("BuildSchematic Mod Initialized Successfully");
    }
}
