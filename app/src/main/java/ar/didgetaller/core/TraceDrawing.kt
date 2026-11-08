package ar.didgetaller.core

/** Source-coordinate drawing decisions, without projection, acoustic inference or platform paths. */
object TraceDrawing {
    data class Vertex(val timeMs: Double, val hz: Double)
    sealed interface Mark
    data class Segment(val from: Vertex, val to: Vertex, val bridged: Boolean = false,
                       val tone: PitchTrace.Tone = PitchTrace.Tone.STABLE) : Mark
    data class Dot(val at: Vertex, val uncertain: Boolean = false,
                   val tone: PitchTrace.Tone = PitchTrace.Tone.STABLE) : Mark

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
