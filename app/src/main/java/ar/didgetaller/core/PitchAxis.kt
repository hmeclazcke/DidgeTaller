package ar.didgetaller.core

import kotlin.math.*

/** Hz stays linear. Musical ticks are exact note frequencies; numeric fallbacks say ≈. */
object PitchAxis {
    data class Tick(val hz:Double,val note:String?=null,val approximate:Boolean=false)
    fun ticks(window:AxisWindow,maximum:Int):List<Tick> {
        val count=maximum.coerceIn(2,8)
        val notes=(0..127).map {Music.frequency(it)}.filter {it in window.low..window.high}
        val selected=mutableListOf<Double>()
        val gap=window.span/(count-1)
        for(hz in notes)if(selected.isEmpty() || hz-selected.last()>=gap*.9)selected+=hz
        if(selected.size>=2)return selected.take(count).map {Tick(it,Music.pitch(it)?.note)}
        val numeric=PlotScales.ticks(window,count).take(count)
        val labeled=numeric.minByOrNull {abs(it-window.value(.5))}
        return numeric.map {hz->val note=if(hz==labeled)Music.pitch(hz)?.note else null;Tick(hz,note,note!=null)}
    }
}
