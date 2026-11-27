package ar.didgetaller.study

import ar.didgetaller.core.InstrumentProfile
import kotlin.math.*

internal fun checkSeparatedLines() {
    val p = InstrumentProfile.DIDGERIDOO
    fun pair(low: Double, high: Double) = Trial("pair", p, 44100, 0, Double.NaN,
        ShortArray(p.windowSamples) { i ->
            (14000 * sin(2 * PI * low * i / 44100) + 2400 * sin(2 * PI * high * i / 44100)).roundToInt().toShort()
        })
    val bed = EvidenceReader(p.windowSamples, Route.SPECTRAL)
    val missing = pair(340.0, 408.0)
    check(abs(bed.read(missing).pitch.hz - 68) < .5)
    check(bed.read(pair(332.57, 406.21)).pitch.hz == 0.0) { "Widely separated orders alone cannot license a source hypothesis" }
    val causal = EvidenceReader(p.windowSamples, Route.CONTEXT)
    causal.read(pair(68.0, 136.0))
    check(abs(causal.read(missing).pitch.hz - 68) < .5)
    check(causal.read(missing.copy(pcm = ShortArray(p.windowSamples))).pitch.hz == 0.0)
    check(causal.read(missing).hypothesis == null)
    val separate = EvidenceReader(p.windowSamples, Route.SEPARATE).read(missing)
    check(separate.pitch == reading(missing, missing.pcm) && abs(separate.hypothesis!!.hz - 68) < .5)
    println("Phase 3 acoustic controls passed")
}
