package ar.didgetaller.core

/** Sequential purple/blue/teal/yellow anchors; magnitude only, never confidence. */
object SpectralPalette {
    private val colors=intArrayOf(0xFF440154.toInt(),0xFF3B528B.toInt(),0xFF21918C.toInt(),0xFF5EC962.toInt(),0xFFFDE725.toInt())
    fun color(relativeDb:Double?,rangeDb:Double=70.0):Int {
        require(rangeDb>0)
        if(relativeDb==null || !relativeDb.isFinite())return 0xFF101C25.toInt()
        val at=((relativeDb+rangeDb)/rangeDb).coerceIn(0.0,1.0)*(colors.size-1)
        val index=at.toInt().coerceAtMost(colors.size-2);val mix=at-index
        fun channel(shift:Int)=(((colors[index] ushr shift) and 255)*(1-mix)+((colors[index+1] ushr shift) and 255)*mix).toInt()
        return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
