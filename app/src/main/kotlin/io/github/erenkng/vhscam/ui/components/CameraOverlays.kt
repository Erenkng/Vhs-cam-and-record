package io.github.erenkng.vhscam.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.ui.theme.CameraColors

/** Rule-of-thirds grid. */
@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.35f)
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = size.height * i / 3f
            drawLine(color, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = stroke)
            drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = stroke)
        }
    }
}

/** REC pill with a blinking dot and the running time in tabular figures. */
@Composable
fun RecordingBadge(elapsedMillis: Long, paused: Boolean, pausedLabel: String, modifier: Modifier = Modifier) {
    val blink = rememberInfiniteTransition(label = "blink")
    val dotAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "dot",
    )
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = CameraColors.RecordContainer.copy(alpha = 0.92f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .alpha(if (paused) 1f else dotAlpha)
                    .background(if (paused) Color.White else CameraColors.Record, CircleShape),
            )
            Text(
                text = if (paused) pausedLabel else formatElapsed(elapsedMillis),
                style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

fun formatElapsed(millis: Long): String {
    val total = millis / 1000
    val h = total / 3600
    val m = (total / 60) % 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Big self-timer digits on a spinning expressive shape. */
@Composable
fun CountdownOverlay(seconds: Int?, modifier: Modifier = Modifier) {
    val spin = rememberInfiniteTransition(label = "countdown")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(6_000)), label = "angle")
    AnimatedContent(
        targetState = seconds,
        modifier = modifier,
        transitionSpec = {
            (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 1.3f) + fadeOut())
        },
        label = "countdown",
    ) { value ->
        if (value != null) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(168.dp)
                        .rotate(angle)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f), MaterialShapes.Cookie9Sided.toShape()),
                )
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.displayLargeEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** "Developing…" / "Recording to tape…" pill with an expressive loading indicator. */
@Composable
fun BusyPill(label: String, progress: Float?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContainedLoadingIndicator(modifier = Modifier.size(36.dp))
            Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
            if (progress != null) {
                LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.width(64.dp))
            }
        }
    }
}
