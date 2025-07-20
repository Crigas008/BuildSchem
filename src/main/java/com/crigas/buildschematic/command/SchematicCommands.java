package com.crigas.buildschematic.command;

import com.crigas.buildschematic.animation.BuildAnimation;
import com.crigas.buildschematic.animation.BuildAnimationManager;
import com.crigas.buildschematic.manager.SchematicManager;
import com.crigas.buildschematic.schematic.SchematicData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.File;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SchematicCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(SchematicCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                       CommandRegistryAccess registryAccess,
                                       CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(literal("schem")
            .requires(source -> source.hasPermissionLevel(2))
            .then(literal("list")
                .executes(SchematicCommands::listSchematics))
            .then(literal("build")
                .then(argument("filename", StringArgumentType.string())
                    .executes(ctx -> buildSchematic(ctx, 5, BuildAnimation.AnimationType.BOTTOM_TO_TOP, false, false))
                    .then(argument("speed", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> buildSchematic(ctx,
                            IntegerArgumentType.getInteger(ctx, "speed"),
                            BuildAnimation.AnimationType.BOTTOM_TO_TOP, false, false))
                        .then(argument("animation_type", StringArgumentType.string())
                            .executes(ctx -> buildSchematic(ctx,
                                IntegerArgumentType.getInteger(ctx, "speed"),
                                parseAnimationType(StringArgumentType.getString(ctx, "animation_type")), false, false))
                            .then(argument("with_falling", BoolArgumentType.bool())
                                .executes(ctx -> buildSchematic(ctx,
                                    IntegerArgumentType.getInteger(ctx, "speed"),
                                    parseAnimationType(StringArgumentType.getString(ctx, "animation_type")),
                                    BoolArgumentType.getBool(ctx, "with_falling"), false))
                            .then(argument("with_jitter", BoolArgumentType.bool())
                                .executes(ctx -> buildSchematic(ctx,
                                    IntegerArgumentType.getInteger(ctx, "speed"),
                                    parseAnimationType(StringArgumentType.getString(ctx, "animation_type")),
                                    BoolArgumentType.getBool(ctx, "with_falling"),
                                    BoolArgumentType.getBool(ctx, "with_jitter"))))))))
            .then(literal("layer")
                .then(argument("filename", StringArgumentType.string())
                    .executes(ctx -> buildSchematicLayered(ctx, 5, 20, false))
                    .then(argument("speed", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> buildSchematicLayered(ctx,
                            IntegerArgumentType.getInteger(ctx, "speed"), 20, false))
                        .then(argument("pause_ticks", IntegerArgumentType.integer(1, 200))
                            .executes(ctx -> buildSchematicLayered(ctx,
                                IntegerArgumentType.getInteger(ctx, "speed"),
                                IntegerArgumentType.getInteger(ctx, "pause_ticks"), false))
                            .then(argument("with_falling", BoolArgumentType.bool())
                                .executes(ctx -> buildSchematicLayered(ctx,
                                    IntegerArgumentType.getInteger(ctx, "speed"),
                                    IntegerArgumentType.getInteger(ctx, "pause_ticks"),
                                    BoolArgumentType.getBool(ctx, "with_falling"))))))))
            .then(literal("instant")
                .then(argument("filename", StringArgumentType.string())
                    .executes(ctx -> buildSchematicInstant(ctx, 40, false, false))
                    .then(argument("pause_ticks", IntegerArgumentType.integer(5, 200))
                        .executes(ctx -> buildSchematicInstant(ctx,
                            IntegerArgumentType.getInteger(ctx, "pause_ticks"), false, false))
                        .then(argument("with_jitter", BoolArgumentType.bool())
                            .executes(ctx -> buildSchematicInstant(ctx,
                                IntegerArgumentType.getInteger(ctx, "pause_ticks"),
                                false,
                                BoolArgumentType.getBool(ctx, "with_jitter")))
                            .then(argument("with_falling", BoolArgumentType.bool())
                                .executes(ctx -> buildSchematicInstant(ctx,
                                    IntegerArgumentType.getInteger(ctx, "pause_ticks"),
                                    BoolArgumentType.getBool(ctx, "with_falling"),
                                    BoolArgumentType.getBool(ctx, "with_jitter")))))))))
            .then(literal("stop")
                .executes(SchematicCommands::stopAnimations))
            .then(literal("pause")
                .executes(SchematicCommands::pauseAnimations))
            .then(literal("resume")
                .executes(SchematicCommands::resumeAnimations))
            .then(literal("status")
                .executes(SchematicCommands::showStatus))
        );
    }

    private static int listSchematics(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        File schematicsDir = new File("schematics");
        if (!schematicsDir.exists() || !schematicsDir.isDirectory()) {
            source.sendFeedback(() -> Text.literal("§cПапка schematics не найдена!"), false);
            return 0;
        }

        File[] files = schematicsDir.listFiles((dir, name) -> name.endsWith(".schem"));
        if (files == null || files.length == 0) {
            source.sendFeedback(() -> Text.literal("§eВ папке schematics нет файлов .schem"), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("§aДоступные схематики:"), false);
        for (File file : files) {
            String name = file.getName();
            source.sendFeedback(() -> Text.literal("§f- " + name), false);
        }

        return files.length;
    }

    private static int buildSchematic(CommandContext<ServerCommandSource> context, int speed,
                                    BuildAnimation.AnimationType type, boolean withFalling, boolean withJitter) {
        ServerCommandSource source = context.getSource();
        String filename = StringArgumentType.getString(context, "filename");

        try {
            SchematicData schematic = SchematicManager.loadSchematic(filename);
            ServerWorld world = source.getWorld();
            BlockPos pos = BlockPos.ofFloored(source.getPosition());

            BuildAnimation.AnimationType animationType = withFalling ? BuildAnimation.AnimationType.FALLING_BLOCKS : type;
            // Исправлено: используем старый конструктор без withJitter для buildSchematic
            BuildAnimation animation = new BuildAnimation(schematic, world, pos, speed, animationType);
            BuildAnimationManager.startAnimation(animation);

            String animationName = withFalling ? "с падающими блоками" : getAnimationTypeName(type);
            source.sendFeedback(() -> Text.literal(
                "§aНачато строительство схематики " + animationName + ": " + filename +
                "\n§7Скорость: " + speed + " блоков/тик" +
                "\n§7Всего блоков: " + schematic.getTotalBlocks()
            ), false);

            return 1;
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("§cОшибка при загрузке схематики: " + e.getMessage()), false);
            return 0;
        }
    }

    private static int buildSchematicLayered(CommandContext<ServerCommandSource> context, int speed,
                                           int pauseTicks, boolean withFalling) {
        ServerCommandSource source = context.getSource();
        String filename = StringArgumentType.getString(context, "filename");

        try {
            SchematicData schematic = SchematicManager.loadSchematic(filename);
            ServerWorld world = source.getWorld();
            BlockPos pos = BlockPos.ofFloored(source.getPosition());

            BuildAnimation.AnimationType animationType = withFalling ? BuildAnimation.AnimationType.FALLING_LAYERS : BuildAnimation.AnimationType.LAYER_BY_LAYER;
            BuildAnimation animation = new BuildAnimation(schematic, world, pos, speed, animationType, pauseTicks);
            BuildAnimationManager.startAnimation(animation);

            String animationName = withFalling ? "с падающими ��лоями" : "по слоям";
            source.sendFeedback(() -> Text.literal(
                "§aНачато строительство схематики " + animationName + ": " + filename +
                "\n§7Скорость: " + speed + " блоков/тик" +
                "\n§7Пауза между слоями: " + pauseTicks + " тиков" +
                "\n§7Всего блоков: " + schematic.getTotalBlocks()
            ), false);

            return 1;
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("§cОшибка при загрузке схематики: " + e.getMessage()), false);
            return 0;
        }
    }

    private static int buildSchematicInstant(CommandContext<ServerCommandSource> context, int pauseTicks, boolean withFalling, boolean withJitter) {
        ServerCommandSource source = context.getSource();
        String filename = StringArgumentType.getString(context, "filename");

        try {
            SchematicData schematic = SchematicManager.loadSchematic(filename);
            ServerWorld world = source.getWorld();
            BlockPos pos = BlockPos.ofFloored(source.getPosition());

            BuildAnimation.AnimationType animationType = withFalling ? BuildAnimation.AnimationType.FALLING_LAYERS : BuildAnimation.AnimationType.INSTANT_LAYER;
            BuildAnimation animation = new BuildAnimation(schematic, world, pos, 1, animationType, pauseTicks, withJitter);
            BuildAnimationManager.startAnimation(animation);

            String animationName = withFalling ? "мгновенно с падающими слоями" : "мгновенно по слоям";
            source.sendFeedback(() -> Text.literal(
                "§aНачато строительство схематики " + animationName + ": " + filename +
                "\n§7Пауза между слоями: " + pauseTicks + " тиков" +
                "\n§7Всего блоков: " + schematic.getTotalBlocks()
            ), false);

            return 1;
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("§cОшибка при загрузке схематики: " + e.getMessage()), false);
            return 0;
        }
    }

    private static BuildAnimation.AnimationType parseAnimationType(String type) {
        return switch (type.toLowerCase()) {
            case "top_to_bottom", "top" -> BuildAnimation.AnimationType.TOP_TO_BOTTOM;
            case "random" -> BuildAnimation.AnimationType.RANDOM;
            case "spiral" -> BuildAnimation.AnimationType.SPIRAL;
            case "layer", "layer_by_layer" -> BuildAnimation.AnimationType.LAYER_BY_LAYER;
            case "instant", "instant_layer" -> BuildAnimation.AnimationType.INSTANT_LAYER;
            case "falling", "falling_blocks" -> BuildAnimation.AnimationType.FALLING_BLOCKS;
            default -> BuildAnimation.AnimationType.BOTTOM_TO_TOP;
        };
    }

    private static String getAnimationTypeName(BuildAnimation.AnimationType type) {
        return switch (type) {
            case BOTTOM_TO_TOP -> "снизу вверх";
            case TOP_TO_BOTTOM -> "сверху вниз";
            case RANDOM -> "случайным образом";
            case SPIRAL -> "по спирали";
            case LAYER_BY_LAYER -> "по слоям";
            case INSTANT_LAYER -> "мгновенно по слоям";
            case FALLING_BLOCKS -> "с падающими блоками";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private static int stopAnimations(CommandContext<ServerCommandSource> context) {
        BuildAnimationManager.stopAllAnimations();
        context.getSource().sendFeedback(() -> Text.literal("§aВсе анимации строительства остановлены"), false);
        return 1;
    }

    private static int pauseAnimations(CommandContext<ServerCommandSource> context) {
        BuildAnimationManager.pauseAllAnimations();
        context.getSource().sendFeedback(() -> Text.literal("§eВсе анимации строительства поставлены на паузу"), false);
        return 1;
    }

    private static int resumeAnimations(CommandContext<ServerCommandSource> context) {
        BuildAnimationManager.resumeAllAnimations();
        context.getSource().sendFeedback(() -> Text.literal("§aВсе анимации строительства возобновлены"), false);
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        int activeCount = BuildAnimationManager.getActiveAnimationCount();
        if (activeCount == 0) {
            context.getSource().sendFeedback(() -> Text.literal("§eНет активных анимаций строительства"), false);
        } else {
            context.getSource().sendFeedback(() -> Text.literal("§aАктивных анимаций строительства: " + activeCount), false);
        }
        return 1;
    }
}
