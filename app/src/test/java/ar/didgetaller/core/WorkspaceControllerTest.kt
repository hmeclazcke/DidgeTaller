package ar.didgetaller.core

import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class WorkspaceControllerTest {
    private class Pending<T>(val complete: (Result<T>) -> Unit) : WorkspaceTask {
        var cancellations = 0
        override fun cancel() { cancellations++ }
        fun success(value: T) = complete(Result.success(value))
        fun failure(error: Throwable = Exception("failed")) = complete(Result.failure(error))
    }
    private class Loader : WorkspaceLoader<String> {
        val loads = mutableListOf<Pair<String, Pending<WorkspaceSource>>>()
        val renders = mutableListOf<Triple<VisualWork, (Int) -> Unit, Pending<PreparedVisual<String>>>>()
        override fun open(sessionId: String, complete: (Result<WorkspaceSource>) -> Unit): WorkspaceTask =
            Pending(complete).also { loads += sessionId to it }
        override fun render(work: VisualWork, progress: (Int) -> Unit,
                            complete: (Result<PreparedVisual<String>>) -> Unit): WorkspaceTask =
            Pending(complete).also { renders += Triple(work, progress, it) }
    }
    private val source = WorkspaceSource("A", 44100, 44100, "fingerprint-A", null)
    private val request = VisualRequest(0, 1000)
    private val wave = AudioVisuals.Wave(0, 44100, 44100, floatArrayOf(-.5f), floatArrayOf(.5f), false)
    private val tile = AudioVisuals.Spectrogram(0, 44100, 44100, 0.0, 500.0, false,
        1, 1, floatArrayOf(1f), 1.0, 32768, 16384)

    @Test fun latestSessionOwnsMetadataAndAllOlderTerminalOutcomesAreHarmless() {
        val loader = Loader(); val snapshots = mutableListOf<WorkspaceState<String>>()
        val owner = WorkspaceController(loader, snapshots::add)
        owner.open("A"); val a = loader.loads.last().second
        owner.open("B"); val b = loader.loads.last().second
        assertEquals(1, a.cancellations)
        b.success(source.copy(sessionId = "B")); val expected = owner.state
        a.success(source); a.failure(); a.failure(CancellationException())
        b.success(source); b.failure()
        assertEquals(expected, owner.state); assertEquals("B", owner.state.source!!.sessionId)
        assertEquals(expected, snapshots.last()); assertFalse(owner.state.loading)
    }

    @Test fun rapidChangesRepeatedLoadsAndLeavingInvalidateEveryPreviousRequest() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        for (id in listOf("A", "B", "A", "A")) owner.open(id)
        owner.clear()
        loader.loads.forEach { (_, pending) -> pending.success(source); pending.failure() }
        assertNull(owner.state.sessionId); assertEquals(WorkspaceStatus.Empty, owner.state.status)
        assertTrue(loader.loads.all { it.second.cancellations == 1 })
        owner.open("B"); loader.loads.last().second.success(source.copy(sessionId = "B"))
        assertEquals("B", owner.state.sessionId)
    }

    @Test fun renderRetainsCompletedTileCoordinatesUntilItsReplacementIsAccepted() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source)
        owner.render("A", request, VisualKind.SPECTROGRAM)
        loader.renders.last().third.success(PreparedVisual.Spectral(tile, "image-A"))
        owner.render("A", request.copy(startMs = 100, endMs = 200), VisualKind.SPECTROGRAM)
        assertSame(tile, owner.state.spectrogram); assertEquals("image-A", owner.state.bitmap)
        val changed = tile.copy(start = 4410, end = 8820)
        loader.renders.last().third.success(PreparedVisual.Spectral(changed, "image-B"))
        assertSame(changed, owner.state.spectrogram); assertEquals("image-B", owner.state.bitmap)
        assertFalse(owner.state.loading); assertEquals(100, owner.state.progress)
    }

    @Test fun oldRenderCannotPublishProgressFailureOrPixelsAfterSessionSwitch() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source)
        owner.render("A", request, VisualKind.SPECTROGRAM); val old = loader.renders.single()
        owner.open("B"); old.second(71); old.third.success(PreparedVisual.Spectral(tile, "old")); old.third.failure()
        assertEquals("B", owner.state.sessionId); assertNull(owner.state.bitmap)
        assertEquals(0, owner.state.progress); assertEquals(WorkspaceStatus.Opening, owner.state.status)
    }

    @Test fun completionClaimsPublicationOnceAndLateProgressCannotRegressIt() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source)
        owner.render("A", request, VisualKind.WAVE); val work = loader.renders.single()
        work.second(-5); assertEquals(0, owner.state.progress)
        work.second(130); assertEquals(100, owner.state.progress)
        work.third.success(PreparedVisual.Wave(wave)); val expected = owner.state
        work.second(2); work.third.failure(); work.third.success(PreparedVisual.Spectral(tile, "unexpected"))
        assertEquals(expected, owner.state); assertNull(owner.state.bitmap)
    }

    @Test fun cancellationRetainsLastCompletedDataButRejectsPendingWorkAndAllowsRetry() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source)
        owner.render("A", request, VisualKind.WAVE); loader.renders.last().third.success(PreparedVisual.Wave(wave))
        owner.render("A", request, VisualKind.SPECTROGRAM); val pending = loader.renders.last()
        owner.cancel(); pending.second(50); pending.third.success(PreparedVisual.Spectral(tile, "late"))
        assertSame(wave, owner.state.wave); assertNotNull(owner.state.error); assertNull(owner.state.bitmap)
        assertEquals(WorkspaceStatus.Cancelled, owner.state.status)
        assertEquals(1, pending.third.cancellations)
        owner.render("A", request, VisualKind.SPECTROGRAM)
        assertEquals(3, loader.renders.size)
    }

    @Test fun dataCacheKeysIncludeViewportButPaletteChangesReusePowerOnly() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source)
        owner.render("A", request, VisualKind.SPECTROGRAM); loader.renders.last().third.success(PreparedVisual.Spectral(tile, "old"))
        val palette = request.copy(rangeDb = 40.0, reference = 2.0)
        owner.render("A", palette, VisualKind.SPECTROGRAM)
        assertSame(tile, loader.renders.last().first.tile)
        owner.render("A", palette.copy(lowHz = 20.0), VisualKind.SPECTROGRAM)
        assertNull(loader.renders.last().first.tile)
        assertSame(tile, owner.state.spectrogram)
    }

    @Test fun repeatedRenderDoesNotCancelIdenticalWorkOrRebuildCompletedPixels() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source)
        repeat(3) { owner.render("A", request, VisualKind.WAVE) }
        assertEquals(1, loader.renders.size)
        loader.renders.single().third.success(PreparedVisual.Wave(wave))
        owner.render("A", request, VisualKind.WAVE)
        assertEquals(1, loader.renders.size)
    }

    @Test fun errorsWithoutMessagesRemainVisibleAndWrongSessionMetadataCannotBeAccepted() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.failure(Exception())
        assertFalse(owner.state.loading); assertFalse(owner.state.error.isNullOrBlank())
        owner.open("A"); loader.loads.last().second.success(source.copy(sessionId = "B"))
        assertNull(owner.state.source); assertNotNull(owner.state.error)
        owner.open("A"); loader.loads.last().second.success(source)
        owner.render("A", request, VisualKind.WAVE); loader.renders.last().third.failure()
        assertSame(source, owner.state.source); assertNotNull(owner.state.error)
    }

    @Test fun renderingBeforeReadinessWrongSessionOrInsufficientFramesStartsNoWork() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.render("A", request, VisualKind.WAVE); owner.open("A"); owner.render("A", request, VisualKind.WAVE)
        loader.loads.last().second.success(source.copy(frames = 1))
        owner.render("A", request, VisualKind.WAVE); owner.render("B", request, VisualKind.WAVE)
        assertTrue(loader.renders.isEmpty())
    }

    @Test fun twoFramesAreRenderableAndChangingViewportCancelsThePreviousTask() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source.copy(frames = 2))
        owner.render("A", request, VisualKind.WAVE); val first = loader.renders.single()
        owner.render("A", request.copy(endMs = 500), VisualKind.WAVE)
        assertEquals(1, first.third.cancellations)
        first.third.success(PreparedVisual.Wave(wave)); assertNull(owner.state.wave)
        loader.renders.last().third.success(PreparedVisual.Wave(wave)); assertSame(wave, owner.state.wave)
        owner.render("A", request.copy(endMs = 500), VisualKind.SPECTROGRAM)
        assertSame(wave, loader.renders.last().first.wave)
        owner.render("A", request, VisualKind.SPECTROGRAM)
        assertNull(loader.renders.last().first.wave)
    }

    @Test fun openingPublishesOneSelectedLoadingStateWithoutAnIntermediateEmptySnapshot() {
        val loader = Loader(); val states = mutableListOf<WorkspaceState<String>>()
        val owner = WorkspaceController(loader, states::add)
        owner.open("A")
        assertEquals(1, states.size); assertEquals("A", states.single().sessionId)
    }

    @Test fun observerDisposalBeforeDispatchStartsNoLoaderAndBlankErrorsRemainVisible() {
        val loader = Loader(); lateinit var owner: WorkspaceController<String>
        owner = WorkspaceController(loader) { if (it.status == WorkspaceStatus.Opening) owner.close() }
        owner.open("A"); assertTrue(loader.loads.isEmpty())
        val other = WorkspaceController(loader) {}
        other.open("B"); loader.loads.single().second.failure(Exception("  "))
        assertEquals("Visualization unavailable.", other.state.error)
    }

    @Test fun disposalCannotBeReopenedAndPendingCallbacksCannotPublish() {
        val loader = Loader(); val states = mutableListOf<WorkspaceState<String>>()
        val owner = WorkspaceController(loader, states::add)
        owner.open("A"); owner.close(); val count = states.size
        loader.loads.single().second.success(source); owner.open("B"); owner.render("A", request, VisualKind.WAVE)
        owner.close(); owner.cancel()
        owner.clear()
        assertEquals(count, states.size); assertNull(owner.state.sessionId); assertEquals(1, loader.loads.size)
    }

    @Test fun malformedLoaderResponsesAreRecoverableErrorsRatherThanUiExceptions() {
        val loader = Loader(); val owner = WorkspaceController(loader) {}
        owner.open("A"); loader.loads.last().second.success(source)
        owner.render("A", request, VisualKind.WAVE)
        loader.renders.last().third.success(PreparedVisual.Spectral(tile, "wrong-kind"))
        assertFalse(owner.state.loading); assertNotNull(owner.state.error); assertNull(owner.state.bitmap)
    }

    @Test fun inlineCompletionAndObserverReplacementCannotBindAnOldHandleToTheNewLoad() {
        val pending = mutableListOf<Pending<WorkspaceSource>>()
        val loader = object : WorkspaceLoader<String> {
            override fun open(sessionId: String, complete: (Result<WorkspaceSource>) -> Unit): WorkspaceTask {
                val task = Pending(complete); pending += task
                if (sessionId == "A") task.success(source)
                return task
            }
            override fun render(work: VisualWork, progress: (Int) -> Unit,
                                complete: (Result<PreparedVisual<String>>) -> Unit): WorkspaceTask = error("Not rendering")
        }
        lateinit var owner: WorkspaceController<String>
        owner = WorkspaceController(loader) { if (it.source?.sessionId == "A") owner.open("B") }
        owner.open("A")
        assertEquals(1, pending.first().cancellations); assertEquals(0, pending.last().cancellations)
        pending.last().success(source.copy(sessionId = "B"))
        assertEquals("B", owner.state.source!!.sessionId)
    }
}
