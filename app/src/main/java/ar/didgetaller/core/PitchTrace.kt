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

    private fun shortReturn(a:SessionAnalysis.Point,p:SessionAnalysis.Point,b:SessionAnalysis.Point)=
        p.timeMs-a.timeMs in 1..400 && b.timeMs-p.timeMs in 1..400 && b.timeMs-a.timeMs<=600

    private fun returnCenter(points:List<SessionAnalysis.Point>,i:Int,instrument:InstrumentProfile):Double? {
        val a=points.getOrNull(i-1) ?: return null
        val b=points.getOrNull(i+1) ?: return null
        val p=points[i]
        if(!supported(a,instrument) || !supported(b,instrument) || p.confidence>=.92)return null
        if(!shortReturn(a,p,b) || cents(a.hz!!,b.hz!!)>80)return null
        return sqrt(a.hz*b.hz)
    }

    private fun isolated(points:List<SessionAnalysis.Point>,i:Int,instrument:InstrumentProfile):Reason? {
        val center=returnCenter(points,i,instrument) ?: return null
        val p=points[i]
        val ratio=max(center/p.hz!!,p.hz/center)
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
        return listOf(-1,1).any {direction->jumpWitness(points,i,direction,instrument)}
    }

    private fun jumpWitness(points:List<SessionAnalysis.Point>,i:Int,direction:Int,instrument:InstrumentProfile):Boolean {
        val p=points[i]
        val a=points.getOrNull(i+direction) ?: return false
        val b=points.getOrNull(i+2*direction) ?: return false
        if(!supported(a,instrument) || !supported(b,instrument))return false
        return abs(p.timeMs-a.timeMs) in 1..400 && abs(a.timeMs-b.timeMs) in 1..400 &&
            cents(a.hz!!,b.hz!!)<=80 && cents(p.hz!!,sqrt(a.hz*b.hz))>=600
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
            if(!lowerAlternative(points[start],before,instrument)){start++;continue}
            val end=alternativeEnd(points,start,before,instrument)
            if(returned(points,start,end,before,instrument))for(j in start until end)flags[j]=true
            start=end
        }
        return flags
    }

    private fun lowerAlternative(p:SessionAnalysis.Point,before:SessionAnalysis.Point,instrument:InstrumentProfile):Boolean {
        if(!supported(p,instrument) || p.confidence>=.90 || p.hz!!>=before.hz!!/1.4)return false
        return listOf(1.5,2.0,3.0,4.0).any {cents(before.hz/p.hz,it)<=80}
    }

    private fun alternativeEnd(points:List<SessionAnalysis.Point>,start:Int,before:SessionAnalysis.Point,instrument:InstrumentProfile):Int {
        var end=start+1
        while(end<points.size && lowerAlternative(points[end],before,instrument) &&
            points[end].timeMs-points[end-1].timeMs in 1..400)end++
        return end
    }

    private fun returned(points:List<SessionAnalysis.Point>,start:Int,end:Int,before:SessionAnalysis.Point,instrument:InstrumentProfile):Boolean {
        val after=points.getOrNull(end) ?: return false
        return end-start<=4 && supported(after,instrument) &&
            points[start].timeMs-before.timeMs in 1..400 && after.timeMs-points[end-1].timeMs in 1..400 &&
            after.timeMs-before.timeMs<=1000 && cents(before.hz!!,after.hz!!)<=80
    }

    private fun renderableFrequency(point:SessionAnalysis.Point)=point.hz?.takeIf {
        point.timeMs>=0 && it.isFinite() && it>0 && it<=22050
    }

    /** One builder owns connection state for one immutable input sequence. */
    fun build(points:List<SessionAnalysis.Point>,instrument:InstrumentProfile):List<Sample> =
        Builder(points,instrument).build()

    private class Builder(val points:List<SessionAnalysis.Point>,val instrument:InstrumentProfile) {
        private val result=ArrayList<Sample>(points.size)
        private val alternatives=alternativePeriods(points,instrument)
        private var previous:Int?=null
        private var omitted=0

        fun build():List<Sample> {
            points.forEachIndexed {i,point->append(i,point)}
            return result
        }

        private fun append(i:Int,point:SessionAnalysis.Point) {
            val hz=renderableFrequency(point) ?: return omit(point)
            val reason=rejection(i,point,hz)
            if(reason!=null){result+=Sample(point,null,true,reason);omitted++;return}
            val backed=hasSupport(points,i,instrument)
            val before=previous?.takeIf {connects(result[it],point,hz,backed)}
            val prior=before?.let {result[it].point}
            val variation=prior?.hz?.let {cents(hz,it)}
            val confidence=min(point.confidence,prior?.confidence ?: point.confidence)
            result+=Sample(point,before,false,bridged=before!=null && omitted>0,
                variationCents=variation,tone=tone(confidence,variation),backed=backed)
            previous=result.lastIndex
            omitted=0
        }

        private fun omit(point:SessionAnalysis.Point) {
            if(point.hz==null && point.timeMs>=0 && point.confidence in .5..<.70)omitted++
            else {previous=null;omitted=0}
        }

        private fun rejection(i:Int,point:SessionAnalysis.Point,hz:Double):Reason?=when {
            hz !in instrument.validMinHz..instrument.validMaxHz->Reason.OUTSIDE_PROFILE
            !supported(point,instrument)->Reason.LOW_CONFIDENCE
            alternatives[i]->Reason.ISOLATED_PERIOD
            unsupportedJump(points,i,instrument)->Reason.UNSUPPORTED_JUMP
            else->isolated(points,i,instrument)
        }

        private fun connects(prior:Sample,point:SessionAnalysis.Point,hz:Double,backed:Boolean):Boolean {
            val gap=point.timeMs-prior.point.timeMs
            val jump=cents(hz,prior.point.hz!!)
            return gap in 1..1200 && (omitted==0 || (omitted<=4 && gap<=1000 && jump<=80)) &&
                !unsupportedSweep(prior,point,jump,backed)
        }

        private fun unsupportedSweep(prior:Sample,point:SessionAnalysis.Point,jump:Double,backed:Boolean):Boolean {
            if(instrument!=InstrumentProfile.DIDGERIDOO)return false
            if(jump>=600)return true
            val unsupported=(point.confidence<.90 && !backed) || (prior.point.confidence<.90 && !prior.backed)
            return jump>=300 && unsupported
        }
    }
}
