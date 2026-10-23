package ar.didgetaller.core

/** SPTK SWIPE' with the documented boundary fix, bounded and serialized. No Android audio access. */
internal object SwipeNative {
    init { System.loadLibrary("swipe") }
    external fun estimate(pcm:ShortArray,count:Int,sampleRate:Int,hop:Int,minHz:Int,maxHz:Int):DoubleArray
    external fun peakBytes():Long
}
