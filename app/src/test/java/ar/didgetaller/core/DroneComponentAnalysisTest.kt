package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.io.File
import java.security.MessageDigest
import kotlin.math.*

class DroneComponentAnalysisTest {
    private fun source(tones: List<Pair<Double, Double>>, seconds: Double = 1.2): MonoPcmSource {
        val pcm = ShortArray((44100 * seconds).toInt()) { i ->
            (tones.sumOf { (hz, amplitude) -> amplitude * sin(2 * PI * hz * i / 44100) } * 32767)
                .toInt().coerceIn(-32768, 32767).toShort()
        }
        return object : MonoPcmSource {
            override val sampleRate = 44100
            override val frameCount = pcm.size.toLong()
            override fun read(startFrame: Long, destination: ShortArray): Int {
                val n = min(destination.size, pcm.size - startFrame.toInt())
                pcm.copyInto(destination, 0, startFrame.toInt(), startFrame.toInt() + n)
                return n
            }
        }
    }

    @Test fun tracksMeasuredHarmonicFamilyWithoutChangingPitchReadings() {
        val curve = DroneComponentAnalysis().analyze(source(listOf(70.0 to .12, 140.0 to .075, 210.0 to .04)))
        assertTrue(curve.samples.count { it.hz != null } >= 3)
        assertTrue(curve.samples.mapNotNull { it.hz }.all { abs(it - 70.0) < 1.5 })
    }

    @Test fun preservesGenuineLowHarmonicFamilyAndAbstainsOnMissingFundamental() {
        val low = DroneComponentAnalysis().analyze(source(listOf(35.0 to .12, 70.0 to .075, 105.0 to .04)))
        assertTrue(low.samples.mapNotNull { it.hz }.all { abs(it - 35.0) < 1.5 })
        val missing = DroneComponentAnalysis().analyze(source(listOf(70.0 to .2, 105.0 to .1, 175.0 to .08)))
        assertTrue(missing.samples.all { it.hz == null })
    }

    @Test fun silenceAndSingleSinusoidDoNotBecomeSupportedDroneFamilies() {
        assertTrue(DroneComponentAnalysis().analyze(source(emptyList())).samples.all { it.hz == null })
        assertTrue(DroneComponentAnalysis().analyze(source(listOf(70.0 to .18))).samples.all { it.hz == null })
    }

    @Test fun harmonicFamiliesAt35And140RemainDistinctWhileWeak35StaysAmbiguous() {
        val high = DroneComponentAnalysis().analyze(source(listOf(
            140.0 to .12, 280.0 to .075, 420.0 to .04)))
        assertTrue(high.samples.mapNotNull { it.hz }.all { abs(it - 140.0) < 1.5 })
        val weakLow = DroneComponentAnalysis().analyze(source(listOf(
            35.0 to .009, 70.0 to .20, 105.0 to .10, 175.0 to .08)))
        assertTrue(weakLow.samples.all { it.hz == null })
    }

    @Test fun loudNoiseDoesNotProduceAStableSpectralFamily() {
        val random = java.util.Random(20261024)
        val pcm = ShortArray(44100) { (random.nextGaussian() * 2600).toInt()
            .coerceIn(-32768,32767).toShort() }
        val noisy = object:MonoPcmSource {
            override val sampleRate=44100
            override val frameCount=pcm.size.toLong()
            override fun read(startFrame:Long,destination:ShortArray):Int {
                pcm.copyInto(destination,0,startFrame.toInt(),startFrame.toInt()+destination.size)
                return destination.size
            }
        }
        assertTrue(DroneComponentAnalysis().analyze(noisy).samples.all { it.hz == null })
    }

    @Test fun overlayKeepsPausesAndHidesComponentsNearAcceptedNotesIncludingToots() {
        val curve = DroneComponentCurve(listOf(
            DroneComponentCurve.Sample(200.0, 70.0, 70.0, .1, DroneComponentCurve.Reason.COMPONENT),
            DroneComponentCurve.Sample(400.0, null, null, 0.0, DroneComponentCurve.Reason.QUIET),
            DroneComponentCurve.Sample(600.0, 67.0, 67.0, .1, DroneComponentCurve.Reason.COMPONENT),
            DroneComponentCurve.Sample(800.0, 66.0, 66.0, .1, DroneComponentCurve.Reason.COMPONENT),
        ), 200.0)
        val notes = listOf(SessionAnalysis.Point(385, 140.0, .95),
            SessionAnalysis.Point(985, 68.0, .95))
        val shown = DroneComponentOverlay.visible(curve, notes, InstrumentProfile.DIDGERIDOO, true)
        assertNull(shown[0].hz) // Accepted 140 Hz toot at the captured window end.
        assertNull(shown[1].hz) // A real pause is never filled.
        assertEquals(67.0, shown[2].hz!!, 0.0)
        assertNull(shown[3].hz)
        assertTrue(DroneComponentOverlay.visible(curve, emptyList(),
            InstrumentProfile.NATIVE_AMERICAN_FLUTE, true).isEmpty())
    }

    @Test fun privateRecordingNumericallyMatchesFrozenSpectralReferenceWhenAvailable() {
        val file = System.getenv("DIDGE_COMPONENT_WAV")?.let(::File)
        assumeTrue("Private WAV is opt-in", file?.isFile == true)
        val hash = MessageDigest.getInstance("SHA-256").digest(file!!.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals("427cf53dfd583b260eb78ba8f4a680cb6084341036d29fdbfe2b9cb6a5c826eb", hash)
        val started = System.nanoTime()
        val curve = Pcm16WavReader(file).use { DroneComponentAnalysis().analyze(it) }
        println("Drone component 15:27 analysis: ${(System.nanoTime()-started)/1e9}s, ${curve.samples.size} windows")
        System.getenv("DIDGE_COMPONENT_OUTPUT")?.let {output ->
            File(output).writeText(buildString {
                appendLine("center_s,component_hz,raw_candidate_hz,rms,reason")
                for(sample in curve.samples)appendLine("${sample.timeMs/1000},${sample.hz ?: ""},${sample.candidateHz ?: ""},${sample.rms},${sample.reason}")
            })
        }
        assertEquals(214, curve.samples.size)
        val segments = listOf(
            Triple(1.6 to 6.8, 71.215, 25),
            Triple(8.4 to 13.8, 66.681, 26),
            Triple(16.4 to 24.5, 65.711, 36),
            Triple(26.4 to 35.2, 66.594, 18),
        )
        for ((range, reference, minimum) in segments) {
            val accepted = curve.samples.filter { it.timeMs / 1000 in range.first..range.second }
                .mapNotNull { it.hz }.sorted()
            assertTrue("Insufficient supported components in $range: ${accepted.size}", accepted.size >= minimum)
            assertEquals(reference, accepted[accepted.size / 2], 1.5)
        }
    }

    @Test fun knownOctaveAmbiguityRemainsAnIndependentComponentNotANewFundamental() {
        val file = System.getenv("DIDGE_OCT21_1917_WAV")?.let(::File)
        assumeTrue("Private WAV is opt-in", file?.isFile == true)
        val hash=MessageDigest.getInstance("SHA-256").digest(file!!.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals("7e877517a57bff4f5222d8d73dbc54494768fc208e14e24530ae65c9ca13eeb5",hash)
        val curve=Pcm16WavReader(file).use {DroneComponentAnalysis().analyze(it)}
        fun accepted(start:Double,end:Double)=curve.samples.filter {it.timeMs/1000 in start..end}
            .mapNotNull {it.hz}.sorted()
        val normal=accepted(3.0,8.0)
        val vocalization=accepted(9.0,17.0)
        assertTrue(normal.size>=20)
        assertTrue(vocalization.size>=35)
        assertEquals(68.93,normal[normal.size/2],2.0)
        assertEquals(65.56,vocalization[vocalization.size/2],2.0)
    }

    @Test fun additionalPrivateOctober23SessionsMatchTheFrozenComponentReference() {
        val first=System.getenv("DIDGE_OCT23_1017_WAV")?.let(::File)
        val second=System.getenv("DIDGE_OCT23_1018_WAV")?.let(::File)
        assumeTrue("Private WAVs are opt-in",first?.isFile==true && second?.isFile==true)
        val checks=listOf(
            Triple(first!!,listOf(Triple(2.5 to 9.3,70.20,35)),37),
            Triple(second!!,listOf(
                Triple(3.0 to 7.0,69.28,18),
                Triple(12.0 to 17.0,66.87,24),
                Triple(23.0 to 28.0,66.25,20),
                Triple(34.0 to 38.0,65.97,18)),233),
        )
        for((file,segments,minimumFrames) in checks) {
            val curve=Pcm16WavReader(file).use {DroneComponentAnalysis().analyze(it)}
            assertTrue(curve.samples.size>=minimumFrames)
            for((range,reference,minimum) in segments){
                val values=curve.samples.filter {it.timeMs/1000 in range.first..range.second}
                    .mapNotNull {it.hz}.sorted()
                assertTrue("${file.name} $range: ${values.size}",values.size>=minimum)
                assertEquals(reference,values[values.size/2],1.5)
            }
        }
    }

    @Test fun privateKotlinFramesMatchFrozenPythonComponentDecisionsWhenAvailable() {
        val wav=System.getenv("DIDGE_COMPONENT_WAV")?.let(::File)
        val csv=System.getenv("DIDGE_COMPONENT_REFERENCE")?.let(::File)
        assumeTrue("Private aligned comparison is opt-in",wav?.isFile==true && csv?.isFile==true)
        val curve=Pcm16WavReader(wav!!).use {DroneComponentAnalysis().analyze(it)}
        val expected=csv!!.readLines().drop(1).map {line ->
            val fields=line.split(',')
            fields[1].toDoubleOrNull()
        }
        assertEquals(expected.size,curve.samples.size)
        var both=0;var extra=0;var missing=0;var maxCents=0.0
        for((reference,sample) in expected.zip(curve.samples)){
            val actual=sample.hz
            when {
                reference==null && actual!=null -> extra++
                reference!=null && actual==null -> missing++
                reference!=null && actual!=null -> {
                    both++
                    maxCents=max(maxCents,abs(1200*log2(actual/reference)))
                }
            }
        }
        println("Kotlin/Python private frame comparison: both=$both, Kotlin-only=$extra, Python-only=$missing, max cents=$maxCents")
        assertTrue(both>=100)
        assertTrue(extra<=8)
        assertTrue(missing<=8)
        assertTrue(maxCents<50)
    }
}
