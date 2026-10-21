package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import java.util.concurrent.CancellationException
import kotlin.math.*

class AudioAnalysisTest {
    private val rate = 44100
    private val n = 32768
    private fun tone(hz: Double, seconds: Double = 2.0, amplitude: Double = 9000.0) =
        ShortArray((rate * seconds).toInt()) { (amplitude * sin(2 * PI * hz * it / rate)).roundToInt().toShort() }
    private fun source(pcm: ShortArray, sampleRate: Int = rate) = object : MonoPcmSource {
        override val sampleRate = sampleRate
        override val frameCount = pcm.size.toLong()
        override fun read(startFrame: Long, destination: ShortArray): Int {
            val count = min(destination.size, pcm.size - startFrame.toInt())
            pcm.copyInto(destination, 0, startFrame.toInt(), startFrame.toInt() + count)
            destination.fill(0, count)
            return count
        }
    }

    @Test fun fftMatchesIndependentComplexDft() {
        val real = DoubleArray(32) { sin(it * 0.19) + it * 0.01 }
        val imaginary = DoubleArray(32) { cos(it * 0.37) }
        val re = real.copyOf(); val im = imaginary.copyOf()
        Radix2Fft.forward(re, im)
        for (k in real.indices) {
            val expectedRe = real.indices.sumOf { real[it] * cos(2 * PI * k * it / 32) + imaginary[it] * sin(2 * PI * k * it / 32) }
            val expectedIm = real.indices.sumOf { imaginary[it] * cos(2 * PI * k * it / 32) - real[it] * sin(2 * PI * k * it / 32) }
            assertEquals(expectedRe, re[k], 1e-10); assertEquals(expectedIm, im[k], 1e-10)
        }
    }

    @Test fun psdIntegratesToKnownDigitalPower() {
        val hz = 64.0 * rate / n
        val spectrum = SpectralAnalysis(n).frame(tone(hz, n.toDouble() / rate, 16384.0), rate)
        assertEquals(0.125, spectrum.totalPower, 0.00002)
        assertEquals(hz, spectrum.centroidHz!!, 0.03)
        assertEquals(1.3458251953125, spectrum.binHz, 1e-12)
    }

    @Test fun peakLocationsAndNotesForGraveReferences() {
        for ((hz, midi) in listOf(65.41 to 36, 69.30 to 37, 73.42 to 38)) {
            val result = WavAnalysis.analyze(source(tone(hz)))
            assertEquals(hz, result.summary!!.typicalHz, 0.25)
            assertEquals(midi, Music.midi(result.summary.typicalHz).roundToInt())
            val peak = result.harmonics.peaks.maxBy { it.relativeDb }
            assertEquals(hz, peak.hz, 0.12) // log-parabolic interpolation, isolated Hann-windowed sinusoid only
            assertEquals(1, peak.order)
            assertEquals(0.0, peak.relativeDb, 1e-10)
        }
    }

    @Test fun seventyHzFundamentalAndPeakAgree() {
        val result=WavAnalysis.analyze(source(tone(70.0)))
        assertEquals(70.0,result.summary!!.typicalHz,0.25)
        val peak=result.harmonics.peaks.maxBy {it.relativeDb}
        assertEquals(70.0,peak.hz,0.12)
        assertEquals(1,peak.order)
        assertEquals(0.0,peak.relativeDb,1e-10)
    }

    @Test fun seventyHzWithStrongSecondAndThirdHarmonicsHasCoherentLevels() {
        val pcm=ShortArray(rate*2) {i ->
            (1000*sin(2*PI*70*i/rate)+9000*sin(2*PI*140*i/rate)+6000*sin(2*PI*210*i/rate)).roundToInt().toShort()
        }
        val result=WavAnalysis.analyze(source(pcm))
        assertEquals(70.0,result.summary!!.typicalHz,0.25)
        val peaks=(1..3).map {order -> result.harmonics.peaks.single {it.order==order}}
        peaks.forEachIndexed {index,peak -> assertEquals((index+1)*70.0,peak.hz,0.20)}
        assertEquals(2,result.harmonics.peaks.maxBy {it.relativeDb}.order)
        // 20 log10(A/A_H2), allowing 0.4 dB for finite Hann windows and PCM quantization.
        assertEquals(20*log10(1000.0/9000),peaks[0].relativeDb,0.4)
        assertEquals(20*log10(6000.0/9000),peaks[2].relativeDb,0.4)
        val expectedCentroid=(70.0+140.0*81+210.0*36)/118
        assertEquals(expectedCentroid,result.spectralCentroidHz!!,0.25)
        assertEquals(1.0/118,result.bandEnergies.first().fraction,0.002)
        assertEquals(1.0,result.bandEnergies.sumOf {it.fraction},1e-12)
    }

    @Test fun amplitudeChangesPowerButNotNormalizedSpectrum() {
        val fft = SpectralAnalysis(n)
        val a = fft.frame(tone(70.0, n.toDouble() / rate, 3000.0), rate)
        val b = fft.frame(tone(70.0, n.toDouble() / rate, 6000.0), rate)
        assertEquals(6.0206, 10 * log10(b.totalPower / a.totalPower), 0.002)
        val bin = a.density.indices.maxBy { a.density[it] }
        assertEquals(a.relativeDb(bin)!!, b.relativeDb(bin)!!, 1e-9)
    }

    @Test fun centroidAndBandsArePowerWeightedAndConserveEnergy() {
        val hz = 64.0 * rate / n
        val pcm = ShortArray(n) { (3000 * sin(2 * PI * hz * it / rate) + 6000 * sin(4 * PI * hz * it / rate)).roundToInt().toShort() }
        val spectrum = SpectralAnalysis(n).frame(pcm, rate)
        assertEquals(hz * 1.8, spectrum.centroidHz!!, 0.04)
        assertEquals(1.0, spectrum.bands().sumOf { it.fraction }, 1e-12)
        assertEquals(0.2, spectrum.bands().first().fraction, 0.001)
    }

    @Test fun weakAndMissingFundamentalsDoNotBecomeStrongestPeakEstimates() {
        for (first in listOf(400.0, 0.0)) {
            val pcm = ShortArray(rate * 2) { i ->
                (first * sin(2 * PI * 69.3 * i / rate) + 10000 * sin(4 * PI * 69.3 * i / rate) +
                    6000 * sin(6 * PI * 69.3 * i / rate)).roundToInt().toShort()
            }
            val result = WavAnalysis.analyze(source(pcm))
            assertEquals(69.3, result.summary!!.typicalHz, 0.25)
            assertEquals(2, result.harmonics.peaks.maxBy { it.relativeDb }.order)
            assertTrue(result.harmonics.peaks.any { it.order == 3 })
            if (first == 0.0) assertFalse(result.harmonics.peaks.any { it.order == 1 })
        }
    }

    @Test fun onlyEvenComponentsDoNotJustifyAnUnheardSubharmonic() {
        val pcm=ShortArray(rate*2) {i ->
            (9000*sin(2*PI*138.6*i/rate)+5000*sin(4*PI*138.6*i/rate)).roundToInt().toShort()
        }
        val result=WavAnalysis.analyze(source(pcm))
        assertEquals(138.6,result.summary!!.typicalHz,0.3)
        // The same audio could be described as H2+H4 of 69.3 Hz; the waveform alone cannot prove that origin.
        assertTrue(result.harmonics.peaks.none {it.order==1 && it.hz<100})
    }

    @Test fun realSampleRateAndConfigurablePitchRangeAreRespected() {
        val actualRate=48000
        val pcm=ShortArray(actualRate*2) {(9000*sin(2*PI*440*it/actualRate)).roundToInt().toShort()}
        val result=WavAnalysis.analyze(source(pcm,actualRate),settings=WavAnalysis.Settings(maxPitchHz=1000))
        assertEquals(actualRate,result.sampleRate)
        assertEquals(440.0,result.summary!!.typicalHz,0.5)
        assertEquals(48000.0/n,result.spectrum.binHz,1e-12)
    }

    @Test fun detuningAndUnrelatedPeaksRemainDistinct() {
        val pcm = ShortArray(n) { i ->
            (4000 * sin(2 * PI * 70 * i / rate) + 9000 * sin(2 * PI * 141.4 * i / rate) +
                6000 * sin(2 * PI * 333 * i / rate)).roundToInt().toShort()
        }
        val spectrum = SpectralAnalysis(n).frame(pcm, rate)
        val result = HarmonicAnalysis.analyze(spectrum, 70.0)
        val second = result.peaks.minBy { abs(it.hz - 141.4) }
        assertEquals(2, second.order)
        assertEquals(1200 * log2(141.4 / 140), second.deviationCents!!, 2.0)
        assertNull(result.peaks.minBy { abs(it.hz - 333) }.order)
        assertTrue(result.candidateBandEnergyFraction!! in 0.5..0.9)
        assertTrue(HarmonicAnalysis.analyze(spectrum, null).peaks.all { it.order == null && it.deviationCents == null })
    }

    @Test fun silenceAndDcHaveNoInventedMetrics() {
        for (level in listOf<Short>(0, 6000)) {
            val result = WavAnalysis.analyze(source(ShortArray(rate * 2) { level }))
            assertNull(result.summary); assertNull(result.medianValidHz); assertNull(result.meanValidHz)
            assertNull(result.spectrum.centroidHz); assertNull(result.spectrum.relativeDb(50))
            assertTrue(result.harmonics.peaks.isEmpty()); assertEquals(0.0, result.validWindowFraction, 0.0)
        }
    }

    @Test fun moderateNoiseAndAmplitudeModulationStillRecoverReference() {
        val random = Random(20261020L)
        val pcm = ShortArray(rate * 3) { i ->
            val amplitude = 5000 * (1 + 0.4 * sin(2 * PI * 3 * i / rate))
            (amplitude * sin(2 * PI * 73.42 * i / rate) + 1800 * sin(4 * PI * 73.42 * i / rate) +
                900 * random.nextGaussian()).roundToInt().toShort()
        }
        val result = WavAnalysis.analyze(source(pcm))
        assertEquals(73.42, result.summary!!.typicalHz, 0.35)
        assertTrue(result.validWindowFraction > 0.8)
    }

    @Test fun noiseAloneDoesNotProduceConfidentHarmonicLabels() {
        val random = Random(17L)
        val result = WavAnalysis.analyze(source(ShortArray(rate * 2) { (2000 * random.nextGaussian()).toInt().toShort() }))
        assertNull(result.summary); assertNull(result.harmonics.fundamentalHz)
        assertTrue(result.harmonics.peaks.all { it.order == null })
    }

    @Test fun fragmentBoundsExcludeTheOtherRegisterAndTimesReferToWindowCenters() {
        val first = tone(65.41, 3.0); val second = tone(73.42, 3.0)
        val wav = source(first + second)
        val a = WavAnalysis.analyze(wav, 0, first.size.toLong())
        val b = WavAnalysis.analyze(wav, first.size.toLong(), wav.frameCount)
        assertEquals(65.41, a.summary!!.typicalHz, 0.25); assertEquals(73.42, b.summary!!.typicalHz, 0.25)
        assertEquals(3.0, b.durationSeconds, 1e-12)
        assertTrue(b.timeline.first().timeMs > 3000 && b.timeline.last().timeMs < 6000)
    }

    @Test fun smoothPitchChangeIncreasesMeasuredVariability() {
        var phase = 0.0
        val pcm = ShortArray(rate * 3) { i ->
            phase += 2 * PI * (68.5 + 1.5 * i / (rate * 3.0)) / rate
            (9000 * sin(phase)).roundToInt().toShort()
        }
        val result = WavAnalysis.analyze(source(pcm))
        val values = result.timeline.mapNotNull { it.hz }
        assertTrue(values.last() > values.first() + 0.5)
        assertTrue(result.summary!!.highHz > result.summary.lowHz + 0.3)
    }

    @Test fun reanalysisIsDeterministicAndDoesNotChangeTheLivePitchAlgorithm() {
        val pcm = tone(66.9)
        val a = WavAnalysis.analyze(source(pcm)); val b = WavAnalysis.analyze(source(pcm))
        assertEquals(a.timeline, b.timeline); assertEquals(a.summary, b.summary)
        assertArrayEquals(a.spectrum.density, b.spectrum.density, 0.0)
        val frame = pcm.copyOf(16384)
        val live = PitchDetector.analyze(frame)
        val offline = PitchDetector.analyze(frame, includeVisuals = false)
        assertEquals(live.hz, offline.hz, 0.0); assertEquals(live.confidence, offline.confidence, 0.0)
        assertEquals(160, live.spectrum.size); assertTrue(offline.spectrum.isEmpty())
    }

    @Test fun fifteenMinuteSourceHasBoundedReadsAndCooperativeCancellation() {
        var reads = 0
        val source = object : MonoPcmSource {
            override val sampleRate = rate
            override val frameCount = 900L * rate
            override fun read(startFrame: Long, destination: ShortArray): Int {
                reads++; assertEquals(n, destination.size); destination.fill(0)
                return min(destination.size.toLong(), frameCount - startFrame).toInt()
            }
        }
        assertThrows(CancellationException::class.java) { WavAnalysis.analyze(source, cancelled = { reads >= 3 }) }
        assertEquals(3, reads)
    }

    @Test fun invalidRangesAndWindowBudgetsFailBeforeReading() {
        val source = source(tone(70.0))
        assertThrows(IllegalArgumentException::class.java) { WavAnalysis.analyze(source, -1) }
        assertThrows(IllegalArgumentException::class.java) { WavAnalysis.analyze(source, settings = WavAnalysis.Settings(maxWindows = 1)) }
        assertThrows(IllegalArgumentException::class.java) { WavAnalysis.Settings(fftSize = 12000) }
    }

    @Test fun shortExcerptReportsInsufficientPitchInsteadOfInventingOne() {
        val result = WavAnalysis.analyze(source(tone(70.0, 0.05)))
        assertNull(result.summary); assertNull(result.harmonics.fundamentalHz)
        assertTrue(result.harmonics.peaks.all { it.order == null })
    }
}
