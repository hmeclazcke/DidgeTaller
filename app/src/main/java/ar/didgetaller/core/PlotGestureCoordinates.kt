package ar.didgetaller.core

/** The measured plot rectangle is the only input origin for touch and viewport transforms. */
class PlotGestureCoordinates(viewport: PlotViewport, private val width: Double, private val height: Double,
    private val left: Double, private val top: Double, logY: Boolean) {
    private val projection = PlotProjection(viewport, width.coerceAtLeast(1.0), height.coerceAtLeast(1.0), left, top, logY)
    data class Transform(val anchorX: Double, val anchorY: Double, val zoomX: Double, val zoomY: Double,
                         val panX: Double, val panY: Double)

    fun contains(point: PlotPoint): Boolean = point.x in left..(left + width) && point.y in top..(top + height)
    fun data(point: PlotPoint): PlotPoint = projection.data(point.x, point.y)
    fun transform(center: PlotPoint, pan: PlotPoint, zoomX: Double, zoomY: Double): Transform = Transform(
        ((center.x - left) / projection.width).coerceIn(0.0, 1.0),
        ((center.y - top) / projection.height).coerceIn(0.0, 1.0), zoomX, zoomY,
        pan.x / projection.width, pan.y / projection.height)
}
