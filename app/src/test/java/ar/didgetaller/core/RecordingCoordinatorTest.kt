package ar.didgetaller.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class RecordingCoordinatorTest {
    private class Harness : RecordingCoordinator.Observer {
        val notices = mutableListOf<String>()
        val states = mutableListOf<RecordingCoordinator.State>()
        val publications = mutableListOf<Pair<CapturedSession, (Result<Unit>) -> Unit>>()
        var libraryChanges = 0
        var closures = 0
        val owner = RecordingCoordinator(this) { _, snapshot, complete -> publications += snapshot to complete }
        override fun changed(state: RecordingCoordinator.State) { states += state }
        override fun saved() { notices += "saved" }
        override fun saveFailed(error: Throwable) { notices += "save: ${error.message}" }
        override fun discarded(success: Boolean) { notices += "discard: $success" }
        override fun finalizationFailed(error: Throwable) { notices += "close: ${error.message}" }
        override fun libraryChanged() { libraryChanges++ }
        override fun idleAfterDisposal() { closures++ }
        val first = CaptureId()
        val second = CaptureId()
        val point = SessionAnalysis.Point(22400, 137.6, 0.95)
        val ready = RecordingCapture.Outcome.Ready(File("synthetic.part"))
        fun begin() { owner.monitorStarted(first); owner.begin(first, 1234, InstrumentProfile.NATIVE_AMERICAN_FLUTE, 5678) }
        fun end(outcome: RecordingCapture.Outcome? = ready) = owner.terminal(CaptureTerminal(first, outcome))
        fun acknowledge(result: Result<Unit> = Result.success(Unit)) = publications.single().second(result)
    }

    @Test fun recordingCapturesItsOwnMetadataAndFreezesReadingsBeforePublication() {
        val h = Harness(); h.begin(); h.owner.append(h.point); h.owner.saving()
        h.owner.append(h.point.copy(timeMs = 23000))
        assertEquals(RecordingPhase.SAVING, h.owner.state.phase); assertFalse(h.owner.state.listening)
        h.end()
        assertEquals(CapturedSession(1234, InstrumentProfile.NATIVE_AMERICAN_FLUTE, listOf(h.point)), h.publications.single().first)
        assertTrue(h.notices.isEmpty())
        h.acknowledge()
        assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
        assertEquals(listOf(h.point), h.owner.state.points)
        assertEquals(5678L, h.owner.state.startedAtMs)
        assertEquals(listOf("saved"), h.notices); assertEquals(1, h.libraryChanges)
        assertEquals(h.owner.state, h.states.last())
        assertTrue(h.states.any { it.phase == RecordingPhase.CAPTURING && it.points == listOf(h.point) })
    }

    @Test fun timeoutCompletesTheCurrentMicrophoneAndPublishesOnce() {
        val h = Harness(); h.begin(); h.end(); h.end()
        assertFalse(h.owner.state.listening); assertEquals(RecordingPhase.SAVING, h.owner.state.phase)
        assertEquals(1, h.publications.size)
        h.acknowledge(); h.acknowledge(Result.failure(Exception("late")))
        assertEquals(listOf("saved"), h.notices); assertEquals(1, h.libraryChanges)
    }

    @Test fun lateSuccessKeepsTheReplacementMonitorRunningAndRefreshesOnlyTheLibrary() {
        val h = Harness(); h.begin(); h.owner.append(h.point); h.owner.saving()
        h.owner.monitorStarted(h.second); h.end(); h.acknowledge()
        assertTrue(h.owner.state.listening); assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
        assertTrue(h.notices.isEmpty()); assertEquals(1, h.libraryChanges)
        h.owner.terminal(CaptureTerminal(h.second, null))
        assertFalse(h.owner.state.listening)
    }

    @Test fun latePublicationFailureDoesNotOverwriteTheReplacementMonitor() {
        val h = Harness(); h.begin(); h.owner.saving(); h.end()
        h.owner.monitorStarted(h.second); h.acknowledge(Result.failure(Exception("disk full")))
        assertTrue(h.owner.state.listening); assertTrue(h.notices.isEmpty())
        assertEquals(0, h.libraryChanges); assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
    }

    @Test fun aFailedReplacementStartAlsoRetainsItsNoticeOwnership() {
        val h = Harness(); h.begin(); h.owner.saving(); h.end()
        assertFalse(h.owner.startMonitor { null })
        h.acknowledge()
        assertTrue("Old save must not overwrite the failed replacement's error", h.notices.isEmpty())
        assertEquals(1, h.libraryChanges)
    }

    @Test fun aCurrentPublicationFailureEndsSavingAndReportsTheFailureOnce() {
        val h = Harness(); h.begin(); h.owner.saving(); h.end()
        h.acknowledge(Result.failure(Exception("disk full"))); h.acknowledge()
        assertEquals(listOf("save: disk full"), h.notices); assertEquals(0, h.libraryChanges)
        assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
    }

    @Test fun discardAndCloseFailuresAreTerminalButNeverAuthorizeImport() {
        for (outcome in listOf(RecordingCapture.Outcome.Discarded(true), RecordingCapture.Outcome.Discarded(false),
            RecordingCapture.Outcome.Failed(Exception("broken header")))) {
            val h = Harness(); h.begin(); h.owner.append(h.point); h.owner.discarding(); h.end(outcome)
            h.end(); assertTrue(h.publications.isEmpty()); assertEquals(1, h.notices.size)
            val expected = when (outcome) {
                is RecordingCapture.Outcome.Discarded -> "discard: ${outcome.success}"
                else -> "close: broken header"
            }
            assertEquals(expected, h.notices.single())
            assertFalse(h.owner.state.listening); assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
            assertTrue(h.owner.state.points.isEmpty())
        }
    }

    @Test fun staleDiscardFailureAndCancellationCannotChangeAReplacementMonitor() {
        for (outcome in listOf(RecordingCapture.Outcome.Discarded(false),
            RecordingCapture.Outcome.Failed(Exception("broken header")), null)) {
            val h = Harness(); h.begin(); h.owner.saving(); h.owner.monitorStarted(h.second)
            h.end(outcome); h.end(outcome)
            assertTrue(h.owner.state.listening); assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
            assertTrue(h.notices.isEmpty()); assertTrue(h.publications.isEmpty())
        }
    }

    @Test fun disposalWaitsForTheOwnedPublicationWithoutPublishingUiFeedback() {
        val h = Harness(); h.begin(); h.owner.saving(); h.owner.dispose()
        assertEquals(0, h.closures)
        h.end(); h.acknowledge()
        assertEquals(1, h.closures); assertTrue(h.notices.isEmpty()); assertEquals(0, h.libraryChanges)
        assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
        assertThrows(IllegalStateException::class.java) { h.owner.monitorStarted(h.second) }
        assertThrows(IllegalStateException::class.java) { h.owner.begin(h.second, 1, InstrumentProfile.DIDGERIDOO) }
    }

    @Test fun disposalAlsoWaitsForFailedFinalizationAndDiscardCleanup() {
        for (outcome in listOf(RecordingCapture.Outcome.Discarded(true), RecordingCapture.Outcome.Failed(Exception()))) {
            val h = Harness(); h.begin(); h.owner.saving(); h.owner.dispose(); h.end(outcome)
            assertEquals(1, h.closures); assertTrue(h.notices.isEmpty()); assertTrue(h.publications.isEmpty())
        }
        val idle = Harness(); idle.owner.dispose(); idle.owner.dispose(); assertEquals(1, idle.closures)
    }

    @Test fun unknownOrDuplicateTerminalsCannotEndTheCurrentCaptureOrStartPublication() {
        val h = Harness(); h.begin(); val expected = h.owner.state
        h.owner.terminal(CaptureTerminal(h.second, h.ready))
        assertEquals(expected, h.owner.state); assertTrue(h.publications.isEmpty())
        assertThrows(IllegalStateException::class.java) { h.owner.begin(h.first, 2, InstrumentProfile.DIDGERIDOO) }
        h.owner.saving(); h.owner.saving(); h.owner.discarding()
        assertEquals(RecordingPhase.SAVING, h.owner.state.phase)
    }

    @Test fun anIdleMonitorProducesNoRecordingAndCannotAccumulateSessionPoints() {
        val h = Harness(); h.owner.append(h.point); h.owner.saving(); h.owner.discarding()
        h.owner.monitorStarted(h.first); h.owner.append(h.point)
        h.end(null)
        assertFalse(h.owner.state.listening); assertTrue(h.owner.state.points.isEmpty())
        assertTrue(h.publications.isEmpty()); assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
    }

    @Test fun aMonitorWithoutARegisteredRecordingCannotAuthorizeImport() {
        val h = Harness(); h.owner.monitorStarted(h.first); h.end()
        assertTrue(h.publications.isEmpty()); assertTrue(h.notices.isEmpty())
        assertFalse(h.owner.state.listening); assertEquals(RecordingPhase.IDLE, h.owner.state.phase)
    }

    @Test fun disposalRejectsRecordingEvenWhenItsOldMonitorStillHasAnIdentity() {
        val h = Harness(); h.owner.monitorStarted(h.first); h.owner.dispose()
        assertThrows(IllegalStateException::class.java) { h.owner.begin(h.first, 2, InstrumentProfile.DIDGERIDOO) }
        assertEquals(RecordingPhase.IDLE, h.owner.state.phase); assertTrue(h.publications.isEmpty())
    }

    @Test fun delayedPermissionResultsCannotReopenHardwareAfterDisposal() {
        val h = Harness(); h.owner.dispose()
        assertFalse(h.owner.startMonitor { error("Disposed owner must not open hardware") })
        assertFalse(h.owner.state.listening)
    }

    @Test fun startingPublishesOnlyASuccessfullyOpenedMicrophone() {
        val h = Harness()
        assertFalse(h.owner.startMonitor { null }); assertFalse(h.owner.state.listening)
        assertTrue(h.owner.startMonitor { h.first }); assertTrue(h.owner.state.listening)
        h.owner.begin(h.first, 1, InstrumentProfile.DIDGERIDOO)
        assertEquals(RecordingPhase.CAPTURING, h.owner.state.phase)
    }

    @Test fun stoppingAMonitorRejectsFurtherReadingsAndDoesNotAuthorizeAnotherRecording() {
        val h = Harness(); h.begin(); h.owner.monitorStopped(); h.owner.append(h.point)
        assertFalse(h.owner.state.listening); assertTrue(h.owner.state.points.isEmpty())
        assertThrows(IllegalStateException::class.java) { h.owner.begin(h.second, 2, InstrumentProfile.DIDGERIDOO) }
    }
}
