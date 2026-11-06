package ru.potuzhnolit;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The server and client must use the same camera origin for gun convergence. */
public final class FlightGeometry {
    private FlightGeometry() { }

    public static Vec3 cameraPosition(FlyingMachineEntity machine, Vec3 position, float yaw, boolean drone) {
        FlightTuning.Offset offset = FlightTuning.cameraOffset(drone, yaw);
        Vec3 desired = position.add(offset.x(), offset.y(), offset.z());
        Vec3 anchor = position.add(0, .65, 0);
        HitResult obstacle = machine.level().clip(new ClipContext(anchor, desired,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, machine));
        if (obstacle.getType() == HitResult.Type.BLOCK) {
            Vec3 path = desired.subtract(anchor);
            double distance = Math.max(0, obstacle.getLocation().distanceTo(anchor) - .2);
            return anchor.add(path.normalize().scale(distance));
        }
        return desired;
    }
}
