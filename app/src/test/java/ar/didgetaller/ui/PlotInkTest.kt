package ar.didgetaller.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import ar.didgetaller.core.*
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

/** Captures the actual public DrawScope operations; no Android canvas or fake acoustic detector. */
class PlotInkTest {
    private data class Ink(val kind: String, val args: List<Any?>)
    private class Surface {
        val ink = mutableListOf<Ink>()
        private var dimensions = Size(400f, 200f).packedValue
        private val canvas = Proxy.newProxyInstance(Canvas::class.java.classLoader, arrayOf(Canvas::class.java)) { _, method, args ->
            ink += Ink(method.name.substringBefore('-'), args?.toList().orEmpty()); null
        } as Canvas
        private val transform = Proxy.newProxyInstance(DrawTransform::class.java.classLoader, arrayOf(DrawTransform::class.java)) { _, method, args ->
            ink += Ink(method.name.substringBefore('-'), args?.toList().orEmpty()); null
        } as DrawTransform
        private val context = Proxy.newProxyInstance(DrawContext::class.java.classLoader, arrayOf(DrawContext::class.java)) { _, method, args ->
            when (method.name.substringBefore('-')) {
                "getCanvas" -> canvas
                "getTransform" -> transform
                "getSize" -> dimensions
                "setSize" -> { dimensions = args!![0] as Long; ink += Ink("restoreSize", args.toList()); null }
                else -> error("Unexpected context dependency: ${method.name}")
            }
        } as DrawContext
        val scope = Proxy.newProxyInstance(DrawScope::class.java.classLoader, arrayOf(DrawScope::class.java)) { _, method, args ->
            val kind = method.name.substringBefore('-')
            when (kind) {
                "getDrawContext" -> context
                else -> {
                    check(kind.startsWith("draw")) { "Unexpected renderer dependency: ${method.name}" }
                    ink += Ink(kind, args?.toList().orEmpty()); null
                }
            }
        } as DrawScope
    }
    private fun <T> nativeToken(type: Class<T>): T = type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
        when (method.name) { "toString" -> "synthetic-${type.simpleName}"; "hashCode" -> 1; "equals" -> false; else -> null }
    })
    private val projection = PlotProjection(PlotViewport(AxisWindow(100.0, 300.0), AxisWindow(50.0, 150.0)), 400.0, 200.0)

    @Test fun tracePainterKeepsOrderGapsToneAndUncertaintyWithoutReadingExplorerState() {
        val s = Surface(); val bridge = nativeToken(PathEffect::class.java); val marker = nativeToken(Path::class.java)
        val a = TraceDrawing.Vertex(100.0, 50.0); val b = TraceDrawing.Vertex(200.0, 100.0)
        val marks = listOf(TraceDrawing.Segment(a, b, true, PitchTrace.Tone.MODERATE),
            TraceDrawing.Dot(b, true), TraceDrawing.Dot(a, false, PitchTrace.Tone.REVIEW))
        s.scope.paintPitch(PitchInk(marks, null, 100.0, listOf(marker)), projection, bridge, null)
        assertEquals(listOf("drawLine", "drawLine", "drawCircle", "drawCircle", "drawPath"), s.ink.map { it.kind })
        val segment = s.ink[1].args
        assertEquals(pitchColor(PitchTrace.Tone.MODERATE).value.toLong(), segment[0])
        assertEquals(Offset(0f, 200f).packedValue, segment[1]); assertEquals(Offset(200f, 100f).packedValue, segment[2])
        assertEquals(2.3f, segment[3]); assertTrue(segment.any { it === bridge })
        assertTrue(s.ink[2].args.any { it is Stroke && it.width == 1.5f })
        assertEquals(2.5f, s.ink[3].args[1]); assertSame(marker, s.ink.last().args[0])
    }

    @Test fun canonicalInkNeverAddsATypicalLineAndUsesCanonicalStyle() {
        val s = Surface(); val color = Color(0xFF65C9D1)
        val a = TraceDrawing.Vertex(100.0, 50.0); val b = TraceDrawing.Vertex(200.0, 100.0)
        s.scope.paintPitch(PitchInk(listOf(TraceDrawing.Segment(a, b), TraceDrawing.Dot(b)), color, null, emptyList()), projection, null, null)
        assertEquals(listOf("drawLine", "drawCircle"), s.ink.map { it.kind })
        assertEquals(color.value.toLong(), s.ink[0].args[0]); assertEquals(2.5f, s.ink[0].args[3])
        assertEquals(color.value.toLong(), s.ink[1].args[0]); assertEquals(3f, s.ink[1].args[1])
        s.ink.clear(); s.scope.paintPitch(PitchInk(emptyList(), null, 200.0, emptyList()), projection, null, null)
        assertTrue(s.ink.isEmpty())
    }

    @Test fun waveformKeepsSamplePathsAndRealEnvelopeGeometry() {
        val s = Surface(); val path = nativeToken(Path::class.java)
        s.scope.paintWave(WaveDrawing.Samples(path)); assertSame(path, s.ink.single().args[0])
        s.ink.clear()
        s.scope.paintWave(WaveDrawing.Envelope(WaveGeometry.Envelope(listOf(WaveGeometry.Column(10.0, 20.0, 30.0)), 4.0)))
        assertEquals("drawLine", s.ink.single().kind)
        assertEquals(Offset(10f, 20f).packedValue, s.ink.single().args[1])
        assertEquals(Offset(10f, 30f).packedValue, s.ink.single().args[2]); assertEquals(4f, s.ink.single().args[3])
        s.ink.clear(); s.scope.paintWave(null); assertTrue(s.ink.isEmpty())
    }

    @Test fun spectrumRetainsGuideCurvePeakOrderAndNonOverlappingTentativeLabels() {
        val s = Surface(); val path = nativeToken(Path::class.java)
        val peaks = listOf(SpectrumGeometry.Peak(PlotPoint(10.0, 10.0), 1),
            SpectrumGeometry.Peak(PlotPoint(20.0, 40.0), 2), SpectrumGeometry.Peak(PlotPoint(100.0, 50.0), null),
            SpectrumGeometry.Peak(PlotPoint(200.0, -5.0), 3), SpectrumGeometry.Peak(PlotPoint(300.0, 60.0), 4))
        val writes = mutableListOf<Triple<String, Float, Float>>()
        s.scope.paintSpectrum(SpectrumInk(SpectrumGeometry(emptyList(), listOf(10.0), peaks), path), 200f, 20f, null,
            { 50f }, { text, x, y -> writes += Triple(text, x, y) })
        assertEquals(listOf("drawLine", "drawPath") + List(5) { "drawCircle" }, s.ink.map { it.kind })
        assertEquals(listOf(Triple("H1?", 14f, 19f), Triple("H4?", 304f, 57f)), writes)
        assertEquals(traceA.copy(alpha = .2f).value.toLong(), s.ink[0].args[0])
        assertEquals(traceA.value.toLong(), s.ink[2].args[0])
        assertEquals(traceB.value.toLong(), s.ink[4].args[0])
    }

    @Test fun timelineDistinguishesSelectionFromPlaybackAndNeverDrawsAnOffscreenCursor() {
        val s = Surface(); val dashed = nativeToken(PathEffect::class.java)
        s.scope.paintTimeline(TimelineInk(120L..180L, 200, -1, 8f), projection, dashed)
        assertEquals(listOf("drawRect", "drawLine"), s.ink.map { it.kind })
        assertEquals(Color(0xFF8CCFF3).copy(alpha = .13f).value.toLong(), s.ink[0].args[0])
        assertEquals(Size(120f, 200f).packedValue, s.ink[0].args[2])
        assertEquals(Color.White.copy(alpha = .7f).value.toLong(), s.ink[1].args[0])
        assertEquals(Offset(200f, 0f).packedValue, s.ink.last().args[1]); assertTrue(s.ink.last().args.any { it === dashed })
        s.ink.clear(); s.scope.paintTimeline(TimelineInk(null, 200, 300, 8f), projection, dashed)
        assertEquals(listOf("drawLine", "drawCircle"), s.ink.map { it.kind })
        assertEquals(Color(0xFF9AD9FF).value.toLong(), s.ink[0].args[0])
        assertEquals(Offset(400f, 190f).packedValue, s.ink.last().args[2]); assertEquals(8f, s.ink.last().args[1])
        s.ink.clear(); s.scope.paintTimeline(TimelineInk(null, 200, 301, 8f), projection, dashed)
        assertTrue(s.ink.isEmpty())
    }

    @Test fun contentPipelineKeepsGridTraceComparisonWaveSpectrumAndCursorInTheirEstablishedOrder() {
        val s = Surface(); val path = nativeToken(Path::class.java)
        val pitch = PitchInk(listOf(TraceDrawing.Dot(TraceDrawing.Vertex(200.0, 100.0))), Color(0xFF65C9D1), null, emptyList())
        val comparison = ComparisonInk(path, listOf(Offset(20f, 20f)))
        val spectrum = SpectrumInk(SpectrumGeometry(emptyList(), emptyList(), emptyList()), path)
        val content = PlotContent(null, pitch, comparison, WaveDrawing.Samples(path), spectrum, TimelineInk(null, 200, 200, 8f))
        val effects = PlotEffects(null, null, null, null, null)
        s.scope.paintPlotContent(content, projection, effects, listOf(200.0), listOf(100.0), 20f, { 10f }, { _, _, _ -> fail("No labels") })
        assertEquals(listOf("drawLine", "drawLine", "drawCircle", "drawPath", "drawCircle", "drawPath", "drawPath", "drawLine", "drawCircle"), s.ink.map { it.kind })
        assertEquals(Color(0xFF65C9D1).value.toLong(), s.ink[2].args[0])
        assertEquals(swipeColor.value.toLong(), s.ink[4].args[0])
        assertEquals(2.5f, s.ink[7].args[3])
    }

    @Test fun levelStripPreservesAmplitudeEvidenceSeparatelyFromAnyPitchTrace() {
        val s = Surface()
        s.scope.paintAudioLevel(floatArrayOf(1f, 0f), 10f, 20f, 16f)
        assertEquals(listOf("drawLine", "drawLine"), s.ink.map { it.kind })
        assertEquals(Offset(10f, 18f).packedValue, s.ink[0].args[1]); assertEquals(Offset(10f, 4f).packedValue, s.ink[0].args[2])
        assertEquals(Offset(11f, 18f).packedValue, s.ink[1].args[2])
    }

    @Test fun completedRasterUsesItsOwnOriginScaleAndRestoresTheCanvasContext() {
        val s = Surface()
        val bitmap = Proxy.newProxyInstance(ImageBitmap::class.java.classLoader, arrayOf(ImageBitmap::class.java)) { _, method, _ ->
            when (method.name) { "getWidth" -> 100; "getHeight" -> 50; else -> error("Unexpected bitmap dependency") }
        } as ImageBitmap
        val raster = RasterInk(bitmap, RasterPlacement(PlotPoint(20.0, 30.0), 200.0, 100.0))
        val content = PlotContent(raster, null, null, null, null, null)
        s.scope.paintPlotContent(content, projection, PlotEffects(null, null, null, null, null), emptyList(), emptyList(), 20f,
            { 0f }, { _, _, _ -> fail("No text") })
        assertEquals(listOf("save", "translate", "scale", "drawImage", "restore", "restoreSize"), s.ink.map { it.kind })
        assertEquals(listOf(20f, 30f), s.ink[1].args)
        assertEquals(listOf(2f, 2f, Offset.Zero.packedValue), s.ink[2].args)
        assertSame(bitmap, s.ink[3].args[0]); assertEquals(Size(400f, 200f).packedValue, s.ink.last().args.single())
        assertEquals(IntSize(100, 50).packedValue, s.ink[3].args[2])
        assertEquals(IntSize(100, 50).packedValue, s.ink[3].args[4])
    }
}
