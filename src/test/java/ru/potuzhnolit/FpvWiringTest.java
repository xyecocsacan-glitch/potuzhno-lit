package ru.potuzhnolit;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

/** These source-wiring checks supplement the numerical tests; they do not simulate gameplay. */
public class FpvWiringTest {
    private String read(String path) throws Exception { return Files.readString(Path.of(path)); }
    @Test public void cameraAndHandHooksAreRequired() throws Exception {
        String config = read("src/main/resources/potuzhnolit.client.mixins.json");
        assertTrue(config.contains("FlightHandMixin"));
        assertTrue(config.contains("FlightMouseMixin"));
        String hand = read("src/client/java/ru/potuzhnolit/client/mixin/FlightHandMixin.java");
        assertTrue(hand.contains("if (PotuzhnoLitClient.remote()) ci.cancel()"));
    }
    @Test public void packetUsesIndependentAim() throws Exception {
        String client = read("src/client/java/ru/potuzhnolit/client/PotuzhnoLitClient.java");
        assertTrue(client.contains("new ControlPayload(flags, FlightCamera.yaw(client), FlightCamera.pitch(client))"));
        String server = read("src/main/java/ru/potuzhnolit/FlyingMachineEntity.java");
        assertTrue(server.contains("FlightGeometry.cameraPosition(this, position(), viewYaw, false)"));
        assertTrue(server.contains("BeamTrace.extendThrough"));
        assertTrue(server.contains("hit.target().hurtServer(server, source, 4)"));
    }
    @Test public void leavingVehiclesRestoresVanillaMouseControl() throws Exception {
        String mouse = read("src/client/java/ru/potuzhnolit/client/mixin/FlightMouseMixin.java");
        assertTrue(mouse.contains("if (!FlightCamera.turn(dx, dy)) player.turn(dx, dy)"));
        String camera = read("src/client/java/ru/potuzhnolit/client/FlightCamera.java");
        assertTrue(camera.contains("aimMachine = null"));
    }
}
