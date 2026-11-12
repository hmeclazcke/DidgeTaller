package ar.didgetaller.core

import java.io.StringReader
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import org.junit.Assert.*
import org.junit.Test

class WorkshopExportsTest {
    @Test fun csvUsesMetricDecimalPointsRegardlessOfDeviceLocale() {
        val previous=Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val csv=WorkshopExports.measurementsCsv(AcousticModel.Shape(),100)
            val rows=csv.trim().lines()
            assertEquals("0,32.000,16.000",rows[1])
            assertEquals("1500,85.000,42.500",rows.last())
            assertTrue(rows.any {it.startsWith("1020,")})
            assertTrue(rows.all {it.split(',').size==3})
        } finally {Locale.setDefault(previous)}
    }
    @Test fun svgIsValidEscapedMetricGeometryWithAnIndependentCalibrationReference() {
        val svg=WorkshopExports.sandwichSvg("A&B <bore> \"test\" 'wood'",AcousticModel.Shape(),100,8.0,1.0)
        val root=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(svg))).documentElement
        assertEquals("1580mm",root.getAttribute("width"))
        assertEquals("A&B <bore> \"test\" 'wood'",root.getElementsByTagName("title").item(0).textContent)
        val text=root.getElementsByTagName("text")
        val stations=(0 until text.length).map {text.item(it) as org.w3c.dom.Element}.filter {it.hasAttribute("data-position-mm")}
        assertEquals(17,stations.size)
        assertEquals("85.000",stations.last().getAttribute("data-inner-diameter-mm"))
        assertTrue(svg.contains("h100"))
        assertTrue(svg.contains("do not assume print scaling"))
        assertEquals(2,root.getElementsByTagName("polyline").length)
        assertEquals("263.000mm",root.getAttribute("height"))
        val paths=root.getElementsByTagName("polyline")
        for(side in 0..1) {
            val points=(paths.item(side) as org.w3c.dom.Element).getAttribute("points").split(' ')
            assertEquals(stations.size,points.size)
            points.forEachIndexed {i,point->
                val xy=point.split(',').map {it.toDouble()}
                val station=stations[i].getAttribute("data-position-mm").toInt()
                assertEquals(station+30.0,xy[0],0.0)
                val radius=AcousticModel.Shape().diameter(station)/2
                assertEquals(86.5+if(side==0)-radius else radius,xy[1],.00051)
            }
        }
        val line=root.getElementsByTagName("line").item(0) as org.w3c.dom.Element
        assertEquals("1530",line.getAttribute("x2"))
        assertEquals("86.500",line.getAttribute("y1"))
        assertEquals("M30 238.000 h100 m-100 -3 v6 m100 -6 v6",(root.getElementsByTagName("path").item(0) as org.w3c.dom.Element).getAttribute("d"))
        assertTrue(stations.all {it.getAttribute("y")=="150.000"})
    }
}
