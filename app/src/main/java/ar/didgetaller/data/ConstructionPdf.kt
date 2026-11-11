package ar.didgetaller.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import ar.didgetaller.core.*
import java.io.ByteArrayOutputStream

/** Android owns page resources only; all geometry and drawing decisions are pure and shared with SVG. */
object ConstructionPdf {
    data class Labels(val language:WorkshopLanguage=WorkshopLanguage.ENGLISH,val targetHz:Double?=null)
    fun create(plan:ConstructionPlan,labels:Labels,tiled:Boolean):ByteArray = write(pages(plan,labels,tiled))
    private fun pages(plan:ConstructionPlan,labels:Labels,tiled:Boolean)=
        if(tiled)WorkshopTemplates.create(plan,labels.language) else WorkshopDocument.create(plan,labels.language,labels.targetHz).pages
    private fun write(sheets:List<WorkshopSheet>):ByteArray {
        val doc=PdfDocument();val output=ByteArrayOutputStream()
        try {
            sheets.forEachIndexed {index,sheet->page(doc,sheet,index+1)}
            doc.writeTo(output);return output.toByteArray()
        } finally {doc.close()}
    }
    private fun page(doc:PdfDocument,sheet:WorkshopSheet,index:Int) {
        val page=doc.startPage(PdfDocument.PageInfo.Builder(sheet.width.toInt(),sheet.height.toInt(),index).create())
        val painter=PdfWorkshopPainter(page.canvas)
        sheet.marks.forEach {it.paint(painter)}
        doc.finishPage(page)
    }
}

private class PdfWorkshopPainter(private val canvas:Canvas):WorkshopPainter {
    private fun ink()=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.BLACK;strokeWidth=.6f}
    override fun text(mark:WorkshopMark.Text) {
        val ink=ink();ink.textSize=mark.size.toFloat()
        canvas.drawText(mark.value,mark.x.toFloat(),mark.y.toFloat(),ink)
    }
    override fun shade(points:List<Pair<Double,Double>>) {
        val ink=ink();ink.color=Color.LTGRAY
        canvas.drawPath(path(points),ink)
    }
    override fun stroke(points:List<Pair<Double,Double>>,dashed:Boolean) {
        val ink=ink();ink.style=Paint.Style.STROKE
        if(dashed)ink.pathEffect=DashPathEffect(floatArrayOf(4f,3f),0f)
        canvas.drawPath(path(points),ink)
    }
    private fun path(points:List<Pair<Double,Double>>):Path {
        val path=Path()
        path.moveTo(points.first().first.toFloat(),points.first().second.toFloat())
        points.drop(1).forEach {(x,y)->path.lineTo(x.toFloat(),y.toFloat())}
        return path
    }
}
