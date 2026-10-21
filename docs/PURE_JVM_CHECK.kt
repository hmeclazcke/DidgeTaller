import ar.didgetaller.core.*
import kotlin.math.*

fun main() {
    check(Music.note(73.416)=="Re · D2")
    check(abs(Music.cents(73.416))<=1)
    val rate=44100
    listOf(50.0,65.4,73.416,82.407,98.0,125.0).forEach {target ->
        val pcm=ShortArray(16384){i->
            (12000*sin(2*PI*target*i/rate)+4300*sin(4*PI*target*i/rate)+1600*sin(6*PI*target*i/rate)).toInt().toShort()
        }
        val reading=PitchDetector.analyze(pcm)
        println("${target}Hz -> ${"%.2f".format(reading.hz)}Hz / confidence ${"%.2f".format(reading.confidence)}")
        check(abs(reading.hz-target)<1.5)
    }
    val missing=ShortArray(16384){i->
        (11000*sin(4*PI*73.416*i/rate)+7000*sin(6*PI*73.416*i/rate)).toInt().toShort()
    }
    check(abs(PitchDetector.analyze(missing).hz-73.416)<1.5)
    val result=AcousticModel.evaluate(AcousticModel.Shape(1500,40,40,40))
    println("PVC simulated: ${result.baseHz} Hz; ${result.frequenciesHz}")
    check(result.baseHz in 30.0..200.0)
    val encoded=WavCodec.encode(shortArrayOf(0,1,-1),3,44100)
    check(encoded.size==50 && String(encoded,0,4)=="RIFF")
    println("CORE JVM PASS")
}
