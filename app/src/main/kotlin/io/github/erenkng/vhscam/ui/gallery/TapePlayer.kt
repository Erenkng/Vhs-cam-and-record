package io.github.erenkng.vhscam.ui.gallery

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.ui.theme.Vt323
import kotlinx.coroutines.delay
import java.util.Locale

/** A double tap's jump, [forward] or back; [at] tells two jumps the same way apart. */
private data class Jump(val forward: Boolean, val at: Long)

/**
 * A tape in the viewer. Tapping shows or hides the controls, which also tuck themselves away
 * while it plays. Dragging the bar winds to any moment, the picture following along with the
 * VCR's FF or REW; a double tap on either side jumps [JUMP_MILLIS] that way. [onPosition] hears
 * where it is, so a GIF can start there.
 */
@OptIn(UnstableApi::class)
@Composable
fun TapePlayer(uri: Uri, active: Boolean, onPosition: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(active) {
        player.playWhenReady = active
        if (!active) player.seekTo(0)
    }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val currentOnPosition by rememberUpdatedState(onPosition)
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            currentOnPosition(position)
            delay(POLL_MILLIS)
        }
    }
    val presentation = rememberPresentationState(player)
    val playPause = rememberPlayPauseButtonState(player)

    var controls by remember { mutableStateOf(true) }
    // Bumped on every touch, so the controls stay while they are being used.
    var touches by remember { mutableIntStateOf(0) }
    // Where the bar is being dragged to (0..1), and which way it last went.
    var scrub by remember { mutableStateOf<Float?>(null) }
    var winding by remember { mutableIntStateOf(0) }
    var playAfterScrub by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf<Jump?>(null) }
    LaunchedEffect(controls, playPause.showPlay, touches, scrub) {
        if (controls && !playPause.showPlay && scrub == null) {
            delay(HIDE_MILLIS)
            controls = false
        }
    }
    LaunchedEffect(jump) {
        if (jump != null) {
            delay(JUMP_SHOWN_MILLIS)
            jump = null
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(player) {
                detectTapGestures(
                    onTap = {
                        controls = !controls
                        touches++
                    },
                    onDoubleTap = { offset ->
                        val forward = offset.x > size.width / 2f
                        val end = player.duration.coerceAtLeast(0L)
                        player.seekTo((player.currentPosition + if (forward) JUMP_MILLIS else -JUMP_MILLIS).coerceIn(0L, end))
                        jump = Jump(forward, System.nanoTime())
                        touches++
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val size = presentation.videoSizeDp
        val aspect = if (size != null && size.height > 0f) size.width / size.height else 4f / 3f
        PlayerSurface(player = player, modifier = Modifier.aspectRatio(aspect))
        AnimatedVisibility(visible = presentation.coverSurface, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.aspectRatio(aspect).background(Color.Black))
        }

        // A shade under the controls, so they read over a bright picture.
        AnimatedVisibility(
            visible = controls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.65f))),
            )
        }

        AnimatedVisibility(
            visible = (controls || playPause.showPlay) && scrub == null,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
        ) {
            FilledIconButton(
                onClick = {
                    playPause.onClick()
                    touches++
                },
                shapes = IconButtonDefaults.shapes(),
                enabled = playPause.isEnabled,
                modifier = Modifier.size(76.dp),
            ) {
                AnimatedContent(
                    targetState = playPause.showPlay,
                    transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
                    label = "playPause",
                ) { showPlay ->
                    Icon(
                        painterResource(if (showPlay) R.drawable.ic_play_arrow else R.drawable.ic_pause),
                        contentDescription = stringResource(if (showPlay) R.string.play else R.string.pause),
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controls,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 92.dp),
        ) {
            val played = if (duration > 0) position.toFloat() / duration else 0f
            val shown = (scrub ?: played).coerceIn(0f, 1f)
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    value = shown,
                    onValueChange = { value ->
                        if (scrub == null) {
                            // Wind with the picture still, then carry on as it was.
                            playAfterScrub = player.isPlaying
                            player.pause()
                            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        }
                        val from = scrub ?: shown
                        if (value != from) winding = if (value > from) 1 else -1
                        scrub = value
                        player.seekTo((value * duration).toLong())
                        touches++
                    },
                    onValueChangeFinished = {
                        scrub?.let { value ->
                            player.setSeekParameters(SeekParameters.EXACT)
                            player.seekTo((value * duration).toLong())
                        }
                        scrub = null
                        winding = 0
                        if (playAfterScrub) player.play()
                    },
                    enabled = duration > 0,
                    colors = SliderDefaults.colors(inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    val timeStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                    Text(clock(scrub?.let { (it * duration).toLong() } ?: position), style = timeStyle, color = Color.White)
                    Text(clock(duration), style = timeStyle, color = Color.White.copy(alpha = 0.75f))
                }
            }
        }

        // The VCR's display while winding.
        val deck = when {
            scrub == null -> null
            winding > 0 -> "▶▶ FF"
            winding < 0 -> "◀◀ REW"
            else -> null
        }
        AnimatedVisibility(
            visible = deck != null,
            enter = fadeIn(tween(80)),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(start = 24.dp, top = 72.dp),
        ) {
            var last by remember { mutableStateOf("") }
            SideEffect { deck?.let { last = it } }
            Text(deck ?: last, style = DeckStyle, color = Color.White)
        }

        jump?.let { current ->
            key(current.at) {
                val pop = remember { Animatable(0f) }
                LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(if (current.forward) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(horizontal = 32.dp)
                        .graphicsLayer {
                            val p = pop.value
                            scaleX = 0.6f + 0.4f * p
                            scaleY = 0.6f + 0.4f * p
                            alpha = p.coerceIn(0f, 1f)
                        },
                ) {
                    Text(
                        if (current.forward) "▶▶ ${JUMP_MILLIS / 1000}s" else "◀◀ ${JUMP_MILLIS / 1000}s",
                        style = DeckStyle.copy(fontSize = 26.sp),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** Minutes and seconds, with hours in front once there are any. */
private fun clock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = total / 60 % 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

private val DeckStyle = TextStyle(
    fontFamily = Vt323,
    fontSize = 34.sp,
    letterSpacing = 2.sp,
    shadow = Shadow(Color.Black.copy(alpha = 0.8f), offset = Offset(3f, 3f)),
)

private const val POLL_MILLIS = 50L
private const val HIDE_MILLIS = 3_000L
private const val JUMP_MILLIS = 5_000L
private const val JUMP_SHOWN_MILLIS = 700L
