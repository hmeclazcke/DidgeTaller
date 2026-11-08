package ar.didgetaller.core

import ar.didgetaller.audio.CoroutineWorkspaceLoader
import java.io.Closeable
import java.io.FileNotFoundException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class WorkspaceCoroutineLoaderTest {
    private class Queue : CoroutineDispatcher() {
        val pending = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { pending += block }
        fun drain() { while (pending.isNotEmpty()) pending.removeFirst().run() }
    }
    private class Source(override val frameCount: Long, private val afterRead: () -> Unit) : MonoPcmSource, Closeable {
        override val sampleRate = 44100
        var reads = 0
        var closed = false
        override fun close() { check(!closed); closed = true }
        override fun read(startFrame: Long, destination: ShortArray): Int {
            check(!closed); destination.fill(1000); reads++; afterRead()
            return minOf(destination.size.toLong(), frameCount - startFrame).toInt()
        }
    }
    private class Harness {
        val ui = Queue(); val worker = Queue(); val failures = mutableListOf<Throwable>()
        val scope = CoroutineScope(SupervisorJob() + ui + CoroutineExceptionHandler { _, error -> failures += error })
        val readers = mutableListOf<Source>()
        var missing = false
        var images = 0
        var frames = 44100L
        var onRead: () -> Unit = {}
        val data = WorkspaceData({ _: String ->
            if (missing) throw FileNotFoundException("missing synthetic WAV")
            Source(frames, onRead).also { readers += it }
        }, { _: String, _: () -> Boolean -> "signature" },
            { _: String, _: String, _: String, compute: () -> AudioVisuals.Spectrogram -> compute() })
        val loader = CoroutineWorkspaceLoader(scope, data, { _: WorkspaceRaster -> images++; "image-$images" }, worker, worker)
        val owner = WorkspaceController(loader) {}
        fun opened() { owner.open("A"); ui.drain(); worker.drain(); ui.drain() }
    }

    @Test fun actualCoroutinesPublishOnlyOnTheOwnerAndCloseReaders() {
        val h = Harness(); h.owner.open("A")
        assertEquals(WorkspaceStatus.Opening, h.owner.state.status)
        h.ui.drain(); assertTrue(h.readers.isEmpty())
        h.worker.drain(); assertTrue(h.owner.state.loading); assertTrue(h.readers.single().closed)
        h.ui.drain(); assertEquals(WorkspaceStatus.Ready, h.owner.state.status)
        h.owner.render("A", VisualRequest(0, 1000), VisualKind.SPECTROGRAM)
        h.ui.drain(); h.worker.drain(); assertTrue(h.owner.state.loading)
        h.ui.drain(); assertEquals("image-1", h.owner.state.bitmap); assertEquals(100, h.owner.state.progress)
        assertTrue(h.readers.all { it.closed }); assertTrue(h.failures.isEmpty())
    }

    @Test fun cancellationAfterWorkerCompletionRejectsQueuedMetadataAndProgress() {
        val h = Harness(); h.owner.open("A"); h.ui.drain(); h.worker.drain()
        h.owner.open("B"); h.ui.drain(); assertNull(h.owner.state.source)
        h.worker.drain(); h.ui.drain(); assertEquals("B", h.owner.state.source!!.sessionId)
        h.owner.render("B", VisualRequest(0, 1000), VisualKind.SPECTROGRAM)
        h.ui.drain(); h.worker.drain(); h.owner.clear(); h.ui.drain()
        assertEquals(WorkspaceStatus.Empty, h.owner.state.status); assertNull(h.owner.state.bitmap)
        assertTrue(h.readers.all { it.closed }); assertTrue(h.failures.isEmpty())
    }

    @Test fun missingInputFailsVisiblyAndAnAlreadyCancelledScopeCannotStartWork() {
        val h = Harness(); h.missing = true; h.opened()
        assertEquals("missing synthetic WAV", h.owner.state.error); assertFalse(h.owner.state.loading)
        h.scope.cancel(); h.owner.open("B")
        assertEquals(WorkspaceStatus.Cancelled, h.owner.state.status); assertFalse(h.owner.state.loading)
        assertTrue(h.readers.isEmpty()); assertTrue(h.failures.isEmpty())
    }

    @Test fun disposalCancelsQueuedWorkWithoutOpeningAReaderOrReopeningTheOwner() {
        val h = Harness(); h.owner.open("A"); h.ui.drain(); h.owner.close()
        h.worker.drain(); h.ui.drain(); h.scope.cancel(); h.owner.open("B")
        assertTrue(h.readers.isEmpty()); assertEquals(WorkspaceStatus.Empty, h.owner.state.status)
        assertTrue(h.failures.isEmpty())
    }

    @Test fun uiObserverFailuresAreNotReportedAgainAsAnalysisFailures() {
        val h = Harness(); var calls = 0
        h.loader.open("A") { calls++; throw IllegalStateException("observer failure") }
        h.ui.drain(); h.worker.drain(); h.ui.drain()
        assertEquals(1, calls); assertEquals("observer failure", h.failures.single().message)
        assertTrue(h.readers.single().closed)
    }

    @Test fun parentCancellationDuringWavePreparationStopsReadingAndClosesTheFile() {
        val h = Harness(); h.frames = 1_000_000; h.opened()
        h.onRead = { h.scope.cancel() }
        h.owner.render("A", VisualRequest(0, 20000), VisualKind.WAVE)
        h.ui.drain(); h.worker.drain(); h.ui.drain()
        assertEquals(1, h.readers.last().reads); assertTrue(h.readers.last().closed)
        assertEquals(WorkspaceStatus.Cancelled, h.owner.state.status)
        assertTrue(h.failures.isEmpty())
    }
}
