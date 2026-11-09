package ar.didgetaller.core

data class TraceProduct(val request: MusicalTraceRequest, val curve: DroneComponentCurve, val trace: MusicalTrace)
data class TraceSnapshot(val products: Map<String, TraceProduct> = emptyMap(), val failures: Set<String> = emptySet())

/** Read-only evidence cache and independently keyed work share one bounded session lifetime. */
class MusicalTraceCatalog(loader: AnalysisLoader<ComponentRequest, DroneComponentCurve>,
    private val changed: (TraceSnapshot) -> Unit, private val capacity: Int = 6) {
    init { require(capacity >= 2) }
    private val products = linkedMapOf<String, TraceProduct>()
    private val failures = mutableSetOf<String>()
    private val flutes = linkedMapOf<MusicalTraceRequest, MusicalTrace>()
    private var pinned: String? = null
    private var closed = false
    private val pending = SessionAnalyses(AnalysisLoader<MusicalTraceRequest, DroneComponentCurve> { r, p, c ->
        loader.submit(ComponentRequest(r.sessionId), p, c)
    }, ::loaded)
    var snapshot = TraceSnapshot(); private set

    fun request(request: MusicalTraceRequest) {
        if (closed || request.instrument != InstrumentProfile.DIDGERIDOO) return
        if (products[request.sessionId]?.request == request || request.sessionId in failures) return
        pending.select(request)
    }

    private fun loaded(state: AnalysisState<MusicalTraceRequest, DroneComponentCurve>) {
        val request = state.request ?: return
        when (state) {
            is AnalysisState.Ready -> {
                products[request.sessionId] = TraceProduct(request, state.result,
                    MusicalTrace.didgeridoo(state.result, request.durationMs, request.captured))
                trim(request.sessionId)
            }
            is AnalysisState.Failed -> failures += request.sessionId
            else -> return
        }
        publish()
    }

    private fun trim(current: String) {
        while (products.size > capacity) {
            val id = products.keys.first { it != pinned && it != current }
            products.remove(id); pending.remove(id)
        }
    }

    /** A snapshot is the UI read model; flute memoization never starts asynchronous work. */
    fun trace(request: MusicalTraceRequest, view: TraceSnapshot = snapshot): MusicalTrace? {
        if (request.instrument == InstrumentProfile.DIDGERIDOO)
            return view.products[request.sessionId]?.takeIf { it.request == request }?.trace
        return flutes.getOrPut(request) {
            MusicalTrace.flute(request.captured, request.durationMs).also {
                while (flutes.size >= capacity) flutes.remove(flutes.keys.first())
            }
        }
    }

    fun pin(id: String?) { if (!closed) pinned = id }

    fun release(id: String) {
        if (closed || id == pinned) return
        pending.remove(id)
        if (failures.remove(id)) publish()
    }

    fun remove(id: String) {
        if (closed) return
        pending.remove(id); products.remove(id); failures.remove(id)
        flutes.keys.removeAll { it.sessionId == id }
        publish()
    }

    fun close() { if (!closed) { closed = true; pending.close() } }
    private fun publish() { snapshot = TraceSnapshot(products.toMap(), failures.toSet()); changed(snapshot) }
}
