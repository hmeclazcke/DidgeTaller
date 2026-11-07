package ar.didgetaller.core

import org.junit.Assert.*
import org.junit.Test

class WavEncodingBoundaryTest {
    @Test fun invalidSampleRateAndCountAreRejectedBeforeEncoding() {
        for(rate in listOf(0,-1)) assertThrows(IllegalArgumentException::class.java) {WavCodec.encode(shortArrayOf(1),1,rate)}
        for(count in listOf(-1,2)) assertThrows(IllegalArgumentException::class.java) {WavCodec.encode(shortArrayOf(1),count,44100)}
    }
    @Test fun emptyHeaderIsAValidProgressiveCaptureStartingPoint() {
        val header=WavCodec.encode(shortArrayOf(),0,44100)
        assertEquals(44,header.size);assertEquals("RIFF",String(header,0,4,Charsets.US_ASCII))
        assertEquals(36,header[4].toInt());assertEquals(0,header[40].toInt())
    }
}
