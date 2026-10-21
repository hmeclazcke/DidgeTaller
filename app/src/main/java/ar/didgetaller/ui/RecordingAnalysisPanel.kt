package ar.didgetaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.didgetaller.TallerViewModel
import ar.didgetaller.core.Music
import ar.didgetaller.data.RecordingStore
import java.util.Locale

/** Reuses the validated Phase A pipeline; cache and interval belong to the session workspace. */
@Composable
internal fun RecordingAnalysisPanel(vm:TallerViewModel,item:RecordingStore.Session) {
    val state=vm.recordingAnalysis.takeIf {it.sessionId==item.id}
    val colors=MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().testTag("recording-analysis"),shape=RoundedCornerShape(20.dp),color=colors.surface) {
        Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Análisis del WAV",fontSize=19.sp,fontWeight=FontWeight.SemiBold)
            Text(if(state?.startMs==0L && state.endMs==null || state==null)"Toda la grabación · cálculo local desde el audio original"
                else "Fragmento ${seconds(state.startMs)}–${state.endMs?.let {seconds(it)} ?: "fin"} · WAV original",color=colors.onSurfaceVariant,fontSize=12.sp)
            if(state?.running==true) {
                LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth())
                Text("Analizando audio · ${state.progress}%",fontSize=13.sp)
                OutlinedButton(onClick={vm.cancelRecordingAnalysis()}) {Text("Cancelar análisis")}
            } else Button(onClick={vm.analyzeRecording(item,force=state?.result!=null)}) {
                Text(if(state?.result!=null)"Volver a analizar" else "Analizar grabación")
            }
            state?.error?.let {Text(it,color=colors.error,fontSize=13.sp)}
            state?.result?.let {result ->
                val summary=result.summary
                Text(summary?.let {"${Music.note(it.typicalHz)} · ${hzLabel(it.typicalHz)}"} ?: "Fundamental incierta",
                    color=colors.primary,fontSize=22.sp,fontWeight=FontWeight.Bold)
                Text("${result.sampleRate} Hz de muestreo · ${result.windows} ventanas · hasta %.2f s por ventana".format(spanish,result.spectrum.fftSize.toDouble()/result.sampleRate),
                    color=colors.onSurfaceVariant,fontSize=12.sp)
                Text("Ventanas con frecuencia válida: ${(result.validWindowFraction*100).toInt()}%"+
                    (result.medianPeriodicityConfidence?.let {" · periodicidad %.2f".format(spanish,it)} ?: ""),fontSize=13.sp)
                summary?.let {Text("Estabilidad: ${it.stabilityPercent}% · rango habitual ${hzLabel(it.lowHz)} – ${hzLabel(it.highHz)}",fontSize=13.sp)}
                result.medianValidHz?.let {median ->
                    Text("Mediana de lecturas válidas: ${hzLabel(median)} · media: ${hzLabel(result.meanValidHz!!)}",
                        color=colors.onSurfaceVariant,fontSize=12.sp)
                }
                Text(result.spectralCentroidHz?.let {"Centroide de potencia: ${hzLabel(it)}"} ?: "Sin energía espectral significativa",
                    fontSize=14.sp)
                Text("Picos del espectro promedio",fontWeight=FontWeight.SemiBold)
                if(result.harmonics.peaks.isEmpty())Text("No se detectaron picos suficientes.",color=colors.onSurfaceVariant)
                result.harmonics.peaks.sortedByDescending {it.relativeDb}.take(8).sortedBy {it.hz}.forEach {peak ->
                    val label=peak.order?.let {"H$it candidato"} ?: "Pico sin asignar"
                    Text("$label · ${hzLabel(peak.hz)} · %.1f dB rel".format(spanish,peak.relativeDb)+
                        (peak.deviationCents?.let {" · %+.1f cents".format(spanish,it)} ?: ""),fontSize=13.sp)
                }
                if(result.harmonics.fundamentalHz==null)Text("Sin una fundamental predominante confiable, no se asignan órdenes armónicos.",
                    color=colors.secondary,fontSize=12.sp)
                result.harmonics.candidateBandEnergyFraction?.let {
                    Text("Energía en bandas de candidatos (±2 bins): ${(it*100).toInt()}%",fontSize=12.sp)
                }
                Text(result.bandEnergies.joinToString(" · ") {band ->
                    "${band.fromHz.toInt()}–${band.toHz.toInt()} Hz: ${(band.fraction*100).toInt()}%"
                },color=colors.onSurfaceVariant,fontSize=12.sp)
                Text("dB relativos al pico mayor del espectro, sin calibración de presión sonora. La periodicidad es un índice del detector, no una probabilidad. El micrófono del teléfono puede alterar las intensidades.",
                    color=colors.onSurfaceVariant,fontSize=12.sp)
                Text("Los resultados derivados se conservan en una caché regenerable; el WAV y el resumen original no cambian. Los picos no identifican resonancias del conducto ni toots.",
                    color=colors.onSurfaceVariant,fontSize=12.sp)
            }
        }
    }
}
