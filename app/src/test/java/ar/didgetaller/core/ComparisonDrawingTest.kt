package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class ComparisonDrawingTest {
    @Test fun comparisonPreservesLinkedSegmentsGapsAndIsolatedMeasurements() {
        val curve = SwipeCurve(listOf(SwipeCurve.Sample(0.0, 70.0), SwipeCurve.Sample(20.0, 72.0),
            SwipeCurve.Sample(40.0, null), SwipeCurve.Sample(60.0, 75.0)), 20.0, 35, 220)
        assertEquals(listOf(TraceDrawing.Segment(TraceDrawing.Vertex(0.0, 70.0), TraceDrawing.Vertex(20.0, 72.0)),
            TraceDrawing.Dot(TraceDrawing.Vertex(60.0, 75.0))), TraceDrawing.comparison(curve, AxisWindow(0.0, 100.0)))
    }
    @Test fun viewportMarginKeepsOneHopAndDoesNotJoinDistantPoints() {
        val curve = SwipeCurve(listOf(-21.0, -20.0, 0.0, 120.0, 121.0).map { SwipeCurve.Sample(it, 70.0) }, 20.0, 35, 220)
        val marks = TraceDrawing.comparison(curve, AxisWindow(0.0, 100.0))
        assertEquals(listOf(TraceDrawing.Segment(TraceDrawing.Vertex(-21.0, 70.0), TraceDrawing.Vertex(-20.0, 70.0)),
            TraceDrawing.Segment(TraceDrawing.Vertex(-20.0, 70.0), TraceDrawing.Vertex(0.0, 70.0))), marks)
        assertTrue(TraceDrawing.comparison(curve, AxisWindow(500.0, 600.0)).isEmpty())
    }
}
