package ar.didgetaller.core

import kotlin.math.*

/** The live acceptance/smoothing decision, shared by microphone and file diagnostics.
 * Stored points keep detector Hz; smoothing affects only the current display. */
object LivePitch {
    data class State(val hz:Double=0.0,val consecutive:Int=0,val lastMeasuredHz:Double=0.0,
        val storedHz:Double?=null)

    fun next(previous:State,incoming:PitchDetector.Reading,profile:InstrumentProfile):State {
        val usable=if(profile==InstrumentProfile.DIDGERIDOO)incoming.hz>0 else
            incoming.hz in profile.validMinHz..profile.validMaxHz && incoming.confidence>=.80
        if(!usable)return State(lastMeasuredHz=previous.lastMeasuredHz)
        val difference=if(previous.hz>0)abs(1200*ln(incoming.hz/previous.hz)/ln(2.0)) else Double.POSITIVE_INFINITY
        val hz=if(difference<70).65*previous.hz+.35*incoming.hz else incoming.hz
        val consecutive=if(difference<45)previous.consecutive+1 else 1
        return State(hz,consecutive,if(consecutive>=2)hz else previous.lastMeasuredHz,incoming.hz.takeIf {it.isFinite()})
    }
}
