package ar.didgetaller.ui

import ar.didgetaller.R
import ar.didgetaller.core.*
import kotlin.math.*

fun interface InspectionStrings { fun text(resource: Int, vararg arguments: Any): String }
internal enum class InspectionMode { MUSICAL, PERIODICITY, COMPARISON }
internal fun inspectionMode(diagnostic: Boolean, comparison: Boolean) = when {
    !diagnostic -> InspectionMode.MUSICAL
    comparison -> InspectionMode.COMPARISON
    else -> InspectionMode.PERIODICITY
}
internal data class InspectionData(val musical: MusicalTrace, val points: List<SessionAnalysis.Point>,
    val pitch: List<PitchTrace.Sample>, val rate: Int, val wave: AudioVisuals.Wave? = null,
    val tile: AudioVisuals.Spectrogram? = null, val harmonics: HarmonicAnalysis.Result? = null,
    val reference: Double = 0.0, val comparison: SwipeCurve? = null)

/** Pure presentation policy: select original evidence, then ask the locale boundary for text. */
internal class SessionInspector(private val strings: InspectionStrings, private val reason: (PitchTrace.Reason?) -> String) {
    fun temporal(plot: AcousticPlot, time: Long, frequency: Double, data: InspectionData, mode: InspectionMode): String {
        val reading = when (plot) {
            AcousticPlot.PITCH -> pitch(time, data, mode)
            AcousticPlot.WAVE -> wave(time, data)
            else -> cell(time, frequency, data)
        }
        if (plot != AcousticPlot.PITCH || mode != InspectionMode.COMPARISON) return reading
        val measured = data.comparison?.nearest(time)?.hz
        return reading + "\n" + strings.text(R.string.swipe_cursor,
            measured?.let(::hzLabel) ?: strings.text(R.string.swipe_missing))
    }

    private fun pitch(time: Long, data: InspectionData, mode: InspectionMode): String {
        val reading = if (mode == InspectionMode.MUSICAL && data.musical.meaning == MusicalTrace.Meaning.SPECTRAL_COMPONENT) {
            data.musical.frequencyAt(time)?.let { strings.text(R.string.musical_component_frequency, hzLabel(it)) }
                ?: strings.text(R.string.ui_no_nearby_reliable_reading)
        } else {
            val point = data.points.minByOrNull { abs(it.timeMs - time) }?.takeIf { abs(it.timeMs - time) <= 750 }
            val sample = point?.let { p -> data.pitch.firstOrNull { it.point === p } }
            sample?.let(::describePitch) ?: strings.text(R.string.ui_no_nearby_reliable_reading)
        }
        return "${seconds(time)} · $reading"
    }

    private fun describePitch(sample: PitchTrace.Sample): String {
        val hz = sample.point.hz ?: return strings.text(R.string.ui_no_nearby_reliable_reading)
        if (sample.uncertain) return strings.text(R.string.ui_uncertain_estimate_confidence_yin_no_confirmed_note,
            hzLabel(hz), reason(sample.reason), sample.point.confidence)
        val musical = Music.pitch(hz) ?: return strings.text(R.string.ui_no_nearby_reliable_reading)
        return strings.text(R.string.ui_cents_a4_440_periodicity_variation, hzLabel(hz), musical.note,
            musical.cents, sample.point.confidence,
            sample.variationCents?.let { "%.0f cents".format(displayLocale, it) } ?: strings.text(R.string.ui_no_neighbor)) +
            if (sample.bridged) strings.text(R.string.ui_display_link) else ""
    }

    private fun wave(time: Long, data: InspectionData): String {
        val wave = data.wave; val frame = time * data.rate / 1000
        if (wave == null || frame !in wave.start until wave.end || wave.min.isEmpty() || wave.max.size != wave.min.size)
            return strings.text(R.string.ui_waiting_for_samples_from_this_region, seconds(time))
        val index = ((frame - wave.start) * wave.min.size / (wave.end - wave.start)).toInt().coerceIn(wave.min.indices)
        return strings.text(R.string.ui_pcm_amplitude_to, seconds(time),
            axisLabel(wave.min[index].toDouble(), .1), axisLabel(wave.max[index].toDouble(), .1))
    }

    private fun cell(time: Long, frequency: Double, data: InspectionData): String {
        val power = cellPower(data.tile, time * data.rate / 1000, frequency)
            ?: return strings.text(R.string.ui_no_calculated_cell, seconds(time), hzLabel(frequency))
        val energy = if (data.reference > 1e-16)
            strings.text(R.string.ui_db_rel_stft_cell, 10 * log10(max(1e-30, power) / data.reference))
            else strings.text(R.string.ui_no_significant_energy)
        return "${seconds(time)} · ${hzLabel(frequency)} · $energy"
    }

    private fun cellPower(tile: AudioVisuals.Spectrogram?, frame: Long, frequency: Double): Double? {
        if (tile == null || frame !in tile.start until tile.end || frequency !in tile.lowHz..tile.highHz) return null
        if (tile.columns <= 0 || tile.rows <= 0 || tile.power.size.toLong() < tile.columns.toLong() * tile.rows) return null
        val column = ((frame - tile.start).toDouble() / (tile.end - tile.start) * tile.columns).toInt().coerceIn(0, tile.columns - 1)
        val row = (AxisWindow(tile.lowHz, tile.highHz).fraction(frequency, tile.logarithmic) * tile.rows).toInt().coerceIn(0, tile.rows - 1)
        return tile.power[column * tile.rows + row].toDouble()
    }

    fun peak(frequency: Double, span: Double, data: InspectionData): String {
        val peak = data.harmonics?.peaks?.minByOrNull { abs(it.hz - frequency) }
        if (peak == null || abs(peak.hz - frequency) >= span * .06)
            return strings.text(R.string.ui_no_nearby_assigned_peak, hzLabel(frequency))
        val identity = peak.order?.let { strings.text(R.string.ui_h_candidate, it) }
            ?: strings.text(R.string.ui_unassigned_peak_66f03c)
        return "$identity · ${hzLabel(peak.hz)} · %.1f dB rel".format(displayLocale, peak.relativeDb) +
            (peak.deviationCents?.let { " · %+.1f cents".format(displayLocale, it) } ?: "")
    }
}

/** Pending decoding is not evidence that the recording contains no supported component. */
internal fun traceNoticeResource(plot: AcousticPlot, trace: MusicalTrace?, failed: Boolean): Int? {
    if (plot != AcousticPlot.PITCH) return null
    if (trace == null && !failed) return R.string.musical_trace_loading
    return if (trace?.samples?.any { it.hz != null } == true) null else R.string.musical_trace_unavailable
}
