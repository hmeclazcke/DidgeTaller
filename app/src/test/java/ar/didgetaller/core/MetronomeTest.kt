package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class MetronomeTest {
    private class Clock : MetronomeClock {
        var time = 0L
        val tasks = mutableListOf<Pair<Long, () -> Unit>>()
        var cancellations = 0
        override fun nowNanos() = time
        override fun at(deadlineNanos: Long, action: () -> Unit): MetronomeTask {
            tasks += deadlineNanos to action
            return MetronomeTask { cancellations++ /* Retain callbacks to challenge ownership. */ }
        }
        fun fireLast(lateness: Long = 0) {
            val task = tasks.last(); time = task.first + lateness; task.second()
        }
    }
    private class Click : MetronomeClick {
        val accents = mutableListOf<Boolean>()
        var silenced = 0
        var closed = false
        override fun play(accent: Boolean, volume: Float): Boolean { accents += accent; return true }
        override fun silence() { silenced++ }
        override fun close() { closed = true }
    }
    @Test fun absoluteGridDoesNotAccumulateCallbackDelayOverTenHours() {
        for (subdivision in MetronomeSubdivision.entries) {
            val settings = MetronomeSettings(bpm = 137, subdivision = subdivision)
            val grid = MetronomeGrid(settings)
            for (i in listOf(0L, 1L, 5000L, 100000L, 500000L)) {
                val expected = (i * 60_000_000_000.0 / (137 * subdivision.pulses)).toLong()
                assertEquals(expected, grid.offset(i))
                assertEquals(i, grid.due(grid.offset(i) + 1))
            }
        }
    }
    @Test fun beatsSubdivisionsAndOnlyFirstBeatAccentFollowTheMeter() {
        for (meter in listOf(2, 3, 4)) for (subdivision in MetronomeSubdivision.entries) {
            val settings = MetronomeSettings(beatsPerBar = meter, subdivision = subdivision)
            for (i in 0 until meter * subdivision.pulses * 2) {
                val pulse = settings.pulse(i.toLong())
                assertEquals(i / subdivision.pulses % meter + 1, pulse.beat)
                assertEquals(i % subdivision.pulses + 1, pulse.subdivision)
                assertEquals(i % (meter * subdivision.pulses) == 0, pulse.accent)
                assertFalse(settings.copy(accentFirst = false).pulse(i.toLong()).accent)
            }
        }
    }
    @Test fun lateCallbacksSkipMissedPulsesWithoutBurstOrCumulativeDrift() {
        val clock = Clock(); val click = Click(); val engine = MetronomeEngine(clock, click) {}
        engine.update(MetronomeSettings(bpm = 120, audible = true))
        engine.start(); clock.fireLast(1_100_000_000)
        assertEquals(3, engine.state.pulse!!.beat)
        assertEquals(1, click.accents.size)
        assertEquals(1_500_000_000, clock.tasks.last().first)
        clock.fireLast(30_000_000)
        assertEquals(2_000_000_000, clock.tasks.last().first)
    }
    @Test fun pauseResumeStopAndCloseRejectStaleAndDuplicateCallbacks() {
        val clock = Clock(); val click = Click(); val engine = MetronomeEngine(clock, click) {}
        engine.update(MetronomeSettings(audible = true)); engine.start()
        val old = clock.tasks.last().second
        clock.fireLast(); old(); assertEquals(1, click.accents.size)
        engine.pause(); val paused = engine.state; old(); assertEquals(paused, engine.state)
        engine.start(); clock.fireLast(); assertEquals(2, engine.state.pulse!!.beat)
        engine.stop(); old(); assertNull(engine.state.pulse)
        assertEquals(MetronomeMode.STOPPED, engine.state.mode)
        engine.start(); clock.fireLast(); assertEquals(1, engine.state.pulse!!.beat)
        engine.close(); val terminal = engine.state
        clock.tasks.last().second(); engine.start(); engine.update(MetronomeSettings(bpm = 140))
        assertEquals(terminal, engine.state); assertTrue(click.closed)
    }
    @Test fun microphoneInterlockSilencesImmediatelyAndCannotBeBypassedBySettings() {
        val clock = Clock(); val click = Click(); val engine = MetronomeEngine(clock, click) {}
        engine.update(MetronomeSettings(audible = true)); engine.start(); clock.fireLast()
        engine.captureActive(true); assertTrue(click.silenced > 0)
        engine.update(engine.state.settings.copy(volume = 1f)); clock.fireLast()
        assertEquals(1, click.accents.size); assertNotNull(engine.state.pulse)
        engine.captureActive(false); clock.fireLast(); assertEquals(2, click.accents.size)
        engine.pause(); clock.tasks.last().second(); assertEquals(2, click.accents.size)
    }
    @Test fun tempoChangeInvalidatesOldDeadlineAndUsesNewInterval() {
        val clock = Clock(); val engine = MetronomeEngine(clock, Click()) {}
        engine.start(); clock.fireLast(); val stale = clock.tasks.last().second
        engine.update(engine.state.settings.copy(bpm = 240))
        assertEquals(250_000_000L, clock.tasks.last().first)
        val pending = engine.state; stale(); assertEquals(pending, engine.state)
        clock.fireLast(); assertEquals(2, engine.state.pulse!!.beat)
    }
    @Test fun visualDefaultZeroVolumeEarlyCallbacksAndAudioFailureAreSafe() {
        val clock = Clock(); val click = Click(); val engine = MetronomeEngine(clock, click) {}
        engine.start(); clock.fireLast(); assertTrue(click.accents.isEmpty())
        val pending = clock.tasks.last(); pending.second(); assertEquals(1, engine.state.pulse!!.beat)
        engine.update(engine.state.settings.copy(audible = true, volume = 0f))
        clock.fireLast(); assertTrue(click.accents.isEmpty())
        val failed = object : MetronomeClick {
            override fun play(accent: Boolean, volume: Float): Boolean = throw IllegalStateException("synthetic audio failure")
            override fun silence() {}
            override fun close() {}
        }
        val other = MetronomeEngine(clock, failed) {}; other.update(MetronomeSettings(audible = true)); other.start(); clock.fireLast()
        assertTrue(other.state.audioFailed); assertEquals(MetronomeMode.RUNNING, other.state.mode)
    }
    @Test fun inputBoundsRejectInvalidStates() {
        for (value in listOf(29, 241)) assertThrows(IllegalArgumentException::class.java) { MetronomeSettings(bpm = value) }
        assertThrows(IllegalArgumentException::class.java) { MetronomeSettings(beatsPerBar = 5) }
        for (value in listOf(-0.1f, 1.1f, Float.NaN)) assertThrows(IllegalArgumentException::class.java) { MetronomeSettings(volume = value) }
    }
    @Test fun repeatedStartsAndSettingsChangesNeverCreateAnotherOwner() {
        val clock = Clock(); val click = Click(); val engine = MetronomeEngine(clock, click) {}
        engine.stop(); engine.pause(); engine.start(); engine.start()
        assertEquals(1, clock.tasks.size)
        engine.update(engine.state.settings); assertEquals(1, clock.tasks.size)
        clock.fireLast()
        engine.update(engine.state.settings.copy(subdivision = MetronomeSubdivision.SIXTEENTH, beatsPerBar = 2))
        clock.fireLast(); assertEquals(MetronomePulse(1, 1, true), engine.state.pulse)
        engine.pause(); engine.update(engine.state.settings.copy(bpm = 30)); engine.start(); clock.fireLast()
        assertEquals(2, engine.state.pulse!!.subdivision)
        engine.captureActive(true); engine.captureActive(true)
        engine.close(); engine.close(); engine.captureActive(false); engine.stop(); engine.pause()
        assertTrue(engine.state.captureActive)
    }
    @Test fun constantCallbackLatenessNeverChangesTheNextGridDeadline() {
        val clock = Clock(); val engine = MetronomeEngine(clock, Click()) {}
        val settings = MetronomeSettings(bpm = 137, subdivision = MetronomeSubdivision.SIXTEENTH)
        engine.update(settings); engine.start()
        val grid = MetronomeGrid(settings)
        repeat(10000) { i ->
            clock.fireLast(7_000_000)
            assertEquals(grid.offset(i + 1L), clock.tasks.last().first)
        }
    }
    @Test fun failedOutputIsReleasedAndNotRetriedUntilExplicitlyReconfigured() {
        val clock = Clock(); var attempts = 0; var released = 0
        val click = object : MetronomeClick {
            override fun play(accent: Boolean, volume: Float): Boolean { attempts++; return false }
            override fun silence() { released++ }
            override fun close() {}
        }
        val engine = MetronomeEngine(clock, click) {}; engine.update(MetronomeSettings(audible = true)); engine.start()
        clock.fireLast(); clock.fireLast(); assertEquals(1, attempts); assertEquals(1, released)
        engine.update(engine.state.settings.copy(volume = .8f)); clock.fireLast(); assertEquals(2, attempts)
    }
    @Test fun pauseStopAndCloseReleasePendingWorkAndSoundImmediately() {
        val clock = Clock(); val click = Click(); val engine = MetronomeEngine(clock, click) {}
        engine.update(MetronomeSettings(bpm = 120, audible = true)); engine.start(); clock.fireLast()
        assertFalse(engine.state.audioFailed)
        engine.pause(); assertEquals(1, clock.cancellations); assertEquals(1, click.silenced)
        clock.time = 8_000_000_000
        engine.start(); assertEquals(8_500_000_000, clock.tasks.last().first)
        clock.fireLast(); assertEquals(2, engine.state.pulse!!.beat)
        engine.stop(); assertEquals(2, clock.cancellations); assertEquals(2, click.silenced)
        engine.start(); clock.fireLast(); engine.close()
        assertEquals(MetronomeMode.STOPPED, engine.state.mode); assertNull(engine.state.pulse)
        assertEquals(3, clock.cancellations); assertEquals(3, click.silenced)
    }
    @Test fun changingMeterOrSubdivisionAloneRestartsTheBarAndClearsTheOldIndicator() {
        val clock = Clock(); val engine = MetronomeEngine(clock, Click()) {}
        engine.start(); clock.fireLast(); clock.fireLast()
        engine.update(engine.state.settings.copy(beatsPerBar = 3)); assertNull(engine.state.pulse)
        clock.fireLast(); assertEquals(MetronomePulse(1, 1, true), engine.state.pulse)
        clock.fireLast()
        engine.update(engine.state.settings.copy(subdivision = MetronomeSubdivision.TRIPLET)); assertNull(engine.state.pulse)
        clock.fireLast(); assertEquals(MetronomePulse(1, 1, true), engine.state.pulse)
    }
    @Test fun earlyDeliverySchedulesTheSameDeadlineAndMinuteBoundaryNeverRunsEarly() {
        val clock = Clock(); val engine = MetronomeEngine(clock, Click()) {}
        engine.update(MetronomeSettings(bpm = 120)); engine.start(); clock.fireLast()
        val due = clock.tasks.last().first; val before = clock.tasks.size
        clock.tasks.last().second()
        assertEquals(before + 1, clock.tasks.size); assertEquals(due, clock.tasks.last().first)
        assertEquals(1, engine.state.pulse!!.beat)
        clock.fireLast(); assertEquals(2, engine.state.pulse!!.beat)
        for (subdivision in MetronomeSubdivision.entries) {
            val grid = MetronomeGrid(MetronomeSettings(bpm = 137, subdivision = subdivision))
            assertEquals(137L * subdivision.pulses - 1, grid.due(59_999_999_999))
            assertEquals(137L * subdivision.pulses, grid.due(60_000_000_000))
        }
    }
    @Test fun mutingOrZeroGainStopsTheCurrentClickAndTempoChangesCancelTheOldQueue() {
        val clock = Clock(); val click = Click(); val engine = MetronomeEngine(clock, click) {}
        engine.update(MetronomeSettings(audible = true)); engine.start(); clock.fireLast()
        engine.update(engine.state.settings.copy(audible = false)); assertEquals(1, click.silenced)
        engine.update(engine.state.settings.copy(audible = true)); clock.fireLast()
        engine.update(engine.state.settings.copy(volume = 0f)); assertEquals(2, click.silenced)
        val cancelled = clock.cancellations
        engine.update(engine.state.settings.copy(bpm = 137)); assertEquals(cancelled + 1, clock.cancellations)
    }
}
