package ar.didgetaller.ui

import androidx.compose.runtime.*
import ar.didgetaller.core.*

enum class AcousticPlot(val title:String,val temporal:Boolean) {
    PITCH("Fundamental",true),SPECTROGRAM("Espectrograma",true),WAVE("Forma de onda",true),SPECTRUM("Armónicos",false)
}

/** Selection belongs to the active session; cameras belong to each presentation. */
class ExplorerSelection {
    var selectedMs by mutableLongStateOf(0)
    var regionStart by mutableLongStateOf(0)
    var regionEnd by mutableLongStateOf(0)
    var mode by mutableIntStateOf(0)
    var looping by mutableStateOf(false)
}
/** Retained interaction state, never persisted in a recording. No audio arrays. */
class ExplorerState(val instrument:InstrumentProfile=InstrumentProfile.DIDGERIDOO,internal val selection:ExplorerSelection=ExplorerSelection()) {
    var plot by mutableStateOf(AcousticPlot.PITCH)
    var selectedMs:Long get()=selection.selectedMs;set(value){selection.selectedMs=value}
    var selectedY by mutableDoubleStateOf(0.0)
    var regionStart:Long get()=selection.regionStart;set(value){selection.regionStart=value}
    var regionEnd:Long get()=selection.regionEnd;set(value){selection.regionEnd=value}
    var mode:Int get()=selection.mode;set(value){selection.mode=value}
    var looping:Boolean get()=selection.looping;set(value){selection.looping=value}
    var follow by mutableStateOf(false)
    var logarithmic by mutableStateOf(false)
    var rangeDb by mutableDoubleStateOf(70.0)
    var spectrumReference by mutableDoubleStateOf(0.0)
    var inspected by mutableStateOf<String?>(null)
    var gestureActive by mutableStateOf(false)
    var showUncertain by mutableStateOf(false)
    // Broad physical/custom ranges require an explicit action, never a pinch zoom-out.
    var extendedPitchRange by mutableStateOf(false)
    private var pitchSource:List<SessionAnalysis.Point>?=null
    private var robustPitch=PitchViewRange(AxisWindow(40.0,120.0),0,0)
    private var fullPitch=robustPitch
    private var generalPitch=robustPitch
    fun pitch(points:List<SessionAnalysis.Point>,all:Boolean=false):PitchViewRange {
        if(pitchSource!==points){pitchSource=points;robustPitch=PlotScales.pitch(points,instrument=instrument);fullPitch=PlotScales.pitch(points,true,instrument);generalPitch=PlotScales.overview(points,instrument)}
        return if(all)fullPitch else robustPitch
    }
    fun overview(points:List<SessionAnalysis.Point>):PitchViewRange {pitch(points);return generalPitch}
    val cameras=mutableStateMapOf<AcousticPlot,PlotViewport>()
    val region get()=if(regionEnd>regionStart)regionStart..regionEnd else null
    fun defaults(kind:AcousticPlot,duration:Long,rate:Int,points:List<SessionAnalysis.Point>):PlotViewport {
        val y=when(kind){AcousticPlot.PITCH->overview(points).window;AcousticPlot.WAVE->AxisWindow(-1.0,1.0);AcousticPlot.SPECTROGRAM->AxisWindow(if(logarithmic)rate/32768.0 else 0.0,instrument.spectralMaxHz.coerceAtMost(rate/2.0));AcousticPlot.SPECTRUM->AxisWindow(-80.0,0.0)}
        return PlotViewport(AxisWindow(0.0,if(kind.temporal)duration.coerceAtLeast(2).toDouble() else minOf(instrument.spectralMaxHz,rate/2.0)),y)
    }
    fun camera(duration:Long,rate:Int,points:List<SessionAnalysis.Point>)=cameras[plot] ?: defaults(plot,duration,rate,points)
    fun domains(duration:Long,rate:Int,points:List<SessionAnalysis.Point>,fullPitchDomain:Boolean=false):Pair<AxisDomain,AxisDomain> {
        val nyquist=rate/2.0
        val x=if(plot.temporal)AxisDomain(0.0,duration.coerceAtLeast(2).toDouble(),if(plot==AcousticPlot.SPECTROGRAM)16384000.0/rate else maxOf(2.0,2000.0/rate)) else AxisDomain(0.0,nyquist,rate/32768.0)
        val y=when(plot){
            AcousticPlot.WAVE->AxisDomain(-1.0,1.0,2.0/32768)
            AcousticPlot.SPECTRUM->AxisDomain(-140.0,10.0,5.0)
            AcousticPlot.PITCH->if(extendedPitchRange || fullPitchDomain)AxisDomain(0.0,maxOf(instrument.maxHz.toDouble(),pitch(points,true).window.high),.2)
                else overview(points).window.let {AxisDomain(it.low,it.high,.2)}
            AcousticPlot.SPECTROGRAM->if(logarithmic)AxisDomain(rate/32768.0,nyquist,kotlin.math.ln(1.03),true) else AxisDomain(0.0,nyquist,rate/32768.0)
        }
        return x to y
    }
    fun reset(duration:Long,rate:Int,points:List<SessionAnalysis.Point>){if(plot==AcousticPlot.PITCH)extendedPitchRange=false;cameras[plot]=defaults(plot,duration,rate,points);follow=false}
    fun transform(duration:Long,rate:Int,points:List<SessionAnalysis.Point>,cx:Double,cy:Double,zx:Double,zy:Double,dx:Double,dy:Double){
        if(!cx.isFinite() || !cy.isFinite() || !zx.isFinite() || !zy.isFinite() || !dx.isFinite() || !dy.isFinite() || zx<=0 || zy<=0)return
        val domains=domains(duration,rate,points)
        cameras[plot]=camera(duration,rate,points).transform(domains.first,domains.second,cx,cy,zx,zy,dx,dy)
        follow=false
    }
}
