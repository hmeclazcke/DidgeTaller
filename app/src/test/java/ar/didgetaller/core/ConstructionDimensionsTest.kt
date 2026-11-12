package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs

/** Independent workshop dimensions, not assertions copied from rendered output. */
class ConstructionDimensionsTest {
    private val shape=AcousticModel.Shape().withControlPoints(listOf(
        AcousticModel.BorePoint(0,32.0),AcousticModel.BorePoint(750,50.0),AcousticModel.BorePoint(1500,80.0)))
    private fun wood()=ConstructionProjects.create("<& wood",shape,ConstructionOptions())
    @Test fun taperedStationsHalfDepthsAndCylinderMidpointsMatchKnownGeometry() {
        val plan=wood()
        val row=ConstructionDrawing.rows(plan).single {it.xMm==500.0}
        assertEquals(44.0,row.idealMm!!,1e-9)
        assertEquals(44.0,row.beforeMm,1e-9);assertEquals(44.0,row.afterMm,1e-9)
        assertEquals(41.0,ConstructionDrawing.diameter(plan,375.0,false),1e-9)
        val csv=ConstructionDrawing.csv(plan).lineSequence().first {it.startsWith("\"0.0 *\"")}.split("\",\"")
        assertEquals("16.0",csv[2]);assertEquals("50.0",csv[3]);assertEquals("25.0",csv[4].trim('"'))
        assertTrue(ConstructionDrawing.table(plan).any {it.contains("16.0")})
        val cylinders=ConstructionProjects.cylinders(plan)
        assertEquals(16,cylinders.size)
        assertEquals(93.75,cylinders.first().lengthMm,0.0)
        assertEquals(33.125,cylinders.first().diameterMm,0.0)
        assertEquals(51.875,cylinders[8].diameterMm,0.0)
        val changed=ConstructionProjects.perturb(plan,1.0,5.0)
        assertEquals(listOf(750.0,755.0),changed.pieces.map {it.lengthMm})
        assertEquals(listOf(33.0,51.0),changed.pieces.map {it.inletMm})
        assertEquals(listOf(51.0,81.0),changed.pieces.map {it.outletMm})
        val base=ConstructionProjects.predict(plan).baseHz
        val expected=plan.copy(pieces=listOf(plan.pieces[0].copy(inletMm=33.0,outletMm=51.0),plan.pieces[1]))
        assertEquals(ConstructionProjects.predict(expected).baseHz-base,ConstructionSearch.sensitivities(plan,1.0).single {it.dimension==plan.pieces[0].label}.deltaHz!!,1e-9)
    }
    @Test fun svgIsDimensionalEscapedAndIncludesBothPreparedHalves() {
        val xml=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ConstructionDrawing.svg(wood()).byteInputStream())
        assertEquals("595.0pt",xml.documentElement.getAttribute("height"))
        assertEquals("<& wood",xml.getElementsByTagName("title").item(0).textContent)
        val paths=xml.getElementsByTagName("polyline")
        val shaded=(0 until paths.length).map {paths.item(it) as org.w3c.dom.Element}.filter {it.getAttribute("data-cavity")=="true"}
        assertEquals(3,shaded.size) // three complete transverse openings, with no duplicate long halves
        val points=shaded.first().getAttribute("points").split(' ').map {it.split(',').map(String::toDouble)}
        assertEquals(3,shaded.size)
        val minX=points.minOf {it[0]};val maxX=points.maxOf {it[0]}
        val minY=points.minOf {it[1]};val maxY=points.maxOf {it[1]}
        assertEquals(maxX-minX,maxY-minY,1e-6)
        val texts=xml.getElementsByTagName("text")
        val labels=(0 until texts.length).map {texts.item(it).textContent}
        assertFalse(labels.any {it.startsWith("Half A") || it.startsWith("Half B")})
        assertTrue(labels.contains("Groove depth per half"))

    }
    @Test fun layerRemainderAndOpeningApproximationAreExplicit() {
        val plan=ConstructionProjects.create("layers",AcousticModel.Shape().withControlPoints(listOf(AcousticModel.BorePoint(0,32.0),AcousticModel.BorePoint(1500,80.0))),ConstructionOptions(method=ConstructionMethod.LAMINATED,layers="128"))
        assertEquals(12,plan.pieces.size);assertEquals(92.0,plan.pieces.last().lengthMm,0.0)
        assertEquals(36.096,plan.pieces.first().inletMm,1e-9)
        assertTrue(plan.parts.first().startsWith("1:"));assertTrue(plan.parts.last().startsWith("12:"))
        assertEquals(4.096,plan.parts.first().substringAfter("Emax=").substringBefore(' ').toDouble(),1e-9)
    }
    @Test fun octaveAndOptionalResonanceScoresPreserveUnitsAndCutScaling() {
        assertEquals(1200.0,ConstructionSearch.cents(880.0,440.0),1e-9)
        assertEquals(-1200.0,ConstructionSearch.cents(220.0,440.0),1e-9)
        val target=ConstructionTarget(higher=listOf(200.0,400.0))
        val candidates=ConstructionSearch.search(shape,ConstructionOptions(),target)
        candidates.forEach {c->
            val higher=target.higher.mapIndexed {i,t->abs(1200*kotlin.math.ln(c.higher[i]/t)/kotlin.math.ln(2.0))}.sum()
            assertEquals(abs(c.cents)+higher*.5+c.plan.pieces.size*.05,c.score,1e-8)
            assertTrue(c.plan.lengthMm in listOf(1125.0,1350.0,1500.0,1650.0,1800.0))
        }
        val pvc=ConstructionOptions(method=ConstructionMethod.PVC,pipes="a,32,40,1500\nb,70,80,1500",
            fittings="c,40.2,80.2,30,25,90,32,0:32|35:70",assembly="a,800,c\nb,650,-")
        assertEquals(setOf(1340.0,1413.0,1485.0,1558.0,1630.0),ConstructionSearch.search(shape,pvc,ConstructionTarget()).map {it.plan.lengthMm}.toSet())
    }
}
