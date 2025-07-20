package com.crigas.buildschematic.entity;

import com.crigas.buildschematic.BuildSchematic;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class SchematicEntityTypes {
    public static final RegistryKey<EntityType<?>> FALLING_SCHEMATIC_BLOCK_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE,
        Identifier.of(BuildSchematic.MOD_ID, "falling_schematic_block")
    );

    public static final EntityType<FallingSchematicBlockEntity> FALLING_SCHEMATIC_BLOCK = Registry.register(
        Registries.ENTITY_TYPE,
        FALLING_SCHEMATIC_BLOCK_KEY.getValue(),
        EntityType.Builder.<FallingSchematicBlockEntity>create(FallingSchematicBlockEntity::new, SpawnGroup.MISC)
            .makeFireImmune()
            .dimensions(0.98f, 0.98f)
            .maxTrackingRange(10)
            .trackingTickInterval(20)
            .build(FALLING_SCHEMATIC_BLOCK_KEY)
    );
    
    public static void register() {
        // Registration happens in static initializer
    }
}
