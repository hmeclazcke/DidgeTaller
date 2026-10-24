package ar.didgetaller.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object ViewerIcons {
    val Expand=ImageVector.Builder("Expand",24.dp,24.dp,24f,24f).apply {
        path(fill=SolidColor(Color.White)){
            moveTo(3f,3f);lineTo(9f,3f);lineTo(9f,5f);lineTo(5f,5f);lineTo(5f,9f);lineTo(3f,9f);close()
            moveTo(15f,3f);lineTo(21f,3f);lineTo(21f,9f);lineTo(19f,9f);lineTo(19f,5f);lineTo(15f,5f);close()
            moveTo(3f,15f);lineTo(5f,15f);lineTo(5f,19f);lineTo(9f,19f);lineTo(9f,21f);lineTo(3f,21f);close()
            moveTo(19f,15f);lineTo(21f,15f);lineTo(21f,21f);lineTo(15f,21f);lineTo(15f,19f);lineTo(19f,19f);close()
        }
    }.build()
    val Pause=ImageVector.Builder("Pause",24.dp,24.dp,24f,24f).apply {
        path(fill=SolidColor(Color.White)){
            moveTo(6f,4f);lineTo(10f,4f);lineTo(10f,20f);lineTo(6f,20f);close()
            moveTo(14f,4f);lineTo(18f,4f);lineTo(18f,20f);lineTo(14f,20f);close()
        }
    }.build()
    val Rotate=ImageVector.Builder("Rotate",24.dp,24.dp,24f,24f).apply {
        path(fill=SolidColor(Color.White)){
            moveTo(7f,2f);lineTo(16f,2f);lineTo(16f,18f);lineTo(7f,18f);close()
            moveTo(9f,4f);lineTo(9f,16f);lineTo(14f,16f);lineTo(14f,4f);close()
            moveTo(19f,9f);lineTo(23f,13f);lineTo(19f,17f);lineTo(19f,14f)
            lineTo(18f,14f);lineTo(18f,12f);lineTo(19f,12f);close()
            moveTo(4f,15f);lineTo(4f,20f);lineTo(14f,20f);lineTo(14f,22f);lineTo(2f,22f);lineTo(2f,15f);close()
        }
    }.build()
}
