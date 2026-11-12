package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class WorkshopDocumentTest {
    private fun wood()=ConstructionProjects.create("123e4567-e89b-12d3-a456-426614174000 / Workshop example",
        AcousticModel.Shape(mouthMm=30,bellMm=83),ConstructionOptions())
    @Test fun documentUsesHumanNameAndTwoSheetsWithTrueHollowDepths() {
        val d=WorkshopDocument.create(wood(),WorkshopLanguage.ENGLISH,69.3)
        assertEquals("Workshop example",d.name)
        assertEquals(2,d.pages.size)
        assertEquals(842.0,d.pages[0].width,0.0)
        assertEquals(842.0,d.pages[1].width,0.0)
        assertEquals(1500.0,d.measurements.last().xMm,0.0)
        assertEquals(15.0,d.measurements.first().beforeMm/2,0.0)
        assertEquals(41.5,d.measurements.last().afterMm/2,0.0)
        assertTrue(d.text().contains("Groove depth per half"))
        assertFalse(d.text().contains("123e4567"))
        assertTrue(d.text().contains("83 mm"))
        assertFalse(d.text().contains("83.000"))
    }
    @Test fun localizedDrawingAndCsvPreserveMeasurementsAndExplicitUnits() {
        val p=wood()
        val d=WorkshopDocument.create(p,WorkshopLanguage.SPANISH)
        assertTrue(d.text().lowercase().contains("plano de unión"))
        assertFalse(d.text().contains("Hollowing"))
        val csv=WorkshopDocument.csv(p,WorkshopLanguage.SPANISH)
        assertTrue(csv.contains("Profundidad por mitad (mm)"))
        assertTrue(csv.contains("41.5"))
        val svg=WorkshopDocument.svg(d)
        assertTrue(svg.contains("842.0pt"))
        assertTrue(svg.contains("data-length-mm=\"1500.0\""))
        assertTrue(svg.contains("100 mm"))
    }
    @Test fun filenamesAreSafeNamedAndDistinguishTemplates() {
        val p=wood().copy(name="../ A: \"test\" / C#2")
        val a=WorkshopDocument.filename(p,"pdf",WorkshopLanguage.ENGLISH,"test-token")
        assertFalse(a.contains(".."));assertFalse(a.contains('/'));assertFalse(a.contains(':'))
        assertTrue(a.endsWith(".pdf"))
        assertNotEquals(a,WorkshopDocument.filename(p,"tiles",WorkshopLanguage.ENGLISH,"test-token"))
    }
    @Test fun bothLanguagesAndAllMethodsHaveBoundedLegibleDrawingCoordinates() {
        val pvc=ConstructionOptions(method=ConstructionMethod.PVC,pipes="a,30,40,1500\nb,70,80,1500",
            fittings="c,40.2,80.2,30,25,90,30,0:30|35:70",assembly="a,800,c\nb,650,-")
        for(options in listOf(ConstructionOptions(),ConstructionOptions(method=ConstructionMethod.LAMINATED),pvc)) {
            val plan=ConstructionProjects.create("Synthetic & <bore>",wood().ideal,options)
            for(l in WorkshopLanguage.entries) {
                val d=WorkshopDocument.create(plan,l,69.3)
                val svg=WorkshopDocument.svg(d)
                assertTrue(svg.contains("Synthetic &amp; &lt;bore&gt;"))
                javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg.byteInputStream())
                for(page in d.pages)for(mark in page.marks)when(mark) {
                    is WorkshopMark.Text->{assertTrue(mark.x>=0 && mark.x<page.width);assertTrue(mark.y>0 && mark.y<page.height);assertTrue(mark.size>=8)}
                    is WorkshopMark.Path->mark.points.forEach {(x,y)->assertTrue(x>=0 && x<=page.width);assertTrue(y>=0 && y<=page.height)}
                }
                assertTrue(WorkshopDocument.csv(plan,l).contains(WorkshopDocument.number(plan.lengthMm)))
                for(f in listOf("pdf","tiles","csv","svg"))assertTrue(WorkshopDocument.filename(plan,f,l,"abc123").contains("abc123"))
            }
        }
    }
    @Test fun dimensionsRemainFullPrecisionAndDenseReportsFailInsteadOfShrinkingText() {
        val original=wood().ideal.copy(lengthMm=3000,mouthMm=15,bellMm=200)
        val plan=ConstructionProjects.create("Long",original,ConstructionOptions(intervalMm=50))
        // Dense station data belongs to CSV; paper stays readable at exactly two sheets.
        assertEquals(2,WorkshopDocument.create(plan,WorkshopLanguage.ENGLISH).pages.size)
        assertTrue(WorkshopDocument.csv(plan,WorkshopLanguage.ENGLISH).contains("3000.000000"))
        val invalid=listOf(Double.NaN,-1.0,0.0)
        for(wall in invalid)try {ConstructionProjects.create("bad",original,ConstructionOptions(wallMm=wall));fail("Invalid material")}catch(_:IllegalArgumentException){}
        assertEquals("Didgeridoo",WorkshopDocument.humanName("123e4567-e89b-12d3-a456-426614174000"))
    }
    @Test fun sensitivityTermsAreLocalizedAndNoVocabularyEntryIsMissing() {
        for(term in WorkshopTerm.entries) {assertTrue(term.en.isNotBlank());assertTrue(term.es.isNotBlank());assertFalse(term.en.contains("?"));assertFalse(term.es.contains("?"))}
        assertEquals("Plano de construcción en taller",WorkshopTerm.TITLE.es)
        for(raw in listOf("ID +1 mm","ID -1 mm","L +5 mm","L -5 mm","ID bell +1 mm","I -1 mm","2")) {
            assertFalse(WorkshopDocument.sensitivity(raw,WorkshopLanguage.SPANISH).contains("bell"))
            assertNotEquals(raw,WorkshopDocument.sensitivity(raw,WorkshopLanguage.ENGLISH))
        }
        assertEquals("Do♯2",WorkshopDocument.note(Music.frequency(37),WorkshopLanguage.SPANISH))
        assertEquals("C♯2",WorkshopDocument.note(Music.frequency(37),WorkshopLanguage.ENGLISH))
        assertEquals("—",WorkshopDocument.note(0.0,WorkshopLanguage.SPANISH))
        try {WorkshopDocument.filename(wood(),"pdf",WorkshopLanguage.ENGLISH,"../");fail("Unsafe token")}catch(_:IllegalArgumentException){}
    }
    @Test fun templateAndPainterPreserveClosedCavitiesAndExactCalibration() {
        val p=wood()
        val templates=WorkshopTemplates.create(p,WorkshopLanguage.SPANISH)
        assertEquals(10,templates.size)
        val first=templates.first();val last=templates.last()
        assertTrue(first.marks.filterIsInstance<WorkshopMark.Text>().any {it.value.contains("1 / 10")})
        assertTrue(last.marks.filterIsInstance<WorkshopMark.Text>().any {it.value.contains("1350.0–1500.0")})
        val events=mutableListOf<String>()
        val painter=object:WorkshopPainter {
            override fun text(mark:WorkshopMark.Text){events+="text"}
            override fun stroke(points:List<Pair<Double,Double>>,dashed:Boolean,color:Int,width:Double){events+="stroke:$dashed:${points.first()==points.last()}"}
            override fun shade(points:List<Pair<Double,Double>>,color:Int){assertEquals(points.first(),points.last());events+="shade"}
        }
        WorkshopDocument.create(p,WorkshopLanguage.ENGLISH).pages.flatMap {it.marks}.forEach {it.paint(painter)}
        assertTrue(events.contains("shade"));assertTrue(events.contains("stroke:true:false"));assertTrue(events.contains("stroke:false:true"));assertTrue(events.contains("text"))
        for(sheet in templates) {
            val rule=sheet.marks.filterIsInstance<WorkshopMark.Path>().single {it.points.first()==34.0 to 797.0 && it.points.last().first>300}
            assertEquals(100.0,(rule.points.last().first-34)/ConstructionDrawing.POINTS_PER_MM,1e-9)
        }
    }
    @Test fun shortWideBoreAndMinimumStockRoundingDoNotDistortOrOverflow() {
        val shape=AcousticModel.Shape(lengthMm=500,points=listOf(AcousticModel.BorePoint(0,15.0),AcousticModel.BorePoint(500,220.0)))
        val p=ConstructionProjects.create("Short wide",shape,ConstructionOptions())
        val d=WorkshopDocument.create(p,WorkshopLanguage.ENGLISH)
        val points=d.pages[1].marks.filterIsInstance<WorkshopMark.Path>().flatMap {it.points}
        assertTrue(points.all {(x,y)->x>=0 && x<=842 && y>=0 && y<=595})
        val measure=d.pages[0].dimensions.first {it.kind=="overall-length"}
        assertEquals(500.0,measure.valueMm,0.0)
        assertEquals(500.0*measure.scale,measure.end.first-measure.start.first,1e-9)
        val cavity=d.pages[0].marks.filterIsInstance<WorkshopMark.Path>().first {it.shaded}
        assertEquals(62+500*measure.scale,cavity.points[2].first,1e-9)
        assertEquals(260+110*measure.scale,cavity.points[2].second,1e-9)
        val peaked=AcousticModel.Shape(lengthMm=500,points=listOf(AcousticModel.BorePoint(0,15.0),AcousticModel.BorePoint(250,220.0),AcousticModel.BorePoint(500,15.0)))
        val layers=ConstructionProjects.create("Wide middle",peaked,ConstructionOptions(method=ConstructionMethod.LAMINATED,layers="50"))
        assertTrue(WorkshopDocument.create(layers,WorkshopLanguage.ENGLISH).text().contains("1:8.0"))
        assertEquals("50.6",WorkshopDocument.minimum(50.51))
        assertEquals("'",WorkshopDocument.csv(p.copy(name="-formula"),WorkshopLanguage.ENGLISH).substringAfter("\"Workshop construction plan\",\"").take(1))
        for(l in WorkshopLanguage.entries)for(page in WorkshopDocument.create(p,l).pages)assertFalse(page.marks.filterIsInstance<WorkshopMark.Text>().any {it.value.contains('?')})
    }
    @Test fun independentWoodCuttingDimensionsMatchLongitudinalAndSemicircularViews() {
        val shape=AcousticModel.Shape(points=listOf(AcousticModel.BorePoint(0,30.0),AcousticModel.BorePoint(1500,83.0)))
        val p=ConstructionProjects.create("Workshop",shape,ConstructionOptions())
        val d=WorkshopDocument.create(p,WorkshopLanguage.ENGLISH)
        val csv=WorkshopDocument.csv(p,WorkshopLanguage.ENGLISH)
        assertTrue(csv.contains("\"0.0 *\",\"30.0\",\"15.0\",\"48.0\",\"24.0\""))
        assertTrue(csv.contains("\"1500.0 *\",\"83.0\",\"41.5\",\"101.0\",\"50.5\""))
        val circles=d.pages[1].marks.filterIsInstance<WorkshopMark.Path>().filter {it.shaded}
        val diameters=d.pages[1].dimensions.filter {it.kind=="bore-diameter"}
        val depths=d.pages[1].dimensions.filter {it.kind=="groove-depth"}
        assertEquals(3,circles.size)
        for(i in circles.indices) {
            val diameter=diameters[i];val centerX=(diameter.start.first+diameter.end.first)/2;val centerY=depths[i].start.second
            circles[i].points.forEach {(x,y)->assertEquals(diameter.valueMm/2*diameter.scale,kotlin.math.hypot(x-centerX,y-centerY),1e-9)}
            assertEquals(diameter.valueMm/2,depths[i].valueMm,0.0)
        }
        assertEquals(30.0,diameters.first().valueMm,0.0)
        assertEquals(83.0,diameters.last().valueMm,0.0)

    }
    @Test fun measuredPartsAndLaminatedRowsPreserveActualWorkshopReferences() {
        val shape=AcousticModel.Shape(points=listOf(AcousticModel.BorePoint(0,30.0),AcousticModel.BorePoint(1500,83.0)))
        val laminated=ConstructionProjects.create("Layers",shape,ConstructionOptions(method=ConstructionMethod.LAMINATED,layers="100"))
        val rows=WorkshopTables.layers(laminated,WorkshopLanguage.ENGLISH).rows
        assertEquals(15,rows.size)
        assertEquals(listOf("1","0.0-100.0","100.0","33.5","51.6"),rows.first())
        assertEquals(listOf("15","1400.0-1500.0","100.0","83.0","101.0"),rows.last())
        val pvc=ConstructionProjects.create("PVC",shape,ConstructionOptions(method=ConstructionMethod.PVC,pipes="a,30,40,1500\nb,70,80,1500",fittings="c,40.2,80.2,30,25,90,30,0:30|35:70",assembly="a,800,c\nb,650,-"))
        assertEquals(1485.0,pvc.lengthMm,0.0)
        val parts=WorkshopTables.pvc(pvc,WorkshopLanguage.ENGLISH).rows
        assertEquals(listOf("1. Tube a @0.0","800","30.0","40.0","—"),parts[0])
        assertEquals(listOf("Connector c @770.0","90","30.0 / 70.0","40.2 / 80.2","30 / 25"),parts[1])
        assertEquals(listOf("2. Tube b @835.0","650","70.0","80.0","—"),parts[2])
        assertTrue(WorkshopDocument.svg(pvc,WorkshopLanguage.ENGLISH).contains("1485.0"))
        val marks=WorkshopDocument.create(pvc,WorkshopLanguage.ENGLISH).pages[1].marks.filterIsInstance<WorkshopMark.Path>()
        val scale=ConstructionDrawing.POINTS_PER_MM/6
        val upper=marks.single {it.points.size==6 && it.points.first()==34.0 to 220-15*scale}
        assertEquals(listOf(34.0 to 220-15*scale,34+800*scale to 220-15*scale,34+800*scale to 220-15*scale,34+835*scale to 220-35*scale,34+835*scale to 220-35*scale,34+1485*scale to 220-35*scale),upper.points)
        val sockets=marks.filter {it.closed && it.dashed}
        assertEquals(2,sockets.size)
        for((index,reference) in listOf(Triple(770.0,30.0,40.2),Triple(835.0,25.0,80.2)).withIndex()) {
            val points=sockets[index].points
            assertEquals(34+reference.first*scale,points.first().first,1e-9)
            assertEquals(reference.second*scale,points[1].first-points[0].first,1e-9)
            assertEquals(reference.third*scale,points[3].second-points[0].second,1e-9)
        }
    }
    @Test fun fullSizeTemplateContoursAndRegistrationUseTrueMillimetresOnEverySheet() {
        val shape=AcousticModel.Shape(points=listOf(AcousticModel.BorePoint(0,30.0),AcousticModel.BorePoint(1500,83.0)))
        val p=ConstructionProjects.create("Pattern",shape,ConstructionOptions())
        val factor=ConstructionDrawing.POINTS_PER_MM
        for((index,sheet) in WorkshopTemplates.create(p,WorkshopLanguage.ENGLISH).withIndex()) {
            val paths=sheet.marks.filterIsInstance<WorkshopMark.Path>()
            val profile=paths.filter {it.points.first().first==34.0 && it.points.last().first>400 && it.points.first().second in 120.0..740.0 && it.points.last().second!=430.0}
            assertEquals(2,profile.size)
            for((sign,path) in listOf(-1,1).zip(profile))for((x,y) in path.points) {
                assertTrue(x>=34 && x<=34+160*factor)
                val position=index*150+(x-34)/factor
                val radius=(30+53*position/1500)/2
                assertEquals(430+sign*radius*factor,y,1e-8)
            }
            assertTrue(paths.any {it.points==listOf(34.0 to 115.0,34.0 to 710.0)})
            if(index<9)assertTrue(paths.any {it.points==listOf(34+150*factor to 115.0,34+150*factor to 710.0)})
        }
        val curved=wood()
        for(sheet in WorkshopTemplates.create(curved,WorkshopLanguage.ENGLISH)) {
            val contour=sheet.marks.filterIsInstance<WorkshopMark.Path>().filter {path->path.points.size>2 && path.points.first().second in 120.0..740.0}
            assertTrue(contour.flatMap {it.points}.all {(x,_)->x>=34 && x<=34+160*factor})
        }
        val bend=WorkshopTemplates.create(curved,WorkshopLanguage.ENGLISH)[6].marks.filterIsInstance<WorkshopMark.Path>().flatMap {it.points}
        assertTrue(bend.any {(x,y)->kotlin.math.abs(x-(34+120*factor))<1e-9 && kotlin.math.abs(y-(430-21.5*factor))<1e-9})
        assertTrue(WorkshopDocument.svg(p.copy(options=p.options.copy(intervalMm=50)),WorkshopLanguage.ENGLISH).contains("data-length-mm"))
    }
}
