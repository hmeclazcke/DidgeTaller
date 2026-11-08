package ar.didgetaller.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class PlaybackControllerTest {
    private class Backend : PlaybackBackend {
        override var positionMs = 0
            get() { check(!positionFails) { "position failed" }; return field }
        override var durationMs = 117000
        override var playing = false
        lateinit var events: PlaybackEvents
        val seeks = mutableListOf<Long>()
        var released = false
        var positionFails = false
        var prepareFails = false
        var releaseFails = false
        var startFails = false
        var starts = 0
        var pauses = 0
        override fun prepare(file: File, events: PlaybackEvents) { this.events = events; check(!prepareFails) }
        override fun start() { check(!startFails); starts++; playing = true }
        override fun pause() { pauses++; playing = false }
        override fun seek(positionMs: Long) { seeks += positionMs }
        override fun release() { released = true; playing = false; check(!releaseFails) }
        fun completeSeek() { positionMs = seeks.last().toInt(); events.seekCompleted() }
    }
    private class Scheduler : PlaybackScheduler {
        var pending: (() -> Unit)? = null
        override fun schedule(task: () -> Unit) { pending = task }
        override fun cancel() { pending = null }
        fun tick() { val task = checkNotNull(pending); pending = null; task() }
    }
    private class Harness {
        val backends = mutableListOf<Backend>()
        val errors = mutableListOf<Throwable>()
        val changes = mutableListOf<Pair<String?,Boolean>>()
        val scheduler = Scheduler()
        val controller = PlaybackController({ Backend().also { backends += it } }, scheduler,
            { id, paused -> changes += id to paused }, { errors += it })
        val file = File("synthetic-only.wav") // Backend never reads a file.
        fun open(play: Boolean = false, position: Long = 0) = controller.open("first", file, play, position)
        val backend get() = backends.last()
    }

    @Test fun selectionBeforePreparationDoesNotStartAudio() {
        val h = Harness(); h.controller.seek("first", h.file, 60000)
        assertEquals(60000, h.controller.position()); assertFalse(h.controller.ready)
        h.backend.events.prepared()
        assertEquals(listOf(60000L), h.backend.seeks); assertFalse(h.backend.playing)
        assertTrue(h.controller.paused); assertEquals(1, h.backends.size)
    }
    @Test fun playingSeekQueuesOnlyTheNewestTargetAndPreservesPlayback() {
        val h = Harness(); h.open(true); h.backend.events.prepared()
        h.controller.seek("first", h.file, 45000)
        h.controller.seek("first", h.file, 30000)
        h.controller.seek("first", h.file, 15000)
        assertEquals(listOf(45000L), h.backend.seeks); assertEquals(15000, h.controller.position())
        h.backend.completeSeek(); assertEquals(listOf(45000L,15000L), h.backend.seeks)
        assertEquals(15000, h.controller.position()); assertTrue(h.backend.playing)
        h.backend.completeSeek(); assertEquals(15000, h.controller.position()); assertFalse(h.controller.paused)
    }
    @Test fun pausedSeekStaysPausedAndClampsToDuration() {
        val h = Harness(); h.open(); h.backend.events.prepared()
        h.controller.seek("first", h.file, 200000)
        assertEquals(117000, h.controller.position()); assertTrue(h.controller.paused)
        h.backend.completeSeek(); assertFalse(h.backend.playing)
    }
    @Test fun stoppedAndPreparingTargetsNeverOverflowThePublicIntegerClock() {
        val h = Harness(); h.open(position = Long.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, h.controller.position())
        h.backend.events.prepared(); assertEquals(117000, h.controller.position())
    }
    @Test fun callbacksFromReleasedPlayerCannotStopOrAlterTheCurrentSession() {
        val h = Harness(); h.open(true); val old = h.backend
        h.controller.open("second", h.file, true, 10000); val current = h.backend
        old.events.prepared(); old.events.failed(IllegalStateException("obsolete"))
        old.events.completed(); old.events.seekCompleted()
        assertFalse(current.released); assertFalse(h.controller.ready); assertTrue(h.errors.isEmpty())
        current.events.prepared(); assertEquals("second", h.controller.id); assertFalse(current.playing)
        current.completeSeek(); assertTrue(current.playing)
    }
    @Test fun stopIsIdempotentAndCancelsPolling() {
        val h = Harness(); h.open(true); h.backend.events.prepared()
        h.controller.stop(); h.controller.stop()
        assertTrue(h.backend.released); assertNull(h.scheduler.pending); assertNull(h.controller.id)
        assertEquals(0, h.controller.position()); assertTrue(h.controller.paused)
    }
    @Test fun completionPreservesPositionAndPublishesPausedState() {
        val h = Harness(); h.open(true); h.backend.events.prepared(); h.backend.positionMs = 117000
        h.backend.playing = false; h.backend.events.completed()
        assertTrue(h.controller.paused); assertEquals(117000, h.controller.position())
        assertEquals("first" to true, h.changes.last())
    }
    @Test fun loopCompletionSeeksToTheRegionAndContinues() {
        val h = Harness(); h.open(true); h.backend.events.prepared(); h.controller.loop(10000L..20000L)
        assertEquals(10000L..20000L,h.controller.loop)
        h.backend.completeSeek(); h.backend.playing=false;h.backend.events.completed()
        assertEquals(2,h.backend.seeks.size);assertEquals(10000L, h.backend.seeks.last()); assertFalse(h.backend.playing)
        h.backend.completeSeek(); assertTrue(h.backend.playing)
    }
    @Test fun aCompletionQueuedBeforePauseCannotRestartTheLoop() {
        val h = Harness(); h.open(true); h.backend.events.prepared(); h.controller.loop(10000L..20000L)
        h.backend.completeSeek(); h.backend.positionMs = 15000
        h.controller.toggle("first", h.file)
        h.backend.events.completed()
        assertTrue(h.controller.paused); assertFalse(h.backend.playing)
        assertEquals(15000, h.controller.position()); assertEquals(1, h.backend.starts)
        assertEquals(listOf(10000L), h.backend.seeks)
    }

    @Test fun pauseUsesPlaybackIntentWhileTheBackendIsTemporarilyNotPlaying() {
        val h = Harness(); h.open(true); h.backend.events.prepared()
        h.controller.seek("first", h.file, 15000)
        h.backend.playing = false
        h.controller.toggle("first", h.file)
        assertTrue(h.controller.paused); assertFalse(h.backend.playing)
        h.backend.completeSeek()
        assertTrue(h.controller.paused); assertFalse(h.backend.playing)
        assertEquals(15000, h.controller.position()); assertEquals(1, h.backend.starts)
    }

    @Test fun completionDuringASeekCannotReplaceTheNewestTargetWithTheLoopStart() {
        val h = Harness(); h.open(true); h.backend.events.prepared(); h.controller.loop(10000L..20000L)
        h.backend.completeSeek()
        h.controller.seek("first", h.file, 15000); h.controller.seek("first", h.file, 17000)
        h.backend.playing = false; h.backend.events.completed()
        assertEquals(17000, h.controller.position()); assertFalse(h.controller.paused)
        h.backend.completeSeek(); assertEquals(listOf(10000L, 15000L, 17000L), h.backend.seeks)
        h.backend.completeSeek()
        assertEquals(17000, h.controller.position()); assertTrue(h.backend.playing)
    }

    @Test fun completionDuringANonloopSeekPreservesTheRequestedPlaybackState() {
        val h = Harness(); h.open(true); h.backend.events.prepared()
        h.controller.seek("first", h.file, 15000)
        h.backend.playing = false; h.backend.events.completed(); h.backend.completeSeek()
        assertFalse(h.controller.paused); assertTrue(h.backend.playing)
        assertEquals(15000, h.controller.position())
    }

    @Test fun initialSeekCompletesBeforePlaybackAndCanBePausedBeforeTheFirstStart() {
        val h = Harness(); h.open(true, 15000); h.backend.events.prepared()
        assertEquals(0, h.backend.starts); assertEquals(15000, h.controller.position())
        h.controller.toggle("first", h.file)
        assertEquals(0, h.backend.pauses); assertTrue(h.controller.paused)
        h.backend.completeSeek(); assertFalse(h.backend.playing)
        h.controller.toggle("first", h.file); assertTrue(h.backend.playing)
        assertEquals(1, h.backend.starts)
    }

    @Test fun aLateCompletionAfterTheSeekSettledCannotRewindPlayingAudio() {
        val h = Harness(); h.open(true); h.backend.events.prepared(); h.controller.loop(10000L..20000L)
        h.backend.completeSeek(); h.controller.seek("first", h.file, 17000); h.backend.completeSeek()
        h.backend.events.completed()
        assertFalse(h.controller.paused); assertTrue(h.backend.playing)
        assertEquals(17000, h.controller.position()); assertEquals(listOf(10000L, 17000L), h.backend.seeks)
    }

    @Test fun seekingToTheEndSettlesAsCompletedWithoutRestartingAtZero() {
        val h = Harness(); h.open(true); h.backend.events.prepared()
        h.controller.seek("first", h.file, h.backend.durationMs.toLong())
        h.backend.playing = false; h.backend.events.completed(); h.backend.completeSeek()
        assertTrue(h.controller.paused); assertFalse(h.backend.playing)
        assertEquals(117000, h.controller.position()); assertEquals(1, h.backend.starts)
    }

    @Test fun resumingAtAnOldEndPositionCannotReplaceThePendingUserSeekWithZero() {
        val h = Harness(); h.open(); h.backend.events.prepared(); h.backend.positionMs = h.backend.durationMs
        h.controller.seek("first", h.file, 15000); h.controller.toggle("first", h.file)
        assertEquals(15000, h.controller.position()); assertEquals(listOf(15000L), h.backend.seeks)
        h.backend.completeSeek(); assertTrue(h.backend.playing); assertEquals(15000, h.controller.position())
    }

    @Test fun pausingDoesNotLetBackendLagTriggerANewLoopSeek() {
        val h = Harness(); h.open(true); h.backend.events.prepared(); h.controller.loop(10000L..20000L)
        h.backend.completeSeek(); h.controller.toggle("first", h.file)
        h.backend.playing = true // Platform pause acknowledgement can lag behind the UI command.
        h.backend.positionMs = 20000
        h.scheduler.pending!!()
        assertTrue(h.controller.paused); assertEquals(20000, h.controller.position())
        assertEquals(listOf(10000L), h.backend.seeks)
    }

    @Test fun pausingAfterCompletionPreventsThePendingLoopRestart() {
        val h = Harness(); h.open(true); h.backend.events.prepared(); h.controller.loop(10000L..20000L)
        h.backend.completeSeek(); h.backend.playing = false; h.backend.events.completed()
        h.controller.toggle("first", h.file); h.backend.completeSeek()
        assertTrue(h.controller.paused); assertFalse(h.backend.playing)
        assertEquals(10000, h.controller.position()); assertEquals(1, h.backend.starts)
    }

    @Test fun preparationAtASelectedPositionStartsExactlyOnceAfterTheSeekCompletes() {
        val h = Harness(); h.open(true, 15000); h.backend.events.prepared()
        assertEquals(0, h.backend.starts)
        h.backend.completeSeek(); h.backend.events.seekCompleted()
        assertTrue(h.backend.playing); assertEquals(15000, h.controller.position())
        assertEquals(1, h.backend.starts)
    }

    @Test fun aCompletionBeforePreparationCannotDiscardTheSelectedStartingPosition() {
        val h = Harness(); h.open(true, 15000); h.backend.events.completed()
        assertFalse(h.controller.ready); assertEquals(15000, h.controller.position())
        assertFalse(h.backend.released); assertTrue(h.errors.isEmpty())
        h.backend.events.prepared(); h.backend.completeSeek()
        assertTrue(h.controller.ready); assertTrue(h.backend.playing); assertEquals(15000, h.controller.position())
    }

    @Test fun aRetiredPollCannotScheduleTheReplacementBeforePreparation() {
        val h = Harness(); h.open(true); h.backend.events.prepared()
        val retired = checkNotNull(h.scheduler.pending)
        h.controller.open("second", h.file, false, 15000); retired()
        assertNull(h.scheduler.pending); assertFalse(h.backend.released)
        h.backend.events.prepared(); assertNotNull(h.scheduler.pending)
        h.backend.completeSeek(); assertEquals(15000, h.controller.position())
    }

    @Test fun seekingANewSessionCannotInheritThePreviousLoopEvenWhilePreparing() {
        for (prepared in listOf(false, true)) {
            val h = Harness(); h.open(true)
            if (prepared) h.backend.events.prepared()
            h.controller.loop(10000L..20000L)
            val old = h.backend
            h.controller.seek("second", h.file, 80000)
            assertTrue(old.released); assertEquals(2, h.backends.size)
            assertEquals(80000, h.controller.position()); assertNull(h.controller.loop)
            old.events.completed(); old.events.seekCompleted()
            h.backend.events.prepared(); assertEquals(listOf(80000L), h.backend.seeks)
            h.backend.completeSeek()
            assertEquals("second", h.controller.id); assertTrue(h.controller.paused); assertFalse(h.backend.playing)
        }
    }

    @Test fun explicitResumeAtTheEndRestartsInsideTheSelectedLoopAfterAcknowledgement() {
        val h = Harness(); h.open(); h.backend.events.prepared()
        h.controller.loop(10000L..117000L); h.backend.completeSeek()
        h.controller.seek("first", h.file, 117000); h.backend.completeSeek()
        h.controller.toggle("first", h.file)
        assertEquals(10000L, h.backend.seeks.last()); assertEquals(0, h.backend.starts)
        h.backend.completeSeek(); assertTrue(h.backend.playing); assertEquals(10000, h.controller.position())
    }

    @Test fun aLoopCanStartAtZeroAndStillClampBothSeekBoundaries() {
        val h = Harness(); h.open(); h.backend.events.prepared(); h.controller.loop(0L..20000L)
        assertEquals(0L..20000L, h.controller.loop)
        h.controller.seek("first", h.file, -1); assertEquals(0, h.controller.position())
        h.controller.seek("first", h.file, 30000); assertEquals(20000, h.controller.position())
        h.backend.completeSeek(); h.backend.completeSeek()
        assertTrue(h.controller.paused); assertFalse(h.backend.playing)
    }

    @Test fun resumingBeforeTheInitialSeekAcknowledgementCannotStartAtZero() {
        val h = Harness(); h.open(false, 15000); h.backend.events.prepared()
        h.controller.toggle("first", h.file)
        assertFalse(h.controller.paused); assertEquals(0, h.backend.starts)
        h.backend.completeSeek(); assertTrue(h.backend.playing); assertEquals(15000, h.controller.position())
    }
    @Test fun pendingToggleAndRestartPreserveTheRequestedConvention() {
        val h = Harness(); h.open(); h.controller.toggle("first", h.file); h.backend.events.prepared()
        assertTrue(h.backend.playing)
        h.controller.toggle("first", h.file); assertTrue(h.controller.paused)
        h.controller.restart("first", h.file); assertFalse(h.controller.paused)
        assertEquals(0L,h.backend.seeks.last())
    }
    @Test fun newSessionDoesNotInheritAnotherRecordingsPosition() {
        val h = Harness(); h.open(position = 80000); h.backend.events.prepared()
        h.controller.toggle("second", h.file); h.backend.events.prepared()
        assertEquals(0, h.controller.position()); assertTrue(h.backend.seeks.isEmpty())
    }
    @Test fun activeBackendErrorReleasesResourcesAndReportsFailure() {
        val h = Harness(); h.open(); h.backend.events.failed(IllegalStateException("synthetic failure"))
        assertTrue(h.backend.released); assertEquals(1,h.errors.size); assertFalse(h.controller.ready)
    }
    @Test fun pollingChecksTheLoopAndPreservesPausedState() {
        val h=Harness();h.open(true);h.backend.events.prepared();h.controller.loop(10000L..20000L)
        h.backend.completeSeek();h.backend.positionMs=20000
        h.scheduler.tick();assertEquals(2,h.backend.seeks.size);assertEquals(10000L,h.backend.seeks.last())
        assertNotNull("The next loop boundary must still be monitored",h.scheduler.pending)
        h.backend.completeSeek();h.controller.toggle("first",h.file);h.backend.positionMs=20000
        h.scheduler.tick();assertEquals(2,h.backend.seeks.size);assertTrue(h.controller.paused)
    }
    @Test fun stalePollAndDuplicateSeekCompletionAreHarmless() {
        val h=Harness();h.open(true);h.backend.events.prepared();val poll=h.scheduler.pending!!
        h.controller.seek("first",h.file,10000);h.backend.completeSeek();h.backend.events.seekCompleted()
        assertEquals(1,h.backend.seeks.size)
        h.controller.stop();poll();assertNull(h.scheduler.pending);assertNull(h.controller.id)
    }
    @Test fun duplicatePreparationDoesNotRestartPlayback() {
        val h=Harness();h.open(true);h.backend.events.prepared();h.backend.events.prepared()
        assertEquals(1,h.backend.starts)
        assertTrue(h.controller.ready);assertFalse(h.backend.released);assertTrue(h.errors.isEmpty())
    }
    @Test fun positionAndStartFailuresStopOwnershipAndSurfaceTheActualError() {
        val h=Harness();h.open();h.backend.events.prepared();h.backend.positionFails=true
        assertEquals(0,h.controller.position());assertTrue(h.backend.released);assertEquals("position failed",h.errors.single().message)
        h.open(true);h.backend.startFails=true;h.backend.events.prepared()
        assertFalse(h.controller.ready);assertTrue(h.backend.released);assertEquals(2,h.errors.size)
    }
    @Test fun failedPreparationAndReleaseLeaveNoActiveBackend() {
        val backend=Backend().apply {prepareFails=true;releaseFails=true}
        val errors=mutableListOf<Throwable>();val scheduler=Scheduler()
        val controller=PlaybackController({backend},scheduler,{_,_->},{errors+=it})
        controller.open("failure",File("synthetic.wav"),true)
        assertTrue(backend.released);assertFalse(controller.ready);assertNull(scheduler.pending);assertEquals(2,errors.size)
    }
    @Test fun loopSeekClampsBothEndsAndRejectsNegativeRegions() {
        val h=Harness();h.open();h.backend.events.prepared();h.controller.loop(10000L..20000L);h.backend.completeSeek()
        h.controller.seek("first",h.file,-10);assertEquals(10000,h.controller.position());h.backend.completeSeek()
        h.controller.seek("first",h.file,100000);assertEquals(20000,h.controller.position());assertTrue(h.controller.paused)
        assertThrows(IllegalArgumentException::class.java){h.controller.loop(-1L..100L)}
        assertThrows(IllegalArgumentException::class.java){h.controller.loop(10L..10L)}
    }
    @Test fun preparingSelectionAndPauseUseTheLatestUserIntent() {
        val h=Harness();h.open(true);h.controller.seek("first",h.file,45000);h.controller.seek("first",h.file,15000)
        h.controller.toggle("first",h.file);h.backend.events.prepared()
        assertFalse(h.backend.playing);assertEquals(15000,h.controller.position());assertEquals(listOf(15000L),h.backend.seeks)
    }
    @Test fun resumeOnlyRestartsInsideTheExistingEndTolerance() {
        for(delta in listOf(20,21)) {
            val h=Harness();h.open();h.backend.events.prepared();h.backend.positionMs=h.backend.durationMs-delta
            h.controller.toggle("first",h.file);assertFalse(h.controller.paused)
            assertEquals(if(delta==20)listOf(0L) else emptyList<Long>(),h.backend.seeks)
            if(delta==20){assertFalse(h.backend.playing);h.backend.completeSeek()}
            assertTrue(h.backend.playing)
        }
    }
    @Test fun preparedListenerCanDisposeWithoutLeavingAPollScheduled() {
        val backend=Backend();val scheduler=Scheduler();lateinit var controller:PlaybackController
        controller=PlaybackController({backend},scheduler,{id,_->if(id!=null)controller.stop()},{fail(it.message)})
        controller.open("first",File("synthetic.wav"),true);backend.events.prepared()
        assertNull(controller.id);assertNull(scheduler.pending);assertTrue(backend.released)
    }
    @Test fun durationAndLoopOwnershipFollowPreparationAndStop() {
        val h=Harness();h.open();assertEquals(0,h.controller.duration())
        h.backend.events.prepared();assertEquals(117000,h.controller.duration())
        h.controller.loop(10000L..20000L);h.backend.completeSeek();h.controller.loop(null)
        h.controller.seek("first",h.file,80000);assertEquals(80000,h.controller.position());assertNull(h.controller.loop)
        h.controller.stop();assertEquals(0,h.controller.duration())
    }
    @Test fun restartFromStoppedStartsOnePlayerAtZero() {
        val h=Harness();h.controller.restart("first",h.file);h.backend.events.prepared()
        assertEquals(1,h.backends.size);assertTrue(h.backend.playing);assertEquals(0,h.controller.position())
    }
}
