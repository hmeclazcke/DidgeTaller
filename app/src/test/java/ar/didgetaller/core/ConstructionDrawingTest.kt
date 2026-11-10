package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory
import java.util.concurrent.CancellationException

class ConstructionDrawingTest {
    private fun step()=ConstructionPlan("synthetic",AcousticModel.Shape(),ConstructionOptions(),
        listOf(ConstructionPiece("a",700.0,32.0,32.0),ConstructionPiece("b",800.0,70.0,70.0)),listOf("a","b"))

    @Test fun exportPreservesBothDiametersAtActualJunctions() {
        val p=step()
        val row=ConstructionDrawing.rows(p).single {it.xMm==700.0}
        assertEquals(32.0,row.beforeMm,0.0);assertEquals(70.0,row.afterMm,0.0)
        assertEquals(listOf(700.0 to 16.0,700.0 to 35.0),ConstructionDrawing.outline(p,1).filter {it.first==700.0})
        val svg=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ConstructionDrawing.svg(p).byteInputStream())
        assertEquals("1580.000mm",svg.documentElement.getAttribute("width"))
        assertTrue(ConstructionDrawing.csv(p).contains("\"32.000\",\"70.000\""))
    }
    @Test fun metricCalibrationTilesAndFractionalEndsHaveNoInventedIdealExtension() {
        assertEquals(283.464566929,100*ConstructionDrawing.POINTS_PER_MM,1e-8)
        val starts=ConstructionDrawing.tileStarts(3000.0)
        assertEquals(20,starts.size)
        assertTrue(starts.zipWithNext().all {(a,b)->a+160-b==10.0})
        assertTrue(starts.last()+160>=3000)
        val changed=ConstructionProjects.perturb(step(),lengthDeltaMm=.5)
        assertNull(ConstructionDrawing.rows(changed).last().idealMm)
        assertEquals(1500.5,ConstructionDrawing.rows(changed).last().xMm,0.0)
    }
    @Test fun legacyGeometryRoundTripsAndSpreadsheetTextIsEscaped() {
        val shape=AcousticModel.Shape()
        val form=ConstructionForm.defaults(shape)
        assertEquals(shape.controlPoints(),ConstructionForm.shape(form).controlPoints())
        assertEquals(ConstructionOptions(),ConstructionForm.options(form))
        assertEquals(73.42,ConstructionForm.target(form).hz,0.0)
        assertTrue(ConstructionDrawing.csv(step().copy(name="=A1")).contains("'=A1"))
    }
    @Test fun searchIsBoundedRankedAndCancellationNeverReturnsAResult() {
        val target=ConstructionTarget()
        val candidates=ConstructionSearch.search(AcousticModel.Shape(),ConstructionOptions(),target)
        assertEquals(5,candidates.size)
        assertTrue(candidates.zipWithNext().all {(a,b)->a.score<=b.score})
        candidates.forEach {assertTrue(it.plan.lengthMm<=target.maximumLengthMm);assertEquals(target.mouthMm,it.plan.pieces.first().inletMm,0.0)}
        val constrained=ConstructionSearch.search(AcousticModel.Shape(),ConstructionOptions(),target.copy(maximumBellMm=32.0))
        assertEquals(5,constrained.map {it.plan.pieces}.distinct().size)
        try {ConstructionSearch.search(AcousticModel.Shape(),ConstructionOptions(),target){true};fail("Must cancel")}
        catch (_:CancellationException) { }
    }
}
