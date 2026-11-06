package ar.didgetaller.ui

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.*
import ar.didgetaller.core.PlotTouch
import ar.didgetaller.core.plotTouch

/** One finger scrubs temporal plots; two fingers own viewport transforms. Vertical drags scroll. */
@Composable
internal fun Modifier.immersiveGestures(onScrubStart:(Offset)->Boolean={false},
    onScrub:(Offset,Boolean)->Unit={_,_->},onTap:(Offset)->Unit,onInspect:(Offset)->Unit,onReset:()->Unit,
    onActive:(Boolean)->Unit,onTransform:(Offset,Offset,Float,Float)->Unit):Modifier {
    val startScrub by rememberUpdatedState(onScrubStart);val scrub by rememberUpdatedState(onScrub)
    val tap by rememberUpdatedState(onTap);val inspect by rememberUpdatedState(onInspect)
    val reset by rememberUpdatedState(onReset);val active by rememberUpdatedState(onActive)
    val transform by rememberUpdatedState(onTransform)
    return pointerInput(Unit){
        var lastTapTime=0L;var lastTap=Offset.Unspecified
        awaitEachGesture {
            val first=awaitFirstDown();var moved=false;var inspecting=false;var point=first.position
            val canScrub=startScrub(point)
            var scrubbing=false
            var transforming=false
            var released=false
            var pressed=true
            try {
                while(pressed){
                    val event=if(!scrubbing && !moved && !inspecting)withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis){awaitPointerEvent()} else awaitPointerEvent()
                    if(event==null){inspecting=true;active(true);inspect(point);continue}
                    val down=event.changes.filter {it.pressed};pressed=down.isNotEmpty()
                    if(!pressed){
                        released=event.changes.none {it.isConsumed}
                        if(scrubbing && released)scrub(point,true)
                        event.changes.forEach {it.consume()};break
                    }
                    if(down.size>1){if(scrubbing)active(false);scrubbing=false;inspecting=false;transforming=true}
                    point=event.calculateCentroid();val displacement=point-first.position
                    if(!transforming && !scrubbing && !inspecting){
                        when(plotTouch(displacement.x,displacement.y,viewConfiguration.touchSlop,down.size,canScrub)){
                            PlotTouch.SCRUB -> scrubbing=true
                            PlotTouch.VIEWPORT -> transforming=true
                            PlotTouch.SCROLL -> {moved=true;break}
                            PlotTouch.WAIT -> Unit
                        }
                    }
                    if(transforming || scrubbing)moved=true
                    if(scrubbing){active(true);scrub(point,false);event.changes.forEach {it.consume()}}
                    else if(inspecting){inspect(point);event.changes.forEach {it.consume()}}
                    else if(transforming && down.size>1 || transforming && !canScrub){
                        active(true)
                        val pan=event.calculatePan()
                        // Radial centroid-size ratio is continuous for every finger angle.
                        // Axis separation becomes singular near horizontal/vertical alignment.
                        val zoom=event.calculateZoom().takeIf {it.isFinite() && it>0f} ?: 1f
                        transform(point,pan,zoom,zoom);event.changes.forEach {it.consume()}
                    }
                }
                if(released && !scrubbing && !moved && !inspecting){
                    val now=first.uptimeMillis
                    if(lastTapTime!=0L && now-lastTapTime in 1..viewConfiguration.doubleTapTimeoutMillis && (point-lastTap).getDistance()<viewConfiguration.touchSlop*3){reset();lastTapTime=0}
                    else {tap(point);lastTapTime=now;lastTap=point}
                }
            }finally {active(false)}
        }
    }
}
