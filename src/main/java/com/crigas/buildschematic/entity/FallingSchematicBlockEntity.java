package com.crigas.buildschematic.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FallingSchematicBlockEntity extends Entity {
    private static final TrackedData<BlockPos> BLOCK_POS = DataTracker.registerData(FallingSchematicBlockEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
    private static final TrackedData<BlockPos> TARGET_POS = DataTracker.registerData(FallingSchematicBlockEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);

    private BlockState blockState = Blocks.STONE.getDefaultState(); // Изменим дефолтное значение
    private int fallTime = 0;
    private BlockPos targetPos;
    private boolean shouldDrop = false;
    private int maxFallTime = 600; // 30 seconds at 20 TPS
    private double fallSpeed = 0.04;
    private boolean hasLanded = false;

    // Enhanced animation properties
    private int landingAnimationTicks = 0;
    private float blockScale = 1.0f;
    private boolean isLanding = false;

    public FallingSchematicBlockEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(false);
    }

    public FallingSchematicBlockEntity(World world, double x, double y, double z, BlockState blockState, BlockPos targetPos) {
        this(SchematicEntityTypes.FALLING_SCHEMATIC_BLOCK, world);
        this.blockState = blockState; // Теперь будет использоваться правильный блок
        this.targetPos = targetPos;
        this.setPosition(x, y, z);
        this.setVelocity(Vec3d.ZERO);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.getDataTracker().set(BLOCK_POS, new BlockPos((int)x, (int)y, (int)z));
        this.getDataTracker().set(TARGET_POS, targetPos);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(BLOCK_POS, BlockPos.ORIGIN);
        builder.add(TARGET_POS, BlockPos.ORIGIN);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Falling blocks can't be damaged
        return false;
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
            return;
        }

        // Get target position from data tracker if not set
        if (this.targetPos == null) {
            this.targetPos = this.getDataTracker().get(TARGET_POS);
        }

        // Safety check - if still null, discard the entity
        if (this.targetPos == null) {
            this.discard();
            return;
        }

        this.prevX = this.getX();
        this.prevY = this.getY();
        this.prevZ = this.getZ();

        if (isLanding) {
            tickLandingAnimation();
            return;
        }

        if (!this.hasNoGravity()) {
            this.setVelocity(this.getVelocity().add(0.0, -fallSpeed, 0.0));
        }

        this.move(net.minecraft.entity.MovementType.SELF, this.getVelocity());

        // Check if we've reached the target position or below
        if (this.getY() <= targetPos.getY() + 1.0 && !hasLanded) {
            this.startLandingAnimation();
            return;
        }

        // Apply some air resistance
        this.setVelocity(this.getVelocity().multiply(0.98));

        ++this.fallTime;

        if (this.fallTime > maxFallTime) {
            this.landBlock();
            return;
        }

        // Spawn falling particles
        if (this.getWorld().isClient) {
            spawnFallingParticles();
        }
    }

    private void startLandingAnimation() {
        if (hasLanded) return;
        hasLanded = true;
        isLanding = true;
        landingAnimationTicks = 0;
        blockScale = 0.1f; // Start very small

        // Position the entity at the exact target position
        this.setPosition(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        this.setVelocity(Vec3d.ZERO);

        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            // Place the block immediately but it will visually scale up
            serverWorld.setBlockState(targetPos, blockState, 3);

            // Enhanced landing particles
            spawnLandingParticles(serverWorld);

            // Play landing sound
            serverWorld.playSound(
                null,
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                blockState.getSoundGroup().getPlaceSound(),
                SoundCategory.BLOCKS,
                1.0f,
                0.8f + serverWorld.random.nextFloat() * 0.4f
            );
        }
    }

    private void tickLandingAnimation() {
        landingAnimationTicks++;

        // Smooth scaling animation
        float progress = Math.min(landingAnimationTicks / 10.0f, 1.0f);
        blockScale = 0.1f + progress * 0.9f;

        // Animation complete
        if (landingAnimationTicks >= 15) {
            this.discard();
        }
    }

    private void landBlock() {
        if (hasLanded) return;
        hasLanded = true;

        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            // Place the block at target position
            serverWorld.setBlockState(targetPos, blockState, 3);

            // Play landing sound
            serverWorld.playSound(
                null,
                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                blockState.getSoundGroup().getPlaceSound(),
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
            );

            // Spawn landing particles
            spawnLandingParticles(serverWorld);
        }

        this.discard();
    }

    private void spawnFallingParticles() {
        if (this.random.nextInt(4) == 0) {
            BlockStateParticleEffect particleEffect = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, blockState);
            this.getWorld().addParticle(
                particleEffect,
                this.getX(),
                this.getY(),
                this.getZ(),
                0.0, 0.0, 0.0
            );
        }
    }

    private void spawnLandingParticles(ServerWorld world) {
        Vec3d blockCenter = Vec3d.ofCenter(targetPos);
        BlockStateParticleEffect particleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState);

        for (int i = 0; i < 12; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 1.0;
            double offsetY = world.random.nextDouble() * 0.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 1.0;
            double velocityX = (world.random.nextDouble() - 0.5) * 0.3;
            double velocityY = world.random.nextDouble() * 0.2 + 0.1;
            double velocityZ = (world.random.nextDouble() - 0.5) * 0.3;

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

    // Getters for client-side rendering
    public BlockState getBlockState() {
        return blockState;
    }

    public float getBlockScale() {
        return blockScale;
    }

    public boolean isLanding() {
        return isLanding;
    }

    public void setFallSpeed(double fallSpeed) {
        this.fallSpeed = fallSpeed;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("BlockState")) {
            this.blockState = NbtHelper.toBlockState(this.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.BLOCK), nbt.getCompound("BlockState"));
        }
        this.fallTime = nbt.getInt("Time");
        this.shouldDrop = nbt.getBoolean("DropItem");
        if (nbt.contains("TargetPos")) {
            this.targetPos = NbtHelper.toBlockPos(nbt, "TargetPos").orElse(BlockPos.ORIGIN);
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("BlockState", NbtHelper.fromBlockState(blockState));
        nbt.putInt("Time", fallTime);
        nbt.putBoolean("DropItem", shouldDrop);
        if (targetPos != null) {
            nbt.put("TargetPos", NbtHelper.fromBlockPos(targetPos));
        }
    }
}
