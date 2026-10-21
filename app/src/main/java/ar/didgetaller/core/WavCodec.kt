package ar.didgetaller.core

/** Header-only PCM16 WAV encoder. No transcoding, stores raw microphone audio locally. */
object WavCodec {
    fun encode(samples: ShortArray, count: Int, sampleRate: Int): ByteArray {
        require(count in 0..samples.size && sampleRate > 0)
        val dataSize = count * 2
        val out = ByteArray(44 + dataSize)
        fun ascii(offset: Int, s: String) { s.forEachIndexed { i, ch -> out[offset+i] = ch.code.toByte() } }
        fun le16(offset: Int, value: Int) { out[offset]=value.toByte(); out[offset+1]=(value ushr 8).toByte() }
        fun le32(offset: Int, value: Int) { repeat(4) { out[offset+it]=(value ushr (8*it)).toByte() } }
        ascii(0,"RIFF"); le32(4,36+dataSize); ascii(8,"WAVE"); ascii(12,"fmt ")
        le32(16,16); le16(20,1); le16(22,1); le32(24,sampleRate); le32(28,sampleRate*2)
        le16(32,2); le16(34,16); ascii(36,"data"); le32(40,dataSize)
        repeat(count) { val v=samples[it].toInt(); le16(44+it*2,v) }
        return out
    }
}
