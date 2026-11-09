package ar.didgetaller.ui

import ar.didgetaller.R
import ar.didgetaller.core.*
import org.junit.*
import org.junit.Assert.*
import java.util.Locale
import kotlin.math.log10

class SessionInspectorTest {
    private lateinit var previousLocale: Locale
    @Before fun locale() { previousLocale = Locale.getDefault(); Locale.setDefault(Locale.US) }
    @After fun restore() { Locale.setDefault(previousLocale) }
    private val calls = mutableListOf<Pair<Int, List<Any>>>()
    private val inspector = SessionInspector(InspectionStrings { id, args ->
        calls += id to args.toList(); "$id:" + args.joinToString("|")
    }) { it?.name ?: "unknown" }
    private fun peak(frequency:Double,span:Double,data:InspectionData):String {
        val first=calls.size
        return inspector.peak(frequency,span,data).also {assertReturned(it,first)}
    }
    private val missing = MusicalTrace.unavailable(InstrumentProfile.DIDGERIDOO, 1000)
    private val point = SessionAnalysis.Point(500, 70.0, .95)
    private val sample = PitchTrace.Sample(point, null, false)
    private val data = InspectionData(missing, listOf(point), listOf(sample), 1000)
    private fun inspect(data: InspectionData, plot: AcousticPlot = AcousticPlot.PITCH, time: Long = 500,
        frequency: Double = 100.0, mode: InspectionMode = InspectionMode.MUSICAL): String {
        val first=calls.size
        val result=inspector.temporal(plot,time,frequency,data,mode)
        assertReturned(result,first)
        if(plot==AcousticPlot.PITCH)assertTrue(result.startsWith("${seconds(time)} · "))
        if(mode==InspectionMode.COMPARISON && plot==AcousticPlot.PITCH)assertTrue(result.contains("\n"))
        return result
    }
    private fun assertReturned(result:String, first:Int) {
        assertTrue("A measured/absent reading must produce actual display text",calls.size>first)
        for((id,args) in calls.drop(first))assertTrue("Lost localized reading $id in $result",
            result.contains("$id:"+args.joinToString("|")))
    }

    @Test fun modesKeepPeriodicityAndComparisonSeparateFromCanonicalMissingEvidence() {
        assertEquals(InspectionMode.MUSICAL, inspectionMode(false, true))
        assertEquals(InspectionMode.PERIODICITY, inspectionMode(true, false))
        assertEquals(InspectionMode.COMPARISON, inspectionMode(true, true))
        inspect(data); assertEquals(R.string.ui_no_nearby_reliable_reading, calls.last().first)
        inspect(data, mode = InspectionMode.PERIODICITY)
        assertEquals(R.string.ui_cents_a4_440_periodicity_variation, calls.last().first)
        assertFalse(inspect(data, time = 1250, mode = InspectionMode.PERIODICITY).contains("${R.string.ui_display_link}:"))
        assertEquals(R.string.ui_cents_a4_440_periodicity_variation,calls.last().first)
        inspect(data, time = 1251, mode = InspectionMode.PERIODICITY)
        assertEquals(R.string.ui_no_nearby_reliable_reading, calls.last().first)
    }
    @Test fun measuredComponentsUseOnlyTheCanonicalHundredMillisecondNeighborhood() {
        val curve = DroneComponentCurve(listOf(DroneComponentCurve.Sample(500.0, 70.0, 70.0, .1,
            DroneComponentCurve.Reason.COMPONENT)), 40.0)
        val evidence = data.copy(musical = MusicalTrace.didgeridoo(curve, 1000))
        inspect(evidence); assertEquals(R.string.musical_component_frequency, calls.last().first)
        assertEquals(listOf("70.0 Hz"), calls.last().second)
        inspect(evidence, time = 601); assertEquals(R.string.ui_no_nearby_reliable_reading, calls.last().first)
    }
    @Test fun uncertainReadingsDoNotInventANoteAndOriginalDisplayLinksRemainExplicit() {
        val uncertain = data.copy(pitch = listOf(sample.copy(uncertain = true, reason = PitchTrace.Reason.LOW_CONFIDENCE)))
        inspect(uncertain, mode = InspectionMode.PERIODICITY)
        assertEquals(R.string.ui_uncertain_estimate_confidence_yin_no_confirmed_note, calls.last().first)
        assertEquals(listOf("70.0 Hz", "LOW_CONFIDENCE", .95), calls.last().second)
        inspect(data.copy(pitch = listOf(sample.copy(bridged = true, variationCents = 5.0))), mode = InspectionMode.PERIODICITY)
        assertEquals(R.string.ui_display_link, calls.last().first)
        assertEquals("5 cents", calls[calls.lastIndex - 1].second.last())
        listOf<Double?>(null, -1.0).forEach { hz ->
            val invalid = SessionAnalysis.Point(500, hz, .95)
            inspect(data.copy(points = listOf(invalid), pitch = listOf(sample.copy(point = invalid))), mode = InspectionMode.PERIODICITY)
            assertEquals(R.string.ui_no_nearby_reliable_reading, calls.last().first)
        }
    }
    @Test fun waveReadoutUsesTheActualWindowAndNeverTreatsAmplitudeAsPitch() {
        val wave = AudioVisuals.Wave(0, 400, 1000, floatArrayOf(-.5f, .2f), floatArrayOf(.7f, .8f), false)
        inspect(data.copy(wave = wave), AcousticPlot.WAVE, 100)
        assertEquals(R.string.ui_pcm_amplitude_to, calls.last().first)
        assertEquals(listOf("0.10 s", "-0.5", "0.7"), calls.last().second)
        inspect(data.copy(wave = wave), AcousticPlot.WAVE, 400)
        assertEquals(R.string.ui_waiting_for_samples_from_this_region, calls.last().first)
        inspect(data, AcousticPlot.WAVE); assertEquals(R.string.ui_waiting_for_samples_from_this_region, calls.last().first)
    }
    private val tile = AudioVisuals.Spectrogram(0, 1000, 1000, 50.0, 150.0, false, 2, 2,
        floatArrayOf(1f, 4f, 9f, 16f), 16.0, 4096, 100)
    @Test fun cellsRespectTimeFrequencyLogarithmicCoordinatesAndReferenceEnergy() {
        val evidence = data.copy(tile = tile, reference = 16.0)
        inspect(evidence, AcousticPlot.SPECTROGRAM, 750, 125.0)
        assertEquals(R.string.ui_db_rel_stft_cell, calls.last().first); assertEquals(listOf(0.0), calls.last().second)
        inspect(evidence.copy(tile = tile.copy(lowHz = 100.0, highHz = 400.0, logarithmic = true)), AcousticPlot.SPECTROGRAM, 100, 250.0)
        assertEquals(10 * log10(4.0 / 16), calls.last().second.single() as Double, 1e-9)
        inspect(evidence.copy(reference = 0.0), AcousticPlot.SPECTROGRAM, 750, 125.0)
        assertEquals(R.string.ui_no_significant_energy, calls.last().first)
        inspect(evidence, AcousticPlot.SPECTROGRAM, 1000, 125.0)
        assertEquals(R.string.ui_no_calculated_cell, calls.last().first)
        inspect(evidence, AcousticPlot.SPECTROGRAM, 100, 40.0)
        assertEquals(R.string.ui_no_calculated_cell, calls.last().first)
        inspect(data, AcousticPlot.SPECTROGRAM); assertEquals(R.string.ui_no_calculated_cell, calls.last().first)
    }
    @Test fun harmonicInspectionKeepsCandidateAssignmentAndStrictProximity() {
        val evidence = data.copy(harmonics = HarmonicAnalysis.Result(null,
            listOf(HarmonicAnalysis.Peak(140.0, -3.0, 2, 5.0)), null))
        assertTrue(peak(143.0, 100.0, evidence).contains("140.0 Hz · -3.0 dB rel · +5.0 cents"))
        assertEquals(R.string.ui_h_candidate, calls.last().first)
        peak(146.0, 100.0, evidence); assertEquals(R.string.ui_no_nearby_assigned_peak, calls.last().first)
        peak(140.0, 100.0, evidence.copy(harmonics = HarmonicAnalysis.Result(null,
            listOf(HarmonicAnalysis.Peak(140.0, -3.0, null, null)), null)))
        assertEquals(R.string.ui_unassigned_peak_66f03c, calls.last().first)
        peak(140.0, 100.0, data); assertEquals(R.string.ui_no_nearby_assigned_peak, calls.last().first)
    }
    @Test fun comparisonReportsOnlyNearbyMeasuredSwipeAndPreservesMissingGaps() {
        inspect(data, mode = InspectionMode.COMPARISON); assertEquals(R.string.swipe_missing, calls[calls.lastIndex - 1].first)
        val comparison = SwipeCurve(listOf(SwipeCurve.Sample(500.0, 71.0)), 40.0, 35, 140)
        inspect(data.copy(comparison = comparison), mode = InspectionMode.COMPARISON)
        assertEquals(R.string.swipe_cursor, calls.last().first); assertEquals(listOf("71.0 Hz"), calls.last().second)
        inspect(data.copy(comparison = comparison), time = 800, mode = InspectionMode.COMPARISON)
        assertEquals(R.string.swipe_missing, calls[calls.lastIndex - 1].first)
    }
    @Test fun emptyWaveDataRemainsInspectableInsteadOfThrowing() {
        inspect(data.copy(wave = AudioVisuals.Wave(0, 1000, 1000, floatArrayOf(), floatArrayOf(), false)), AcousticPlot.WAVE)
        assertEquals(R.string.ui_waiting_for_samples_from_this_region, calls.last().first)
        inspect(data.copy(wave = AudioVisuals.Wave(0, 1000, 1000, floatArrayOf(1f), floatArrayOf(), false)), AcousticPlot.WAVE)
        assertEquals(R.string.ui_waiting_for_samples_from_this_region, calls.last().first)
    }
    @Test fun emptySpectrogramDataRemainsInspectableInsteadOfThrowing() {
        inspect(data.copy(tile = tile.copy(columns = 0, power = floatArrayOf())), AcousticPlot.SPECTROGRAM)
        assertEquals(R.string.ui_no_calculated_cell, calls.last().first)
        listOf(tile.copy(rows = 0), tile.copy(power = floatArrayOf()), tile.copy(columns = Int.MAX_VALUE)).forEach {
            inspect(data.copy(tile = it), AcousticPlot.SPECTROGRAM)
            assertEquals(R.string.ui_no_calculated_cell, calls.last().first)
        }
    }
    @Test fun pendingTraceIsDistinctFromAnalyzedMissingEvidence() {
        assertEquals(R.string.musical_trace_loading,traceNoticeResource(AcousticPlot.PITCH,null,false))
        assertEquals(R.string.musical_trace_unavailable,traceNoticeResource(AcousticPlot.PITCH,null,true))
        assertEquals(R.string.musical_trace_unavailable,traceNoticeResource(AcousticPlot.PITCH,missing,false))
        val trace=MusicalTrace.flute(listOf(SessionAnalysis.Point(500,440.0,.95)),1000)
        assertNull(traceNoticeResource(AcousticPlot.PITCH,trace,false))
        assertNull(traceNoticeResource(AcousticPlot.SPECTRUM,null,false))
    }

    @Test fun everyInteriorCellUsesItsActualColumnAndRowAndShiftedOrigin() {
        val raster=tile.copy(start=300,end=1500,columns=4,rows=3,power=FloatArray(12){(it+1).toFloat()})
        for(column in 0..3)for(row in 0..2) {
            val time=300L+column*300+150
            val frequency=50.0+(row+.5)*100/3
            inspect(data.copy(tile=raster,reference=12.0),AcousticPlot.SPECTROGRAM,time,frequency)
            assertEquals(10*log10((column*3+row+1)/12.0),calls.last().second.single() as Double,1e-9)
        }
        val wave=AudioVisuals.Wave(100,500,1000,floatArrayOf(-.5f,.2f),floatArrayOf(.7f,.8f),false)
        inspect(data.copy(wave=wave),AcousticPlot.WAVE,299)
        assertEquals(listOf("0.30 s","-0.5","0.7"),calls.last().second)
        inspect(data.copy(wave=wave),AcousticPlot.WAVE,300)
        assertEquals(listOf("0.30 s","0.2","0.8"),calls.last().second)
    }

}
