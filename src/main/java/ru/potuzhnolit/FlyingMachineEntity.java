package ru.potuzhnolit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.projectile.ProjectileUtil;

public class FlyingMachineEntity extends Entity {
    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(FlyingMachineEntity.class, EntityDataSerializers.INT);
    private int controls, controlsTick, armedTicks, gunCooldown;
    private final java.util.List<Display.BlockDisplay> visualParts = new java.util.ArrayList<>();

    public FlyingMachineEntity(EntityType<? extends FlyingMachineEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VARIANT, 0);
    }

    public void setVariant(int variant) { entityData.set(VARIANT, variant == 1 ? 1 : 0); }
    public int getVariant() { return entityData.get(VARIANT); }
    public void applyControls(int bits) { controls = bits & 127; controlsTick = tickCount; }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setVariant(input.getIntOr("Variant", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Variant", getVariant());
    }

    @Override
    public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        if (isRemoved() || amount <= 0.0f || isInvulnerableToBase(source)) {
            return false;
        }
        discard();
        return true;
    }

    @Override public boolean isPickable() { return true; }
    @Override public boolean canCollideWith(Entity entity) { return entity.isPickable() && !hasPassenger(entity); }
    @Override protected boolean canAddPassenger(Entity entity) { return getPassengers().isEmpty(); }
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scaleFactor) {
        return new Vec3(0, 0.38, 0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide() && getPassengers().isEmpty()) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        updateVisualParts();
        Entity pilot = getFirstPassenger();
        if (pilot == null || tickCount - controlsTick > 10) controls = 0;
        if (pilot != null) {
            setYRot(pilot.getYRot());
            double yaw = Math.toRadians(getYRot());
            Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            double f = ((controls & 1) != 0 ? 1 : 0) - ((controls & 2) != 0 ? 1 : 0);
            double r = ((controls & 8) != 0 ? 1 : 0) - ((controls & 4) != 0 ? 1 : 0);
            double up = ((controls & 16) != 0 ? 1 : 0) - ((controls & 32) != 0 ? 1 : 0);
            Vec3 horizontal = forward.scale(f).add(right.scale(r));
            if (horizontal.lengthSqr() > 1) horizontal = horizontal.normalize();
            double speed = getVariant() == 0 ? 0.7 : 0.42;
            setDeltaMovement(horizontal.x * speed, up * speed * .8, horizontal.z * speed);
            armedTicks++;
        } else {
            setDeltaMovement(getDeltaMovement().scale(.65));
        }

        Vec3 velocity = getDeltaMovement();
        HitResult blockHit = level().clip(new ClipContext(position(), position().add(velocity),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockImpact = blockHit.getType() == HitResult.Type.BLOCK;
        AABB search = getBoundingBox().expandTowards(velocity).inflate(.15);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, position(),
                position().add(velocity), search,
                e -> e.isPickable() && e != pilot && !hasPassenger(e), 0.3f);
        if (getVariant() == 0 && pilot != null && armedTicks > 5
                && velocity.lengthSqr() > .03 && (blockImpact || entityHit != null)) {
            explode();
            return;
        }
        if (getVariant() == 1 && blockImpact) setDeltaMovement(Vec3.ZERO);
        move(MoverType.SELF, getDeltaMovement());
        if (getVariant() == 0 && pilot != null && armedTicks > 5
                && velocity.lengthSqr() > .03 && (horizontalCollision || verticalCollision)) {
            explode();
            return;
        }
        updateVisualParts();
        if (getVariant() == 1 && pilot != null && (controls & 64) != 0 && gunCooldown == 0) {
            shoot(pilot);
            gunCooldown = 5;
        }
        if (gunCooldown > 0) gunCooldown--;
    }

    // The visible hull uses vanilla block-display entities, so no custom model is needed.
    private void updateVisualParts() {
        if (visualParts.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                Display.BlockDisplay part = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level());
                part.setBlockState(i == 0 ? Blocks.BLUE_CONCRETE.defaultBlockState()
                    : i == 1 ? Blocks.YELLOW_CONCRETE.defaultBlockState()
                    : Blocks.IRON_BLOCK.defaultBlockState());
                level().addFreshEntity(part);
                visualParts.add(part);
            }
        }
        double[][] offsets = {
            {-.55, .2, -.45}, {-.55, -.25, -.45},
            {-.95, -.3, -.8}, {-.95, -.3, .35}
        };
        double yaw = Math.toRadians(getYRot());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        for (int i = 0; i < visualParts.size(); i++) {
            Display.BlockDisplay part = visualParts.get(i);
            double localX = offsets[i][0], localZ = offsets[i][2];
            part.setPos(getX() + localX * cos - localZ * sin,
                        getY() + offsets[i][1],
                        getZ() + localX * sin + localZ * cos);
            part.setYRot(getYRot());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        for (Display.BlockDisplay display : visualParts) display.discard();
        visualParts.clear();
        super.remove(reason);
    }

    private void shoot(Entity pilot) {
        Vec3 start = position().add(0, .7, 0);
        Vec3 end = start.add(pilot.getLookAngle().scale(36));
        HitResult blockHit = level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) end = blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, end,
            getBoundingBox().expandTowards(end.subtract(start)).inflate(1.5),
            e -> e instanceof LivingEntity && e != pilot && e.isAlive(), 0.3f);
        if (entityHit != null && level() instanceof ServerLevel server) {
            entityHit.getEntity().hurtServer(server, damageSources().generic(), 4.0f);
            end = entityHit.getLocation();
        }
        playSound(SoundEvents.FIREWORK_ROCKET_BLAST, .3f, 1.8f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, .12, .12, .12, .02);
        }
    }

    private void explode() {
        level().explode(this, getX(), getY(), getZ(), 3.0f, Level.ExplosionInteraction.TNT);
        discard();
    }
}
