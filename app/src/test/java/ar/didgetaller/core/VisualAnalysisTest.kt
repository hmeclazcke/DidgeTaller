package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CancellationException
import kotlin.math.*

class VisualAnalysisTest {
    private fun source(seconds:Double=3.0,rate:Int=44100,signal:(Long)->Double) = object:MonoPcmSource {
        override val sampleRate=rate
        override val frameCount=(seconds*rate).toLong()
        override fun read(startFrame:Long,destination:ShortArray):Int {
            val count=min(destination.size.toLong(),frameCount-startFrame).toInt()
            destination.fill(0);repeat(count){destination[it]=signal(startFrame+it).roundToInt().coerceIn(-32768,32767).toShort()};return count
        }
    }
    @Test fun waveformPreservesExtremaAndExactSamplesWhenZoomed() {
        val signal=source(1.0){when(it){123L->32767.0;124L-> -32768.0;else->0.0}}
        val wave=AudioVisuals.waveform(signal,0,44100,100)
        assertEquals(100,wave.min.size);assertFalse(wave.samples)
        assertEquals(-1.0,wave.min[0].toDouble(),0.0);assertEquals(32767/32768.0,wave.max[0].toDouble(),0.0)
        val zoom=AudioVisuals.waveform(signal,120,127,100)
        assertTrue(zoom.samples);assertEquals(7,zoom.min.size);assertEquals(32767/32768.0,zoom.min[3].toDouble(),0.0)
        assertArrayEquals(zoom.min,zoom.max,0f)
    }
    @Test fun stftTracksActualNoteChangeAndSampleRate() {
        val rate=48000;val signal=source(4.0,rate){i ->9000*sin(2*PI*(if(i<2*rate)65.41 else 73.42)*i/rate)}
        val tile=AudioVisuals.spectrogram(signal,0,signal.frameCount,highHz=500.0,rows=256)
        fun peak(col:Int)= (0 until tile.rows).maxBy {tile.power[col*tile.rows+it]}.let {tile.frequency((it+.5)/tile.rows)}
        assertEquals(65.41,peak(1),3.0);assertEquals(73.42,peak(tile.columns-2),3.0)
        assertEquals(rate,tile.rate);assertEquals(32768,tile.fftSize);assertEquals(16384,tile.hopSize)
        assertEquals(0,tile.start);assertEquals(192000,tile.end)
        assertTrue(tile.power.all {it.isFinite() && it>=0});assertTrue(tile.reference>0)
    }
    @Test fun stftHarmonicsAndRelativePowerAreReal() {
        val signal=source {i ->1000*sin(2*PI*70*i/44100)+9000*sin(2*PI*140*i/44100)+6000*sin(2*PI*210*i/44100)}
        val tile=AudioVisuals.spectrogram(signal,0,signal.frameCount,rows=256)
        fun db(hz:Double):Double {val row=(hz/500*tile.rows).toInt();return (row-1..row+1).maxOf {tile.relativeDb(1,it)!!}}
        assertTrue(db(140.0)>db(70.0)+12);assertEquals(20*log10(6000.0/9000),db(210.0)-db(140.0),3.0)
    }
    @Test fun stftSilenceAndLogarithmicCoordinates() {
        val silent=source {0.0}
        val tile=AudioVisuals.spectrogram(silent,0,silent.frameCount,logarithmic=true,highHz=2000.0)
        assertTrue(tile.lowHz>0);assertEquals(tile.lowHz,tile.frequency(0.0),1e-10);assertEquals(2000.0,tile.frequency(1.0),1e-9)
        assertEquals(sqrt(tile.lowHz*tile.highHz),tile.frequency(.5),1e-8)
        assertNull(tile.relativeDb(0,0));assertTrue(tile.power.all {it==0f})
    }
    @Test fun longWaveformHasBoundedDisplayMemory() {
        val signal=source(900.0){i ->if(i==39_000_000L)32767.0 else 0.0}
        val wave=AudioVisuals.waveform(signal,0,signal.frameCount)
        assertEquals(1024,wave.min.size);assertTrue(wave.max.any {it>0.99});assertEquals(39_690_000L,wave.end)
    }
    @Test fun fifteenMinuteStftKeepsOneBoundedViewport() {
        val signal=source(900.0){0.0};val started=System.nanoTime()
        val tile=AudioVisuals.spectrogram(signal,0,signal.frameCount,maxColumns=256,rows=256)
        assertTrue(tile.columns<=256);assertEquals(256,tile.rows);assertTrue(tile.power.size<=65536)
        assertEquals(39_690_000L,tile.end);assertNull(tile.relativeDb(0,0))
        println("STFT 900 s: ${(System.nanoTime()-started)/1_000_000} ms JVM; ${tile.power.size*4} bytes de celdas; no valida batería/teléfono.")
    }
    @Test fun transformsCancelWithoutReturningPartialResults() {
        val signal=source {i ->sin(i.toDouble())}
        var checks=0
        try {AudioVisuals.spectrogram(signal,0,signal.frameCount,cancelled={++checks>2});fail("Debe cancelar")}catch(_:CancellationException){}
        checks=0
        try {AudioVisuals.waveform(signal,0,signal.frameCount,cancelled={++checks>1});fail("Debe cancelar")}catch(_:CancellationException){}
    }
    @Test fun streamingWriterSupportsFifteenMinutesAndHeaderLength() {
        val file=File.createTempFile("stream-15min",".part")
        try {
            ProgressiveWavWriter(file).use {writer ->
                val oneSecond=ShortArray(44100){1234}
                repeat(900){writer.append(oneSecond,oneSecond.size)}
                assertEquals(39_690_000L,writer.samplesWritten)
            }
            assertEquals(79_380_044L,file.length())
            Pcm16WavReader(file).use {reader ->assertEquals(900_000L,reader.frameCount*1000/reader.sampleRate);val end=ShortArray(16);assertEquals(16,reader.read(reader.frameCount-16,end));assertTrue(end.all {it==1234.toShort()})}
        } finally {file.delete()}
    }
    @Test fun streamingWriterKeepsCompletedAudioOnLowSpace() {
        val file=File.createTempFile("stream-space",".part");var available=10_000_000L
        try {ProgressiveWavWriter(file,freeBytes={available}).use {writer ->writer.append(shortArrayOf(123,456),2);available=0
            try {writer.append(shortArrayOf(789),1);fail("Debe informar espacio insuficiente")}catch(_:IllegalStateException){}
        };Pcm16WavReader(file).use {reader ->assertEquals(2,reader.frameCount)}}finally {file.delete()}
    }
    @Test fun repairOnlyChangesPartialHeaderAndPreservesPcm() {
        val file=File.createTempFile("stream-repair",".part")
        try {file.writeBytes(WavCodec.encode(shortArrayOf(10,20,30),3,44100))
            RandomAccessFile(file,"rw").use {it.seek(4);it.writeInt(0);it.seek(40);it.writeInt(0);it.seek(it.length());it.write(7)}
            assertTrue(ProgressiveWavWriter.repair(file));assertEquals(50,file.length())
            Pcm16WavReader(file).use {reader ->val pcm=ShortArray(3);reader.read(0,pcm);assertArrayEquals(shortArrayOf(10,20,30),pcm)}
        }finally {file.delete()}
    }
    @Test fun paletteIsOrderedAndSilenceHasDistinctBackground() {
        val dark=SpectralPalette.color(null);assertEquals(dark,SpectralPalette.color(Double.NaN))
        assertNotEquals(SpectralPalette.color(-70.0),SpectralPalette.color(0.0))
        assertEquals(SpectralPalette.color(-70.0),SpectralPalette.color(-120.0))
        assertEquals(SpectralPalette.color(0.0),SpectralPalette.color(30.0))
    }
}
