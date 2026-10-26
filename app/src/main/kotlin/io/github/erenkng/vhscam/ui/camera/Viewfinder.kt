package io.github.erenkng.vhscam.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.camera.DeviceOrientation
import io.github.erenkng.vhscam.camera.LookFactory
import io.github.erenkng.vhscam.camera.ZoomInfo
import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.ui.components.AppSlider
import io.github.erenkng.vhscam.ui.components.FocusIndicator
import io.github.erenkng.vhscam.ui.components.GridOverlay
import io.github.erenkng.vhscam.ui.components.RecordingBadge
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val ViewfinderCorner = 28.dp

/**
 * The viewfinder card: camera picture clipped to the current look's frame, plus tap/long-press
 * focus, pinch zoom, grid, focus ring, exposure slider, zoom presets and the REC badge.
 */
@Composable
fun ViewfinderPanel(
    state: CameraUiState,
    actions: CameraActions,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    val motion = MaterialTheme.motionScheme
    val boxAspect = if (settings.mode == CaptureMode.VIDEO && settings.aspect == AspectSetting.RATIO_16_9) 9f / 16f else 3f / 4f
    // Photo looks crop further (3:2 film, square instant film). The screen is portrait, so the
    // crop seen here is the portrait version of the look's aspect, whichever way the phone is held.
    val cropAspect = LookFactory.lookAspect(settings)?.let { 1f / it } ?: boxAspect
    val animatedBox by animateFloatAsState(boxAspect, motion.defaultSpatialSpec(), label = "box")
    val animatedCrop by animateFloatAsState(cropAspect, motion.defaultSpatialSpec(), label = "crop")
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val currentZoom by rememberUpdatedState(state.zoom)
    val iconRotation by animateFloatAsState(
        DeviceOrientation.iconRotationDegrees(state.rotation),
        motion.defaultSpatialSpec(),
        label = "rotation",
    )

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val widthLimited = maxWidth / maxHeight < animatedBox
        Box(
            Modifier
                .aspectRatio(animatedBox, matchHeightConstraintsFirst = !widthLimited)
                .clip(CenteredCropShape(animatedCrop, ViewfinderCorner))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val display = view.display ?: return@detectTapGestures
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            actions.onTapFocus(offset.x, offset.y, size.width.toFloat(), size.height.toFloat(), display, false)
                        },
                        onLongPress = { offset ->
                            val display = view.display ?: return@detectTapGestures
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            actions.onTapFocus(offset.x, offset.y, size.width.toFloat(), size.height.toFloat(), display, true)
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) actions.onZoom(currentZoom.ratio * zoom)
                    }
                },
        ) {
            viewfinder(Modifier.fillMaxSize())

            ShutterBlink(state.shutterCount)

            if (settings.grid) {
                GridOverlay(Modifier.fillMaxSize().clip(CenteredCropShape(animatedCrop, 0.dp)))
            }

            state.focus?.let { focus ->
                val half = with(LocalDensity.current) { 38.dp.toPx() }
                Column(
                    modifier = Modifier.offset {
                        IntOffset((focus.x - half).roundToInt(), (focus.y - half).roundToInt())
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FocusIndicator(status = focus.status, locked = focus.locked)
                    if (focus.locked) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = actions.onClearFocusLock,
                        ) {
                            Text(
                                text = stringResource(R.string.ae_af_lock),
                                style = MaterialTheme.typography.labelMediumEmphasized,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.focus != null && state.exposure.supported,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            ) {
                ExposureControl(state, actions)
            }

            val active = state.recording as? RecordingUi.Active
            if (active != null) {
                RecordingBadge(
                    elapsedMillis = active.elapsedMillis,
                    paused = active.paused,
                    pausedLabel = stringResource(R.string.paused),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp).rotate(iconRotation),
                )
            }

            ZoomPresets(
                zoom = state.zoom,
                rotation = iconRotation,
                onZoom = actions.onZoom,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            )
        }
    }
}

@Composable
private fun ExposureControl(state: CameraUiState, actions: CameraActions) {
    val exposure = state.exposure
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_exposure),
            contentDescription = stringResource(R.string.exposure),
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        AppSlider(
            value = exposure.index.toFloat(),
            onValueChange = { actions.onExposure(it.roundToInt()) },
            valueRange = exposure.min.toFloat()..exposure.max.toFloat(),
            steps = (exposure.max - exposure.min - 1).coerceAtLeast(0),
            vertical = true,
            modifier = Modifier.height(168.dp),
        )
    }
}

/** 0.5× / 1× / 2× / 5× buttons, limited to what the camera can do. */
@Composable
private fun ZoomPresets(zoom: ZoomInfo, rotation: Float, onZoom: (Float) -> Unit, modifier: Modifier = Modifier) {
    val presets = remember(zoom.min, zoom.max) {
        buildList {
            if (zoom.min < 0.95f) add(zoom.min)
            add(1f)
            if (zoom.max >= 2f) add(2f)
            if (zoom.max >= 5f) add(5f)
        }
    }
    if (presets.size < 2) return
    val haptics = LocalHapticFeedback.current
    // The active preset is the largest one not above the current zoom.
    val active = presets.lastOrNull { it <= zoom.ratio + 0.01f } ?: presets.first()
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEach { preset ->
                val selected = preset == active
                val label = if (selected && abs(zoom.ratio - preset) > 0.05f) zoom.ratio else preset
                FilledTonalIconToggleButton(
                    checked = selected,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onZoom(preset)
                    },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    modifier = Modifier.size(if (selected) 44.dp else 36.dp),
                ) {
                    Text(
                        text = stringResource(R.string.zoom_value, formatZoom(label)),
                        style = if (selected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelMedium,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
        }
    }
}

private fun formatZoom(value: Float): String =
    if (abs(value - value.roundToInt()) < 0.05f) value.roundToInt().toString() else String.format(Locale.US, "%.1f", value)

/** A quick dark blink over the viewfinder each time a photo is taken. */
@Composable
private fun ShutterBlink(count: Int) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(count) {
        if (count > 0) {
            alpha.snapTo(0.85f)
            alpha.animateTo(0f, tween(240))
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(Color.Black),
    )
}

/** A rounded rectangle with [aspect] (width / height), centred in the available space. */
class CenteredCropShape(private val aspect: Float, private val corner: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val boxAspect = size.width / size.height
        val (w, h) = if (aspect < boxAspect) size.height * aspect to size.height else size.width to size.width / aspect
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f
        val r = with(density) { corner.toPx() }
        return Outline.Rounded(RoundRect(left, top, left + w, top + h, CornerRadius(r, r)))
    }
}
