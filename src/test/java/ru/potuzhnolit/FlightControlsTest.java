package ru.potuzhnolit;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlightControlsTest {
    @Test public void bothMachinesDescend() {
        assertEquals(-.56, FlightControls.motion(FlightControls.DOWN, 0, true).y(), 1e-9);
        assertEquals(-.336, FlightControls.motion(FlightControls.DOWN, 0, false).y(), 1e-9);
    }
    @Test public void descentWinsOverJump() {
        assertEquals(-1, FlightControls.verticalAxis(FlightControls.UP | FlightControls.DOWN));
    }
    @Test public void releasingKeysHovers() {
        assertEquals(new FlightControls.Motion(0, 0, 0), FlightControls.motion(0, 90, true));
    }
    @Test public void ascentIsPositive() {
        assertTrue(FlightControls.motion(FlightControls.UP, 0, true).y() > 0);
        assertTrue(FlightControls.motion(FlightControls.UP, 0, false).y() > 0);
    }
    @Test public void diagonalMovementDoesNotBoostSpeed() {
        FlightControls.Motion value = FlightControls.motion(FlightControls.FORWARD | FlightControls.RIGHT, 47, true);
        assertEquals(.7, Math.hypot(value.x(), value.z()), 1e-9);
    }
    @Test public void yawChangesHeadingNotAltitude() {
        FlightControls.Motion value = FlightControls.motion(FlightControls.FORWARD | FlightControls.DOWN, 90, false);
        assertEquals(-.42, value.x(), 1e-9);
        assertEquals(0, value.z(), 1e-9);
        assertTrue(value.y() < 0);
    }
    @Test public void detonationNeedsAuthorizedDrone() {
        assertTrue(FlightControls.mayDetonate(FlightControls.DETONATE, true, true));
        assertFalse(FlightControls.mayDetonate(FlightControls.DETONATE, false, true));
        assertFalse(FlightControls.mayDetonate(FlightControls.DETONATE, true, false));
        assertFalse(FlightControls.mayDetonate(FlightControls.FIRE, true, true));
    }
    @Test public void commandBitsDoNotMoveAircraft() {
        FlightControls.Motion base = FlightControls.motion(FlightControls.DOWN, 35, true);
        assertEquals(base, FlightControls.motion(FlightControls.DOWN | FlightControls.DETONATE | FlightControls.INTERACT | FlightControls.EXIT, 35, true));
    }
    @Test public void everyInputCombinationKeepsDescentAndSpeedBounds() {
        for (boolean drone : new boolean[]{true, false}) {
            for (int flags = 0; flags < 1024; flags++) {
                for (float yaw : new float[]{0, 45, 90, 179}) {
                    FlightControls.Motion m = FlightControls.motion(flags, yaw, drone);
                    assertTrue(Math.hypot(m.x(), m.z()) <= (drone ? .7 : .42) + 1e-9);
                    if ((flags & FlightControls.DOWN) != 0) assertTrue(m.y() < 0);
                }
            }
        }
    }
}
