package ar.didgetaller.core

internal object WorkshopSvg {
    private fun xml(value:String)=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    private fun n(v:Double)=WorkshopDocument.number(v)
    private fun c(v:Double)=java.lang.String.format(java.util.Locale.ROOT,"%.6f",v)
    fun render(sheet:WorkshopSheet,name:String,length:Double):String = buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${n(sheet.width)}pt\" height=\"${n(sheet.height)}pt\" viewBox=\"0 0 ${n(sheet.width)} ${n(sheet.height)}\" data-length-mm=\"${n(length)}\"><title>${xml(name)}</title>")
        sheet.marks.forEach {append(mark(it))};append("</svg>")
    }
    private fun mark(m:WorkshopMark):String = when(m) {
        is WorkshopMark.Text->"<text x=\"${c(m.x)}\" y=\"${c(m.y)}\" font-size=\"${n(m.size)}\" font-family=\"sans-serif\">${xml(m.value)}</text>"
        is WorkshopMark.Path->"<polyline points=\"${m.points.joinToString(" "){c(it.first)+","+c(it.second)}}${if(m.closed)" "+c(m.points.first().first)+","+c(m.points.first().second) else ""}\" stroke=\"black\" stroke-width=\"0.6\" fill=\"${if(m.shaded)"#dddddd" else "none"}\" stroke-dasharray=\"${if(m.dashed)"4 3" else "none"}\"/>"
    }
}
