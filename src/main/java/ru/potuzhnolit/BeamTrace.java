package ru.potuzhnolit;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server raycast used for both selecting the camera aim point and applying the shot. */
public final class BeamTrace {
    private BeamTrace() { }
    public record Hit(Vec3 position, LivingEntity target) { }

    public static Vec3 extendThrough(Vec3 muzzle, Vec3 aim, double maxRange) {
        Vec3 path = aim.subtract(muzzle);
        double distance = path.length();
        if (!Double.isFinite(distance) || distance < 1.0e-8 || maxRange <= 0) return muzzle;
        // AABB.clip excludes an entry point exactly at the segment endpoint. Cross it instead.
        return muzzle.add(path.scale(Math.min(maxRange, distance + 1.0) / distance));
    }

    public static Optional<Vec3> intersect(AABB box, Vec3 start, Vec3 end) {
        return box.contains(start) ? Optional.of(start) : box.clip(start, end);
    }

    public static Hit cast(Level level, Entity vehicle, Entity pilot, Vec3 start, Vec3 end) {
        HitResult wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, vehicle));
        Vec3 limit = wall.getType() == HitResult.Type.BLOCK ? wall.getLocation() : end;
        double nearest = start.distanceToSqr(limit);
        LivingEntity target = null;
        Vec3 point = limit;
        for (Entity candidate : level.getEntities(vehicle, new AABB(start, limit).inflate(.1),
                e -> e instanceof LivingEntity && e != pilot && e.isAlive() && !e.isSpectator())) {
            Optional<Vec3> intersection = intersect(candidate.getBoundingBox(), start, limit);
            if (intersection.isEmpty()) continue;
            double distance = start.distanceToSqr(intersection.get());
            if (distance < nearest) {
                nearest = distance;
                point = intersection.get();
                target = (LivingEntity) candidate;
            }
        }
        return new Hit(point, target);
    }
}
