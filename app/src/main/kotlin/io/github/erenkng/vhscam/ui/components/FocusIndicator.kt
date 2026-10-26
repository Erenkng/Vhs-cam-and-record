package io.github.erenkng.vhscam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import io.github.erenkng.vhscam.ui.camera.FocusStatus

/**
 * Focus ring drawn where the user tapped: a spinning cookie while searching that settles into
 * a circle once focused (or shakes and turns red when focus fails). Locked focus stays square-ish.
 */
@Composable
fun FocusIndicator(status: FocusStatus, locked: Boolean, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    val settled by animateFloatAsState(
        targetValue = if (status == FocusStatus.FOCUSING) 0f else 1f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "settled",
    )
    val color by animateColorAsState(
        targetValue = when (status) {
            FocusStatus.FOCUSING -> MaterialTheme.colorScheme.inverseSurface
            FocusStatus.FOCUSED -> MaterialTheme.colorScheme.primary
            FocusStatus.FAILED -> MaterialTheme.colorScheme.error
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "color",
    )
    val scale = remember { Animatable(1.35f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(status) {
        when (status) {
            FocusStatus.FOCUSING -> scale.animateTo(1f, motion.slowSpatialSpec())
            FocusStatus.FAILED -> {
                for (target in listOf(8f, -8f, 5f, -3f, 0f)) shake.animateTo(target, motion.fastSpatialSpec())
            }
            FocusStatus.FOCUSED -> Unit
        }
    }
    val morph = remember(locked) {
        Morph(MaterialShapes.Cookie9Sided, if (locked) MaterialShapes.Square else MaterialShapes.Circle)
    }
    val path = remember { Path() }
    val matrix = remember { Matrix() }
    Canvas(modifier.size(76.dp)) {
        val extent = size.minDimension * scale.value
        val inset = (size.minDimension - extent) / 2f
        morph.toPath(settled, path)
        matrix.reset()
        matrix.scale(extent, extent)
        path.transform(matrix)
        translate(inset + shake.value, inset) {
            rotate(degrees = (1f - settled) * 40f, pivot = center.copy(x = extent / 2f, y = extent / 2f)) {
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            }
        }
    }
}
