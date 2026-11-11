package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class WoodWorkshopTest {
    private val shape=AcousticModel.Shape(mouthMm=30,bellMm=83)
    private fun plan(stock:WoodStock?)=ConstructionProjects.create("Río claro",shape,ConstructionOptions(wallMm=9.0,radialToleranceMm=0.0,woodStock=stock))
    @Test fun dimensionArrowsDistinguishDiameterDepthAndStockAndBothHalvesAreMirrored() {
        val stocks=listOf(WoodStock.Log(1500.0,101.0,emptyList(),0.0,0.0,WoodPreparation(),true),WoodStock.Timber(1500.0,101.0,50.5,true,WoodPreparation(),true))
        for(stock in stocks)for(l in WorkshopLanguage.entries) {
            val d=WorkshopDocument.create(plan(stock),l)
            assertEquals(2,d.pages.size)
            d.pages.forEach {assertEquals(842.0,it.width,0.0);assertEquals(595.0,it.height,0.0)}
            val measures=d.pages[1].dimensions
            assertEquals(83.0,measures.last {it.kind=="bore-diameter"}.valueMm,0.0)
            assertEquals(41.5,measures.last {it.kind=="groove-depth"}.valueMm,0.0)
            assertEquals(9.0,measures.last {it.kind=="base-wall"}.valueMm,0.0)
            assertEquals(101.0,measures.last {it.kind=="stock-width"}.valueMm,0.0)
            for(m in d.pages.flatMap {it.dimensions})assertEquals(m.valueMm*m.scale,hypot(m.end.first-m.start.first,m.end.second-m.start.second),1e-8)
            val cavities=d.pages[1].marks.filterIsInstance<WorkshopMark.Path>().filter {it.shaded}
            assertEquals(3,cavities.size)
            val diameters=measures.filter {it.kind=="bore-diameter"}
            val depths=measures.filter {it.kind=="groove-depth"}
            for(i in cavities.indices) {
                val cx=(diameters[i].start.first+diameters[i].end.first)/2
                val cy=depths[i].start.second
                for((a,b) in cavities[i].points.zip(cavities[i].points.asReversed())) {
                    assertEquals(a.first,b.first,1e-8);assertEquals(2*cy,a.second+b.second,1e-8)
                    assertEquals(diameters[i].valueMm/2*diameters[i].scale,hypot(a.first-cx,a.second-cy),1e-8)
                }
            }
            // The actual profile bend remains on page1, independently of the transverse circles.
            val s=d.pages[0].dimensions.first {it.kind=="overall-length"}.scale
            val profile=d.pages[0].marks.filterIsInstance<WorkshopMark.Path>().single {it.shaded}
            assertTrue(profile.points.any {(x,y)->kotlin.math.abs(x-(62+1020*s))<1e-8 && kotlin.math.abs(y-(260-21.5*s))<1e-8})
            assertFalse(d.text().contains("UUID"));assertFalse(d.text().contains("83.000"))
        }
    }
    @Test fun naturalLogNeverBorrowsTheBoreTaperAndRectangularSectionsStayRectangular() {
        val stock=WoodStock.Log(1500.0,120.0,emptyList(),0.0,0.0,WoodPreparation(),true)
        val log=WorkshopDocument.create(plan(stock),WorkshopLanguage.ENGLISH)
        val outer=log.pages[0].marks.filterIsInstance<WorkshopMark.Path>().first {it.fillColor==WoodInk.WOOD}
        assertEquals(outer.points[0].second,outer.points[1].second,0.0)
        assertEquals(outer.points[2].second,outer.points[3].second,0.0)
        val boards=stock.let {WoodStock.Timber(1500.0,101.0,50.5,true,WoodPreparation(),true)}
        val section=WorkshopDocument.create(plan(boards),WorkshopLanguage.ENGLISH).pages[1].marks.filterIsInstance<WorkshopMark.Path>().first {it.fillColor==WoodInk.WOOD && it.points.size==4}
        assertEquals(section.points[0].first,section.points[3].first,0.0)
        assertEquals(section.points[0].second,section.points[1].second,0.0)
        assertTrue(log.text().contains("minimum diameter"))
        assertTrue(WorkshopDocument.create(plan(null),WorkshopLanguage.ENGLISH).text().contains(WoodTerm.UNKNOWN.en))
    }
    @Test fun additiveFormFieldsRoundTripAndLegacyProjectsRemainUnconfirmed() {
        val v=ConstructionForm.defaults(shape)+mapOf("stockKind" to "BOARDS","stockLength" to "1500","stockWidth" to "101","stockThickness" to "50.5","stockFace" to "0","stockTrim" to "0","stockUncertainty" to "0","stockConfirmed" to "true","wall" to "9","tolerance" to "0")
        val o=ConstructionForm.options(v)
        assertEquals(WoodFitStatus.FITS,WoodFit.assess(shape,o.woodStock,9.0,0.0).status)
        assertEquals(o,ConstructionForm.options(v.toMap()))
        assertNull(ConstructionForm.options(v.filterKeys {!it.startsWith("stock")}).woodStock)
        assertNull(ConstructionForm.options(v+mapOf("method" to "PVC","stockLength" to "invalid")).woodStock)
        assertEquals(WoodFitStatus.UNCONFIRMED,WoodFit.assess(shape,null,9.0,0.0).status)
        for(term in WoodTerm.entries) {assertTrue(term.en.isNotBlank());assertTrue(term.es.isNotBlank());assertFalse(term.es.contains('?'))}
    }
    @Test fun inverseSearchRejectsTooSmallWoodRatherThanChangingTheGivenStock() {
        val stock=WoodStock.Timber(1600.0,70.0,40.0,true,WoodPreparation(),true)
        val o=ConstructionOptions(woodStock=stock)
        val candidates=ConstructionSearch.search(shape,o,ConstructionTarget(mouthMm=30.0))
        assertTrue(candidates.isEmpty())
        val good=ConstructionSearch.search(shape,o.copy(woodStock=stock.copy(widthMm=150.0,thicknessMm=80.0)),ConstructionTarget(mouthMm=30.0))
        assertTrue(good.isNotEmpty())
        good.forEach {assertTrue(it.plan.lengthMm<=1600);assertTrue(WoodStockForm.accepts(it.plan))}
    }
    @Test fun detailedCsvPreservesFractionalBreakpointsAndUnknownWoodDoesNotInventCapacity() {
        val stock=WoodStock.Log(1500.0,120.0,listOf(WoodSection(0.0,120.0),WoodSection(731.25,119.8),WoodSection(1500.0,120.0)),0.0,0.0,WoodPreparation(),true)
        val csv=WorkshopDocument.csv(plan(stock),WorkshopLanguage.ENGLISH)
        assertTrue(csv.contains("731.250000"));assertTrue(csv.contains("119.800000"))
        val raw=WorkshopDocument.csv(plan(null),WorkshopLanguage.SPANISH)
        assertTrue(raw.contains("Interior antes de la posición (mm)"))
        assertTrue(raw.lines().last {it.startsWith("\"1500.000000")}.endsWith("\"\",\"\",\"\",\"\""))
    }    @Test fun formParsesBothStockAxesAndRejectsAmbiguousOrMalformedBounds() {
        val defaults=ConstructionForm.defaults(shape)
        assertEquals("1520",defaults["stockLength"])
        val base=defaults+mapOf("stockKind" to "LOG","stockSections" to "0,120\n\n1520,121","stockConfirmed" to "true")
        val log=WoodStockForm.read(base) as WoodStock.Log
        assertEquals(listOf(WoodSection(0.0,120.0),WoodSection(1520.0,121.0)),log.sections)
        val block=WoodStockForm.read(base+mapOf("stockKind" to "BLOCK","stockBlockThickness" to "130,5")) as WoodStock.Timber
        assertFalse(block.twoBoards);assertEquals(130.5,block.thicknessMm,0.0)
        assertNull(WoodStockForm.read(emptyMap()))
        for(changes in listOf(mapOf("stockKind" to "unknown"),mapOf("stockSections" to "1,2,3"),mapOf("stockSections" to "1,120\n0,120"),mapOf("stockSections" to "x".repeat(8193)),mapOf("stockConfirmed" to "yes"))) {
            try {WoodStockForm.read(base+changes);fail("Malformed measurements")}catch(_:IllegalArgumentException){}
        }
        val p=plan(block)
        for(method in listOf(ConstructionMethod.LAMINATED,ConstructionMethod.PVC))assertTrue(WoodStockForm.accepts(p.copy(options=p.options.copy(method=method,woodStock=block.copy(widthMm=10.0,thicknessMm=1.0)))))
        assertEquals("Río claro",ConstructionForm.name(mapOf("planName" to "Río claro"),"Legacy"))
        assertEquals("Legacy",ConstructionForm.name(mapOf("planName" to ""),"Legacy"))
        assertEquals("Legacy",ConstructionForm.name(emptyMap(),"Legacy"))
    }
    @Test fun printedMinimumStockNeverRoundsBelowTheFullPrecisionRequirement() {
        val fractional=shape.copy(points=listOf(AcousticModel.BorePoint(0,30.0),AcousticModel.BorePoint(1500,83.02)))
        val p=ConstructionProjects.create("Fractional stock",fractional,ConstructionOptions(wallMm=9.0,radialToleranceMm=0.0))
        val text=WorkshopDocument.create(p,WorkshopLanguage.ENGLISH).text()
        assertTrue(text.contains("width 101.1 mm"))
        assertTrue(text.contains("thickness 50.6 mm"))
        assertEquals(50.51,WoodFit.assess(fractional,null,9.0,0.0).requiredHalfDepthMm,1e-9)
    }

}
