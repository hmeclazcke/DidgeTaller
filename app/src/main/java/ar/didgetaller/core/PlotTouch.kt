package ar.didgetaller.core

import kotlin.math.abs

enum class PlotTouch { WAIT, SCRUB, VIEWPORT, SCROLL }

/** Classifies movement before gesture ownership is locked; multiple fingers always navigate. */
fun plotTouch(dx:Float,dy:Float,slop:Float,pointers:Int,timeline:Boolean):PlotTouch = when {
    pointers>1 -> PlotTouch.VIEWPORT
    dx*dx+dy*dy<=slop*slop -> PlotTouch.WAIT
    !timeline -> PlotTouch.VIEWPORT
    abs(dx)>abs(dy) -> PlotTouch.SCRUB
    else -> PlotTouch.SCROLL
}
