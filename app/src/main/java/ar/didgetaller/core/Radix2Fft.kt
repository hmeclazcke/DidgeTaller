package ar.didgetaller.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Existing radix-2 FFT, shared by the live display and digital power spectra. */
internal object Radix2Fft {
    fun forward(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        require(n >= 2 && n and (n - 1) == 0 && im.size == n)
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val real = re[i]; re[i] = re[j]; re[j] = real
                val imaginary = im[i]; im[i] = im[j]; im[j] = imaginary
            }
        }
        var span = 2
        while (span <= n) {
            val cw = cos(-2 * PI / span); val sw = sin(-2 * PI / span)
            var i = 0
            while (i < n) {
                var wr = 1.0; var wi = 0.0
                for (k in 0 until span / 2) {
                    val a = i + k; val b = a + span / 2
                    val tr = wr * re[b] - wi * im[b]; val ti = wr * im[b] + wi * re[b]
                    re[b] = re[a] - tr; im[b] = im[a] - ti; re[a] += tr; im[a] += ti
                    val next = wr * cw - wi * sw; wi = wr * sw + wi * cw; wr = next
                }
                i += span
            }
            span *= 2
        }
    }
}
