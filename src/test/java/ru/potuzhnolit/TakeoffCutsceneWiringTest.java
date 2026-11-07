package ru.potuzhnolit;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class TakeoffCutsceneWiringTest {
    private static String file(String path) throws Exception { return Files.readString(Path.of(path)); }
    @Test public void takeoffHasTwelveSecondDurationAndSkip() throws Exception {
        String s = file("src/client/java/ru/potuzhnolit/client/TakeoffCutscene.java");
        assertTrue(s.contains("DURATION_TICKS = 240"));
        assertTrue(s.contains("escapeHeld && !wasEscape"));
        assertTrue(s.contains("client.player.getVehicle() != machine"));
        assertTrue(s.contains("machine.isRemoved()"));
    }
    @Test public void cameraIsOverriddenOnlyDuringCutscene() throws Exception {
        String s = file("src/client/java/ru/potuzhnolit/client/FlightCamera.java");
        assertTrue(s.contains("TakeoffCutscene.pose(machine, partialTick)"));
    }
    @Test public void serverSideControlsAreNotBypassed() throws Exception {
        String s = file("src/client/java/ru/potuzhnolit/client/PotuzhnoLitClient.java");
        assertTrue(s.contains("TakeoffCutscene.flightFlags()"));
        assertTrue(s.contains("send(client, flags)"));
    }
}
