package ar.didgetaller.ui

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.*

/** Dedicated plot surface: pan in both axes; no enclosing scroll or gesture Back handler. */
@Composable
internal fun Modifier.immersiveGestures(onTap:(Offset)->Unit,onInspect:(Offset)->Unit,onReset:()->Unit,
    onActive:(Boolean)->Unit,onTransform:(Offset,Offset,Float,Float)->Unit):Modifier {
    val tap by rememberUpdatedState(onTap);val inspect by rememberUpdatedState(onInspect)
    val reset by rememberUpdatedState(onReset);val active by rememberUpdatedState(onActive)
    val transform by rememberUpdatedState(onTransform)
    return pointerInput(Unit){
        var lastTapTime=0L;var lastTap=Offset.Unspecified
        awaitEachGesture {
            val first=awaitFirstDown();var moved=false;var inspecting=false;var point=first.position
            var pressed=true
            try {
                while(pressed){
                    val event=if(!moved && !inspecting)withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis){awaitPointerEvent()} else awaitPointerEvent()
                    if(event==null){inspecting=true;active(true);inspect(point);continue}
                    val down=event.changes.filter {it.pressed};pressed=down.isNotEmpty()
                    if(!pressed){event.changes.forEach {it.consume()};break}
                    point=event.calculateCentroid();val displacement=point-first.position
                    if(down.size>1 || displacement.getDistance()>viewConfiguration.touchSlop)moved=true
                    if(inspecting){inspect(point);event.changes.forEach {it.consume()}}
                    else if(moved){
                        active(true)
                        val pan=event.calculatePan()
                        // Radial centroid-size ratio is continuous for every finger angle.
                        // Axis separation becomes singular near horizontal/vertical alignment.
                        val zoom=event.calculateZoom().takeIf {it.isFinite() && it>0f} ?: 1f
                        transform(point,pan,zoom,zoom);event.changes.forEach {it.consume()}
                    }
                }
                if(!moved && !inspecting){
                    val now=first.uptimeMillis
                    if(lastTapTime!=0L && now-lastTapTime in 1..viewConfiguration.doubleTapTimeoutMillis && (point-lastTap).getDistance()<viewConfiguration.touchSlop*3){reset();lastTapTime=0}
                    else {tap(point);lastTapTime=now;lastTap=point}
                }
            }finally {active(false)}
        }
    }
}
