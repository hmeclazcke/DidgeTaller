package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class MetronomeOutputTest {
    private class Focus : MetronomeFocus {
        val callbacks = mutableListOf<(Int) -> Unit>()
        var denied = false
        var releases = 0
        override fun request(changed: (Int) -> Unit): MetronomeTask? {
            callbacks += changed
            return if (denied) null else MetronomeTask { releases++ }
        }
    }
    private class Output : MetronomeClick {
        var clicks = 0
        var silenced = 0
        var closed = 0
        var failSilence = false
        var playable = true
        var lastGain = 0f
        override fun play(accent: Boolean, volume: Float): Boolean { clicks++; lastGain = volume; return playable }
        override fun silence() { silenced++; if (failSilence) error("synthetic stop failure") }
        override fun close() { closed++ }
    }
    @Test fun outputOwnsOneFocusLeaseAndRejectsOldLossAfterReleaseAndRestart() {
        val focus = Focus(); val output = Output(); var losses = 0
        val owner = FocusedMetronomeClick(output, focus) { losses++ }
        assertTrue(owner.play(true, .5f)); assertTrue(owner.play(false, .7f))
        assertEquals(.7f, output.lastGain)
        assertEquals(1, focus.callbacks.size); assertEquals(2, output.clicks)
        focus.callbacks[0](1); assertEquals(0, losses)
        focus.callbacks[0](-1); assertEquals(1, losses)
        owner.silence(); assertEquals(1, focus.releases); assertEquals(1, output.silenced)
        owner.play(true, .5f); focus.callbacks[0](-1); assertEquals(1, losses)
        focus.callbacks[1](-2); assertEquals(2, losses)
        owner.close(); owner.close(); assertEquals(1, output.closed); assertEquals(2, focus.releases)
        focus.callbacks[1](-1); assertEquals(2, losses); assertFalse(owner.play(true, .5f))
    }
    @Test fun deniedFocusNeverPlaysAndStopFailureStillReleasesTheLease() {
        val focus = Focus(); val output = Output(); val owner = FocusedMetronomeClick(output, focus) {}
        focus.denied = true; assertFalse(owner.play(true, .5f)); assertEquals(0, output.clicks)
        focus.denied = false; assertTrue(owner.play(false, .5f))
        output.failSilence = true
        assertThrows(IllegalStateException::class.java) { owner.silence() }
        assertEquals(1, focus.releases)
    }
    @Test fun restoredSettingsValidateExternalValuesWithoutChangingTheStoredFormat() {
        val restored = MetronomeSettings.restore(900, 1, "unknown", false, true, Float.NaN)
        assertEquals(MetronomeSettings(240, 2, MetronomeSubdivision.QUARTER, false, true, .5f), restored)
        assertFalse(restored.accentFirst); assertEquals(2, restored.beatsPerBar)
        assertTrue(MetronomeSettings.restore(90, 4, null, true, false, .5f).accentFirst)
        assertEquals(30, MetronomeSettings.restore(1, 9, "TRIPLET", true, false, -1f).bpm)
        assertEquals(1f, MetronomeSettings.restore(90, 4, "EIGHTH", true, true, 2f).volume)
        assertEquals(MetronomeSubdivision.SIXTEENTH, MetronomeSettings.restore(90, 4, "SIXTEENTH", true, false, .7f).subdivision)
    }
    @Test fun screenAwakeFollowsRecordingAudiblePlaybackOrRunningRhythm() {
        for (recording in listOf(false, true)) for (playing in listOf(false, true))
            for (paused in listOf(false, true)) for (rhythm in MetronomeMode.entries) {
                val expected = when {
                    recording -> true
                    playing -> !paused || rhythm == MetronomeMode.RUNNING
                    else -> rhythm == MetronomeMode.RUNNING
                }
                assertEquals(expected, practiceKeepsScreenAwake(recording, playing, paused, rhythm))
            }
    }
    @Test fun failedNativeOutputIsNotReportedAsSuccessfulAndNeutralFocusDoesNotPause() {
        val focus = Focus(); val output = Output(); output.playable = false
        var losses = 0
        val owner = FocusedMetronomeClick(output, focus) { losses++ }
        assertFalse(owner.play(false, .5f)); focus.callbacks.single()(0)
        assertEquals(0, losses); owner.close(); assertEquals(1, focus.releases)
    }
}
