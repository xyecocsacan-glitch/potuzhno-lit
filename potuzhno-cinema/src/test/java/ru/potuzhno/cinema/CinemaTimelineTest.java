package ru.potuzhno.cinema;

import org.junit.Test;
import static org.junit.Assert.*;

public class CinemaTimelineTest {
    @Test public void cutsceneDuration() {
        assertFalse(CinemaTimeline.finished(0));
        assertFalse(CinemaTimeline.finished(239));
        assertTrue(CinemaTimeline.finished(240));
    }
    @Test public void allThreeCaptions() {
        assertEquals(0, CinemaTimeline.cue(0));
        assertEquals(0, CinemaTimeline.cue(69));
        assertEquals(1, CinemaTimeline.cue(70));
        assertEquals(1, CinemaTimeline.cue(159));
        assertEquals(2, CinemaTimeline.cue(160));
    }
    @Test public void smoothEndpoints() {
        assertEquals(0, CinemaTimeline.smooth(-4), 0);
        assertEquals(0, CinemaTimeline.smooth(0), 0);
        assertEquals(.5, CinemaTimeline.smooth(.5), 0);
        assertEquals(1, CinemaTimeline.smooth(1), 0);
        assertEquals(1, CinemaTimeline.smooth(10), 0);
    }
}
