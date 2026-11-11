package ar.didgetaller.core

import java.text.Normalizer
import java.util.Locale

interface WorkshopPainter {
    fun text(mark:WorkshopMark.Text)
    fun stroke(points:List<Pair<Double,Double>>,dashed:Boolean)
    fun shade(points:List<Pair<Double,Double>>)
}

sealed interface WorkshopMark {
    fun paint(painter:WorkshopPainter)
    data class Text(val x:Double,val y:Double,val size:Double,val value:String):WorkshopMark {
        override fun paint(painter:WorkshopPainter)=painter.text(this)
    }
    data class Path(val points:List<Pair<Double,Double>>,val closed:Boolean=false,val shaded:Boolean=false,val dashed:Boolean=false):WorkshopMark {
        override fun paint(painter:WorkshopPainter) {
            val outline=if(closed)points+points.first() else points
            if(shaded)painter.shade(outline)
            painter.stroke(outline,dashed)
        }
    }
}
data class WorkshopSheet(val width:Double,val height:Double,val marks:List<WorkshopMark>)
data class WorkshopDocument(val name:String,val language:WorkshopLanguage,val measurements:List<ConstructionDrawing.Row>,val pages:List<WorkshopSheet>,val lengthMm:Double) {
    fun text()=pages.flatMap {it.marks}.filterIsInstance<WorkshopMark.Text>().joinToString("\n"){it.value}
    companion object {
        fun create(plan:ConstructionPlan,language:WorkshopLanguage,targetHz:Double?=null):WorkshopDocument {
            require(targetHz==null || targetHz.isFinite() && targetHz>0)
            val rows=ConstructionDrawing.rows(plan)
            return WorkshopDocument(humanName(plan.name),language,rows,
                listOf(WorkshopSpecifications.page(plan,language,targetHz),WorkshopDiagram.page(plan,language)),plan.lengthMm)
        }
        fun humanName(raw:String):String {
            val clean=raw.replace(Regex("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\s*/?"),"").trim()
            return clean.ifBlank {"Didgeridoo"}.take(65)
        }
        fun number(value:Double)=String.format(Locale.ROOT,"%.1f",value)
        fun minimum(value:Double)=number(kotlin.math.ceil(value*10)/10)
        fun method(p:ConstructionPlan)=when(p.options.method) {
            ConstructionMethod.SANDWICH->WorkshopTerm.SANDWICH
            ConstructionMethod.LAMINATED->WorkshopTerm.LAMINATED
            ConstructionMethod.PVC->WorkshopTerm.PVC
        }
        fun note(hz:Double,l:WorkshopLanguage):String {
            val pitch=Music.pitch(hz) ?: return "—"
            if(l==WorkshopLanguage.ENGLISH)return pitch.note
            val names=listOf("Do","Do sostenido","Re","Mi bemol","Mi","Fa","Fa sostenido","Sol","Sol sostenido","La","Si bemol","Si")
            return names[Math.floorMod(pitch.midi,12)]+(Math.floorDiv(pitch.midi,12)-1)
        }
        fun filename(p:ConstructionPlan,format:String,l:WorkshopLanguage,token:String):String {
            require(token.matches(Regex("[A-Za-z0-9-]{1,50}")))
            val name=Normalizer.normalize(humanName(p.name),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
                .replace(Regex("[^A-Za-z0-9_-]+"),"_").trim('_').take(60).ifBlank {"Didgeridoo"}
            val suffix=mapOf("pdf" to "${l.word(method(p))}","tiles" to l.word(WorkshopTerm.TEMPLATE),"csv" to l.word(WorkshopTerm.MEASUREMENTS),"svg" to l.word(WorkshopTerm.DRAWING)).getValue(format)
            val safe=Normalizer.normalize(suffix,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").replace(Regex("[^A-Za-z0-9]+"),"_").trim('_')
            return "${name}_${safe}_$token.${if(format=="tiles")"pdf" else format}"
        }
        fun svg(p:ConstructionPlan,l:WorkshopLanguage)=WorkshopSvg.render(WorkshopDiagram.page(p,l),humanName(p.name),p.lengthMm)
        fun csv(p:ConstructionPlan,l:WorkshopLanguage)=WorkshopTables.csv(p,l)
        fun svg(d:WorkshopDocument)=WorkshopSvg.render(d.pages[1],d.name,d.lengthMm)
        fun sensitivity(raw:String,l:WorkshopLanguage):String = when(raw) {
            "ID +1 mm"->l.word(WorkshopTerm.DIAMETER_PLUS)
            "ID -1 mm"->l.word(WorkshopTerm.DIAMETER_MINUS)
            "L +5 mm"->l.word(WorkshopTerm.LENGTH_PLUS)
            "L -5 mm"->l.word(WorkshopTerm.LENGTH_MINUS)
            "ID bell +1 mm"->l.word(WorkshopTerm.BELL_CHANGE)
            "I -1 mm"->l.word(WorkshopTerm.INSERTION_CHANGE)
            else->"${l.word(WorkshopTerm.LOCAL_SECTION)} $raw"
        }
    }
}

/** Drawing coordinates are PDF points; geometry and annotations share the same transform in SVG. */
internal class WorkshopPage(val width:Double,val height:Double) {
    val marks=mutableListOf<WorkshopMark>()
    fun text(x:Double,y:Double,value:String,size:Double=10.0) {marks+=WorkshopMark.Text(x,y,size,value)}
    fun line(x:Double,y:Double,endX:Double,endY:Double,dashed:Boolean=false) {marks+=WorkshopMark.Path(listOf(x to y,endX to endY),dashed=dashed)}
    fun path(points:List<Pair<Double,Double>>,closed:Boolean=false,shaded:Boolean=false) {marks+=WorkshopMark.Path(points,closed,shaded)}
    fun paragraph(x:Double,y:Double,value:String,width:Double,size:Double=9.0):Double {
        val limit=(width/(size*.56)).toInt().coerceAtLeast(12)
        var line="";var cursor=y
        value.split(' ').forEach {word->
            if(line.isNotEmpty() && line.length+word.length+1>limit) {text(x,cursor,line,size);cursor+=size+3;line=""}
            line=if(line.isEmpty())word else "$line $word"
        }
        text(x,cursor,line,size);return cursor+size+3
    }
    fun dimension(x:Double,y:Double,endX:Double,endY:Double,label:String) {
        line(x,y,endX,endY);line(x-3,y-3,x+3,y+3);line(endX-3,endY-3,endX+3,endY+3)
        text((x+endX)/2+3,(y+endY)/2-5,label,9.0)
    }
    fun ruler(l:WorkshopLanguage) {
        val y=height-45;dimension(34.0,y,34+100*ConstructionDrawing.POINTS_PER_MM,y,"100 mm")
        text(34.0,height-22,l.word(WorkshopTerm.RULER),9.0)
    }
    fun sheet()=WorkshopSheet(width,height,marks.toList())
}
