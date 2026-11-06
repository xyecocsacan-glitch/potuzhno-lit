package ru.potuzhnolit;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FlyingMachineEntity extends Entity {
    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(FlyingMachineEntity.class, EntityDataSerializers.INT);
    private int controls, controlsTick, armedTicks, gunCooldown;
    private boolean rightGun, exploding, clearedLegacyDisplays;
    UUID remoteOwner;

    public FlyingMachineEntity(EntityType<? extends FlyingMachineEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
        setNoGravity(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(VARIANT, 0); }
    public void setVariant(int variant) { entityData.set(VARIANT, variant == 1 ? 1 : 0); }
    public int getVariant() { return entityData.get(VARIANT); }
    public boolean available() { return !isRemoved() && remoteOwner == null && getPassengers().isEmpty(); }
    public void applyControls(int bits, float yaw, float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
        controls = bits & FlightControls.MOVEMENT_MASK;
        controlsTick = tickCount;
        setYRot(Mth.wrapDegrees(yaw));
        setXRot(Mth.clamp(pitch, -85, 85));
    }
    @Override protected void readAdditionalSaveData(ValueInput input) { setVariant(input.getIntOr("Variant", 0)); }
    @Override protected void addAdditionalSaveData(ValueOutput output) { output.putInt("Variant", getVariant()); }
    @Override public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        if (isRemoved() || exploding || amount <= 0 || isInvulnerableToBase(source)) return false;
        discard();
        return true;
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean canCollideWith(Entity entity) {
        return !(entity instanceof Display) && entity.isPickable() && !hasPassenger(entity)
            && (remoteOwner == null || !entity.getUUID().equals(remoteOwner));
    }
    @Override protected boolean canAddPassenger(Entity entity) { return getVariant() == 1 && getPassengers().isEmpty(); }
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scaleFactor) {
        return new Vec3(0, 0.38, 0);
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) RemoteControl.use(serverPlayer, this);
        return InteractionResult.SUCCESS;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (!clearedLegacyDisplays || tickCount == 20) {
            removeLegacyDisplays();
            clearedLegacyDisplays = true;
        }
        Entity pilot = getVariant() == 0 ? RemoteControl.pilot(this) : getFirstPassenger();
        if (pilot == null || tickCount - controlsTick > 10) controls = 0;
        if (pilot != null) {
            FlightControls.Motion motion = FlightControls.motion(controls, getYRot(), getVariant() == 0);
            setDeltaMovement(motion.x(), motion.y(), motion.z());
            armedTicks++;
        } else {
            setDeltaMovement(Vec3.ZERO);
            armedTicks = 0;
        }
        if (getVariant() == 0 && pilot != null && !RemoteControl.withinSignal(this, position().add(getDeltaMovement()))) {
            setDeltaMovement(Vec3.ZERO);
        }
        Vec3 velocity = getDeltaMovement();
        if (getVariant() == 0 && pilot != null && armedTicks > 5 && velocity.lengthSqr() > .03) {
            HitResult blockHit = level().clip(new ClipContext(position().add(0, .45, 0),
                position().add(velocity).add(0, .45, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            AABB search = getBoundingBox().expandTowards(velocity).inflate(.15);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this,
                position().add(0, .45, 0), position().add(velocity).add(0, .45, 0), search,
                e -> !(e instanceof Display) && e.isPickable() && e != pilot && !hasPassenger(e), .3f);
            if (blockHit.getType() == HitResult.Type.BLOCK || entityHit != null) { detonate(); return; }
        }
        // Preserve the 0.3.0 descent fix: let collision resolution move the vehicle.
        move(MoverType.SELF, velocity);
        if (getVariant() == 0 && pilot != null && armedTicks > 5 && velocity.lengthSqr() > .03
                && (horizontalCollision || verticalCollision)) { detonate(); return; }
        if (gunCooldown > 0) gunCooldown--;
        if (getVariant() == 1 && pilot != null && (controls & FlightControls.FIRE) != 0 && gunCooldown == 0) {
            shoot(pilot);
            gunCooldown = 5;
        }
    }

    /** Migration: remove only displays tagged with THIS vehicle's UUID in 0.3.0. */
    private void removeLegacyDisplays() {
        String tag = "potuzhnolit_part_" + getUUID();
        for (Display.BlockDisplay old : level().getEntitiesOfClass(Display.BlockDisplay.class,
                getBoundingBox().inflate(5), e -> e.getTags().contains(tag))) old.discard();
    }

    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide()) RemoteControl.removed(this);
        super.remove(reason);
    }

    private void shoot(Entity pilot) {
        if (!(level() instanceof ServerLevel server)) return;
        double yaw = Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        rightGun = !rightGun;
        Vec3 start = position().add(0, .7, 0).add(forward.scale(1.45)).add(right.scale(rightGun ? .65 : -.65));
        Vec3 end = start.add(getLookAngle().scale(36));
        HitResult blockHit = level().clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) end = blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, start, end,
            new AABB(start, end).inflate(1), e -> e instanceof LivingEntity && e != pilot && e.isAlive(), .3f);
        if (entityHit != null) {
            entityHit.getEntity().hurtServer(server, damageSources().generic(), 4);
            end = entityHit.getLocation();
            server.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, .08, .08, .08, .01);
        }
        playSound(SoundEvents.FIREWORK_ROCKET_BLAST, .45f, 1.8f);
        server.sendParticles(ParticleTypes.FLAME, start.x, start.y, start.z, 2, .02, .02, .02, 0);
        Vec3 path = end.subtract(start);
        int steps = Math.max(1, Math.min(18, (int) Math.ceil(path.length() / 2)));
        for (int i = 1; i <= steps; i++) {
            Vec3 point = start.add(path.scale((double) i / steps));
            server.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    public void detonate() {
        if (level().isClientSide() || isRemoved() || exploding || getVariant() != 0) return;
        exploding = true;
        RemoteControl.removed(this);
        level().explode(this, getX(), getY(), getZ(), 3, Level.ExplosionInteraction.TNT);
        discard();
    }
}
