package ar.didgetaller.core

import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/** Equal temperament A4=440 Hz. No music-theory background required. */
object Music {
    data class Pitch(val midi:Int,val note:String,val referenceHz:Double,val cents:Double)
    private val namesEs = listOf("Do", "Do♯", "Re", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "Si♭", "Si")
    private val namesEn = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

    // Supported equal-temperament range: MIDI 0..127, including their half-semitone edges.
    // Tiny positive values are not useful musical measurements and must never acquire absurd octaves.
    private val minMusicalHz=440.0*2.0.pow((-69.5)/12.0)
    private val maxMusicalHz=440.0*2.0.pow((127.5-69.0)/12.0)
    fun midi(hz: Double): Double = if (hz.isFinite() && hz>=minMusicalHz && hz<maxMusicalHz) 69.0 + 12.0 * log2(hz / 440.0) else Double.NaN
    fun frequency(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)
    fun pitch(hz:Double):Pitch? {
        val value=midi(hz)
        if(!value.isFinite())return null
        val nearest=value.roundToInt()
        val reference=frequency(nearest)
        return Pitch(nearest,"${namesEn[Math.floorMod(nearest,12)]}${Math.floorDiv(nearest,12)-1}",reference,
            1200.0*log2(hz/reference))
    }
    fun note(hz: Double): String {
        val m = midi(hz)
        if (!m.isFinite()) return "—"
        val n = m.roundToInt()
        return "${namesEs[Math.floorMod(n,12)]} · ${namesEn[Math.floorMod(n,12)]}${Math.floorDiv(n,12)-1}"
    }
    fun cents(hz: Double): Int {
        val m = midi(hz)
        return if (m.isFinite()) ((m - m.roundToInt()) * 100).roundToInt() else 0
    }
    fun hzText(hz: Double): String = if (hz > 0 && hz.isFinite()) String.format(Locale.US, "%.1f Hz",hz) else "— Hz"
}
