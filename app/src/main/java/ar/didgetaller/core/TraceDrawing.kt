package ar.didgetaller.core

/** Source-coordinate drawing decisions, without projection, acoustic inference or platform paths. */
object TraceDrawing {
    data class Vertex(val timeMs: Double, val hz: Double)
    sealed interface Mark
    data class Segment(val from: Vertex, val to: Vertex, val bridged: Boolean = false,
                       val tone: PitchTrace.Tone = PitchTrace.Tone.STABLE) : Mark
    data class Dot(val at: Vertex, val uncertain: Boolean = false,
                   val tone: PitchTrace.Tone = PitchTrace.Tone.STABLE) : Mark

    data class Prepared(val historical: List<PitchTrace.Sample>, val marks: List<Mark>, val evidence: TraceEvidence)

    /** Canonical absence and a canonical gap are distinct; a gap never falls back to historical pitch. */
    fun prepare(trace: MusicalTrace?, points: List<SessionAnalysis.Point>, instrument: InstrumentProfile,
                visible: Boolean, showUncertain: Boolean): Prepared {
        val history = if (trace == null) PitchTrace.build(points, instrument) else emptyList()
        val marks = if (!visible) emptyList() else trace?.let(::canonical) ?: historical(history, showUncertain)
        return Prepared(history, marks, TraceEvidence.of(trace, history))
    }

    /** Comparison evidence keeps its own gaps and one-hop viewport margin. */
    fun comparison(curve: SwipeCurve, window: AxisWindow): List<Mark> = curve.samples.indices
        .filter { index -> curve.samples[index].let { it.hz != null && it.timeMs >= window.low - curve.hopMs && it.timeMs <= window.high + curve.hopMs } }
        .mapNotNull { comparisonMark(curve, it) }

    private fun comparisonMark(curve: SwipeCurve, index: Int): Mark? {
        val sample = curve.samples[index]
        val at = Vertex(sample.timeMs, checkNotNull(sample.hz))
        if (curve.linked(index)) {
            val previous = curve.samples[index - 1]
            return Segment(Vertex(previous.timeMs, checkNotNull(previous.hz)), at)
        }
        return if (!curve.linked(index + 1)) Dot(at) else null
    }

    fun canonical(trace: MusicalTrace): List<Mark> = buildList {
        for ((index, sample) in trace.samples.withIndex()) {
            val hz = sample.hz ?: continue
            val at = Vertex(sample.timeMs, hz)
            val prior = trace.samples.getOrNull(index - 1)
            if (sample.linkedToPrevious && prior?.hz != null) {
                add(Segment(Vertex(prior.timeMs, prior.hz), at, sample.bridged))
            } else if (trace.samples.getOrNull(index + 1)?.linkedToPrevious != true) {
                add(Dot(at))
            }
        }
    }

    fun historical(trace: List<PitchTrace.Sample>, showUncertain: Boolean): List<Mark> = buildList {
        for (sample in trace) {
            val point = sample.point
            // PitchTrace.build omits absent readings; malformed display input is not another gap.
            val hz = requireNotNull(point.hz) { "Historical drawing requires a rendered pitch sample" }
            val at = Vertex(point.timeMs.toDouble(), hz)
            if (sample.uncertain) {
                if (showUncertain) add(Dot(at, uncertain = true))
                continue
            }
            val prior = sample.previous?.let { trace[it].point }
            if (prior?.hz != null && point.timeMs - prior.timeMs <= 1200) {
                add(Segment(Vertex(prior.timeMs.toDouble(), prior.hz), at, sample.bridged, sample.tone))
            } else {
                add(Dot(at, tone = sample.tone))
            }
        }
    }
}
