package io.github.erenkng.vhscam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import io.github.erenkng.vhscam.ui.theme.CameraColors

/**
 * The big capture button. Photo: a white disc that squishes into a cookie shape when pressed.
 * Video: a red disc that morphs into a rounded "stop" square while recording, inside a slowly
 * turning reel.
 */
@Composable
fun ShutterButton(
    video: Boolean,
    recording: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = MaterialTheme.motionScheme
    val press by animateFloatAsState(if (pressed) 1f else 0f, motion.fastSpatialSpec(), label = "press")
    val record by animateFloatAsState(if (recording) 1f else 0f, motion.defaultSpatialSpec(), label = "record")
    val inner by animateColorAsState(if (video) CameraColors.Record else Color.White, motion.defaultEffectsSpec(), label = "inner")
    val reel = rememberInfiniteTransition(label = "reel")
    val spin by reel.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    val pressMorph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val stopMorph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Square) }
    val reelMorph = remember { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Cookie12Sided) }
    val path = remember { Path() }
    val matrix = remember { Matrix() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val d = this.size.minDimension
            val ringWidth = 4.dp.toPx()
            if (record > 0.01f) {
                // A cassette reel turning behind the ring while the tape runs.
                rotate(spin) {
                    drawPolygonPath(reelMorph.toPath(0f, path), d, inset = 0f, color = CameraColors.Record.copy(alpha = 0.28f * record), matrix = matrix)
                }
            }
            drawCircle(
                color = Color.White,
                radius = d / 2f - ringWidth / 2f - (if (record > 0.01f) 6.dp.toPx() * record else 0f),
                style = Stroke(ringWidth),
            )
            val innerSize = d * (0.74f - 0.34f * record) * (1f - 0.1f * press)
            val shape = if (video) stopMorph.toPath(record, path) else pressMorph.toPath(press, path)
            drawPolygonPath(shape, innerSize, inset = (d - innerSize) / 2f, color = inner, matrix = matrix)
        }
    }
}

/** Draws a path in the unit square scaled to [extent] and offset by [inset] on both axes. */
private fun DrawScope.drawPolygonPath(path: Path, extent: Float, inset: Float, color: Color, matrix: Matrix) {
    matrix.reset()
    matrix.scale(extent, extent)
    path.transform(matrix)
    translate(inset, inset) {
        drawPath(path, color)
    }
}
