package ar.didgetaller.ui

import ar.didgetaller.core.AcousticModel
import org.junit.Assert.*
import org.junit.Test

class ConstructionProfileEditingTest {
    @Test fun resizingDenseCustomPointsKeepsEveryDiameterAndOrderedEndpoints() {
        val shape=AcousticModel.Shape(lengthMm=3000,points=listOf(AcousticModel.BorePoint(0,30.0),AcousticModel.BorePoint(1,31.0),AcousticModel.BorePoint(2,32.0),AcousticModel.BorePoint(3000,83.0)))
        val resized=resizeProfile(shape,500)
        assertEquals(shape.controlPoints().map {it.diameterMm},resized.controlPoints().map {it.diameterMm})
        assertEquals(0,resized.controlPoints().first().positionMm);assertEquals(500,resized.controlPoints().last().positionMm)
        assertTrue(resized.controlPoints().zipWithNext().all {(a,b)->a.positionMm<b.positionMm})
    }
    @Test fun legacyResizeAndEndpointEditsRetainTheOtherDimensions() {
        val shape=AcousticModel.Shape()
        assertEquals(shape.copy(lengthMm=1800),resizeProfile(shape,1800))
        assertEquals(40,changeDiameter(shape,0,40).mouthMm)
        assertEquals(50,changeDiameter(shape,1,50).middleMm)
        assertEquals(95,changeDiameter(shape,2,95).bellMm)
        val custom=shape.withControlPoints(shape.controlPoints())
        assertEquals(40.0,changeDiameter(custom,0,40).controlPoints().first().diameterMm,0.0)
        assertEquals(50.0,changeDiameter(custom,1,50).controlPoints()[1].diameterMm,0.0)
        assertEquals(95.0,changeDiameter(custom,2,95).controlPoints().last().diameterMm,0.0)
    }
}
