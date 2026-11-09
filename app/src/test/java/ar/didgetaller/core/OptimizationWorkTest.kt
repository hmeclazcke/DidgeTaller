package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class OptimizationWorkTest {
    private val request = OptimizationRequest(AcousticModel.Shape(1500, 30, 40, 60),
        BoreOptimizer.Target(), BoreOptimizer.Settings(1, 4, 7))
    @Test fun wrapperPreservesTheOriginalSeededSearchAndPublishesImmutablePartialBest() {
        val progress = mutableListOf<AnalysisProgress<OptimizationSnapshot>>()
        val expected = BoreOptimizer.search(request.shape, request.target, request.settings)
        val result = optimizeBore(request, { false }, progress::add)
        assertEquals(expected, result.best)
        assertTrue(progress.isNotEmpty()); assertEquals(1, result.generation)
        assertTrue(progress.all { it.preview != null && it.percent == it.preview.generation })
        assertEquals(progress.last().preview?.best, result.best)
    }
    @Test fun cancellationBeforeSearchProducesNoCandidateOrProgress() {
        val progress = mutableListOf<AnalysisProgress<OptimizationSnapshot>>()
        val result = optimizeBore(request, { true }, progress::add)
        assertNull(result.best); assertEquals(0, result.generation); assertTrue(progress.isEmpty())
    }
    @Test fun terminalNoticesPreserveExistingLocalizedDiagnosticContracts() {
        assertNull(optimizationNotice(AnalysisState.Idle))
        assertNull(optimizationNotice(AnalysisState.Disposed))
        assertNull(optimizationNotice(AnalysisState.Loading(request)))
        assertNull(optimizationNotice(AnalysisState.Ready(request, OptimizationSnapshot(0, null))))
        val best = BoreOptimizer.Candidate(request.shape, 12.0, listOf(70.0))
        assertEquals("Búsqueda terminada: probá el diseño encontrado.",
            optimizationNotice(AnalysisState.Ready(request, OptimizationSnapshot(1, best))))
        assertEquals("Error de búsqueda: invalid", optimizationNotice(AnalysisState.Failed(request, 0, Exception("invalid"))))
        assertEquals("Búsqueda cancelada. Podés aplicar el mejor resultado parcial.",
            optimizationNotice(AnalysisState.Cancelled(request, 10)))
    }
}
