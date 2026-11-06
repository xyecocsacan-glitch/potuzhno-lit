package ru.potuzhnolit;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlightTuningTest {
    @Test public void accelerationIsGradualAndMonotone() {
        double speed = 0;
        for (int i = 0; i < 30; i++) {
            double next = FlightTuning.velocity(speed, .7);
            assertTrue(next >= speed && next <= .7);
            if (i == 0) assertTrue(next < .4);
            speed = next;
        }
        assertEquals(.7, speed, .00001);
    }
    @Test public void brakesReachExactZero() {
        double speed = .7;
        for (int i = 0; i < 20; i++) {
            double next = FlightTuning.velocity(speed, 0);
            assertTrue(next <= speed && next >= 0);
            speed = next;
        }
        assertEquals(0, speed, 0);
    }
    @Test public void descentIsSmoothedNotDisabled() {
        double speed = FlightTuning.velocity(0, -.56);
        assertTrue(speed < 0 && speed > -.56);
        for (int i = 0; i < 30; i++) speed = FlightTuning.velocity(speed, -.56);
        assertEquals(-.56, speed, .00001);
    }
    @Test public void reversalDoesNotJumpToOppositeSpeed() {
        double speed = FlightTuning.velocity(.7, -.7);
        assertTrue(speed > 0 && speed < .7);
        for (int i = 0; i < 30; i++) speed = FlightTuning.velocity(speed, -.7);
        assertEquals(-.7, speed, .00001);
    }
    @Test public void invalidVelocityStopsSafely() {
        assertEquals(0, FlightTuning.velocity(Double.NaN, .7), 0);
        assertEquals(0, FlightTuning.velocity(.7, Double.POSITIVE_INFINITY), 0);
    }
    @Test public void turnTakesShortestPathAcrossNorth() {
        assertEquals(179.7f, FlightTuning.turn(179, -179), .0001f);
        assertEquals(-179.7f, FlightTuning.turn(-179, 179), .0001f);
    }
    @Test public void turnConvergesWithoutOvershooting() {
        float yaw = 0;
        for (int i = 0; i < 40; i++) {
            float next = FlightTuning.turn(yaw, 90);
            assertTrue(next >= yaw && next <= 90);
            yaw = next;
        }
        assertEquals(90, yaw, .0001f);
    }
    @Test public void planeCameraIsRaisedAndBehind() {
        FlightTuning.Offset offset = FlightTuning.cameraOffset(false, 0);
        assertEquals(0, offset.x(), .00001);
        assertTrue(offset.y() > 2);
        assertTrue(offset.z() < -2);
    }
    @Test public void cameraOffsetRotatesWithHull() {
        FlightTuning.Offset offset = FlightTuning.cameraOffset(false, 90);
        assertEquals(FlightTuning.PLANE_CAMERA_BACK, offset.x(), .00001);
        assertEquals(0, offset.z(), .00001);
    }
    @Test public void droneViewStaysAheadOfCameraLens() {
        FlightTuning.Offset offset = FlightTuning.cameraOffset(true, 0);
        assertEquals(.65, offset.y(), .00001);
        assertTrue(offset.z() > 0);
    }
    @Test public void cameraDistanceDoesNotVaryWithYaw() {
        for (int yaw = -360; yaw <= 360; yaw++) {
            FlightTuning.Offset offset = FlightTuning.cameraOffset(false, yaw);
            assertEquals(FlightTuning.PLANE_CAMERA_BACK, Math.hypot(offset.x(), offset.z()), .00001);
        }
    }
}
