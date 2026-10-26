package ar.didgetaller.core

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

/** One measured quantity for the musician-facing session views. Original evidence stays separate. */
data class MusicalTrace(
    val instrument: InstrumentProfile,
    val meaning: Meaning,
    val durationMs: Long,
    val samples: List<Sample>,
    val highRegisterCandidates: List<HighRegisterCandidate> = emptyList(),
) {
    enum class Meaning { SPECTRAL_COMPONENT, ESTIMATED_PITCH }

    sealed interface Evidence {
        data class Spectral(
            val candidateHz: Double?,
            val rms: Double,
            val reason: DroneComponentCurve.Reason,
        ) : Evidence

        data class Yin(val periodicity: Double, val reason: PitchTrace.Reason?) : Evidence
    }

    data class Sample(
        val timeMs: Double,
        val hz: Double?,
        val linkedToPrevious: Boolean,
        val evidence: Evidence,
        val bridged: Boolean = false,
    )
    /** Historical YIN evidence, marked separately; it does not change the spectral trace. */
    data class HighRegisterCandidate(val timeMs:Long,val hz:Double)

    fun frequencyRange(): AxisWindow {
        val frequencies = samples.mapNotNull { it.hz }
        if (frequencies.isEmpty()) return AxisWindow(instrument.minHz.toDouble(), instrument.initialMaxHz.toDouble())
        val context = 2.0.pow(3.0 / 12.0)
        return AxisWindow(frequencies.min() / context, frequencies.max() * context)
    }

    fun medianHz(): Double? {
        val frequencies = samples.mapNotNull { it.hz }.sorted()
        if (frequencies.isEmpty()) return null
        val middle = frequencies.size / 2
        return if (frequencies.size % 2 == 0) (frequencies[middle - 1] + frequencies[middle]) / 2 else frequencies[middle]
    }
    fun normalized(sample: Sample,range:AxisWindow=frequencyRange()): Pair<Double,Double>? {
        val hz=sample.hz ?: return null
        return (sample.timeMs / durationMs.coerceAtLeast(2)).coerceIn(0.0,1.0) to
            ((range.high-hz)/range.span).coerceIn(0.0,1.0)
    }

    companion object {
        fun unavailable(instrument:InstrumentProfile,durationMs:Long)=MusicalTrace(
            instrument,if(instrument==InstrumentProfile.DIDGERIDOO)Meaning.SPECTRAL_COMPONENT else Meaning.ESTIMATED_PITCH,
            durationMs,emptyList())
        fun didgeridoo(curve: DroneComponentCurve, durationMs: Long,
                       historical:List<SessionAnalysis.Point> = emptyList()): MusicalTrace {
            val samples = curve.samples.mapIndexed { index, point ->
                val previous = curve.samples.getOrNull(index - 1)
                val linked = point.hz != null && previous?.hz != null &&
                    point.timeMs - previous.timeMs in 0.0..(curve.hopMs * 1.01) &&
                    abs(1200 * log2(point.hz / previous.hz)) < 300
                Sample(
                    point.timeMs, point.hz, linked,
                    Evidence.Spectral(point.candidateHz, point.rms, point.reason),
                )
            }
            val baseline=MusicalTrace(InstrumentProfile.DIDGERIDOO,Meaning.SPECTRAL_COMPONENT,durationMs,samples).medianHz()
            val candidates=if(baseline==null)emptyList() else {
                val original=PitchTrace.build(historical,InstrumentProfile.DIDGERIDOO)
                original.mapIndexedNotNull {index,reading ->
                    val hz=reading.point.hz ?: return@mapIndexedNotNull null
                    if(reading.uncertain || reading.point.confidence<.90 ||
                        hz<maxOf(InstrumentProfile.DIDGERIDOO.initialMaxHz.toDouble(),baseline*1.7))
                        return@mapIndexedNotNull null
                    val neighbor=listOfNotNull(original.getOrNull(index-1),original.getOrNull(index+1))
                        .any {other -> other.point.hz?.let {frequency ->
                            !other.uncertain && other.point.confidence>=.90 &&
                                abs(other.point.timeMs-reading.point.timeMs)<=350 &&
                                abs(1200*log2(frequency/hz))<=80
                        }==true }
                    if(neighbor)HighRegisterCandidate(reading.point.timeMs,hz) else null
                }
            }
            return MusicalTrace(InstrumentProfile.DIDGERIDOO, Meaning.SPECTRAL_COMPONENT,
                durationMs, samples,candidates)
        }

        fun flute(points: List<SessionAnalysis.Point>, durationMs: Long): MusicalTrace {
            val trace = PitchTrace.build(points, InstrumentProfile.NATIVE_AMERICAN_FLUTE)
            val samples = trace.map { point ->
                Sample(
                    point.point.timeMs.toDouble(), point.point.hz.takeUnless { point.uncertain },
                    !point.uncertain && point.previous != null,
                    Evidence.Yin(point.point.confidence, point.reason),
                    point.bridged,
                )
            }
            return MusicalTrace(InstrumentProfile.NATIVE_AMERICAN_FLUTE, Meaning.ESTIMATED_PITCH, durationMs, samples)
        }
    }
}
