package ar.didgetaller.audio

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File

/** Position always comes from MediaPlayer; the polling task only enforces the loop boundary. */
class SessionPlayer(private val changed:(String?,Boolean)->Unit,private val error:(String)->Unit) {
    private val handler=Handler(Looper.getMainLooper())
    private var player:MediaPlayer?=null
    var id:String?=null;private set
    var paused=true;private set
    var ready=false;private set
    var loop:LongRange?=null;private set
    private var requested=0L
    private var pendingId:String?=null
    private var playWhenReady=false
    private var seeking=false
    private var queuedSeek:Long?=null
    private fun requestSeek(target:Long){
        if(seeking){queuedSeek=target;return}
        seeking=true
        player?.seekTo(target,MediaPlayer.SEEK_CLOSEST)
    }
    private val poll=object:Runnable {override fun run(){
        val current=player
        if(current!=null && ready){runCatching {val region=loop;if(!seeking && region!=null && current.isPlaying && current.currentPosition>=region.last)requestSeek(region.first)}.onFailure {fail(it)}
            if(player!=null)handler.postDelayed(this,40)}
    }}
    private fun fail(e:Throwable){stop();error("No se pudo reproducir: ${e.message}")}
    fun open(sessionId:String,file:File,play:Boolean,position:Long=0) {
        stop();requested=position;pendingId=sessionId;playWhenReady=play;val candidate=MediaPlayer();player=candidate
        runCatching {
            candidate.setDataSource(file.absolutePath)
            candidate.setOnSeekCompleteListener {current ->if(player===current){
                seeking=false;val next=queuedSeek;queuedSeek=null
                if(next!=null)runCatching {requestSeek(next)}.onFailure {fail(it)}
            }}
            candidate.setOnPreparedListener {current ->if(player===current){
                ready=true;id=sessionId;paused=!playWhenReady
                if(requested>0)requestSeek(requested.coerceAtMost(current.duration.toLong()))
                if(playWhenReady)current.start();changed(id,paused);handler.post(poll)
            }}
            candidate.setOnCompletionListener {current ->if(player===current){
                val region=loop
                if(region!=null){requestSeek(region.first);current.start()}
                else {paused=true;changed(id,true)}
            }}
            candidate.setOnErrorListener {_,what,extra ->fail(IllegalStateException("Audio $what/$extra"));true}
            candidate.prepareAsync()
        }.onFailure {fail(it)}
    }
    fun toggle(sessionId:String,file:File){
        if(!ready && pendingId==sessionId){playWhenReady=!playWhenReady;return}
        val current=player
        if(id!=sessionId || !ready || current==null){open(sessionId,file,true,requested);return}
        runCatching {if(current.isPlaying){current.pause();paused=true}else{if(current.currentPosition>=current.duration-20)requestSeek(loop?.first ?: 0L);current.start();paused=false};changed(id,paused)}.onFailure {fail(it)}
    }
    fun seek(sessionId:String,file:File,position:Long){
        val region=loop.takeIf {id==sessionId || pendingId==sessionId}
        val target=region?.let {position.coerceIn(it.first,it.last)} ?: position.coerceAtLeast(0)
        requested=target
        if(!ready && pendingId==sessionId)return
        if(id!=sessionId || !ready){open(sessionId,file,false,target);return}
        runCatching {requestSeek(target.coerceIn(0,duration().toLong()))}.onFailure {fail(it)}
    }
    fun loop(region:LongRange?){require(region==null || region.last>region.first);loop=region
        if(region!=null && ready && position().toLong() !in region)runCatching {requestSeek(region.first)}.onFailure {fail(it)}}
    fun position():Int=if(ready)runCatching {player?.currentPosition ?: 0}.getOrDefault(0) else requested.toInt()
    fun duration():Int=if(ready)runCatching {player?.duration ?: 0}.getOrDefault(0) else 0
    fun restart(sessionId:String,file:File){if(id!=sessionId || !ready)open(sessionId,file,true) else {seek(sessionId,file,0);if(paused)toggle(sessionId,file)}}
    fun stop(){handler.removeCallbacks(poll);val old=player;player=null;id=null;pendingId=null;playWhenReady=false;seeking=false;queuedSeek=null;ready=false;paused=true;requested=0;loop=null
        old?.let {runCatching {it.stop()};runCatching {it.release()}};changed(null,false)}
}
