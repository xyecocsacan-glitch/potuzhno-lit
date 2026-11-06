package ru.potuzhnolit;

import org.junit.Test;
import static org.junit.Assert.*;

public class AimAnglesTest {
    @Test public void horizontalAndVerticalMouseInputWorkIndependently() {
        AimAngles aim = new AimAngles();
        aim.reset(30, -5);
        aim.turn(100, -100);
        assertEquals(45, aim.yaw(), 1e-6);
        assertEquals(-20, aim.pitch(), 1e-6);
    }
    @Test public void yawWrapsWithoutStoppingAtBodyAngle() {
        AimAngles aim = new AimAngles();
        aim.reset(179, 0); aim.turn(20, 0);
        assertEquals(-178, aim.yaw(), 1e-6);
    }
    @Test public void pitchHasSafeLimits() {
        AimAngles aim = new AimAngles();
        aim.turn(0, 10000); assertEquals(85, aim.pitch(), 0);
        aim.turn(0, -20000); assertEquals(-85, aim.pitch(), 0);
    }
    @Test public void invalidMouseInputDoesNotCorruptView() {
        AimAngles aim = new AimAngles();
        aim.reset(17, 23); aim.turn(Double.NaN, 8); aim.turn(3, Double.POSITIVE_INFINITY);
        assertEquals(17, aim.yaw(), 0); assertEquals(23, aim.pitch(), 0);
    }
    @Test public void sessionResetDoesNotKeepPreviousVehicleView() {
        AimAngles aim = new AimAngles();
        aim.turn(1000, 1000); aim.reset(-20, 7);
        assertEquals(-20, aim.yaw(), 0); assertEquals(7, aim.pitch(), 0);
    }
    @Test public void cumulativeMouseInputDoesNotDependOnTickRate() {
        AimAngles a = new AimAngles(), b = new AimAngles();
        a.turn(60, 30);
        for (int i = 0; i < 60; i++) b.turn(1, .5);
        assertEquals(a.yaw(), b.yaw(), 1e-4); assertEquals(a.pitch(), b.pitch(), 1e-4);
    }
    @Test public void droneRangeIsDoubledAndBounded() {
        assertEquals(128, FlightControls.REMOTE_RANGE, 0);
        assertTrue(FlightControls.REMOTE_RANGE > 64);
    }
}
