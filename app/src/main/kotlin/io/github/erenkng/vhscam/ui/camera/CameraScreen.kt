package io.github.erenkng.vhscam.ui.camera

import android.view.Display
import android.view.Surface
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.VhsCamApp
import io.github.erenkng.vhscam.camera.DeviceOrientation
import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.PhotoLook
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.media.CapturedMedia
import io.github.erenkng.vhscam.settings.AppSettings
import io.github.erenkng.vhscam.settings.FlashSetting
import io.github.erenkng.vhscam.settings.TimerSetting
import io.github.erenkng.vhscam.settings.WhiteBalance
import io.github.erenkng.vhscam.ui.components.BusyPill
import io.github.erenkng.vhscam.ui.components.ConnectedToggleGroup
import io.github.erenkng.vhscam.ui.components.CountdownOverlay
import io.github.erenkng.vhscam.ui.components.ProvideHaptics
import io.github.erenkng.vhscam.ui.components.ShutterButton
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
)

@Composable
fun CameraScreen(onOpenGallery: () -> Unit, onOpenSettings: () -> Unit) {
    val app = LocalContext.current.applicationContext as VhsCamApp
    val viewModel: CameraViewModel = viewModel(factory = CameraViewModel.factory(app))
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
        )
    }

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
    viewfinder: @Composable (Modifier) -> Unit,
) {
    // The camera UI is always dark, whatever the system theme says.
    VhsCamTheme(darkTheme = true, dynamicColor = state.settings.dynamicColor) {
        ProvideHaptics(enabled = state.settings.haptics) {
            CameraLayout(state, actions, modifier, viewfinder)
        }
    }
}

@Composable
private fun CameraLayout(
    state: CameraUiState,
    actions: CameraActions,
    modifier: Modifier,
    viewfinder: @Composable (Modifier) -> Unit,
) {
    var showEffects by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messageText = state.message?.let { stringResource(it.label()) }
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

    Box(modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            CameraTopBar(
                state = state,
                actions = actions,
                iconRotation = iconRotation,
                onOpenEffects = { showEffects = true },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
            ViewfinderPanel(
                state = state,
                actions = actions,
                viewfinder = viewfinder,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
            BottomControls(state = state, actions = actions, iconRotation = iconRotation)
        }

        CountdownOverlay(seconds = state.countdown, modifier = Modifier.align(Alignment.Center))

        AnimatedVisibility(
            visible = state.busy != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 76.dp),
        ) {
            // Keeps showing the last job while the pill animates away.
            val busy = state.busy ?: lastBusy
            if (busy != null) {
                BusyPill(
                    label = stringResource(if (busy.kind == BusyKind.DEVELOPING) R.string.busy_developing else R.string.busy_tape),
                    progress = busy.progress,
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 220.dp),
        )
    }

    if (showEffects) {
        EffectsSheet(state = state, actions = actions, onDismiss = { showEffects = false })
    }
}

private fun CameraMessage.label(): Int = when (this) {
    CameraMessage.VIDEO_SAVED -> R.string.msg_video_saved
    CameraMessage.CAPTURE_FAILED -> R.string.msg_capture_failed
    CameraMessage.RECORDING_FAILED -> R.string.msg_recording_failed
    CameraMessage.CAMERA_ERROR -> R.string.msg_camera_error
}

@Composable
private fun CameraTopBar(
    state: CameraUiState,
    actions: CameraActions,
    iconRotation: Float,
    onOpenEffects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    val video = settings.mode == CaptureMode.VIDEO
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        ),
    ) {
        if (state.capabilities.hasFlash) {
            if (video) {
                ToolbarIcon(
                    icon = if (state.torchOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off,
                    description = stringResource(if (state.torchOn) R.string.torch_on else R.string.torch_off),
                    rotation = iconRotation,
                    onClick = actions.onTorch,
                )
            } else {
                val next = FlashSetting.entries[(settings.flash.ordinal + 1) % FlashSetting.entries.size]
                ToolbarIcon(
                    icon = when (settings.flash) {
                        FlashSetting.OFF -> R.drawable.ic_flash_off
                        FlashSetting.AUTO -> R.drawable.ic_flash_auto
                        FlashSetting.ON -> R.drawable.ic_flash_on
                    },
                    description = stringResource(
                        when (settings.flash) {
                            FlashSetting.OFF -> R.string.flash_off
                            FlashSetting.AUTO -> R.string.flash_auto
                            FlashSetting.ON -> R.string.flash_on
                        },
                    ),
                    rotation = iconRotation,
                    onClick = { actions.onFlash(next) },
                )
            }
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
            onClick = {
                val next = TimerSetting.entries[(settings.timer.ordinal + 1) % TimerSetting.entries.size]
                actions.onSettingsChange { it.copy(timer = next) }
            },
        )
        if (video || settings.photoLook == PhotoLook.VHS) {
            IconButton(
                onClick = {
                    val next = if (settings.aspect == AspectSetting.RATIO_4_3) AspectSetting.RATIO_16_9 else AspectSetting.RATIO_4_3
                    actions.onSettingsChange { it.copy(aspect = next) }
                },
                shapes = IconButtonDefaults.shapes(),
                enabled = !state.isRecording,
            ) {
                Text(
                    text = stringResource(if (settings.aspect == AspectSetting.RATIO_4_3) R.string.aspect_4_3 else R.string.aspect_16_9),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    modifier = Modifier.rotate(iconRotation),
                )
            }
        }
        ToolbarIcon(
            icon = if (settings.grid) R.drawable.ic_grid_on else R.drawable.ic_grid_off,
            description = stringResource(if (settings.grid) R.string.grid_on else R.string.grid_off),
            rotation = iconRotation,
            onClick = { actions.onSettingsChange { it.copy(grid = !it.grid) } },
        )
        ToolbarIcon(
            icon = R.drawable.ic_tune,
            description = stringResource(R.string.effects),
            rotation = iconRotation,
            onClick = onOpenEffects,
        )
        ToolbarIcon(
            icon = R.drawable.ic_settings,
            description = stringResource(R.string.settings),
            rotation = iconRotation,
            onClick = actions.onOpenSettings,
            enabled = !state.isRecording,
        )
    }
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

@Composable
private fun BottomControls(state: CameraUiState, actions: CameraActions, iconRotation: Float) {
    val settings = state.settings
    val video = settings.mode == CaptureMode.VIDEO
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (video) {
            ConnectedToggleGroup(
                options = TapeSpeed.entries,
                selected = settings.tapeSpeed,
                onSelect = { speed -> actions.onSettingsChange { it.copy(tapeSpeed = speed) } },
                buttonSize = ToggleButtonSize.ExtraSmall,
            ) { speed ->
                Text(stringResource(speed.label()))
            }
        } else {
            ConnectedToggleGroup(
                options = PhotoLook.entries,
                selected = settings.photoLook,
                onSelect = { look -> actions.onSettingsChange { it.copy(photoLook = look) } },
                buttonSize = ToggleButtonSize.ExtraSmall,
            ) { look ->
                Text(stringResource(look.label()))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GalleryButton(latest = state.latest, rotation = iconRotation, onClick = actions.onOpenGallery)
            ShutterButton(
                video = video,
                recording = state.isRecording,
                onClick = actions.onShutter,
                contentDescription = stringResource(
                    when {
                        !video -> R.string.shutter_photo
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
                        targetValue = if (settings.frontCamera) 180f else 0f,
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

        ConnectedToggleGroup(
            options = CaptureMode.entries,
            selected = settings.mode,
            onSelect = actions.onModeChange,
            enabled = !state.isRecording,
            icon = { mode ->
                Icon(
                    painterResource(if (mode == CaptureMode.PHOTO) R.drawable.ic_photo_camera else R.drawable.ic_videocam),
                    contentDescription = null,
                )
            },
        ) { mode ->
            Text(stringResource(if (mode == CaptureMode.PHOTO) R.string.mode_photo else R.string.mode_video))
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

fun PhotoLook.label(): Int = when (this) {
    PhotoLook.VHS -> R.string.look_vhs
    PhotoLook.CCD -> R.string.look_ccd
    PhotoLook.FILM -> R.string.look_film
    PhotoLook.POLAROID -> R.string.look_polaroid
}
