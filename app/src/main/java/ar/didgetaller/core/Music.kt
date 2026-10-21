package ar.didgetaller.core

import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/** Equal temperament A4=440 Hz. No music-theory background required. */
object Music {
    private val namesEs = listOf("Do", "Do♯", "Re", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "Si♭", "Si")
    private val namesEn = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

    fun midi(hz: Double): Double = if (hz > 0.0 && hz.isFinite()) 69.0 + 12.0 * log2(hz / 440.0) else Double.NaN
    fun frequency(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)
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
