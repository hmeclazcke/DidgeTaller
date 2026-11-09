package ar.didgetaller.core

data class OptimizationRequest(val shape: AcousticModel.Shape, val target: BoreOptimizer.Target,
    val settings: BoreOptimizer.Settings = BoreOptimizer.Settings())
data class OptimizationSnapshot(val generation: Int, val best: BoreOptimizer.Candidate?)

/** Preserve the experiment's partial-best and cancellation semantics behind the shared owner. */
fun optimizeBore(request: OptimizationRequest, cancelled: () -> Boolean,
    progress: (AnalysisProgress<OptimizationSnapshot>) -> Unit): OptimizationSnapshot {
    var generation = 0
    val best = BoreOptimizer.search(request.shape, request.target, request.settings, cancelled) { step, candidate ->
        generation = step
        progress(AnalysisProgress(step, OptimizationSnapshot(step, candidate)))
    }
    return OptimizationSnapshot(generation, best)
}

/** Existing diagnostic text remains a localized compatibility boundary. */
fun optimizationNotice(state: AnalysisState<OptimizationRequest, OptimizationSnapshot>): String? = when (state) {
    is AnalysisState.Ready -> state.result.best?.let { "Búsqueda terminada: probá el diseño encontrado." }
    is AnalysisState.Failed -> "Error de búsqueda: ${state.failure?.message}"
    is AnalysisState.Cancelled -> "Búsqueda cancelada. Podés aplicar el mejor resultado parcial."
    else -> null
}
