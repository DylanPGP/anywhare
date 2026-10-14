package com.dylanpgp.anywhare.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dylanpgp.anywhare.R

/** Joystick circular. Emite x (este +) e y (norte +) en el rango [-1, 1]. */
@Composable
fun Joystick(onMove: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    val currentOnMove by rememberUpdatedState(onMove)
    var knob by remember { mutableStateOf(Offset.Zero) }
    val track = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val knobColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface

    DisposableEffect(Unit) {
        onDispose { currentOnMove(0f, 0f) }
    }

    Box(modifier.size(160.dp)) {
        Canvas(
            Modifier
                .size(160.dp)
                .pointerInput(Unit) {
                    val radius = size.width / 2f
                    val maxR = radius * 0.62f
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(pos: Offset) {
                            val v = pos - Offset(radius, radius)
                            val len = v.getDistance()
                            val clamped = if (len > maxR) v * (maxR / len) else v
                            knob = clamped
                            currentOnMove(clamped.x / maxR, -clamped.y / maxR)
                        }
                        update(down.position)
                        drag(down.id) { change ->
                            change.consume()
                            update(change.position)
                        }
                        knob = Offset.Zero
                        currentOnMove(0f, 0f)
                    }
                },
        ) {
            val r = size.minDimension / 2f
            drawCircle(color = track, radius = r)
            drawCircle(color = outline, radius = r - 1.dp.toPx(), style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(color = knobColor, radius = r * 0.30f, center = center + knob)
        }
        val pad = Modifier.padding(6.dp)
        Text(stringResource(R.string.dir_n), pad.align(Alignment.TopCenter), color = labelColor, fontSize = 12.sp)
        Text(stringResource(R.string.dir_s), pad.align(Alignment.BottomCenter), color = labelColor, fontSize = 12.sp)
        Text(stringResource(R.string.dir_w), pad.align(Alignment.CenterStart), color = labelColor, fontSize = 12.sp)
        Text(stringResource(R.string.dir_e), pad.align(Alignment.CenterEnd), color = labelColor, fontSize = 12.sp)
    }
}
