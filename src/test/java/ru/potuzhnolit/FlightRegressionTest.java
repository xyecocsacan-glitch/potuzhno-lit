package ru.potuzhnolit;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

/** Source/resource wiring checks; these are not a substitute for a live gameplay test. */
public class FlightRegressionTest {
    private String read(String path) throws Exception { return Files.readString(Path.of(path)); }
    @Test public void cameraMixinIsClientOnlyAndRequired() throws Exception {
        String metadata = read("src/main/resources/fabric.mod.json");
        assertTrue(metadata.contains("potuzhnolit.client.mixins.json"));
        assertTrue(metadata.contains("\"environment\": \"client\""));
        String mixin = read("src/main/resources/potuzhnolit.client.mixins.json");
        assertTrue(mixin.contains("FlightCameraMixin"));
        assertTrue(mixin.contains("\"defaultRequire\": 1"));
    }
    @Test public void randomEventsAndBindingsAreRemoved() throws Exception {
        String hud = read("src/client/java/ru/potuzhnolit/client/FlightHud.java");
        assertFalse(hud.contains("playLocalSound"));
        assertFalse(hud.contains("ParticleTypes.NOTE"));
        assertFalse(hud.contains("untilJoke"));
        String client = read("src/client/java/ru/potuzhnolit/client/PotuzhnoLitClient.java");
        assertFalse(client.contains("JOKES"));
        for (String language : new String[]{"en_us", "ru_ru"}) {
            String text = read("src/main/resources/assets/potuzhnolit/lang/" + language + ".json");
            assertFalse(text.contains("joke.potuzhnolit"));
            assertFalse(text.contains("key.potuzhnolit.jokes"));
        }
    }
    @Test public void freeAimAndExplosionAreWired() throws Exception {
        String client = read("src/client/java/ru/potuzhnolit/client/PotuzhnoLitClient.java");
        String entity = read("src/main/java/ru/potuzhnolit/FlyingMachineEntity.java");
        String camera = read("src/client/java/ru/potuzhnolit/client/FlightCamera.java");
        assertTrue(client.contains("mouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT)"));
        assertTrue(client.contains("flags |= FlightControls.AIM_MODE"));
        assertTrue(entity.contains("FlightControls.freeAim(controls, getVariant() == 1)"));
        assertTrue(entity.contains("FlightTuning.DRONE_EXPLOSION_POWER"));
        assertTrue(camera.contains("wasPlaneAiming && !aiming"));
        assertEquals(5.0f, FlightTuning.DRONE_EXPLOSION_POWER, 0);
    }
    @Test public void interpolationAndSharedAimGeometryRemainWired() throws Exception {
        String entity = read("src/main/java/ru/potuzhnolit/FlyingMachineEntity.java");
        assertTrue(entity.contains("getInterpolation()"));
        assertTrue(entity.contains("interpolation.interpolate()"));
        assertTrue(entity.contains("FlightGeometry.cameraPosition"));
        String camera = read("src/client/java/ru/potuzhnolit/client/FlightCamera.java");
        assertTrue(camera.contains("machine.getPosition(partialTick)"));
        assertTrue(camera.contains("FlightGeometry.cameraPosition"));
    }
}
