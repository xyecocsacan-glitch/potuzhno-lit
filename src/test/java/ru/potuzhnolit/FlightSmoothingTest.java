package ru.potuzhnolit;

import org.junit.Test;
import static org.junit.Assert.*;
import ru.potuzhnolit.FlightControls.Motion;

public class FlightSmoothingTest {
    private static final Motion ZERO = new Motion(0, 0, 0);
    private static double length(Motion a) { return Math.sqrt(a.x()*a.x()+a.y()*a.y()+a.z()*a.z()); }
    private static double delta(Motion a, Motion b) { return length(new Motion(a.x()-b.x(),a.y()-b.y(),a.z()-b.z())); }
    @Test public void startsGraduallyForBothVehicles() {
        for (boolean drone : new boolean[]{true,false}) {
            Motion target = FlightControls.motion(FlightControls.FORWARD,0,drone);
            Motion first = FlightSmoothing.accelerate(ZERO,target,drone);
            assertTrue(first.z()>0); assertTrue(first.z()<target.z());
            assertTrue(length(first)<=(drone?.18:.12)+1e-9);
        }
    }
    @Test public void reachesRequestedSpeedWithoutOvershoot() {
        for (boolean drone : new boolean[]{true,false}) {
            Motion target = FlightControls.motion(FlightControls.FORWARD|FlightControls.UP,37,drone);
            Motion value=ZERO;
            for(int i=0;i<20;i++) {
                Motion next=FlightSmoothing.accelerate(value,target,drone);
                assertTrue(length(next)<=length(target)+1e-9);
                assertTrue(delta(next,value)<=(drone?.18:.12)+1e-9);
                value=next;
            }
            assertEquals(target,value);
        }
    }
    @Test public void stopsExactlyWithoutResidualDrift() {
        for(boolean drone:new boolean[]{true,false}) {
            Motion value=FlightControls.motion(FlightControls.FORWARD|FlightControls.UP,0,drone);
            for(int i=0;i<8;i++) {
                Motion next=FlightSmoothing.accelerate(value,ZERO,drone);
                assertTrue(length(next)<=length(value)+1e-9); value=next;
            }
            assertEquals(ZERO,value);
        }
    }
    @Test public void reversalDoesNotJumpFromFullForwardToFullBackward() {
        Motion forward=FlightControls.motion(FlightControls.FORWARD,0,true);
        Motion back=FlightControls.motion(FlightControls.BACK,0,true);
        Motion next=FlightSmoothing.accelerate(forward,back,true);
        assertTrue(next.z()>0); assertTrue(delta(forward,next)<=.18000001);
        for(int i=0;i<16;i++) next=FlightSmoothing.accelerate(next,back,true);
        assertEquals(back,next);
    }
    @Test public void descentWorksAfterAscendingAndWhenJumpIsHeld() {
        for(boolean drone:new boolean[]{true,false}) {
            Motion value=FlightControls.motion(FlightControls.UP,0,drone);
            Motion target=FlightControls.motion(FlightControls.UP|FlightControls.DOWN,0,drone);
            for(int i=0;i<12;i++) value=FlightSmoothing.accelerate(value,target,drone);
            assertTrue(value.y()<0); assertEquals(target,value);
        }
    }
    @Test public void diagonalInputHasBoundedVectorAcceleration() {
        Motion target=FlightControls.motion(FlightControls.FORWARD|FlightControls.RIGHT|FlightControls.UP,45,true);
        assertEquals(.18,length(FlightSmoothing.accelerate(ZERO,target,true)),1e-9);
    }
    @Test public void invalidVelocityCannotPropagateNaN() {
        assertEquals(ZERO,FlightSmoothing.accelerate(ZERO,new Motion(Double.NaN,0,0),true));
        assertTrue(Double.isFinite(FlightSmoothing.accelerate(new Motion(Double.POSITIVE_INFINITY,0,0),new Motion(0,0,.7),true).x()));
    }
    @Test public void rotorChangesSpeedWithoutJumpingPhase() {
        FlightSmoothing.Rotor rotor=new FlightSmoothing.Rotor();
        float previous=rotor.sample(1);
        for(int i=0;i<150;i++) {
            rotor.tick(i>=20&&i<90);
            double difference=rotor.sample(1)-previous;
            difference=(difference+Math.PI*4)%(Math.PI*2);
            assertEquals(rotor.speed(),difference,1e-5);
            assertEquals(rotor.speed()/2,rotor.sample(1)-rotor.sample(.5f),1e-5);
            previous=rotor.sample(1);
        }
    }
    @Test public void rotorPhaseStaysBoundedDuringLongFlight() {
        FlightSmoothing.Rotor rotor=new FlightSmoothing.Rotor();
        for(int i=0;i<100000;i++) rotor.tick(true);
        assertTrue(rotor.sample(1)>=0&&rotor.sample(1)<Math.PI*2);
        assertEquals(2.4,rotor.speed(),1e-9);
    }
    @Test public void animationClampsInvalidFrameFractions() {
        assertEquals(0f,FlightSmoothing.partial(Float.NaN),0);
        assertEquals(0f,FlightSmoothing.partial(-1),0);
        assertEquals(1f,FlightSmoothing.partial(2),0);
        assertEquals(.5f,FlightSmoothing.partial(.5f),0);
    }
}
