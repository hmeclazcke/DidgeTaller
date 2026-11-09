package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.min

/** Validated source-frame interval; subtraction cannot overflow before budget validation. */
internal class RecordingWindow private constructor(val start: Long, val end: Long) {
    companion object {
        fun of(source: MonoPcmSource, start: Long, end: Long, settings: WavAnalysis.Settings): RecordingWindow {
            require(source.sampleRate in 8000..192000)
            require(start >= 0 && end <= source.frameCount && end >= 2 && start <= end - 2)
            require(settings.maxPitchHz < source.sampleRate / 4) { "Rango de fundamental demasiado alto para este muestreo." }
            require((end - start - 1) / settings.hopSize + 1 <= settings.maxWindows) {
                "Demasiadas ventanas: seleccioná un fragmento menor o aumentá el presupuesto de análisis."
            }
            return RecordingWindow(start, end)
        }
    }
}

internal fun checkOfflineCancellation(cancelled: () -> Boolean) {
    if (cancelled() || Thread.currentThread().isInterrupted) throw CancellationException("Análisis cancelado")
}

/** Division first preserves exact nonnegative frame time without overflowing the intermediate product. */
internal fun recordingFrameTimeMs(frame: Long, rate: Int): Long = frame / rate * 1000 + frame % rate * 1000 / rate

/** Sample-weighted spectrum, sealed once so callers cannot renormalize or append after publication. */
internal class OfflineSpectrum(private val rate: Int, private val fftSize: Int) {
    private val analyzer = SpectralAnalysis(fftSize)
    private val sum = DoubleArray(fftSize / 2 + 1)
    private var weight = 0.0
    private var result: PowerSpectrum? = null
    var windows = 0; private set

    fun append(pcm: ShortArray, count: Int) {
        check(result == null)
        val spectrum = analyzer.frame(pcm, rate, count)
        val contribution = count.toDouble() / fftSize
        weight += contribution
        for (bin in sum.indices) sum[bin] += spectrum.density[bin] * contribution
        windows++
    }

    fun finish(): PowerSpectrum {
        result?.let { return it }
        check(weight > 0)
        for (bin in sum.indices) sum[bin] /= weight
        return PowerSpectrum(rate, fftSize, sum).also { result = it }
    }
}

/** One reusable PCM window serves the spectrum and didgeridoo pitch without rereading the source. */
internal class OfflineWindowPass(private val source: MonoPcmSource, private val window: RecordingWindow,
    private val settings: WavAnalysis.Settings, private val cancelled: () -> Boolean, private val progress: (Int) -> Unit) {
    private val pcm = ShortArray(settings.fftSize)
    val spectrum = OfflineSpectrum(source.sampleRate, settings.fftSize)
    private val points = mutableListOf<SessionAnalysis.Point>()
    private var lastProgress = -1

    fun run(): List<SessionAnalysis.Point> {
        var at = window.start
        while (at < window.end) {
            checkOfflineCancellation(cancelled)
            val requested = min(settings.fftSize.toLong(), window.end - at).toInt()
            check(source.read(at, pcm) >= requested) { "El WAV terminó antes del intervalo solicitado." }
            if (requested < 2) break
            spectrum.append(pcm, requested)
            appendPitch(at, requested)
            report(at + requested)
            if (at + requested >= window.end) break
            at += settings.hopSize
        }
        return points
    }

    private fun appendPitch(at: Long, requested: Int) {
        if (settings.instrument != InstrumentProfile.DIDGERIDOO) return
        val pitch = PitchDetector.analyze(if (requested == pcm.size) pcm else pcm.copyOf(requested),
            source.sampleRate, settings.minPitchHz, settings.maxPitchHz, includeVisuals = false)
        points += SessionAnalysis.Point(recordingFrameTimeMs(at + requested / 2, source.sampleRate),
            pitch.hz.takeIf { it > 0 && pitch.confidence >= .80 }, pitch.confidence)
    }

    private fun report(end: Long) {
        val percent = ((end - window.start) * 100 / (window.end - window.start)).toInt().coerceAtMost(99)
        if (percent != lastProgress) { checkOfflineCancellation(cancelled); progress(percent); lastProgress = percent }
    }
}

/** Flute pitch cadence remains independent of the spectrum's longer FFT resolution. */
internal fun offlineFlutePitch(source: MonoPcmSource, window: RecordingWindow,
    settings: WavAnalysis.Settings, cancelled: () -> Boolean): List<SessionAnalysis.Point> {
    val frame = ShortArray(settings.instrument.windowSamples)
    val hop = frame.size / 2
    require((window.end - window.start - 1) / hop + 1 <= settings.maxWindows * 4L) {
        "Demasiadas lecturas: seleccioná un fragmento menor."
    }
    val points = mutableListOf<SessionAnalysis.Point>()
    var position = window.start
    while (position < window.end) {
        checkOfflineCancellation(cancelled)
        val count = min(frame.size.toLong(), window.end - position).toInt()
        if (count < 4096) break
        check(source.read(position, frame) >= count)
        val pitch = PitchDetector.analyze(if (count == frame.size) frame else frame.copyOf(count), source.sampleRate,
            settings.minPitchHz, settings.maxPitchHz, includeVisuals = false)
        points += SessionAnalysis.Point(recordingFrameTimeMs(position + count / 2, source.sampleRate),
            pitch.hz.takeIf { it in settings.minPitchHz * .995..settings.maxPitchHz * 1.005 && pitch.confidence >= .80 }, pitch.confidence)
        if (position + count >= window.end) break
        position += hop
    }
    return points
}

/** Recording summaries use accepted points, not the number of spectral windows. */
internal object OfflineStatistics {
    private fun median(values: List<Double>): Double? = values.sorted().takeIf { it.isNotEmpty() }?.let {
        (it[(it.size - 1) / 2] + it[it.size / 2]) / 2
    }

    fun result(window: RecordingWindow, settings: WavAnalysis.Settings, spectrum: PowerSpectrum,
        windows: Int, points: List<SessionAnalysis.Point>): WavAnalysis.Result {
        val guard = if (settings.instrument == InstrumentProfile.NATIVE_AMERICAN_FLUTE) .005 else 0.0
        val low = settings.minPitchHz * (1 - guard); val high = settings.maxPitchHz * (1 + guard)
        val summary = SessionAnalysis.summarize(points, low, high)
        val valid = points.filter { it.isUsable(low, high) }
        val frequencies = valid.map { checkNotNull(it.hz) }
        return WavAnalysis.Result(window.start, window.end, spectrum.sampleRate, windows, spectrum,
            HarmonicAnalysis.analyze(spectrum, summary?.typicalHz), points, summary, median(frequencies),
            frequencies.takeIf { it.isNotEmpty() }?.average(),
            if (points.isEmpty()) 0.0 else valid.size.toDouble() / points.size, median(valid.map { it.confidence }))
    }
}
