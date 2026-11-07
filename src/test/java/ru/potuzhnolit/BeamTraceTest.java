package ru.potuzhnolit;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression tests against Minecraft's actual AABB clip implementation, not a mock. */
public class BeamTraceTest {
    private final AABB target = new AABB(-.5, -.5, 10, .5, .5, 11);
    @Test public void reproducesOldEndpointMissAndFixesIt() {
        Vec3 start = Vec3.ZERO, entry = new Vec3(0, 0, 10);
        assertTrue(target.clip(start, entry).isEmpty());
        Vec3 end = BeamTrace.extendThrough(start, entry, 36);
        assertTrue(BeamTrace.intersect(target, start, end).isPresent());
        assertEquals(10, BeamTrace.intersect(target, start, end).get().z, 1e-7);
    }
    @Test public void offsetMuzzleHitsCameraSelectedSurface() {
        Vec3 muzzle = new Vec3(.65, .7, 1.45);
        Vec3 entry = new Vec3(0, 1, 10);
        AABB mob = new AABB(-.4, 0, 10, .4, 1.8, 10.8);
        assertTrue(BeamTrace.intersect(mob, muzzle, BeamTrace.extendThrough(muzzle, entry, 36)).isPresent());
    }
    @Test public void startInsideEntityIsAHit() {
        Vec3 start = new Vec3(0, 0, 10.5);
        assertEquals(start, BeamTrace.intersect(target, start, new Vec3(0, 0, 20)).get());
    }
    @Test public void clippingAtWallCannotReachEntityBehindIt() {
        Vec3 beforeWall = new Vec3(0, 0, 9);
        assertTrue(BeamTrace.intersect(target, Vec3.ZERO, beforeWall).isEmpty());
    }
    @Test public void offAxisRayDoesNotDamageTarget() {
        assertTrue(BeamTrace.intersect(target, Vec3.ZERO, new Vec3(8, 0, 11)).isEmpty());
    }
    @Test public void rangeIsNotExtendedPastLimit() {
        Vec3 end = BeamTrace.extendThrough(Vec3.ZERO, new Vec3(0, 0, 50), 36);
        assertEquals(36, end.length(), 1e-7);
    }
    @Test public void zeroLengthAimIsSafe() {
        assertEquals(Vec3.ZERO, BeamTrace.extendThrough(Vec3.ZERO, Vec3.ZERO, 36));
    }
}
