package io.github.erenkng.vhscam.ui.camera

import android.view.Display
import android.view.Surface
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.VhsCamApp
import io.github.erenkng.vhscam.camera.DeviceOrientation
import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CamcorderEffect
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.LookCategory
import io.github.erenkng.vhscam.core.look.ShotKind
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.media.CapturedMedia
import io.github.erenkng.vhscam.settings.AppSettings
import io.github.erenkng.vhscam.settings.FlashSetting
import io.github.erenkng.vhscam.settings.TimerSetting
import io.github.erenkng.vhscam.settings.WhiteBalance
import io.github.erenkng.vhscam.ui.components.BusyPill
import io.github.erenkng.vhscam.ui.components.CountdownOverlay
import io.github.erenkng.vhscam.ui.components.LookDial
import io.github.erenkng.vhscam.ui.components.ProvideHaptics
import io.github.erenkng.vhscam.ui.components.RecordingBadge
import io.github.erenkng.vhscam.ui.components.ShutterButton
import io.github.erenkng.vhscam.ui.components.ThumbnailDial
import io.github.erenkng.vhscam.ui.theme.CameraColors
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme
import kotlinx.coroutines.launch

/** Everything the camera screen can ask for. */
class CameraActions(
    val onShutter: () -> Unit = {},
    val onTogglePause: () -> Unit = {},
    val onFlip: () -> Unit = {},
    val onModeChange: (CaptureMode) -> Unit = {},
    val onTapFocus: (x: Float, y: Float, width: Float, height: Float, display: Display, lock: Boolean) -> Unit =
        { _, _, _, _, _, _ -> },
    val onClearFocusLock: () -> Unit = {},
    val onZoom: (Float) -> Unit = {},
    val onExposure: (Int) -> Unit = {},
    val onFlash: (FlashSetting) -> Unit = {},
    val onTorch: () -> Unit = {},
    val onSettingsChange: ((AppSettings) -> AppSettings) -> Unit = {},
    val onManualFocus: (Float?) -> Unit = {},
    val onWhiteBalance: (WhiteBalance) -> Unit = {},
    val onOpenGallery: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
    val onEffect: (CamcorderEffect) -> Unit = {},
    val onIso: (Int?) -> Unit = {},
    val onShutterSpeed: (Long?) -> Unit = {},
    val onSendRoll: () -> Unit = {},
    val onDevelopNow: () -> Unit = {},
    val onCancelDoubleExposure: () -> Unit = {},
    val onUndoFrame: () -> Unit = {},
    val onDiscardReel: () -> Unit = {},
    val onFinishReel: () -> Unit = {},
    val onSnapshot: () -> Unit = {},
)

/** The camera. [secure] over the lock screen: the gallery button only shows what was shot since. */
@Composable
fun CameraScreen(onOpenGallery: () -> Unit, onOpenSettings: () -> Unit, secure: Boolean = false) {
    val app = LocalContext.current.applicationContext as VhsCamApp
    val viewModel: CameraViewModel = viewModel(factory = CameraViewModel.factory(app, secure))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val displayRotation = LocalView.current.display?.rotation ?: Surface.ROTATION_0

    if (state.settingsLoaded) {
        LaunchedEffect(lifecycleOwner, state.bindKey, displayRotation) {
            viewModel.bindCamera(lifecycleOwner, displayRotation)
        }
    }
    DisposableEffect(viewModel) {
        viewModel.setCameraVisible(true)
        onDispose { viewModel.setCameraVisible(false) }
    }

    val actions = remember(viewModel) {
        CameraActions(
            onShutter = viewModel::onShutter,
            onTogglePause = viewModel::togglePause,
            onFlip = viewModel::flipCamera,
            onModeChange = viewModel::setMode,
            onTapFocus = viewModel::onTapFocus,
            onClearFocusLock = viewModel::clearFocusLock,
            onZoom = viewModel::setZoom,
            onExposure = viewModel::setExposure,
            onFlash = viewModel::setFlash,
            onTorch = viewModel::toggleTorch,
            onSettingsChange = viewModel::updateSettings,
            onManualFocus = viewModel::setManualFocus,
            onWhiteBalance = viewModel::setWhiteBalance,
            onOpenGallery = onOpenGallery,
            onOpenSettings = onOpenSettings,
            onMessageShown = viewModel::consumeMessage,
            onEffect = viewModel::setEffect,
            onIso = viewModel::setIso,
            onShutterSpeed = viewModel::setShutter,
            onSendRoll = viewModel::sendRoll,
            onDevelopNow = viewModel::developNow,
            onCancelDoubleExposure = viewModel::cancelDoubleExposure,
            onUndoFrame = viewModel::undoFrame,
            onDiscardReel = viewModel::discardReel,
            onFinishReel = viewModel::finishReel,
            onSnapshot = viewModel::takeSnapshot,
        )
    }
    // Prints may have come back from the lab while the camera was away.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.developRolls() }

    CameraContent(state = state, actions = actions) { modifier ->
        surfaceRequest?.let { request ->
            CameraXViewfinder(surfaceRequest = request, modifier = modifier, contentScale = ContentScale.Crop)
        }
    }
}

/** The camera screen without CameraX, so it can be previewed and screenshot-tested. */
@Composable
fun CameraContent(
    state: CameraUiState,
    actions: CameraActions,
    modifier: Modifier = Modifier,
    effectRowOpen: Boolean = false,
    proSetting: ProSetting? = null,
    viewfinder: @Composable (Modifier) -> Unit,
) {
    // The camera UI is always dark, whatever the system theme says, and draws straight on black:
    // icons and text are white unless a component picks its own colours.
    VhsCamTheme(darkTheme = true, dynamicColor = state.settings.dynamicColor) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            ProvideHaptics(enabled = state.settings.haptics) {
                CameraLayout(state, actions, modifier, effectRowOpen, proSetting, viewfinder)
            }
        }
    }
}

/** Height of the row of icons above the viewfinder. */
private val TopBarHeight = 56.dp

/** Space between the top of the viewfinder and the camcorder effect buttons over it. */
private val EffectRowGap = 10.dp

/**
 * Laid out like a phone's own camera: icons along the top, the picture edge to edge under
 * them, and the looks, modes and shutter at the bottom (over the picture when it is tall).
 */
@Composable
private fun CameraLayout(
    state: CameraUiState,
    actions: CameraActions,
    modifier: Modifier,
    effectRowOpen: Boolean,
    initialProSetting: ProSetting?,
    viewfinder: @Composable (Modifier) -> Unit,
) {
    var showEffects by rememberSaveable { mutableStateOf(false) }
    var showLooks by rememberSaveable { mutableStateOf(false) }
    var showRoll by rememberSaveable { mutableStateOf(false) }
    var showEffectRow by rememberSaveable { mutableStateOf(effectRowOpen) }
    var proSetting by rememberSaveable { mutableStateOf(initialProSetting) }
    val effectRowVisible = showEffectRow && state.activeLook == Look.VHS
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messageText = state.message?.let { message ->
        if (message == CameraMessage.PRINTS_READY) {
            pluralStringResource(R.plurals.msg_prints_ready, state.developed, state.developed)
        } else {
            stringResource(message.label())
        }
    }
    LaunchedEffect(state.message) {
        val text = messageText ?: return@LaunchedEffect
        actions.onMessageShown()
        // Outside this effect: consuming the message restarts it, which must not cancel the snackbar.
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }
    val iconRotation by animateFloatAsState(
        targetValue = DeviceOrientation.iconRotationDegrees(state.rotation),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "iconRotation",
    )
    var lastBusy by remember { mutableStateOf(state.busy) }
    SideEffect { if (state.busy != null) lastBusy = state.busy }
    val density = LocalDensity.current
    var controlsHeight by remember { mutableStateOf(0.dp) }
    var effectRowHeight by remember { mutableStateOf(0.dp) }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            CameraTopBar(
                state = state,
                actions = actions,
                iconRotation = iconRotation,
                effectRowOpen = effectRowVisible,
                onToggleEffectRow = { showEffectRow = !showEffectRow },
                onOpenEffects = { showEffects = true },
                modifier = Modifier.fillMaxWidth().height(TopBarHeight),
            )
            ViewfinderPanel(
                state = state,
                actions = actions,
                viewfinder = viewfinder,
                coveredTop = if (effectRowHeight > 0.dp) EffectRowGap + effectRowHeight else 0.dp,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onOpenRoll = { showRoll = true },
            )
        }

        // The camcorder's effect buttons float over the top of the picture.
        AnimatedVisibility(
            visible = effectRowVisible,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(top = TopBarHeight + EffectRowGap)
                .onSizeChanged { effectRowHeight = with(density) { it.height.toDp() } },
        ) {
            EffectChips(selected = state.settings.effect, onSelect = actions.onEffect)
        }

        BottomControls(
            state = state,
            actions = actions,
            iconRotation = iconRotation,
            proSetting = proSetting,
            onProSetting = { setting -> proSetting = if (proSetting == setting) null else setting },
            onOpenLooks = { showLooks = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Keeps the controls readable where they cover a tall picture.
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .onSizeChanged { controlsHeight = with(density) { it.height.toDp() } },
        )

        CountdownOverlay(seconds = state.countdown, modifier = Modifier.align(Alignment.Center))

        AnimatedVisibility(
            visible = state.busy != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = TopBarHeight + 12.dp),
        ) {
            // Keeps showing the last job while the pill animates away.
            val busy = state.busy ?: lastBusy
            if (busy != null) {
                BusyPill(
                    label = stringResource(
                        when (busy.kind) {
                            BusyKind.DEVELOPING -> R.string.busy_developing
                            BusyKind.TAPE_AUDIO -> R.string.busy_tape
                            BusyKind.ANIMATION -> R.string.busy_animation
                        },
                    ),
                    progress = busy.progress,
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = controlsHeight + 8.dp),
        )
    }

    if (showEffects) {
        EffectsSheet(state = state, actions = actions, onDismiss = { showEffects = false })
    }
    if (showRoll) {
        RollSheet(state = state, actions = actions, onDismiss = { showRoll = false })
    }
    if (showLooks) {
        LookBrowserSheet(
            options = if (state.isFilming) Look.videoLooks else Look.entries,
            selected = state.activeLook,
            onSelect = { look -> actions.onSettingsChange { if (state.isFilming) it.copy(videoLook = look) else it.copy(photoLook = look) } },
            onDismiss = { showLooks = false },
            // Double exposure is a shooting mode, but people look for it with the films.
            extras = if (state.isFilming) {
                emptyList()
            } else {
                listOf(
                    LookBrowserExtra(
                        category = LookCategory.FILM,
                        image = R.drawable.onboarding_double,
                        label = R.string.shot_double,
                        description = R.string.look_desc_double,
                        icon = R.drawable.ic_join,
                        selected = state.shot == ShotKind.DOUBLE,
                    ) { actions.onSettingsChange { it.copy(mode = CaptureMode.PHOTO, shot = ShotKind.DOUBLE) } },
                )
            },
        )
    }
}

private fun CameraMessage.label(): Int = when (this) {
    CameraMessage.VIDEO_SAVED -> R.string.msg_video_saved
    CameraMessage.CAPTURE_FAILED -> R.string.msg_capture_failed
    CameraMessage.RECORDING_FAILED -> R.string.msg_recording_failed
    CameraMessage.CAMERA_ERROR -> R.string.msg_camera_error
    CameraMessage.DUAL_UNAVAILABLE -> R.string.msg_dual_unavailable
    CameraMessage.ROLL_SENT -> R.string.msg_roll_sent
    CameraMessage.PRINTS_READY -> R.string.msg_prints_ready_short
    CameraMessage.ANIMATION_SAVED -> R.string.msg_animation_saved
    CameraMessage.ANIMATION_FAILED -> R.string.msg_animation_failed
}

/**
 * A flat row of icons, spread across the top like the phone's own camera. Every mode keeps the
 * same seven places, so nothing jumps around when the look changes: what a look can't use is
 * dimmed, not removed. While recording, the row gives way to the REC counter, between the torch
 * and the camcorder effects.
 */
@Composable
private fun CameraTopBar(
    state: CameraUiState,
    actions: CameraActions,
    iconRotation: Float,
    effectRowOpen: Boolean,
    onToggleEffectRow: () -> Unit,
    onOpenEffects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = state.isRecording,
        transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
        label = "topBar",
        modifier = modifier,
    ) { recording ->
        if (recording) {
            RecordingTopBar(state, actions, iconRotation, effectRowOpen, onToggleEffectRow)
        } else {
            ToolsTopBar(state, actions, iconRotation, effectRowOpen, onToggleEffectRow, onOpenEffects)
        }
    }
}

@Composable
private fun ToolsTopBar(
    state: CameraUiState,
    actions: CameraActions,
    iconRotation: Float,
    effectRowOpen: Boolean,
    onToggleEffectRow: () -> Unit,
    onOpenEffects: () -> Unit,
) {
    val settings = state.settings
    val filming = state.isFilming
    val look = state.activeLook
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarIcon(
            icon = R.drawable.ic_settings,
            description = stringResource(R.string.settings),
            rotation = iconRotation,
            onClick = actions.onOpenSettings,
        )
        if (filming) {
            TorchIcon(state, actions, iconRotation)
        } else {
            // Under manual exposure the flash can't fire.
            val flash = if (state.manualExposure) FlashSetting.OFF else settings.flash
            val next = FlashSetting.entries[(settings.flash.ordinal + 1) % FlashSetting.entries.size]
            ToolbarIcon(
                icon = when (flash) {
                    FlashSetting.OFF -> R.drawable.ic_flash_off
                    FlashSetting.AUTO -> R.drawable.ic_flash_auto
                    FlashSetting.ON -> R.drawable.ic_flash_on
                },
                description = stringResource(
                    when (flash) {
                        FlashSetting.OFF -> R.string.flash_off
                        FlashSetting.AUTO -> R.string.flash_auto
                        FlashSetting.ON -> R.string.flash_on
                    },
                ),
                rotation = iconRotation,
                onClick = { actions.onFlash(next) },
                enabled = state.capabilities.hasFlash && !state.manualExposure,
            )
        }
        ToolbarIcon(
            icon = when (settings.timer) {
                TimerSetting.OFF -> R.drawable.ic_timer_off
                TimerSetting.THREE -> R.drawable.ic_timer_3_alt_1
                TimerSetting.TEN -> R.drawable.ic_timer_10_alt_1
            },
            description = stringResource(
                when (settings.timer) {
                    TimerSetting.OFF -> R.string.timer_off
                    TimerSetting.THREE -> R.string.timer_3
                    TimerSetting.TEN -> R.string.timer_10
                },
            ),
            rotation = iconRotation,
            // The photobooth runs its own countdown.
            enabled = state.shot != ShotKind.BOOTH,
            onClick = {
                val next = TimerSetting.entries[(settings.timer.ordinal + 1) % TimerSetting.entries.size]
                actions.onSettingsChange { it.copy(timer = next) }
            },
        )
        // Only the VHS look and video let you pick the frame; other looks have their own (3:2 film, square instant).
        val aspectChoice = filming || look == Look.VHS
        IconButton(
            onClick = {
                val next = if (settings.aspect == AspectSetting.RATIO_4_3) AspectSetting.RATIO_16_9 else AspectSetting.RATIO_4_3
                actions.onSettingsChange { it.copy(aspect = next) }
            },
            shapes = IconButtonDefaults.shapes(),
            enabled = aspectChoice,
        ) {
            Text(
                text = if (aspectChoice) {
                    stringResource(if (settings.aspect == AspectSetting.RATIO_4_3) R.string.aspect_4_3 else R.string.aspect_16_9)
                } else {
                    aspectLabel(look.cropAspect)
                },
                style = MaterialTheme.typography.labelLargeEmphasized,
                modifier = Modifier.rotate(iconRotation),
            )
        }
        if (filming) {
            ToolbarToggle(
                icon = R.drawable.ic_gradient,
                description = stringResource(if (settings.fade) R.string.fade_on else R.string.fade_off),
                checked = settings.fade,
                rotation = iconRotation,
                onCheckedChange = { on -> actions.onSettingsChange { it.copy(fade = on) } },
            )
        } else {
            // Pro mode takes single photos only.
            ShotMenu(
                shot = if (state.isPro) ShotKind.SINGLE else settings.shot,
                rotation = iconRotation,
                enabled = !state.isPro,
                onSelect = { shot -> actions.onSettingsChange { it.copy(shot = shot) } },
            )
        }
        EffectsToggle(state, iconRotation, effectRowOpen, onToggleEffectRow)
        ToolbarIcon(
            icon = R.drawable.ic_tune,
            description = stringResource(R.string.effects),
            rotation = iconRotation,
            onClick = onOpenEffects,
        )
    }
}

/** While recording: the torch, the REC counter level in the middle, and the camcorder effects. */
@Composable
private fun RecordingTopBar(
    state: CameraUiState,
    actions: CameraActions,
    iconRotation: Float,
    effectRowOpen: Boolean,
    onToggleEffectRow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            TorchIcon(state, actions, iconRotation)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val active = state.recording as? RecordingUi.Active
            if (active != null) {
                RecordingBadge(
                    elapsedMillis = active.elapsedMillis,
                    // Interval REC pauses between snippets by itself; that isn't a pause to report.
                    paused = active.paused && state.interval == null,
                    pausedLabel = stringResource(R.string.paused),
                )
            }
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            EffectsToggle(state, iconRotation, effectRowOpen, onToggleEffectRow)
        }
    }
}

@Composable
private fun TorchIcon(state: CameraUiState, actions: CameraActions, iconRotation: Float) {
    ToolbarIcon(
        icon = if (state.torchOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off,
        description = stringResource(if (state.torchOn) R.string.torch_on else R.string.torch_off),
        rotation = iconRotation,
        onClick = actions.onTorch,
        enabled = state.capabilities.hasFlash,
    )
}

/** The camcorder's digital effects, for the VHS look; a dot shows one is on. */
@Composable
private fun EffectsToggle(state: CameraUiState, iconRotation: Float, effectRowOpen: Boolean, onToggleEffectRow: () -> Unit) {
    val vhs = state.activeLook == Look.VHS
    BadgedBox(
        badge = {
            if (vhs && state.settings.effect != CamcorderEffect.NONE) Badge(containerColor = CameraColors.Record)
        },
    ) {
        ToolbarToggle(
            icon = R.drawable.ic_auto_awesome,
            description = stringResource(R.string.effects_button),
            checked = effectRowOpen,
            rotation = iconRotation,
            enabled = vhs,
            onCheckedChange = { onToggleEffectRow() },
        )
    }
}

/** "3:2" for a look's own frame. */
private fun aspectLabel(aspect: Float?): String {
    val ratio = aspect ?: (4f / 3f)
    val known = listOf(4 to 3, 16 to 9, 3 to 2, 1 to 1, 8 to 7)
    val (w, h) = known.minBy { (w, h) -> kotlin.math.abs(w.toFloat() / h - ratio) }
    return "$w:$h"
}

@Composable
private fun ToolbarIcon(
    icon: Int,
    description: String,
    rotation: Float,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes(), enabled = enabled) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.rotate(rotation))
    }
}

/** A toolbar switch: the icon sits in a filled pill while it is on. */
@Composable
private fun ToolbarToggle(
    icon: Int,
    description: String,
    checked: Boolean,
    rotation: Float,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    FilledIconToggleButton(
        checked = checked && enabled,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shapes = IconButtonDefaults.toggleableShapes(),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.rotate(rotation))
    }
}

/**
 * What the shutter does in photo mode, picked from a menu: one photo, a photobooth strip, a
 * double exposure or stop-motion. The button is filled while it does something other than one photo.
 */
@Composable
private fun ShotMenu(shot: ShotKind, rotation: Float, onSelect: (ShotKind) -> Unit, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    val active = shot != ShotKind.SINGLE
    Box {
        FilledIconButton(
            onClick = { open = true },
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
        ) {
            Icon(
                painterResource(shot.icon()),
                contentDescription = stringResource(R.string.shot_menu, stringResource(shot.label())),
                modifier = Modifier.rotate(rotation),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ShotKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(stringResource(kind.label()), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(kind.description()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        open = false
                        onSelect(kind)
                    },
                    leadingIcon = { Icon(painterResource(kind.icon()), contentDescription = null) },
                    trailingIcon = if (kind == shot) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

fun ShotKind.label(): Int = when (this) {
    ShotKind.SINGLE -> R.string.shot_single
    ShotKind.BOOTH -> R.string.shot_booth
    ShotKind.DOUBLE -> R.string.shot_double
    ShotKind.STOP_MOTION -> R.string.shot_stop_motion
}

private fun ShotKind.description(): Int = when (this) {
    ShotKind.SINGLE -> R.string.shot_single_desc
    ShotKind.BOOTH -> R.string.shot_booth_desc
    ShotKind.DOUBLE -> R.string.shot_double_desc
    ShotKind.STOP_MOTION -> R.string.shot_stop_motion_desc
}

private fun ShotKind.icon(): Int = when (this) {
    ShotKind.SINGLE -> R.drawable.ic_photo_camera
    ShotKind.BOOTH -> R.drawable.ic_view_agenda
    ShotKind.DOUBLE -> R.drawable.ic_join
    ShotKind.STOP_MOTION -> R.drawable.ic_animation
}

/** The camcorder's digital effect button, as a row of chips. */
@Composable
private fun EffectChips(selected: CamcorderEffect, onSelect: (CamcorderEffect) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CamcorderEffect.entries.forEach { effect ->
            FilterChip(
                selected = effect == selected,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(effect)
                },
                label = { Text(stringResource(effect.label())) },
                leadingIcon = { Icon(painterResource(effect.icon()), contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

private enum class BottomPanel { LOOKS, PRO, REEL }

@Composable
private fun BottomControls(
    state: CameraUiState,
    actions: CameraActions,
    iconRotation: Float,
    proSetting: ProSetting?,
    onProSetting: (ProSetting) -> Unit,
    onOpenLooks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.motionScheme
    Column(
        modifier = modifier.padding(top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Under the picture, never over the date a look prints in its corner.
        ZoomPresets(
            presets = state.zoomPresets,
            ratio = state.effectiveZoom,
            rotation = iconRotation,
            onZoom = actions.onZoom,
        )
        // While recording only the shutter row stays: nothing to change mid-take.
        AnimatedVisibility(
            visible = !state.isRecording,
            enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoicePanels(state, actions, proSetting, onProSetting, onOpenLooks)
            }
        }
        ShutterRow(state, actions, iconRotation)
    }
}

/** Looks, Pro controls or the stop-motion reel, and the modes under them. */
@Composable
private fun ChoicePanels(
    state: CameraUiState,
    actions: CameraActions,
    proSetting: ProSetting?,
    onProSetting: (ProSetting) -> Unit,
    onOpenLooks: () -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    // Looks to pick from, Pro mode's manual controls, or the stop-motion reel. While an
    // animation is being shot its look stays put, so every frame matches.
    val panel = when {
        state.isPro -> BottomPanel.PRO
        state.shot == ShotKind.STOP_MOTION && state.reel.frames > 0 -> BottomPanel.REEL
        else -> BottomPanel.LOOKS
    }
    AnimatedContent(
        targetState = panel,
        transitionSpec = {
            (fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()))
                .using(SizeTransform(clip = false))
        },
        label = "bottomPanel",
    ) { shown ->
        when (shown) {
            BottomPanel.PRO -> ProPanel(state, actions, open = proSetting, onOpen = onProSetting)
            BottomPanel.REEL -> StopMotionBar(
                frames = state.reel.frames,
                rate = state.settings.stopMotionRate,
                busy = state.animating,
                onUndo = actions.onUndoFrame,
                onRate = { rate -> actions.onSettingsChange { it.copy(stopMotionRate = rate) } },
                onDiscard = actions.onDiscardReel,
                onFinish = actions.onFinishReel,
            )
            // Photos and videos offer different looks, so each gets its own dial.
            BottomPanel.LOOKS -> key(state.isFilming) {
                ThumbnailDial(
                    options = if (state.isFilming) Look.videoLooks else Look.entries,
                    selected = state.activeLook,
                    onSelect = { look ->
                        actions.onSettingsChange { if (state.isFilming) it.copy(videoLook = look) else it.copy(photoLook = look) }
                    },
                    image = { it.thumbnail() },
                    label = { stringResource(it.label()) },
                    enabled = state.booth == null,
                    onSelectedClick = onOpenLooks,
                    selectedClickLabel = stringResource(R.string.looks_open),
                )
            }
        }
    }

    val modes = buildList {
        add(CaptureMode.PRO)
        add(CaptureMode.PHOTO)
        add(CaptureMode.VIDEO)
        if (state.capabilities.dualCamera) add(CaptureMode.DUAL)
    }
    LookDial(
        options = modes,
        selected = state.settings.mode,
        onSelect = actions.onModeChange,
        label = { stringResource(it.label()) },
        enabled = state.booth == null,
    )
}

@Composable
private fun ShutterRow(state: CameraUiState, actions: CameraActions, iconRotation: Float) {
    val filming = state.isFilming
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isRecording) {
            // A still of the frame being recorded, look and all.
            FilledTonalIconButton(
                onClick = actions.onSnapshot,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.snapshot),
                    modifier = Modifier.rotate(iconRotation),
                )
            }
        } else {
            GalleryButton(latest = state.latest, rotation = iconRotation, onClick = actions.onOpenGallery)
        }
        ShutterButton(
            video = filming,
            recording = state.isRecording,
            onClick = actions.onShutter,
            contentDescription = stringResource(
                when {
                    state.shot == ShotKind.STOP_MOTION -> R.string.shutter_frame
                    !filming -> R.string.shutter_photo
                    state.isRecording -> R.string.shutter_stop
                    else -> R.string.shutter_record
                },
            ),
        )
        val active = state.recording as? RecordingUi.Active
        if (active != null) {
            FilledTonalIconButton(
                onClick = actions.onTogglePause,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(56.dp),
                // Interval recording pauses and resumes by itself.
                enabled = state.interval == null,
            ) {
                Icon(
                    painterResource(if (active.paused) R.drawable.ic_play_arrow else R.drawable.ic_pause),
                    contentDescription = stringResource(if (active.paused) R.string.resume_recording else R.string.pause_recording),
                    modifier = Modifier.rotate(iconRotation),
                )
            }
        } else {
            FilledTonalIconButton(
                onClick = actions.onFlip,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(56.dp),
                enabled = state.capabilities.hasFrontCamera,
            ) {
                val flipRotation by animateFloatAsState(
                    targetValue = if (state.settings.frontCamera) 180f else 0f,
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                    label = "flip",
                )
                Icon(
                    painterResource(R.drawable.ic_cameraswitch),
                    contentDescription = stringResource(R.string.flip_camera),
                    modifier = Modifier.rotate(flipRotation + iconRotation),
                )
            }
        }
    }
}

/** Latest capture as a cookie-shaped thumbnail that pops in, morphing from a circle, after each new shot. */
@Composable
private fun GalleryButton(latest: CapturedMedia?, rotation: Float, onClick: () -> Unit) {
    val description = stringResource(R.string.open_gallery)
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie4Sided) }
    val reveal = remember { Animatable(1f) }
    val revealSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    var shownUri by remember { mutableStateOf(latest?.uri) }
    LaunchedEffect(latest?.uri) {
        val uri = latest?.uri
        if (uri != null && uri != shownUri) {
            shownUri = uri
            reveal.snapTo(0f)
            reveal.animateTo(1f, revealSpec)
        }
    }
    val progress = reveal.value
    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                val scale = 0.6f + 0.4f * progress
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .clip(MorphShape(morph, progress.coerceIn(0f, 1f)))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (latest != null) {
            AsyncImage(
                model = latest.uri,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painterResource(R.drawable.ic_video_library),
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** [morph] at [progress], scaled to the bounds. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

fun TapeSpeed.label(): Int = when (this) {
    TapeSpeed.SP -> R.string.speed_sp
    TapeSpeed.LP -> R.string.speed_lp
    TapeSpeed.EP -> R.string.speed_ep
}

fun CaptureMode.label(): Int = when (this) {
    CaptureMode.PRO -> R.string.mode_pro
    CaptureMode.PHOTO -> R.string.mode_photo
    CaptureMode.VIDEO -> R.string.mode_video
    CaptureMode.DUAL -> R.string.mode_dual
}

/** The beach from the onboarding, developed with the look: what the look does, at a glance. */
fun Look.sample(): Int = when (this) {
    Look.VHS -> R.drawable.onboarding_vhs
    Look.SUPER8 -> R.drawable.onboarding_super8
    Look.MINIDV -> R.drawable.onboarding_minidv
    Look.NIGHTSHOT -> R.drawable.onboarding_nightshot
    Look.CRT -> R.drawable.onboarding_crt
    Look.CCD -> R.drawable.onboarding_ccd
    Look.MAVICA -> R.drawable.onboarding_mavica
    Look.FILM -> R.drawable.onboarding_film
    Look.GOLD -> R.drawable.onboarding_gold
    Look.PORTRAIT -> R.drawable.onboarding_portrait
    Look.CINE -> R.drawable.onboarding_cine
    Look.RED_GRAIN -> R.drawable.onboarding_red_grain
    Look.REDSCALE -> R.drawable.onboarding_redscale
    Look.SILVER -> R.drawable.onboarding_silver
    Look.AEROCHROME -> R.drawable.onboarding_aerochrome
    Look.CROSS -> R.drawable.onboarding_cross
    Look.SLIDE -> R.drawable.onboarding_slide
    Look.DISPOSABLE -> R.drawable.onboarding_disposable
    Look.POLAROID -> R.drawable.onboarding_instant
    Look.GAMEBOY -> R.drawable.onboarding_gameboy
    Look.WEBCAM -> R.drawable.onboarding_webcam
    Look.FLIP -> R.drawable.onboarding_flip
    Look.THERMAL -> R.drawable.onboarding_thermal
    Look.CCTV -> R.drawable.onboarding_cctv
}

/**
 * The look's round thumbnail in the dials: a summer day through the look, closed in on what
 * tells it apart (its frame, its glow or grain, or the text it prints).
 */
fun Look.thumbnail(): Int = when (this) {
    Look.VHS -> R.drawable.thumb_vhs
    Look.SUPER8 -> R.drawable.thumb_super8
    Look.MINIDV -> R.drawable.thumb_minidv
    Look.NIGHTSHOT -> R.drawable.thumb_nightshot
    Look.CRT -> R.drawable.thumb_crt
    Look.FILM -> R.drawable.thumb_film
    Look.GOLD -> R.drawable.thumb_gold
    Look.PORTRAIT -> R.drawable.thumb_portrait
    Look.CINE -> R.drawable.thumb_cine
    Look.RED_GRAIN -> R.drawable.thumb_red_grain
    Look.REDSCALE -> R.drawable.thumb_redscale
    Look.SILVER -> R.drawable.thumb_silver
    Look.AEROCHROME -> R.drawable.thumb_aerochrome
    Look.CROSS -> R.drawable.thumb_cross
    Look.SLIDE -> R.drawable.thumb_slide
    Look.DISPOSABLE -> R.drawable.thumb_disposable
    Look.POLAROID -> R.drawable.thumb_instant
    Look.CCD -> R.drawable.thumb_ccd
    Look.MAVICA -> R.drawable.thumb_mavica
    Look.GAMEBOY -> R.drawable.thumb_gameboy
    Look.WEBCAM -> R.drawable.thumb_webcam
    Look.FLIP -> R.drawable.thumb_flip
    Look.THERMAL -> R.drawable.thumb_thermal
    Look.CCTV -> R.drawable.thumb_cctv
}

fun Look.label(): Int = when (this) {
    Look.VHS -> R.string.look_vhs
    Look.SUPER8 -> R.string.look_super8
    Look.MINIDV -> R.string.look_minidv
    Look.NIGHTSHOT -> R.string.look_nightshot
    Look.CRT -> R.string.look_crt
    Look.CCD -> R.string.look_ccd
    Look.MAVICA -> R.string.look_mavica
    Look.FILM -> R.string.look_film
    Look.GOLD -> R.string.look_gold
    Look.PORTRAIT -> R.string.look_portrait
    Look.CINE -> R.string.look_cine
    Look.RED_GRAIN -> R.string.look_red_grain
    Look.REDSCALE -> R.string.look_redscale
    Look.SILVER -> R.string.look_silver
    Look.AEROCHROME -> R.string.look_aerochrome
    Look.CROSS -> R.string.look_cross
    Look.SLIDE -> R.string.look_slide
    Look.DISPOSABLE -> R.string.look_disposable
    Look.POLAROID -> R.string.look_polaroid
    Look.GAMEBOY -> R.string.look_gameboy
    Look.WEBCAM -> R.string.look_webcam
    Look.FLIP -> R.string.look_flip
    Look.THERMAL -> R.string.look_thermal
    Look.CCTV -> R.string.look_cctv
}

fun CamcorderEffect.label(): Int = when (this) {
    CamcorderEffect.NONE -> R.string.effect_none
    CamcorderEffect.NIGHT_VISION -> R.string.effect_night
    CamcorderEffect.SEPIA -> R.string.effect_sepia
    CamcorderEffect.MONO -> R.string.effect_mono
    CamcorderEffect.MOSAIC -> R.string.effect_mosaic
    CamcorderEffect.STROBE -> R.string.effect_strobe
    CamcorderEffect.TRAIL -> R.string.effect_trail
}

private fun CamcorderEffect.icon(): Int = when (this) {
    CamcorderEffect.NONE -> R.drawable.ic_block
    CamcorderEffect.NIGHT_VISION -> R.drawable.ic_nightlight
    CamcorderEffect.SEPIA -> R.drawable.ic_filter_vintage
    CamcorderEffect.MONO -> R.drawable.ic_filter_b_and_w
    CamcorderEffect.MOSAIC -> R.drawable.ic_auto_awesome_mosaic
    CamcorderEffect.STROBE -> R.drawable.ic_shutter_speed
    CamcorderEffect.TRAIL -> R.drawable.ic_motion_blur
}
