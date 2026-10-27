package io.github.erenkng.vhscam.ui.camera

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.ui.theme.CameraColors
import io.github.erenkng.vhscam.ui.theme.Vt323
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

/** Text in the style of the camcorder's on-screen display. */
private val OsdText = TextStyle(
    fontFamily = Vt323,
    fontSize = 22.sp,
    color = Color.White,
    shadow = Shadow(Color.Black.copy(alpha = 0.65f), Offset(0f, 2f), 4f),
)

/** The VCR's blue screen, shown while no camera picture arrives. */
@Composable
fun NoSignalScreen(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(CameraColors.VcrBlue)
            .drawWithContent {
                drawContent()
                // Faint CRT lines.
                val pitch = 3.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(Color.Black.copy(alpha = 0.08f), topLeft = Offset(0f, y), size = Size(size.width, pitch / 3f))
                    y += pitch
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.no_signal),
            style = OsdText.copy(
                fontSize = 34.sp,
                letterSpacing = 3.sp,
                shadow = Shadow(Color.White.copy(alpha = 0.45f), Offset.Zero, 12f),
            ),
        )
    }
}

/**
 * The camcorder's W–T zoom bar: the lens range, a divider, then the digital zoom range.
 * Positions are logarithmic, so every doubling of the zoom moves the bar the same distance.
 */
@Composable
fun CamcorderZoomBar(zoom: Float, min: Float, lensMax: Float, max: Float, modifier: Modifier = Modifier) {
    val span = ln(max / min)
    val position = if (span > 0f) (ln(zoom / min) / span).coerceIn(0f, 1f) else 0f
    val divider = if (span > 0f) (ln(lensMax / min) / span).coerceIn(0f, 1f) else 1f
    val animated by animateFloatAsState(position, MaterialTheme.motionScheme.fastSpatialSpec(), label = "zoomBar")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("W", style = OsdText)
            Canvas(Modifier.width(168.dp).height(12.dp)) {
                val stroke = 1.5.dp.toPx()
                val shadow = Color.Black.copy(alpha = 0.45f)
                drawRect(shadow, topLeft = Offset(0f, stroke), size = size, style = Stroke(stroke))
                // The digital range is shaded, like on the camcorder.
                drawRect(
                    Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(size.width * divider, 0f),
                    size = Size(size.width * (1f - divider), size.height),
                )
                drawRect(Color.White, size = Size(size.width * animated, size.height))
                drawRect(Color.White, style = Stroke(stroke))
                val x = size.width * divider
                drawLine(Color.White, Offset(x, -3.dp.toPx()), Offset(x, size.height + 3.dp.toPx()), strokeWidth = stroke)
            }
            Text("T", style = OsdText)
        }
        if (zoom > lensMax + 0.01f) {
            Text(stringResource(R.string.dzoom), style = OsdText.copy(fontSize = 20.sp))
        }
    }
}

/** "INT REC" and the countdown to the next snippet. */
@Composable
fun IntervalBadge(nextInSeconds: Int?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_timelapse),
                contentDescription = null,
                tint = CameraColors.Amber,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.interval_badge),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = CameraColors.Amber,
            )
            AnimatedContent(
                targetState = nextInSeconds,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "intervalNext",
            ) { seconds ->
                Text(
                    text = if (seconds != null) stringResource(R.string.interval_next, seconds) else "",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

/** Photobooth progress: one dot per shot, the current one stretched into a pill. */
@Composable
fun BoothProgress(booth: BoothUi, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                for (shot in 1..booth.total) {
                    val width by animateDpAsState(
                        if (shot == booth.shot) 22.dp else 8.dp,
                        motion.fastSpatialSpec(),
                        label = "boothDot",
                    )
                    val color by animateColorAsState(
                        when {
                            shot < booth.shot -> Color.White
                            shot == booth.shot -> MaterialTheme.colorScheme.primary
                            else -> Color.White.copy(alpha = 0.3f)
                        },
                        motion.defaultEffectsSpec(),
                        label = "boothDotColor",
                    )
                    Box(Modifier.size(width, 8.dp).background(color, CircleShape))
                }
            }
            Text(
                text = stringResource(R.string.booth_progress, booth.shot, booth.total),
                style = MaterialTheme.typography.labelLargeEmphasized,
            )
        }
    }
}

/** The single-use camera's frame counter, like the little window on top of one: shots left. */
@Composable
fun RollBadge(left: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_camera_roll),
                contentDescription = null,
                tint = CameraColors.Amber,
                modifier = Modifier.size(16.dp),
            )
            // Counts down like the number wheel of a real one.
            AnimatedContent(
                targetState = left,
                transitionSpec = {
                    (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                },
                label = "rollLeft",
            ) { shots ->
                Text(
                    text = shots.toString(),
                    style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
                    color = CameraColors.Amber,
                )
            }
            Text(text = stringResource(R.string.roll_badge), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * A picture the camera took, laid faintly over the viewfinder to line the next one up with:
 * the first shot of a double exposure, or the last stop-motion frame. [bitmap] is upright, as
 * the user saw it, so it is turned by [rotation] (the icons' turn) to sit on the live picture
 * however the phone is held.
 */
@Composable
fun GhostOverlay(bitmap: Bitmap, rotation: Float, alpha: Float, modifier: Modifier = Modifier) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(modifier) {
        // Turned a quarter, the picture fills the box the other way round.
        val sideways = ((rotation / 90f).roundToInt() % 2) != 0
        val width = if (sideways) size.height else size.width
        val height = if (sideways) size.width else size.height
        val fill = max(width / image.width, height / image.height)
        withTransform({
            translate(size.width / 2f, size.height / 2f)
            rotate(rotation, pivot = Offset.Zero)
            scale(fill, fill, pivot = Offset.Zero)
            translate(-image.width / 2f, -image.height / 2f)
        }) {
            drawImage(image, alpha = alpha)
        }
    }
}

/** Double exposure: which of the two shots is next. Once the first is in, a tap throws it away. */
@Composable
fun ExposureBadge(second: Boolean, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onCancel,
        enabled = second,
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_join),
                contentDescription = null,
                tint = CameraColors.Amber,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.exposure_step, if (second) 2 else 1),
                style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
            )
            if (second) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.exposure_cancel),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Stop-motion's frame counter. */
@Composable
fun ReelBadge(frames: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_animation),
                contentDescription = null,
                tint = CameraColors.Amber,
                modifier = Modifier.size(16.dp),
            )
            AnimatedContent(
                targetState = frames,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "reelFrames",
            ) { count ->
                Text(
                    text = pluralStringResource(R.plurals.reel_frames, count, count),
                    style = MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}
