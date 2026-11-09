package ar.didgetaller.core

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CaptureHandoffTest {
    private val handoff = CaptureHandoff<String>()
    private val first = CaptureId()
    private val second = CaptureId()
    private val ready = RecordingCapture.Outcome.Ready(File("synthetic.part"))

    private fun begin(id: CaptureId = first, snapshot: String = "first samples") {
        handoff.start(id)
        handoff.record(id, snapshot)
    }

    @Test fun lateSuccessPreservesItsSnapshotWithoutEndingTheReplacement() {
        begin(); handoff.stop(); begin(second, "second samples")
        val delivery = checkNotNull(handoff.terminal(CaptureTerminal(first, ready)))
        assertEquals("first samples", delivery.snapshot)
        assertFalse(delivery.endsMicrophone); assertFalse(delivery.ownsRecording)
        assertFalse(handoff.canNotify(first)); assertTrue(handoff.canNotify(second))
        assertFalse(handoff.published(first))
        assertEquals("second samples", handoff.terminal(CaptureTerminal(second, ready))?.snapshot)
        assertTrue(handoff.published(second))
    }

    @Test fun delayedFailureDiscardAndCancellationCannotEndAnotherRecording() {
        val outcomes = listOf(RecordingCapture.Outcome.Failed(IllegalStateException("close failed")),
            RecordingCapture.Outcome.Discarded(true), RecordingCapture.Outcome.Discarded(false), null)
        for (outcome in outcomes) {
            val owner = CaptureHandoff<String>()
            owner.start(first); owner.record(first, "old"); owner.stop()
            owner.start(second); owner.record(second, "new")
            val delivery = checkNotNull(owner.terminal(CaptureTerminal(first, outcome)))
            assertEquals("old", delivery.snapshot)
            assertFalse(delivery.endsMicrophone); assertFalse(delivery.ownsRecording)
            assertNull(owner.terminal(CaptureTerminal(first, outcome)))
            assertEquals("new", owner.terminal(CaptureTerminal(second, ready))?.snapshot)
            assertTrue(owner.published(second))
        }
    }

    @Test fun stoppedRecordingCanSaveWhileReplacementOnlyMonitors() {
        begin(); handoff.stop(); handoff.start(second)
        val delivery = checkNotNull(handoff.terminal(CaptureTerminal(first, ready)))
        assertTrue(delivery.ownsRecording); assertFalse(delivery.endsMicrophone)
        assertTrue(handoff.published(first)); assertFalse(handoff.canNotify(first))
        assertTrue(checkNotNull(handoff.terminal(CaptureTerminal(second, null))).endsMicrophone)
    }

    @Test fun terminalAndPublicationAreClaimedExactlyOnce() {
        begin()
        val delivery = checkNotNull(handoff.terminal(CaptureTerminal(first, ready)))
        assertTrue(delivery.endsMicrophone); assertTrue(delivery.ownsRecording)
        assertSame(ready, delivery.event.outcome)
        assertTrue(handoff.canNotify(first))
        assertNull(handoff.terminal(CaptureTerminal(first, ready)))
        assertNull(handoff.terminal(CaptureTerminal(first, RecordingCapture.Outcome.Discarded(true))))
        assertNull(handoff.terminal(CaptureTerminal(first, RecordingCapture.Outcome.Failed(Exception()))))
        assertTrue(handoff.published(first)); assertFalse(handoff.published(first))
    }

    @Test fun discardClaimsTheSnapshotBeforeAnyLateSuccess() {
        begin()
        val discard = RecordingCapture.Outcome.Discarded(false)
        val delivery = checkNotNull(handoff.terminal(CaptureTerminal(first, discard)))
        assertSame(discard, delivery.event.outcome)
        assertTrue(delivery.endsMicrophone); assertTrue(delivery.ownsRecording)
        assertNull(handoff.terminal(CaptureTerminal(first, ready)))
        assertFalse(handoff.published(first))
    }

    @Test fun onlyTheRegisteredSnapshotCanBeUpdatedAndFinalizationFreezesIt() {
        begin()
        assertNull(handoff.update(second) { error("Unowned transformation") })
        assertEquals("first samples + final frame", handoff.update(first) { "$it + final frame" })
        val delivery = checkNotNull(handoff.terminal(CaptureTerminal(first, ready)))
        assertEquals("first samples + final frame", delivery.snapshot)
        assertNull(handoff.update(first) { error("Published snapshot must remain immutable") })
    }

    @Test fun stoppedAndUnknownMonitorEventsDoNotCreateRecordingPublications() {
        assertFalse(handoff.canNotify(first)); assertNull(handoff.terminal(CaptureTerminal(first, ready)))
        handoff.start(first); handoff.stop()
        assertNull(handoff.terminal(CaptureTerminal(first, null)))
        handoff.start(second)
        val delivery = checkNotNull(handoff.terminal(CaptureTerminal(second, null)))
        assertNull(delivery.snapshot); assertFalse(delivery.ownsRecording)
        assertTrue(delivery.endsMicrophone); assertTrue(handoff.canNotify(second))
        assertNull(handoff.terminal(CaptureTerminal(second, null)))
        assertFalse(handoff.published(second))
    }

    @Test fun aNewerMonitorKeepsNoticeOwnershipEvenAfterItStops() {
        begin(); handoff.stop(); handoff.start(second); handoff.stop()
        assertFalse(handoff.canNotify(first)); assertTrue(handoff.canNotify(second))
        assertNotNull(handoff.terminal(CaptureTerminal(first, ready)))
        assertTrue(handoff.published(first)); assertFalse(handoff.canNotify(first))
    }

    @Test fun registrationRequiresTheActiveMicrophoneAndCannotReuseAClaim() {
        assertThrows(IllegalStateException::class.java) { handoff.record(first, "unowned") }
        begin()
        assertThrows(IllegalStateException::class.java) { handoff.record(first, "duplicate") }
        handoff.terminal(CaptureTerminal(first, ready)); handoff.start(first)
        assertThrows(IllegalStateException::class.java) { handoff.record(first, "already publishing") }
    }
}
