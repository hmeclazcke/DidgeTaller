package ar.didgetaller.core

/** Exact SPTK SWIPE' reference, bounded and serialized by the native adapter. No Android audio access. */
internal object SwipeNative {
    init { System.loadLibrary("swipe") }
    external fun estimate(pcm:ShortArray,count:Int,sampleRate:Int,hop:Int,minHz:Int,maxHz:Int):DoubleArray
    external fun peakBytes():Long
}
