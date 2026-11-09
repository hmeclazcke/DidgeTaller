package ar.didgetaller.ui

import ar.didgetaller.core.*
import org.junit.Assert.*
import org.junit.Test

class ComparisonCamerasTest {
    private val a = SwipeComparisonRequest("a")
    private val b = SwipeComparisonRequest("b")
    private val curve = SwipeCurve(emptyList(), 10.0, 35, 140)
    private val original = PlotViewport(AxisWindow(0.0, 1000.0), AxisWindow(60.0, 100.0))
    private fun explorer() = ExplorerState(InstrumentProfile.DIDGERIDOO).apply { cameras[AcousticPlot.PITCH] = original }
    private fun ExplorerState.view() = camera(1000, 44100, emptyList())

    @Test fun enablingUsesTheExistingCameraAndProgressOrReadinessCannotResetAPan() {
        val models = mutableMapOf("a" to explorer())
        val create: (String) -> ExplorerState = { models.getOrPut(it) { explorer() } }
        val loading = SwipeComparisonStatus.Loading(a)
        reconcileComparisonCameras(SwipeComparisonStatus.Idle, loading, models, create)
        assertEquals(AxisWindow(33.25, 147.0), models.getValue("a").view().y)
        val panned = original.copy(x = AxisWindow(100.0, 500.0), y = AxisWindow(50.0, 120.0))
        models.getValue("a").cameras[AcousticPlot.PITCH] = panned
        val progress = SwipeComparisonStatus.Loading(a, 70)
        reconcileComparisonCameras(loading, progress, models, create)
        reconcileComparisonCameras(progress, SwipeComparisonStatus.Ready(a, curve), models, create)
        assertEquals(panned, models.getValue("a").view())
    }

    @Test fun disablingAndDisposalRestoreTheOriginalViewportWithoutCreatingAnExplorer() {
        val model = explorer(); val models = mapOf("a" to model)
        model.configureComparisonRange(swipeComparisonWindow(true))
        reconcileComparisonCameras(SwipeComparisonStatus.Ready(a, curve), SwipeComparisonStatus.Disabled(a, curve),
            models) { error("Disabling must not create a camera") }
        assertEquals(original, model.view())
        model.configureComparisonRange(swipeComparisonWindow(true))
        reconcileComparisonCameras(SwipeComparisonStatus.Loading(a), SwipeComparisonStatus.Disposed,
            models) { error("Disposal must not create a camera") }
        assertEquals(original, model.view())
    }

    @Test fun replacingTheEnabledSessionRestoresItsPredecessorAndCreatesOnlyTheNewCamera() {
        val first = explorer(); first.configureComparisonRange(swipeComparisonWindow(true))
        val models = mutableMapOf("a" to first); val created = mutableListOf<String>()
        reconcileComparisonCameras(SwipeComparisonStatus.Ready(a, curve), SwipeComparisonStatus.Loading(b), models) {
            created += it; explorer().also { model -> models[it] = model }
        }
        assertEquals(listOf("b"), created); assertEquals(original, first.view())
        assertEquals(AxisWindow(33.25, 147.0), models.getValue("b").view().y)
    }

    @Test fun absentRetiredOrDisabledSessionsNeverAcquireNewCameras() {
        val created = mutableListOf<String>()
        reconcileComparisonCameras(SwipeComparisonStatus.Ready(a, curve), SwipeComparisonStatus.Idle, emptyMap()) {
            created += it; explorer()
        }
        reconcileComparisonCameras(SwipeComparisonStatus.Disabled(a), SwipeComparisonStatus.Disabled(b), emptyMap()) {
            created += it; explorer()
        }
        assertTrue(created.isEmpty())
    }
}
