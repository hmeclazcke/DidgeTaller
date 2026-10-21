package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class CoreTest {
    @Test fun notesAndCents() {
        assertEquals("Re · D2",Music.note(73.416))
        assertTrue(abs(Music.cents(73.416))<=1)
        assertEquals("—",Music.note(0.0))
        assertEquals(440.0,Music.frequency(69),0.001)
    }
    @Test fun pitchDetection() {
        val rate=44100
        for(target in listOf(50.0,65.4,73.416,82.407,98.0,125.0)) {
            val signal=ShortArray(16384){idx ->
                (12000*sin(2*PI*target*idx/rate) + 4300*sin(4*PI*target*idx/rate) +
                 1600*sin(6*PI*target*idx/rate)).toInt().toShort()
            }
            val result=PitchDetector.analyze(signal,rate,35,140)
            assertEquals("Pitch $target",target,result.hz,1.5)
            assertTrue(result.confidence>0.7)
            assertEquals(160,result.spectrum.size)
        }
    }
    @Test fun missingFundamental() {
        val frequency=73.416
        val pcm=ShortArray(16384) {i ->
            (11000*sin(4*PI*frequency*i/44100)+7000*sin(6*PI*frequency*i/44100)).toInt().toShort()
        }
        val r=PitchDetector.analyze(pcm)
        assertEquals(frequency,r.hz,1.5)
    }
    @Test fun geometricPresetsAndSimulation(){
        val pvc=AcousticModel.Shape(1500,40,40,40)
        assertEquals(40.0,pvc.diameter(700),0.001)
        val result=AcousticModel.evaluate(pvc)
        assertTrue(result.baseHz in 30.0..200.0)
        assertEquals(621,result.impedance.size)
        assertTrue(pvc.csv().startsWith("distancia_mm,diametro_interior_mm"))
    }
    @Test fun profileByArbitraryPoints() {
        val source=AcousticModel.Shape(1500,32,43,85)
        val shape=source.withControlPoints(listOf(
            AcousticModel.BorePoint(0,32.0),
            AcousticModel.BorePoint(400,34.0),
            AcousticModel.BorePoint(900,60.0),
            AcousticModel.BorePoint(1500,95.0)
        ))
        assertEquals(47.0,shape.diameter(650),0.00001)
        assertEquals(4,shape.points.size)
        assertEquals(95.0,shape.diameter(1500),0.00001)
        assertTrue(AcousticModel.evaluate(shape).baseHz > 0)
    }
    @Test fun evolutionImprovesFitnessAndCanCancel() {
        val base=AcousticModel.Shape(1500,32,60,95)
        var initial=Double.POSITIVE_INFINITY
        val final=BoreOptimizer.search(base,BoreOptimizer.Target(),
            BoreOptimizer.Settings(8,10),
            progress={gen,candidate -> if(gen==1)initial=candidate.fitness }
        )!!
        assertTrue(final.fitness<=initial)
        assertEquals(6,final.shape.controlPoints().size)
        assertNull(BoreOptimizer.search(base,BoreOptimizer.Target(),
            BoreOptimizer.Settings(2,4),cancelled={true}))
    }
    @Test fun wavHeader() {
        val bytes=WavCodec.encode(shortArrayOf(0,1000,-1000),3,44100)
        assertEquals(50,bytes.size)
        assertEquals("RIFF",String(bytes,0,4))
        assertEquals("WAVE",String(bytes,8,4))
        assertEquals("data",String(bytes,36,4))
    }
    @Test fun sessionUsesPredominantNoteRatherThanLastReading() {
        val points=(0..9).map { SessionAnalysis.Point(it*200L,66.0+it%3*0.2,0.94) }+
            listOf(SessionAnalysis.Point(2200,132.0,0.96),SessionAnalysis.Point(2400,null,0.0),
                SessionAnalysis.Point(2600,180.0,0.25))
        val summary=SessionAnalysis.summarize(points)!!
        assertEquals(66.2,summary.typicalHz,0.3)
        assertTrue(summary.lowHz>=66.0 && summary.highHz<=66.4)
        assertEquals(10,summary.validPoints)
        assertEquals(points.size,summary.totalPoints)
        assertTrue(summary.stabilityPercent>=90)
    }
    @Test fun sessionDoesNotInventNoteFromSilenceOrMixedSignals() {
        assertNull(SessionAnalysis.summarize(listOf(
            SessionAnalysis.Point(100,null,0.0),SessionAnalysis.Point(200,66.0,0.3),
            SessionAnalysis.Point(300,66.1,0.9))))
        assertNull(SessionAnalysis.summarize(listOf(66.0,66.1,98.0,98.1).mapIndexed {index,hz ->
            SessionAnalysis.Point(index*100L,hz,0.9)
        }))
    }
    @Test fun sessionStabilityReflectsPitchVariation() {
        val steady=(0..8).map {SessionAnalysis.Point(it*200L,73.4,0.95)}
        val varying=listOf(71.0,72.0,73.4,74.2,75.0,72.1,74.6,73.1,75.1)
            .mapIndexed {i,hz ->SessionAnalysis.Point(i*200L,hz,0.95)}
        assertEquals(100,SessionAnalysis.summarize(steady)?.stabilityPercent)
        assertTrue((SessionAnalysis.summarize(varying)?.stabilityPercent ?: 100)<100)
    }
}
