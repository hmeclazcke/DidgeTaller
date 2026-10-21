package ar.didgetaller.core

import java.util.concurrent.CancellationException
import kotlin.math.min

/** Deterministic, read-only block analysis; no Android, PCM of whole file, or spectrogram matrix. */
object WavAnalysis {
    data class Settings(val fftSize: Int = 32768, val hopSize: Int = fftSize / 2,
                        val minPitchHz: Int = 35, val maxPitchHz: Int = 220,
                        val maxWindows: Int = 10000,val instrument:InstrumentProfile=InstrumentProfile.DIDGERIDOO) {
        init {
            require(fftSize in 4096..65536 && fftSize and (fftSize - 1) == 0)
            require(hopSize in 1..fftSize && minPitchHz >= 20 && maxPitchHz > minPitchHz && maxWindows > 0)
        }
    }
    data class Result(val startFrame: Long, val endFrame: Long, val sampleRate: Int, val windows: Int,
                      val spectrum: PowerSpectrum, val harmonics: HarmonicAnalysis.Result,
                      val timeline: List<SessionAnalysis.Point>, val summary: SessionAnalysis.Summary?,
                      val medianValidHz: Double?, val meanValidHz: Double?, val validWindowFraction: Double,
                      val medianPeriodicityConfidence: Double?) {
        val durationSeconds: Double get() = (endFrame - startFrame).toDouble() / sampleRate
        // Computed once on the analysis worker, not on Compose recompositions/playback ticks.
        val spectralCentroidHz: Double? = spectrum.centroidHz
        val bandEnergies: List<PowerSpectrum.Band> = spectrum.bands()
    }

    fun analyze(source: MonoPcmSource, startFrame: Long = 0, endFrame: Long = source.frameCount,
                settings: Settings = Settings(), cancelled: () -> Boolean = { false },
                progress: (Int) -> Unit = {}): Result {
        require(source.sampleRate in 8000..192000 && startFrame >= 0 && endFrame in (startFrame + 2)..source.frameCount)
        require(settings.maxPitchHz < source.sampleRate / 4) { "Rango de fundamental demasiado alto para este muestreo." }
        // Stop with an explicit error instead of silently dropping data or growing metadata indefinitely.
        require((endFrame - startFrame - 1) / settings.hopSize + 1 <= settings.maxWindows) {
            "Demasiadas ventanas: seleccioná un fragmento menor o aumentá el presupuesto de análisis."
        }
        fun checkCancelled() { if (cancelled() || Thread.currentThread().isInterrupted) throw CancellationException("Análisis cancelado") }
        val analyzer = SpectralAnalysis(settings.fftSize)
        val pcm = ShortArray(settings.fftSize)
        val average = DoubleArray(settings.fftSize / 2 + 1)
        val points = mutableListOf<SessionAnalysis.Point>()
        var windows = 0; var at = startFrame; var lastProgress = -1; var totalWeight = 0.0
        while (at < endFrame) {
            checkCancelled()
            val requested = min(settings.fftSize.toLong(), endFrame - at).toInt()
            val read = source.read(at, pcm)
            check(read >= requested) { "El WAV terminó antes del intervalo solicitado." }
            if (requested < 2) break
            val spectrum = analyzer.frame(pcm, source.sampleRate, requested)
            val weight = requested.toDouble() / settings.fftSize
            totalWeight += weight
            for (bin in average.indices) average[bin] += spectrum.density[bin] * weight
            if(settings.instrument==InstrumentProfile.DIDGERIDOO){
            val pitch = PitchDetector.analyze(if (requested == pcm.size) pcm else pcm.copyOf(requested),
                source.sampleRate, settings.minPitchHz, settings.maxPitchHz, includeVisuals = false)
            points += SessionAnalysis.Point((at + requested / 2) * 1000 / source.sampleRate,
                pitch.hz.takeIf { it > 0 && pitch.confidence >= 0.80 }, pitch.confidence)
            }
            windows++
            val percent = ((at + requested - startFrame) * 100 / (endFrame - startFrame)).toInt().coerceAtMost(99)
            if (percent != lastProgress) { checkCancelled(); progress(percent); lastProgress = percent }
            // One final partial window covers the tail; do not keep adding shorter duplicates.
            if (at + requested >= endFrame) break
            at += settings.hopSize
        }
        if(settings.instrument==InstrumentProfile.NATIVE_AMERICAN_FLUTE){
            // A separate short pitch pass preserves melodic changes. The spectrum/STFT
            // retains its original 32768-point frequency resolution and averaging.
            val frame=ShortArray(settings.instrument.windowSamples)
            val hop=frame.size/2
            require((endFrame-startFrame-1)/hop+1<=settings.maxWindows*4L){"Demasiadas lecturas: seleccioná un fragmento menor."}
            var position=startFrame
            while(position<endFrame){
                checkCancelled()
                val n=min(frame.size.toLong(),endFrame-position).toInt()
                if(n<4096)break
                check(source.read(position,frame)>=n)
                val pitch=PitchDetector.analyze(if(n==frame.size)frame else frame.copyOf(n),source.sampleRate,
                    settings.minPitchHz,settings.maxPitchHz,includeVisuals=false)
                points+=SessionAnalysis.Point((position+n/2)*1000/source.sampleRate,
                    pitch.hz.takeIf {it in settings.minPitchHz*.995..settings.maxPitchHz*1.005 && pitch.confidence>=.80},pitch.confidence)
                if(position+n>=endFrame)break
                position+=hop
            }
        }
        checkCancelled()
        for (bin in average.indices) average[bin] /= totalWeight
        val spectrum = PowerSpectrum(source.sampleRate, settings.fftSize, average)
        val guard=if(settings.instrument==InstrumentProfile.NATIVE_AMERICAN_FLUTE).005 else 0.0
        val summary = SessionAnalysis.summarize(points, settings.minPitchHz*(1-guard), settings.maxPitchHz*(1+guard))
        val valid = points.filter { it.isUsable(settings.minPitchHz*(1-guard), settings.maxPitchHz*(1+guard)) }
        fun median(values: List<Double>): Double? = values.sorted().takeIf { it.isNotEmpty() }?.let {
            (it[(it.size - 1) / 2] + it[it.size / 2]) / 2
        }
        val result = Result(startFrame, endFrame, source.sampleRate, windows, spectrum,
            HarmonicAnalysis.analyze(spectrum, summary?.typicalHz), points, summary,
            median(valid.map { it.hz!! }), valid.takeIf { it.isNotEmpty() }?.map { it.hz!! }?.average(),
            if(points.isEmpty())0.0 else valid.size.toDouble() / points.size, median(valid.map { it.confidence }))
        checkCancelled(); progress(100)
        return result
    }
}
