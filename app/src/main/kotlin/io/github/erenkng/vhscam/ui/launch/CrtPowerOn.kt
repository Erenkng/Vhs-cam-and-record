package io.github.erenkng.vhscam.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Material's emphasized decelerate curve: a fast start that settles gently. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * The picture tube warming up: a bright line across the middle stretches out, opens into the
 * picture and fades from white. It is drawn over the app as a black curtain that pulls away,
 * so even the camera's own surface underneath is covered.
 */
@Composable
fun CrtPowerOn(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val currentOnFinished = rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(DURATION_MILLIS, easing = LinearEasing))
        currentOnFinished.value()
    }
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        val stretch = FastOutSlowInEasing.transform((p / 0.3f).coerceIn(0f, 1f))
        val open = EmphasizedDecelerate.transform(((p - 0.24f) / 0.56f).coerceIn(0f, 1f))
        val glow = 1f - ((p - 0.24f) / 0.76f).coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        val bandWidth = w * stretch
        val line = 2.dp.toPx()
        val bandHeight = line + (h - line) * open
        val top = (h - bandHeight) / 2f
        val bottom = top + bandHeight
        val left = (w - bandWidth) / 2f
        val right = left + bandWidth
        // Black everywhere the picture hasn't reached yet.
        drawRect(Color.Black, size = Size(w, top))
        drawRect(Color.Black, topLeft = Offset(0f, bottom), size = Size(w, h - bottom))
        drawRect(Color.Black, topLeft = Offset(0f, top), size = Size(left, bandHeight))
        drawRect(Color.Black, topLeft = Offset(right, top), size = Size(w - right, bandHeight))
        // The phosphor flares white, then clears to the picture.
        drawRect(Color.White.copy(alpha = 0.95f * glow), topLeft = Offset(left, top), size = Size(bandWidth, bandHeight))
        // A soft bloom around the line while it is still thin.
        val bloom = 28.dp.toPx() * (1f - open)
        if (bloom > 0.5f) {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.4f * glow), Color.Transparent),
                    startY = top - bloom,
                    endY = bottom + bloom,
                ),
                topLeft = Offset(left, top - bloom),
                size = Size(bandWidth, bandHeight + 2 * bloom),
            )
        }
    }
}

private const val DURATION_MILLIS = 760
