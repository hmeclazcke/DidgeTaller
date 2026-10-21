package ar.didgetaller.ui

import androidx.compose.runtime.*
import ar.didgetaller.core.*

enum class AcousticPlot(val title:String,val temporal:Boolean) {
    PITCH("Fundamental",true),SPECTROGRAM("Espectrograma",true),WAVE("Forma de onda",true),SPECTRUM("Armónicos",false)
}

/** Small, session-scoped camera/selection state, owned by the retained ViewModel. No audio arrays. */
class ExplorerState {
    var plot by mutableStateOf(AcousticPlot.PITCH)
    var selectedMs by mutableLongStateOf(0)
    var selectedY by mutableDoubleStateOf(0.0)
    var regionStart by mutableLongStateOf(0)
    var regionEnd by mutableLongStateOf(0)
    var mode by mutableIntStateOf(0)
    var looping by mutableStateOf(false)
    var follow by mutableStateOf(false)
    var logarithmic by mutableStateOf(false)
    var rangeDb by mutableDoubleStateOf(70.0)
    var spectrumReference by mutableDoubleStateOf(0.0)
    var inspected by mutableStateOf<String?>(null)
    var gestureActive by mutableStateOf(false)
    private var pitchSource:List<SessionAnalysis.Point>?=null
    private var robustPitch=PitchViewRange(AxisWindow(40.0,120.0),0,0)
    private var fullPitch=robustPitch
    fun pitch(points:List<SessionAnalysis.Point>,all:Boolean=false):PitchViewRange {
        if(pitchSource!==points){pitchSource=points;robustPitch=PlotScales.pitch(points);fullPitch=PlotScales.pitch(points,true)}
        return if(all)fullPitch else robustPitch
    }
    val cameras=mutableStateMapOf<AcousticPlot,PlotViewport>()
    val region get()=if(regionEnd>regionStart)regionStart..regionEnd else null
    fun defaults(kind:AcousticPlot,duration:Long,rate:Int,points:List<SessionAnalysis.Point>):PlotViewport {
        val y=when(kind){AcousticPlot.PITCH->pitch(points).window;AcousticPlot.WAVE->AxisWindow(-1.0,1.0);AcousticPlot.SPECTROGRAM->AxisWindow(if(logarithmic)rate/32768.0 else 0.0,500.0.coerceAtMost(rate/2.0));AcousticPlot.SPECTRUM->AxisWindow(-80.0,0.0)}
        return PlotViewport(AxisWindow(0.0,if(kind.temporal)duration.coerceAtLeast(2).toDouble() else minOf(500.0,rate/2.0)),y)
    }
    fun camera(duration:Long,rate:Int,points:List<SessionAnalysis.Point>)=cameras[plot] ?: defaults(plot,duration,rate,points)
    fun domains(duration:Long,rate:Int,points:List<SessionAnalysis.Point>):Pair<AxisDomain,AxisDomain> {
        val nyquist=rate/2.0
        val x=if(plot.temporal)AxisDomain(0.0,duration.coerceAtLeast(2).toDouble(),maxOf(2.0,2000.0/rate)) else AxisDomain(0.0,nyquist,rate/32768.0)
        val y=when(plot){
            AcousticPlot.WAVE->AxisDomain(-1.0,1.0,2.0/32768)
            AcousticPlot.SPECTRUM->AxisDomain(-140.0,10.0,5.0)
            AcousticPlot.PITCH->AxisDomain(0.0,maxOf(500.0,pitch(points,true).window.high),.2)
            AcousticPlot.SPECTROGRAM->if(logarithmic)AxisDomain(rate/32768.0,nyquist,kotlin.math.ln(1.03),true) else AxisDomain(0.0,nyquist,rate/32768.0)
        }
        return x to y
    }
    fun reset(duration:Long,rate:Int,points:List<SessionAnalysis.Point>){cameras[plot]=defaults(plot,duration,rate,points);inspected=null;follow=false}
    fun transform(duration:Long,rate:Int,points:List<SessionAnalysis.Point>,cx:Double,cy:Double,zx:Double,zy:Double,dx:Double,dy:Double){
        val domains=domains(duration,rate,points)
        cameras[plot]=camera(duration,rate,points).transform(domains.first,domains.second,cx,cy,zx,zy,dx,dy)
        follow=false
    }
}
