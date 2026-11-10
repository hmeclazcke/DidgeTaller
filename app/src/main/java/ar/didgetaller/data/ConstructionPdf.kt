package ar.didgetaller.data

import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import ar.didgetaller.core.*
import java.io.ByteArrayOutputStream

/** A4 overview/tables plus optional overlapping, dimensionally scaled inner-profile tiles. */
object ConstructionPdf {
    data class Labels(val title:String,val overview:String,val ruler:String,val limitations:String,val stations:String,val parts:String,val instructions:String,val method:String="")
    fun create(plan:ConstructionPlan,labels:Labels,tiled:Boolean):ByteArray {
        val doc=PdfDocument()
        try {
            val simulation=ConstructionProjects.predict(plan)
            overview(doc,plan,labels,simulation)
            tablePages(doc,plan,labels)
            if(tiled)tilePages(doc,plan,labels)
            val output=ByteArrayOutputStream()
            doc.writeTo(output)
            return output.toByteArray()
        } finally {doc.close()}
    }
    private fun page(doc:PdfDocument):PdfDocument.Page = doc.startPage(PdfDocument.PageInfo.Builder(595,842,doc.pages.size+1).create())
    private fun ink(size:Float=10f)=Paint(Paint.ANTI_ALIAS_FLAG).apply {textSize=size;color=android.graphics.Color.BLACK}
    private fun f(x:Double)=ConstructionDrawing.number(x)
    private fun overview(doc:PdfDocument,p:ConstructionPlan,l:Labels,s:AcousticModel.Simulation) {
        val page=page(doc);val c=page.canvas
        c.drawText(l.title,40f,40f,ink(16f));c.drawText(p.name.take(60),40f,62f,ink())
        c.drawText("${l.method} | L=${f(p.lengthMm)} mm | ID=${f(p.pieces.first().inletMm)} / ${f(p.pieces.last().outletMm)} mm",40f,84f,ink())
        c.drawText("Hz: "+s.frequenciesHz.take(5).joinToString(" / "){f(it)},40f,106f,ink())
        val scale=500f/p.lengthMm.toFloat()
        c.save();c.translate(40f,320f);c.scale(scale,scale);drawProfile(c,p,.65f/scale);c.restore()
        lengthDimension(c,p)
        c.drawText(l.overview+" 1:${f(ConstructionDrawing.POINTS_PER_MM/scale)}",40f,480f,ink())
        wrap(l.limitations).forEachIndexed {i,line->c.drawText(line,40f,520f+i*15,ink())}
        crossSection(c,p)
        ruler(c,l.ruler);doc.finishPage(page)
    }
    private fun lengthDimension(c:android.graphics.Canvas,p:ConstructionPlan) {
        val paint=ink()
        c.drawLine(40f,456f,540f,456f,paint)
        c.drawLine(40f,452f,40f,460f,paint);c.drawLine(540f,452f,540f,460f,paint)
        c.drawText("L=${f(p.lengthMm)} mm",40f,448f,paint)
    }
    private fun drawProfile(c:android.graphics.Canvas,p:ConstructionPlan,strokeMm:Float=.35f) {
        val paint=ink().apply {style=Paint.Style.STROKE;strokeWidth=strokeMm}
        drawOutline(c,p,paint,-1)
        drawOutline(c,p,paint,1)
        c.drawLine(0f,0f,p.lengthMm.toFloat(),0f,paint)
    }
    private fun drawOutline(c:android.graphics.Canvas,p:ConstructionPlan,paint:Paint,sign:Int) {
        val points=ConstructionDrawing.outline(p,sign)
        val path=Path()
        path.moveTo(points.first().first.toFloat(),points.first().second.toFloat())
        points.drop(1).forEach {(x,y)->path.lineTo(x.toFloat(),y.toFloat())}
        c.drawPath(path,paint)
    }
    private fun crossSection(c:android.graphics.Canvas,p:ConstructionPlan) {
        if(p.options.method!=ConstructionMethod.SANDWICH)return
        val stock=WorkshopPlans.sandwich(p.ideal,p.options.intervalMm,p.options.wallMm,p.options.radialToleranceMm)
        val radius=p.pieces.maxOf {maxOf(it.inletMm,it.outletMm)}/2
        c.drawText("A / B: r=${f(radius)}; W=${f(stock.minimumBlankWidthMm)}; H=${f(stock.minimumHalfThicknessMm)} mm",40f,670f,ink())
        drawHalf(c,150f,730f,stock,radius,1f)
        drawHalf(c,430f,730f,stock,radius,-1f)
    }
    private fun drawHalf(c:android.graphics.Canvas,x:Float,y:Float,stock:WorkshopPlans.SplitPlan,radius:Double,sign:Float) {
        val scale=minOf(180/stock.minimumBlankWidthMm,45/stock.minimumHalfThicknessMm).toFloat()
        val paint=ink().apply {style=Paint.Style.STROKE;strokeWidth=1/scale}
        c.save();c.translate(x,y);c.scale(scale,sign*scale)
        val w=stock.minimumBlankWidthMm.toFloat()/2;val h=stock.minimumHalfThicknessMm.toFloat();val r=radius.toFloat()
        c.drawRect(-w,-h,w,0f,paint)
        c.drawArc(-r,-r,r,r,180f,180f,false,paint)
        c.restore()
    }
    private fun tablePages(doc:PdfDocument,p:ConstructionPlan,l:Labels) {
        val lines=listOf(p.name,l.stations,"x mm | ID ideal | ID (-) / (+) | r (-) / (+)")+
            ConstructionDrawing.table(p)+listOf(l.parts)+p.parts+listOf(l.instructions,l.limitations)
        lines.flatMap {wrap(it)}.chunked(38).forEach {part->table(doc,p,l,part)}
    }
    private fun tilePages(doc:PdfDocument,p:ConstructionPlan,l:Labels) {
        ConstructionDrawing.tileStarts(p.lengthMm).forEach {start->tile(doc,p,l,start)}
    }
    private fun table(doc:PdfDocument,p:ConstructionPlan,l:Labels,lines:List<String>) {
        val page=page(doc)
        page.canvas.drawText(p.name.take(60),40f,35f,ink(13f))
        lines.forEachIndexed {i,line->page.canvas.drawText(line,40f,65f+i*17,ink())}
        ruler(page.canvas,l.ruler);doc.finishPage(page)
    }
    private fun tile(doc:PdfDocument,p:ConstructionPlan,l:Labels,start:Double) {
        val page=page(doc);val c=page.canvas;val factor=ConstructionDrawing.POINTS_PER_MM.toFloat()
        c.drawText("1:1 | x=${f(start)}-${f(minOf(start+160,p.lengthMm))} mm",40f,40f,ink())
        c.save();c.clipRect(40f,80f,40+160*factor,760f);c.translate(40-start.toFloat()*factor,421f);c.scale(factor,factor);drawProfile(c,p);c.restore()
        registration(c,40f,start)
        if(start+150<p.lengthMm)registration(c,40+150*factor,start+150)
        ruler(c,l.ruler);doc.finishPage(page)
    }
    private fun registration(c:android.graphics.Canvas,point:Float,xMm:Double) {
        c.drawLine(point,80f,point,760f,ink().apply {strokeWidth=.5f})
        c.drawText("x=${f(xMm)}",point,70f,ink())
    }
    private fun ruler(c:android.graphics.Canvas,label:String) {
        val end=(40+100*ConstructionDrawing.POINTS_PER_MM).toFloat()
        c.drawLine(40f,785f,end,785f,ink().apply {strokeWidth=1f})
        c.drawLine(40f,781f,40f,789f,ink());c.drawLine(end,781f,end,789f,ink())
        wrap(label).forEachIndexed {i,line->c.drawText(line,40f,807f+i*12,ink(9f))}
    }
    private fun wrap(text:String):List<String> {
        val layout=StaticLayout.Builder.obtain(text,0,text.length,TextPaint(ink()),510).setIncludePad(false).build()
        return List(layout.lineCount){i->text.substring(layout.getLineStart(i),layout.getLineEnd(i)).trimEnd()}
    }
}
