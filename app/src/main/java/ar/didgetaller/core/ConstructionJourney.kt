package ar.didgetaller.core

import kotlin.math.abs

enum class ConstructionReadiness { READY, MINIMUM, INCOMPATIBLE, UNCONFIRMED, ALTERNATIVE, ACCEPTED_ALTERNATIVE, LEGACY }

/** Draft inputs and approval have one owner; changing an input revokes the old proposal. */
data class ConstructionJourney(val values:Map<String,String>,val step:Int=0,val acceptedValues:Map<String,String>?=null) {
    init {require(step in 0..3)}
    val accepted:Boolean get()=acceptedValues==values
    fun edit(key:String,value:String)=copy(values=values+(key to value),acceptedValues=null)
    companion object {
        fun restore(saved:Map<String,String>):ConstructionJourney {
            val values=saved.filterKeys {!it.startsWith("_wizard")}
            val step=(saved["_wizardStep"]?.toIntOrNull() ?: 0).coerceIn(0,3)
            return ConstructionJourney(values,step,if(saved["_wizardAccepted"]=="true")values else null)
        }
        fun available(values:Map<String,String>,step:Int,validated:Boolean=false):Boolean {
            if(step==0)return true
            if(runCatching {ConstructionForm.shape(values);ConstructionForm.target(values)}.isFailure)return false
            if(step==1)return true
            if(runCatching {ConstructionForm.options(values)}.isFailure)return false
            return step==2 || step==3 && validated
        }
        fun canPlan(p:ConstructionPlan,accepted:Boolean)=readiness(p,accepted) in setOf(ConstructionReadiness.READY,ConstructionReadiness.MINIMUM,ConstructionReadiness.ACCEPTED_ALTERNATIVE,ConstructionReadiness.LEGACY)
        fun readiness(p:ConstructionPlan,accepted:Boolean):ConstructionReadiness = when(p.options.method) {
            ConstructionMethod.SANDWICH->woodReadiness(WoodFit.assess(p.ideal,p.options.woodStock,p.options.wallMm,p.options.radialToleranceMm))
            ConstructionMethod.PVC->when {
                deviation(p)<=0.1 && abs(p.lengthMm-p.ideal.lengthMm)<=0.1->ConstructionReadiness.READY
                accepted->ConstructionReadiness.ACCEPTED_ALTERNATIVE
                else->ConstructionReadiness.ALTERNATIVE
            }
            ConstructionMethod.LAMINATED->ConstructionReadiness.LEGACY
        }
        private fun woodReadiness(fit:WoodAssessment):ConstructionReadiness = when(fit.status) {
            WoodFitStatus.DOES_NOT_FIT->ConstructionReadiness.INCOMPATIBLE
            WoodFitStatus.UNCONFIRMED->ConstructionReadiness.UNCONFIRMED
            WoodFitStatus.FITS->if(fit.marginMm!!<=1e-9)ConstructionReadiness.MINIMUM else ConstructionReadiness.READY
        }
        /** Maximum station diameter difference; length mismatch is reported separately. */
        fun deviation(p:ConstructionPlan):Double = ConstructionDrawing.rows(p).maxOf {r->
            r.idealMm?.let {maxOf(abs(it-r.beforeMm),abs(it-r.afterMm))} ?: 0.0
        }
    }
}
