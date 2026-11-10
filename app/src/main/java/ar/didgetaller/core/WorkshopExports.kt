package ar.didgetaller.core

import java.util.Locale

/** Vector coordinates are millimetres. Printers must be checked against the reference ruler. */
object WorkshopExports {
    private fun number(value:Double)=String.format(Locale.ROOT,"%.3f",value)

    fun measurementsCsv(shape:AcousticModel.Shape,intervalMm:Int):String = buildString {
        append("position_mm,ideal_inner_diameter_mm,symmetric_half_depth_mm\n")
        WorkshopPlans.stations(shape,intervalMm).forEach {
            append("${it.positionMm},${number(it.diameterMm)},${number(it.halfDepthMm)}\n")
        }
    }

    fun sandwichSvg(name:String,shape:AcousticModel.Shape,intervalMm:Int,wallMm:Double,toleranceMm:Double):String {
        val plan=WorkshopPlans.sandwich(shape,intervalMm,wallMm,toleranceMm)
        val width=shape.lengthMm+80
        val center=plan.minimumHalfThicknessMm+35
        val height=center*2+90
        val upper=plan.stations.joinToString(" ") {"${it.positionMm+30},${number(center-it.halfDepthMm)}"}
        val lower=plan.stations.joinToString(" ") {"${it.positionMm+30},${number(center+it.halfDepthMm)}"}
        return buildString {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${width}mm\" height=\"${number(height)}mm\" viewBox=\"0 0 $width ${number(height)}\">")
            append("<title>${xml(name)}</title><desc>Two mirrored semicircular halves. All dimensions in mm. Verify the 100 mm ruler after printing.</desc>")
            append("<g fill=\"none\" stroke=\"black\" stroke-width=\"0.3\">")
            append("<line x1=\"30\" y1=\"${number(center)}\" x2=\"${shape.lengthMm+30}\" y2=\"${number(center)}\" stroke-dasharray=\"3 2\"/>")
            append("<polyline points=\"$upper\"/><polyline points=\"$lower\"/>")
            append("<path d=\"M30 ${number(height-25)} h100 m-100 -3 v6 m100 -6 v6\"/></g>")
            append("<g font-family=\"sans-serif\" font-size=\"3\">")
            append("<text x=\"30\" y=\"12\">${xml(name)} — length ${shape.lengthMm} mm</text>")
            append("<text x=\"30\" y=\"18\">Minimum stock width ${number(plan.minimumBlankWidthMm)}; each half thickness ${number(plan.minimumHalfThicknessMm)} mm</text>")
            append("<text x=\"30\" y=\"24\">Minimum wall ${number(wallMm)}; radial overcut allowance ${number(toleranceMm)} mm. Mirror both halves; align station marks.</text>")
            plan.stations.forEach {
                append("<text x=\"${it.positionMm+30}\" y=\"${number(center+plan.minimumHalfThicknessMm+12)}\" data-position-mm=\"${it.positionMm}\" data-inner-diameter-mm=\"${number(it.diameterMm)}\">${it.positionMm}: ${number(it.diameterMm)}</text>")
            }
            append("<text x=\"30\" y=\"${number(height-30)}\">100 mm calibration — do not assume print scaling</text>")
            append("<text x=\"30\" y=\"${number(height-10)}\">Geometric template only. Passive resonance is not a guaranteed played note. Verify material, glue and wall integrity in the workshop.</text></g></svg>")
        }
    }

    private fun xml(text:String)=text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
}
