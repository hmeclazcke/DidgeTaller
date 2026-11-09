package ar.didgetaller.core


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
        val window = RecordingWindow.of(source, startFrame, endFrame, settings)
        val pass = OfflineWindowPass(source, window, settings, cancelled, progress)
        val windowPoints = pass.run()
        val points = if (settings.instrument == InstrumentProfile.NATIVE_AMERICAN_FLUTE)
            offlineFlutePitch(source, window, settings, cancelled) else windowPoints
        checkOfflineCancellation(cancelled)
        val result = OfflineStatistics.result(window, settings, pass.spectrum.finish(), pass.spectrum.windows, points)
        checkOfflineCancellation(cancelled); progress(100)
        return result
    }
}
