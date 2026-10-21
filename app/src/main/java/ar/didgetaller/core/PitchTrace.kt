package ar.didgetaller.core

import kotlin.math.*

/** Presentation only: original samples stay intact. Lines join measured endpoints, not invented samples. */
object PitchTrace {
    enum class Reason(val label:String) {
        LOW_CONFIDENCE("confianza insuficiente"), OUTSIDE_PROFILE("fuera del rango del instrumento"),
        ISOLATED_PERIOD("periodicidad alternativa breve; original disponible"),
        ISOLATED_RETURN("excursión aislada fuera de la tendencia"),
        NOTE_EDGE("estimación aislada en el borde de la señal")
    }
    data class Sample(val point:SessionAnalysis.Point,val previous:Int?,val uncertain:Boolean,
                      val reason:Reason?=null,val bridged:Boolean=false)
    // Restore pre-3.2.3 acceptance; periodicity .70-.85 is not automatically silence.
    const val MIN_CURVE_CONFIDENCE=.70
    private fun cents(a:Double,b:Double)=abs(1200*log2(a/b))
    private fun supported(p:SessionAnalysis.Point,instrument:InstrumentProfile)=instrument.accepts(p) && p.timeMs>=0

    private fun isolatedPeriod(points:List<SessionAnalysis.Point>,i:Int,instrument:InstrumentProfile):Boolean {
        if(i==0 || i==points.lastIndex)return false
        val a=points[i-1];val p=points[i];val b=points[i+1]
        fun context(v:SessionAnalysis.Point)=v.hz!=null && v.hz.isFinite() && v.hz in instrument.validMinHz..instrument.validMaxHz && v.confidence in .65..1.0
        if(!context(a) || !context(b) || p.confidence>=.92)return false
        if(p.timeMs-a.timeMs !in 1..400 || b.timeMs-p.timeMs !in 1..400 || b.timeMs-a.timeMs>600)return false
        if(cents(a.hz!!,b.hz!!)>80)return false
        val middle=sqrt(a.hz*b.hz);val ratio=max(middle/p.hz!!,p.hz/middle)
        val multiple=ratio.roundToInt()
        return multiple in 2..4 && cents(ratio,multiple.toDouble())<=80
    }

    /** Only the drone profile has a dominant register. Never apply this to flute melodies. */
    private fun shortAlternativePeriods(points:List<SessionAnalysis.Point>,instrument:InstrumentProfile):BooleanArray {
        val flags=BooleanArray(points.size)
        if(instrument!=InstrumentProfile.DIDGERIDOO)return flags
        val values=points.filter {supported(it,instrument)}.map {it.hz!!}
        val mode=values.groupBy {Music.midi(it).roundToInt()}.maxByOrNull {it.value.size}?.value ?: return flags
        val center=mode.sorted()[mode.size/2]
        val band=values.filter {cents(it,center)<=160}.sorted()
        if(band.size<6 || band.size<values.size*.55)return flags
        val median=band[band.size/2]
        fun alternative(p:SessionAnalysis.Point):Boolean {
            val hz=p.hz ?: return false
            if(!hz.isFinite() || hz<=0 || p.confidence !in .65..<.90 || hz>=median/1.4)return false
            val ratio=median/hz
            return listOf(1.5,2.0,3.0,4.0).any {cents(ratio,it)<=80}
        }
        var i=0
        while(i<points.size){
            if(!alternative(points[i])){i++;continue}
            val start=i++
            while(i<points.size && alternative(points[i]) && points[i].timeMs-points[i-1].timeMs in 1..400)i++
            if(i-start<=4 && points[i-1].timeMs-points[start].timeMs<=800)
                for(j in start until i)flags[j]=true
        }
        return flags
    }

    private fun droneExcursion(points:List<SessionAnalysis.Point>,i:Int,instrument:InstrumentProfile):Reason? {
        if(instrument!=InstrumentProfile.DIDGERIDOO)return null
        val p=points[i]
        if(i>0 && i<points.lastIndex){
            val a=points[i-1];val b=points[i+1]
            if(supported(a,instrument) && supported(b,instrument) &&
                p.timeMs-a.timeMs in 1..400 && b.timeMs-p.timeMs in 1..400 &&
                cents(a.hz!!,b.hz!!)<=80 && cents(p.hz!!,sqrt(a.hz*b.hz))>=100)
                return Reason.ISOLATED_RETURN
        }
        if(p.confidence>=.90)return null
        for(direction in listOf(-1,1)){
            val a=i+direction;val b=i+2*direction;val outside=i-direction
            if(a !in points.indices || b !in points.indices)continue
            val edge=outside !in points.indices || points[outside].hz==null
            if(edge && supported(points[a],instrument) && supported(points[b],instrument) &&
                abs(points[a].timeMs-p.timeMs) in 1..400 && abs(points[b].timeMs-points[a].timeMs) in 1..400 &&
                cents(points[a].hz!!,points[b].hz!!)<=80 && cents(p.hz!!,sqrt(points[a].hz!!*points[b].hz!!))>=100 &&
                p.confidence<=max(points[a].confidence,points[b].confidence)-.04)
                return Reason.NOTE_EDGE
        }
        return null
    }

    fun build(points:List<SessionAnalysis.Point>,instrument:InstrumentProfile):List<Sample> {
        val result=ArrayList<Sample>(points.size)
        val alternatives=shortAlternativePeriods(points,instrument)
        var previous:Int?=null;var omitted=0
        for((i,point) in points.withIndex()){
            val hz=point.hz
            if(point.timeMs<0 || hz==null || !hz.isFinite() || hz<=0 || hz>22050){
                // Hard absence breaks. Brief confidence dips may join agreeing measured
                // endpoints; no frequency is inserted at missing timestamps.
                if(hz==null && point.timeMs>=0 && point.confidence in .5..<.70)omitted++
                else {previous=null;omitted=0}
                continue
            }
            val reason=when {
                hz !in instrument.validMinHz..instrument.validMaxHz->Reason.OUTSIDE_PROFILE
                !instrument.accepts(point)->Reason.LOW_CONFIDENCE
                alternatives[i] || isolatedPeriod(points,i,instrument)->Reason.ISOLATED_PERIOD
                else->droneExcursion(points,i,instrument)
            }
            if(reason!=null){result+=Sample(point,null,true,reason);omitted++;continue}
            val before=previous?.takeIf {idx->
                val prior=result[idx].point;val gap=point.timeMs-prior.timeMs
                gap in 1..1200 && (omitted==0 || (omitted<=4 && gap<=1000 && cents(hz,prior.hz!!)<=80))
            }
            result+=Sample(point,before,false,bridged=before!=null && omitted>0)
            previous=result.lastIndex;omitted=0
        }
        return result
    }
}
