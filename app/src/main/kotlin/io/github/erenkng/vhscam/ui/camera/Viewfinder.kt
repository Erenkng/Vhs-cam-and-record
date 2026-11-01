package io.github.erenkng.vhscam.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.camera.DeviceOrientation
import io.github.erenkng.vhscam.camera.LookFactory
import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.ShotKind
import io.github.erenkng.vhscam.ui.components.AppSlider
import io.github.erenkng.vhscam.ui.components.FocusIndicator
import io.github.erenkng.vhscam.ui.components.GridOverlay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/**
 * The viewfinder, edge to edge at the top of the available space: camera picture cropped to
 * the current look's frame, plus tap/long-press focus, pinch zoom, grid, focus ring, exposure
 * slider and the badges for what the shutter is doing. Nothing sits over the bottom of the
 * picture, where the looks print their dates; the zoom buttons live under it.
 *
 * Text never turns with the phone (it would hang sideways); only icons turn, in place.
 *
 * @param coveredTop height of the controls drawn over the top of this space (the camcorder
 *   effect buttons); the badges stay below them.
 */
@Composable
fun ViewfinderPanel(
    state: CameraUiState,
    actions: CameraActions,
    viewfinder: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    coveredTop: Dp = 0.dp,
    onOpenRoll: () -> Unit = {},
) {
    val settings = state.settings
    val motion = MaterialTheme.motionScheme
    val boxAspect = if (state.isFilming && settings.aspect == AspectSetting.RATIO_16_9) 9f / 16f else 3f / 4f
    // Photo looks crop further (3:2 film, square instant film). The screen is portrait, so the
    // crop seen here is the portrait version of the look's aspect, whichever way the phone is held.
    val cropAspect = LookFactory.lookAspect(settings)?.let { 1f / it } ?: boxAspect
    val animatedBox by animateFloatAsState(boxAspect, motion.defaultSpatialSpec(), label = "box")
    val animatedCrop by animateFloatAsState(cropAspect, motion.defaultSpatialSpec(), label = "crop")
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val currentZoom by rememberUpdatedState(state.effectiveZoom)
    val iconRotation by animateFloatAsState(
        DeviceOrientation.iconRotationDegrees(state.rotation),
        motion.defaultSpatialSpec(),
        label = "rotation",
    )
    // The camcorder's zoom bar shows while zooming; past the lens a chip says so and undoes it.
    var zooming by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { currentZoom }.drop(1).collectLatest {
            zooming = true
            delay(1_500)
            zooming = false
        }
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        val widthLimited = maxWidth / maxHeight < animatedBox
        Box(
            Modifier
                .aspectRatio(animatedBox, matchHeightConstraintsFirst = !widthLimited)
                .clip(CenteredCropShape(animatedCrop, 0.dp))
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
                        if (zoom != 1f) actions.onZoom(currentZoom * zoom)
                    }
                },
        ) {
            viewfinder(Modifier.fillMaxSize())

            // The shot to line the next one up with: a double exposure's first, stop-motion's last.
            val ghost = when (state.shot) {
                ShotKind.DOUBLE -> state.ghost
                ShotKind.STOP_MOTION -> state.reel.onion
                else -> null
            }
            Crossfade(targetState = ghost, modifier = Modifier.matchParentSize(), animationSpec = tween(220), label = "ghost") { bitmap ->
                if (bitmap != null) {
                    GhostOverlay(
                        bitmap = bitmap,
                        rotation = iconRotation,
                        alpha = if (state.shot == ShotKind.DOUBLE) DOUBLE_EXPOSURE_ALPHA else ONION_SKIN_ALPHA,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            AnimatedVisibility(
                visible = !state.signal,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(280)),
                modifier = Modifier.matchParentSize(),
            ) {
                NoSignalScreen(Modifier.fillMaxSize())
            }

            ShutterBlink(state.shutterCount)

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

            // The controls sit inside the look's crop, so a square or 8-bit frame doesn't cut them off.
            CropBox(animatedCrop, coveredTop) { cropCoveredTop ->
                // Thirds of the picture the look keeps, not of the whole camera frame.
                if (settings.grid) GridOverlay(Modifier.fillMaxSize())

                AnimatedVisibility(
                    visible = state.focus != null && state.exposure.supported,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                ) {
                    ExposureControl(state, actions)
                }

                // The REC counter sits in the top bar; these badges stay level however the phone is held.
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp + cropCoveredTop),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedVisibility(
                        visible = state.interval != null,
                        enter = fadeIn() + expandVertically(motion.fastSpatialSpec()),
                        exit = fadeOut() + shrinkVertically(motion.fastSpatialSpec()),
                    ) {
                        IntervalBadge(nextInSeconds = state.interval?.nextInSeconds)
                    }
                    AnimatedVisibility(
                        visible = state.booth != null,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    ) {
                        var lastBooth by remember { mutableStateOf(state.booth) }
                        SideEffect { if (state.booth != null) lastBooth = state.booth }
                        (state.booth ?: lastBooth)?.let { BoothProgress(it) }
                    }
                    AnimatedVisibility(
                        visible = state.onRoll && state.booth == null,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    ) {
                        RollBadge(left = state.roll.left, onClick = onOpenRoll)
                    }
                    AnimatedVisibility(
                        visible = state.shot == ShotKind.DOUBLE,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    ) {
                        ExposureBadge(second = state.ghost != null, onCancel = actions.onCancelDoubleExposure)
                    }
                    AnimatedVisibility(
                        visible = state.shot == ShotKind.STOP_MOTION,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    ) {
                        ReelBadge(frames = state.reel.frames)
                    }
                }

                AnimatedVisibility(
                    visible = state.digitalZoomAvailable && zooming,
                    enter = fadeIn(tween(90)),
                    exit = fadeOut(tween(400)),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    val main = state.mainZoom
                    CamcorderZoomBar(
                        zoom = state.effectiveZoom,
                        min = minOf(main.min, state.capabilities.wideLens?.ratio ?: main.min),
                        lensMax = main.max,
                        max = main.max * CameraViewModel.DIGITAL_ZOOM_MAX,
                    )
                }
                AnimatedVisibility(
                    visible = state.digitalZoom > 1f && !state.isRecording,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                ) {
                    DigitalZoomChip(zoom = state.digitalZoom, onReset = { actions.onZoom(1f) })
                }
            }
        }
    }
}

/**
 * A box the size of a centred crop with [aspect] (width / height) of the available space.
 * [content] gets how much of the crop's top is covered, given [coveredTop] for the whole space.
 */
@Composable
private fun CropBox(
    aspect: Float,
    coveredTop: Dp,
    content: @Composable BoxScope.(top: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val boxAspect = maxWidth / maxHeight
        val (width, height) = if (aspect < boxAspect) maxHeight * aspect to maxHeight else maxWidth to maxWidth / aspect
        val margin = (maxHeight - height) / 2
        Box(Modifier.size(width, height)) {
            content((coveredTop - margin).coerceAtLeast(0.dp))
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

/**
 * 0.5× / 1× / 2× / 5× buttons like a phone camera's: round buttons in a round pill, the active
 * one filled and showing "×". [presets] are what the lenses can do (see
 * [CameraUiState.zoomPresets]); [ratio] is the zoom the user sees, digital zoom included, and
 * the active button shows it when it is between presets. The numbers turn in place with the
 * phone, like icons.
 */
@Composable
fun ZoomPresets(presets: List<Float>, ratio: Float, rotation: Float, onZoom: (Float) -> Unit, modifier: Modifier = Modifier) {
    if (presets.size < 2) return
    val haptics = LocalHapticFeedback.current
    val motion = MaterialTheme.motionScheme
    // The active preset is the largest one not above the current zoom.
    val active = presets.lastOrNull { it <= ratio + 0.01f } ?: presets.first()
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(ZoomPillPadding),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEach { preset ->
                val selected = preset == active
                val label = formatZoom(if (selected && abs(ratio - preset) > 0.05f) ratio else preset)
                val description = stringResource(R.string.zoom_value, label)
                // Same size touch target for every button; only the disc inside grows.
                val disc by animateDpAsState(if (selected) ZoomButtonSize else 34.dp, motion.fastSpatialSpec(), label = "disc")
                val fill by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    motion.defaultEffectsSpec(),
                    label = "fill",
                )
                Box(
                    modifier = Modifier
                        .size(ZoomButtonSize)
                        .clip(CircleShape)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onZoom(preset)
                            },
                        )
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(disc).background(fill, CircleShape))
                    Text(
                        text = if (selected) description else label,
                        style = if (selected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
        }
    }
}

/** How strongly the first shot of a double exposure shows over the second. */
private const val DOUBLE_EXPOSURE_ALPHA = 0.5f

/** Stop-motion's onion skin: the last frame, just enough to line the next one up. */
private const val ONION_SKIN_ALPHA = 0.35f

/** Zoom buttons are round and sit concentric with the pill's round ends. */
private val ZoomButtonSize = 44.dp
private val ZoomPillPadding = 4.dp

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
