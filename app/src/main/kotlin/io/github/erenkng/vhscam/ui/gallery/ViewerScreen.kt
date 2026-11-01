package io.github.erenkng.vhscam.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.media.CapturedMedia
import io.github.erenkng.vhscam.ui.components.BusyPill
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(
    viewModel: GalleryViewModel,
    startIndex: Int,
    onBack: () -> Unit,
    onRestyle: (CapturedMedia) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val gifProgress by viewModel.gifProgress.collectAsStateWithLifecycle()
    val tvProgress by viewModel.tvProgress.collectAsStateWithLifecycle()
    val list = items
    if (list == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }
    if (list.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, list.lastIndex)) { list.size }
    var confirmDelete by remember { mutableStateOf<CapturedMedia?>(null) }
    // Where the video on screen is, so a GIF starts there.
    var videoPosition by remember { mutableLongStateOf(0L) }
    val snackbar = remember { SnackbarHostState() }
    val messageTexts = mapOf(
        GalleryMessage.GIF_SAVED to stringResource(R.string.msg_gif_saved),
        GalleryMessage.GIF_FAILED to stringResource(R.string.msg_gif_failed),
        GalleryMessage.TV_SAVED to stringResource(R.string.msg_tv_saved),
        GalleryMessage.TV_FAILED to stringResource(R.string.msg_tv_failed),
    )
    val noOriginal = stringResource(R.string.restyle_no_original)
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(messageTexts.getValue(message))
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, key = { list.getOrNull(it)?.uri?.toString() ?: it.toString() }) { page ->
            val item = list.getOrNull(page) ?: return@HorizontalPager
            if (item.isVideo) {
                val active = pager.currentPage == page
                TapePlayer(item.uri, active = active, onPosition = { if (active) videoPosition = it })
            } else {
                ZoomableImage(item)
            }
        }

        FilledTonalIconButton(
            onClick = onBack,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 16.dp),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        ) {
            val current = list.getOrNull(pager.currentPage)
            IconButton(onClick = { current?.let { GalleryViewModel.share(context, it) } }, shapes = IconButtonDefaults.shapes()) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.share))
            }
            if (current?.isVideo == true) {
                IconButton(
                    onClick = { viewModel.makeGif(current, (videoPosition - GIF_LEAD_MILLIS).coerceAtLeast(0L)) },
                    shapes = IconButtonDefaults.shapes(),
                    enabled = gifProgress == null && tvProgress == null,
                ) {
                    Icon(painterResource(R.drawable.ic_gif_box), contentDescription = stringResource(R.string.make_gif))
                }
                IconButton(
                    onClick = { viewModel.makeTvClip(current) },
                    shapes = IconButtonDefaults.shapes(),
                    enabled = gifProgress == null && tvProgress == null,
                ) {
                    Icon(painterResource(R.drawable.ic_live_tv), contentDescription = stringResource(R.string.make_tv_clip))
                }
            } else if (current != null && !current.isGif) {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (viewModel.hasOriginal(current)) {
                                onRestyle(current)
                            } else {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(noOriginal)
                            }
                        }
                    },
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(painterResource(R.drawable.ic_style), contentDescription = stringResource(R.string.restyle))
                }
            }
            IconButton(onClick = { confirmDelete = current }, shapes = IconButtonDefaults.shapes()) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
            }
        }

        val busy = gifProgress?.let { R.string.gif_making to it } ?: tvProgress?.let { R.string.tv_making to it }
        AnimatedVisibility(
            visible = busy != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 16.dp),
        ) {
            // Keeps the last label and value while the pill animates away.
            var last by remember { mutableStateOf(R.string.gif_making to 0f) }
            SideEffect { busy?.let { last = it } }
            val (label, progress) = busy ?: last
            BusyPill(label = stringResource(label), progress = progress)
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 96.dp),
        )
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            title = { Text(stringResource(if (item.isVideo) R.string.delete_video_title else R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item)
                    confirmDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Pinch or double-tap to zoom; single-finger drags pass through to the pager unless zoomed in. */
@Composable
private fun ZoomableImage(item: CapturedMedia) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { _, zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }
    AsyncImage(
        model = item.uri,
        contentDescription = item.displayName,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1f) 1f else 2.5f
                    offset = Offset.Zero
                })
            }
            .transformable(transform, canPan = { scale > 1f })
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

/** The GIF starts this much before the moment on screen, so it catches what the user saw. */
private const val GIF_LEAD_MILLIS = 1_000L
