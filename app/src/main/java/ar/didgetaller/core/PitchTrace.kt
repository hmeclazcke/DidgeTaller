package ar.didgetaller.core

import kotlin.math.*

/** Original estimates and presentation decisions; never rewrites or adds measurements. */
object PitchTrace {
    enum class Reason(val label:String) {
        LOW_CONFIDENCE("periodicidad insuficiente"), OUTSIDE_PROFILE("fuera del rango del instrumento"),
        ISOLATED_PERIOD("período alternativo breve con retorno; original disponible"),
        ISOLATED_RETURN("excursión débil aislada con retorno; original disponible"),
        UNSUPPORTED_JUMP("salto amplio sin apoyo temporal; original disponible")
    }
    enum class Tone { STABLE, MODERATE, REVIEW }
    data class Sample(val point:SessionAnalysis.Point,val previous:Int?,val uncertain:Boolean,
                      val reason:Reason?=null,val bridged:Boolean=false,
                      val variationCents:Double?=null,val tone:Tone=Tone.REVIEW,val backed:Boolean=false)
    // Same minimum periodicity as YIN acceptance; the summary's .70 threshold stays independent.
    const val MIN_CURVE_CONFIDENCE=.65
    private fun cents(a:Double,b:Double)=abs(1200*log2(a/b))
    private fun supported(p:SessionAnalysis.Point,instrument:InstrumentProfile)=
        p.timeMs>=0 && p.hz!=null && p.hz.isFinite() && p.hz in instrument.validMinHz..instrument.validMaxHz &&
            p.confidence in MIN_CURVE_CONFIDENCE..1.0

    /** Historical colour semantics shared by all surfaces. Confidence is not stability. */
    fun tone(confidence:Double,variationCents:Double?):Tone=when {
        !confidence.isFinite() || confidence<.80 || (variationCents ?: 0.0)>60->Tone.REVIEW
        variationCents==null || confidence<.90 || variationCents>25->Tone.MODERATE
        else->Tone.STABLE
    }

    private fun isolated(points:List<SessionAnalysis.Point>,i:Int,instrument:InstrumentProfile):Reason? {
        if(i==0 || i==points.lastIndex)return null
        val a=points[i-1];val p=points[i];val b=points[i+1]
        if(!supported(a,instrument) || !supported(b,instrument) || p.confidence>=.92)return null
        if(p.timeMs-a.timeMs !in 1..400 || b.timeMs-p.timeMs !in 1..400 || b.timeMs-a.timeMs>600)return null
        if(cents(a.hz!!,b.hz!!)>80)return null
        val center=sqrt(a.hz*b.hz);val ratio=max(center/p.hz!!,p.hz/center)
        val multiple=ratio.roundToInt()
        if(multiple in 2..4 && cents(ratio,multiple.toDouble())<=80)return Reason.ISOLATED_PERIOD
        if(instrument==InstrumentProfile.DIDGERIDOO && p.confidence<.90 && cents(p.hz,center)>=100)
            return Reason.ISOLATED_RETURN
        return null
    }

    private fun unsupportedJump(points:List<SessionAnalysis.Point>,i:Int,instrument:InstrumentProfile):Boolean {
        val p=points[i]
        if(instrument!=InstrumentProfile.DIDGERIDOO || p.confidence>=.90)return false
        if(hasSupport(points,i,instrument))return false
        // Require two coherent measured neighbours; do not remove ordinary attacks/releases.
        return listOf(-1,1).any {d->
            val a=points.getOrNull(i+d);val b=points.getOrNull(i+2*d)
            a!=null && b!=null && supported(a,instrument) && supported(b,instrument) &&
                abs(p.timeMs-a.timeMs) in 1..400 && abs(a.timeMs-b.timeMs) in 1..400 &&
                cents(a.hz!!,b.hz!!)<=80 && cents(p.hz!!,sqrt(a.hz*b.hz))>=600
        }
    }

    private fun hasSupport(points:List<SessionAnalysis.Point>,i:Int,instrument:InstrumentProfile):Boolean {
        val p=points[i]
        return (-4..4).any {d->d!=0 && points.getOrNull(i+d)?.let {n->
            supported(n,instrument) && abs(n.timeMs-p.timeMs) in 1..1000 && cents(n.hz!!,p.hz!!)<=80
        }==true}
    }

    /** A short lower period is ambiguous only with measured return on BOTH sides.
     * No global dominant register, no filtering attacks/releases next to missing signal. */
    private fun alternativePeriods(points:List<SessionAnalysis.Point>,instrument:InstrumentProfile):BooleanArray {
        val flags=BooleanArray(points.size)
        if(instrument!=InstrumentProfile.DIDGERIDOO)return flags
        var start=1
        while(start<points.lastIndex){
            val before=points[start-1]
            if(!supported(before,instrument)){start++;continue}
            fun alternative(p:SessionAnalysis.Point):Boolean {
                if(!supported(p,instrument) || p.confidence>=.90 || p.hz!!>=before.hz!!/1.4)return false
                return listOf(1.5,2.0,3.0,4.0).any {cents(before.hz/p.hz,it)<=80}
            }
            if(!alternative(points[start])){start++;continue}
            var end=start+1
            while(end<points.size && alternative(points[end]) && points[end].timeMs-points[end-1].timeMs in 1..400)end++
            if(end<points.size && end-start<=4 && supported(points[end],instrument) &&
                points[start].timeMs-before.timeMs in 1..400 && points[end].timeMs-points[end-1].timeMs in 1..400 &&
                points[end].timeMs-before.timeMs<=1000 && cents(before.hz!!,points[end].hz!!)<=80)
                for(j in start until end)flags[j]=true
            start=end
        }
        return flags
    }

    fun build(points:List<SessionAnalysis.Point>,instrument:InstrumentProfile):List<Sample> {
        val result=ArrayList<Sample>(points.size)
        val alternatives=alternativePeriods(points,instrument)
        var previous:Int?=null;var omitted=0
        for((i,point) in points.withIndex()){
            val hz=point.hz
            if(point.timeMs<0 || hz==null || !hz.isFinite() || hz<=0 || hz>22050){
                if(hz==null && point.timeMs>=0 && point.confidence in .5..<.70)omitted++
                else {previous=null;omitted=0}
                continue
            }
            val reason=when {
                hz !in instrument.validMinHz..instrument.validMaxHz->Reason.OUTSIDE_PROFILE
                !supported(point,instrument)->Reason.LOW_CONFIDENCE
                alternatives[i]->Reason.ISOLATED_PERIOD
                unsupportedJump(points,i,instrument)->Reason.UNSUPPORTED_JUMP
                else->isolated(points,i,instrument)
            }
            if(reason!=null){result+=Sample(point,null,true,reason);omitted++;continue}
            val backed=hasSupport(points,i,instrument)
            val before=previous?.takeIf {idx->
                val prior=result[idx].point;val gap=point.timeMs-prior.timeMs
                val jump=cents(hz,prior.hz!!)
                val weakUnsupportedEnd=(point.confidence<.90 && !backed) || (prior.confidence<.90 && !result[idx].backed)
                gap in 1..1200 && (omitted==0 || (omitted<=4 && gap<=1000 && jump<=80)) &&
                    // Preserve the estimate as a point, but do not imply a supported pitch sweep.
                    !(instrument==InstrumentProfile.DIDGERIDOO && jump>=300 && weakUnsupportedEnd)
            }
            val prior=before?.let {result[it].point}
            val variation=prior?.hz?.let {cents(hz,it)}
            val confidence=min(point.confidence,prior?.confidence ?: point.confidence)
            result+=Sample(point,before,false,bridged=before!=null && omitted>0,
                variationCents=variation,tone=tone(confidence,variation),backed=backed)
            previous=result.lastIndex;omitted=0
        }
        return result
    }
}
