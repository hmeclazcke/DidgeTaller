package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class TraceDrawingTest {
    private fun vertex(time: Double, hz: Double) = TraceDrawing.Vertex(time, hz)
    private fun sample(time: Double, hz: Double?, linked: Boolean = false, bridged: Boolean = false) =
        MusicalTrace.Sample(time, hz, linked, MusicalTrace.Evidence.Yin(.95, null), bridged)
    private fun trace(vararg samples: MusicalTrace.Sample) = MusicalTrace(
        InstrumentProfile.NATIVE_AMERICAN_FLUTE, MusicalTrace.Meaning.ESTIMATED_PITCH, 3000, samples.toList())
    private fun legacy(time: Long, hz: Double?, previous: Int? = null, uncertain: Boolean = false,
                       bridged: Boolean = false, tone: PitchTrace.Tone = PitchTrace.Tone.STABLE) =
        PitchTrace.Sample(SessionAnalysis.Point(time, hz, .95), previous, uncertain, bridged = bridged, tone = tone)

    @Test fun canonicalGeometryPreservesLinksAndNeverJoinsAMissingInterval() {
        val source = trace(sample(0.0, 440.0), sample(100.0, 450.0, true), sample(200.0, null),
            sample(300.0, 460.0), sample(400.0, 470.0, true), sample(500.0, 480.0))
        val original = source.samples.toList()
        val drawing = TraceDrawing.canonical(source)
        assertEquals(listOf(
            TraceDrawing.Segment(vertex(0.0, 440.0), vertex(100.0, 450.0)),
            TraceDrawing.Segment(vertex(300.0, 460.0), vertex(400.0, 470.0)),
            TraceDrawing.Dot(vertex(500.0, 480.0))), drawing)
        assertEquals(original, source.samples)
        assertNull(source.frequencyAt(200))
    }

    @Test fun canonicalIsolatedReadingsAndBridgeStyleRetainTheirOriginalMeaning() {
        val source = trace(sample(0.0, 440.0), sample(100.0, 450.0, true, true),
            sample(200.0, null), sample(300.0, 460.0))
        assertEquals(listOf(TraceDrawing.Segment(vertex(0.0, 440.0), vertex(100.0, 450.0), bridged = true),
            TraceDrawing.Dot(vertex(300.0, 460.0))), TraceDrawing.canonical(source))
        assertNull(source.frequencyAt(100))
    }

    @Test fun emptyAndSingleReadingTracesHaveNoFabricatedConnection() {
        assertTrue(TraceDrawing.canonical(trace()).isEmpty())
        assertTrue(TraceDrawing.canonical(trace(sample(0.0, null))).isEmpty())
        assertEquals(listOf(TraceDrawing.Dot(vertex(0.0, 440.0))), TraceDrawing.canonical(trace(sample(0.0, 440.0))))
        // Even an invalid first-link flag cannot manufacture a predecessor.
        assertEquals(1, TraceDrawing.canonical(trace(sample(0.0, 440.0, true))).size)
    }

    @Test fun historicalGeometryRetainsToneBridgesAndThe1200MillisecondBoundary() {
        val source = listOf(legacy(0, 70.0), legacy(1200, 71.0, 0, bridged = true, tone = PitchTrace.Tone.MODERATE),
            legacy(2401, 72.0, 1, tone = PitchTrace.Tone.REVIEW))
        val drawing = TraceDrawing.historical(source, false)
        assertEquals(listOf(TraceDrawing.Dot(vertex(0.0, 70.0)),
            TraceDrawing.Segment(vertex(0.0, 70.0), vertex(1200.0, 71.0), bridged = true, tone = PitchTrace.Tone.MODERATE),
            TraceDrawing.Dot(vertex(2401.0, 72.0), tone = PitchTrace.Tone.REVIEW)), drawing)
    }

    @Test fun uncertaintyIsAnOptionalRingAndNeverExtendsAConnection() {
        val source = listOf(legacy(0, 70.0), legacy(100, 140.0, 0, uncertain = true), legacy(200, 71.0, 0))
        val original = source.toList()
        val hidden = TraceDrawing.historical(source, false)
        val visible = TraceDrawing.historical(source, true)
        val first = TraceDrawing.Dot(vertex(0.0, 70.0))
        val line = TraceDrawing.Segment(vertex(0.0, 70.0), vertex(200.0, 71.0))
        assertEquals(listOf(first, line), hidden)
        assertEquals(listOf(first, TraceDrawing.Dot(vertex(100.0, 140.0), uncertain = true), line), visible)
        assertEquals(original, source)
    }

    @Test(expected = IllegalArgumentException::class) fun historicalRejectsSamplesThatBypassThePitchTraceBoundary() {
        val source = listOf(legacy(0, null), legacy(100, 70.0, 0))
        TraceDrawing.historical(source, false)
    }

    @Test fun historicalConnectivityDependsOnElapsedTimeRatherThanAbsoluteTimestamp() {
        for (offset in listOf(0L, 1L, 10000L)) {
            val source = listOf(legacy(offset, 70.0), legacy(offset + 1200, 71.0, 0),
                legacy(offset + 2401, 72.0, 1))
            assertEquals(listOf(TraceDrawing.Dot(vertex(offset.toDouble(), 70.0)),
                TraceDrawing.Segment(vertex(offset.toDouble(), 70.0), vertex(offset + 1200.0, 71.0)),
                TraceDrawing.Dot(vertex(offset + 2401.0, 72.0))), TraceDrawing.historical(source, false))
        }
    }
}
