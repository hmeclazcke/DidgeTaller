package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class WorkshopUsabilityTest {
    private val shape=AcousticModel.Shape(mouthMm=30,bellMm=83)
    private fun boards(thickness:Double=50.5)=WoodStock.Timber(1500.0,101.0,thickness,true,WoodPreparation(),true)
    private fun plan(stock:WoodStock?)=ConstructionProjects.create("Río claro",shape,ConstructionOptions(wallMm=9.0,radialToleranceMm=0.0,woodStock=stock))
    @Test fun secondSheetReplacesDuplicateHalfViewsWithAUsefulCarvingTable() {
        val page=WorkshopDocument.create(plan(boards()),WorkshopLanguage.SPANISH).pages[1]
        val text=page.marks.filterIsInstance<WorkshopMark.Text>().joinToString("\n"){it.value}
        assertFalse(text.contains("Mitad A"));assertFalse(text.contains("Mitad B"))
        for(label in listOf("Desde la boquilla","Diámetro interior","Profundidad por mitad","Espesor restante"))assertTrue(label,text.contains(label))
        assertEquals(3,page.marks.filterIsInstance<WorkshopMark.Path>().count {it.shaded})
        assertTrue(text.contains("medidas completas en CSV"));assertFalse(text.contains("CSV / SVG"))
    }
    @Test fun overviewMarksRealBoreDiametersAtDesignPositions() {
        val d=WorkshopDocument.create(plan(boards()),WorkshopLanguage.ENGLISH)
        val markers=d.pages[0].dimensions.filter {it.kind=="station-bore"}
        assertTrue(markers.size in 3..5)
        assertTrue(markers.any {it.valueMm==30.0});assertTrue(markers.any {it.valueMm==43.0});assertTrue(markers.any {it.valueMm==83.0})
        assertEquals(1,d.pages[0].marks.filterIsInstance<WorkshopMark.Path>().count {it.fillColor==WoodInk.WOOD})
    }
    @Test fun spanishPlanUsesNaturalWorkshopTermsAndAProperAccidental() {
        val text=WorkshopDocument.create(plan(boards()),WorkshopLanguage.SPANISH,Music.frequency(37)).text()
        assertTrue(text.contains("Do♯2"))
        assertTrue(text.contains("Mínimo geométrico: sin margen adicional"))
        for(jargon in listOf("envolvente","Posición limitante","restricción:","mandan las cotas","sostenido2"))assertFalse(jargon,text.contains(jargon))
    }    @Test fun carvingDepthsAndActualRemainingBoardThicknessAgreeAtEveryKeyPosition() {
        val p=plan(boards())
        val rows=listOf(0.0,1020.0,1500.0).map {CarvingMeasurements.row(p,it)}
        assertEquals(listOf(30.0,43.0,83.0),rows.map {it.diameterMm})
        assertEquals(listOf(15.0,21.5,41.5),rows.map {it.depthMm})
        assertEquals(listOf(35.5,29.0,9.0),rows.map {it.remainingMm})
        assertEquals(18.5,CarvingMeasurements.row(plan(boards(60.0)),1500.0).remainingMm!!,0.0)
        val text=WorkshopDocument.create(p,WorkshopLanguage.SPANISH).text()
        for(value in listOf("35,5","21,5","41,5"))assertTrue(text.contains(value))
    }
    @Test fun logWallUsesThePreparedCircularCapAndNeverAnInventedBoardThickness() {
        val stock=WoodStock.Log(1500.0,120.0,emptyList(),2.0,1.0,WoodPreparation(kerfMm=3.0,faceMm=1.0,uncertaintyMm=1.0),true)
        assertEquals(12.0,CarvingMeasurements.row(plan(stock),1500.0).remainingMm!!,1e-9)
        val partial=stock.copy(sections=listOf(WoodSection(100.0,120.0),WoodSection(1400.0,120.0)))
        assertNull(CarvingMeasurements.row(plan(partial),0.0).remainingMm)
        assertNull(CarvingMeasurements.row(plan(partial),1500.0).remainingMm)
        assertNotNull(CarvingMeasurements.row(plan(partial),500.0).remainingMm)
        assertNull(CarvingMeasurements.row(plan(stock.copy(confirmed=false)),500.0).remainingMm)
        assertNull(CarvingMeasurements.row(plan(null),1500.0).remainingMm)
        assertFalse(WorkshopDocument.create(plan(partial),WorkshopLanguage.SPANISH).text().contains(WoodTerm.FITS.es))
    }
    @Test fun selectionIncludesTransitionsAndTheTrueMinimumWallWithoutOvercrowding() {
        val stock=WoodStock.Log(1500.0,120.0,listOf(WoodSection(0.0,120.0),WoodSection(733.25,60.0),WoodSection(1500.0,140.0)),0.0,0.0,WoodPreparation(),true)
        val p=plan(stock)
        val positions=CarvingMeasurements.positions(p,9)
        assertTrue(positions.size in 3..9)
        assertTrue(positions.containsAll(listOf(0.0,733.25,1020.0,1500.0)))
        assertEquals(733.25,CarvingMeasurements.minimum(p)!!.positionMm,0.0)
        assertEquals(listOf(0.0,733.25,1500.0),CarvingMeasurements.sections(p))
        assertEquals(positions.sorted().distinct(),positions)
        assertEquals(3,CarvingMeasurements.positions(p,3).size)
    }
    @Test fun assembledBoardSectionIsRectangularAndDiameterArrowsSpanTheWholeOpening() {
        val d=WorkshopDocument.create(plan(boards()),WorkshopLanguage.ENGLISH)
        val page=d.pages[1]
        val outlines=page.marks.filterIsInstance<WorkshopMark.Path>().filter {it.fillColor==WoodInk.WOOD}
        assertEquals(3,outlines.size)
        val dimensions=page.dimensions.filter {it.kind=="bore-diameter"}
        for((i,path) in outlines.withIndex()) {
            assertEquals(4,path.points.size)
            val scale=dimensions[i].scale
            assertEquals(101*scale,path.points.maxOf {it.first}-path.points.minOf {it.first},1e-8)
            assertEquals(101*scale,path.points.maxOf {it.second}-path.points.minOf {it.second},1e-8)
        }
        for(m in d.pages.flatMap {it.dimensions}) {
            assertEquals(m.valueMm*m.scale,kotlin.math.hypot(m.end.first-m.start.first,m.end.second-m.start.second),1e-8)
            assertTrue(d.pages.flatMap {it.marks}.filterIsInstance<WorkshopMark.Path>().any {it.points==listOf(m.start,m.end)})
        }
    }
    @Test fun intermediateStationsCoverTheLengthAndTheThreeSectionsAreDistinct() {
        val p=plan(boards())
        val positions=CarvingMeasurements.positions(p,9)
        assertTrue(positions.size>=6)
        assertTrue(positions.zipWithNext().all {(a,b)->b-a<=p.lengthMm/4})
        assertEquals(listOf(0.0,1020.0,1500.0),CarvingMeasurements.sections(p))
        val linear=AcousticModel.Shape(lengthMm=500,points=listOf(AcousticModel.BorePoint(0,30.0),AcousticModel.BorePoint(500,83.0)))
        val short=ConstructionProjects.create("Short",linear,ConstructionOptions(intervalMm=500))
        assertEquals(listOf(0.0,250.0,500.0),CarvingMeasurements.sections(short))
        for(limit in listOf(2,13))try {CarvingMeasurements.positions(p,limit);fail("Invalid page density")}catch(_:IllegalArgumentException){}
        assertTrue(CarvingMeasurements.positions(p,12).size<=12)
    }
    @Test fun baseWoodAndSmallestSideWallRemainDifferentQuantities() {
        val p=plan(boards(70.0))
        val row=CarvingMeasurements.row(p,1500.0)
        assertEquals(28.5,row.remainingMm!!,0.0)
        assertEquals(9.0,row.minimumWallMm!!,0.0)
        val d=WorkshopDocument.create(p,WorkshopLanguage.ENGLISH)
        assertTrue(d.text().contains("Smallest remaining wall: 9 mm"))
        assertTrue(d.pages[1].marks.filterIsInstance<WorkshopMark.Text>().any {it.value=="28.5"})
    }
    @Test fun preservedUiFitDetailsNeverInventMeasurementsOrHideAFailure() {
        val l=WorkshopLanguage.ENGLISH
        val unknown=WoodFit.assess(shape,null,9.0,0.0)
        assertTrue(WoodPresentation.detail(unknown,l).contains(l.wood(WoodTerm.LIMIT_UNKNOWN)))
        assertFalse(WoodPresentation.detail(unknown,l).contains(l.wood(WoodTerm.MARGIN)))
        val good=WoodFit.assess(shape,boards(),9.0,0.0)
        assertTrue(WoodPresentation.detail(good,l).contains("1500"))
        assertTrue(WoodPresentation.detail(good,l).contains(l.wood(WoodTerm.MARGIN)+": 0 mm"))
        val bad=WoodFit.assess(shape,boards().copy(widthMm=90.0),9.0,0.0)
        assertTrue(WoodPresentation.detail(bad,l).contains(l.wood(WoodTerm.LIMIT_WIDTH)))
        assertTrue(WoodPresentation.detail(bad,l).contains("-5.5 mm"))
    }
    @Test fun anUnmeasuredPlanDoesNotInventALimitingLocation() {
        val page=WorkshopDocument.create(plan(null),WorkshopLanguage.ENGLISH).pages[0]
        val text=page.marks.filterIsInstance<WorkshopMark.Text>().joinToString("\n"){it.value}
        assertTrue(text.contains(WoodTerm.UNKNOWN.en))
        assertFalse(text.contains("at the mouthpiece"))
        assertFalse(page.marks.filterIsInstance<WorkshopMark.Path>().any {it.fillColor==WoodInk.WOOD})
    }
    @Test fun everyPrintedStationAlsoHasFullPrecisionCsvMeasurements() {
        val p=plan(boards())
        val csv=WorkshopDocument.csv(p,WorkshopLanguage.ENGLISH)
        for(x in CarvingMeasurements.positions(p,9)) {
            val number=java.lang.String.format(java.util.Locale.ROOT,"%.6f",x)
            assertTrue(number,csv.lineSequence().any {it.startsWith("\"$number\",")})
        }
        assertTrue(csv.contains("\"750.000000\",\"39.558824\",\"39.558824\",\"19.779412\""))
    }

    @Test fun bilingualPdfStatesAndStockTermsAreDistinctAndUserNamesArePreserved() {
        for(l in WorkshopLanguage.entries) {
            val minimum=plan(boards())
            fun text(stock:WoodStock?)=WorkshopDocument.create(plan(stock),l).text()
            assertTrue(text(boards()).contains(l.wood(WoodTerm.ZERO_MARGIN)))
            assertTrue(text(boards().copy(widthMm=120.0,thicknessMm=60.0)).contains(l.wood(WoodTerm.POSITIVE_MARGIN)))
            assertTrue(text(boards().copy(widthMm=90.0)).contains(l.wood(WoodTerm.FAIL)))
            assertTrue(text(null).contains(l.wood(WoodTerm.UNKNOWN)))
            assertTrue(WorkshopDocument.create(minimum,l).text().startsWith("Río claro"))
            val log=WoodStock.Log(1500.0,101.0,emptyList(),0.0,0.0,WoodPreparation(),true)
            val page=WorkshopDocument.create(plan(log),l).pages[1].marks.filterIsInstance<WorkshopMark.Text>().joinToString("\n"){it.value}
            assertTrue(page.contains(l.wood(WoodTerm.JOIN_WIDTH)))
            assertTrue(page.contains(l.wood(WoodTerm.LOG_DEPTH)))
            assertFalse(page.contains(l.wood(WoodTerm.HALF_T)))
            assertTrue(text(boards()).contains(l.dimension(50.5)))
        }
    }

    @Test fun professionalTitlesPreserveNamesAndDescribeStockOnBothPages() {
        val named=plan(boards())
        for(l in WorkshopLanguage.entries) {
            val blank=named.copy(name="")
            val d=WorkshopDocument.create(blank,l,Music.frequency(37))
            val title="Didgeridoo "+WorkshopDocument.note(Music.frequency(37),l)
            for(page in d.pages) {
                val labels=page.marks.filterIsInstance<WorkshopMark.Text>().map {it.value}
                assertTrue(labels.contains(title))
                assertTrue(labels.contains(if(l==WorkshopLanguage.SPANISH)"Madera partida · Dos tablas" else "Split wood · Two boards"))
            }
            assertTrue(WorkshopDocument.create(named,l).text().contains("Río claro"))
        }
        assertEquals(listOf(ConstructionMethod.SANDWICH,ConstructionMethod.PVC),ConstructionForm.activeMethods())
        assertEquals(ConstructionMethod.LAMINATED,ConstructionForm.options(ConstructionForm.defaults(shape)+("method" to "LAMINATED")).method)
    }

}
