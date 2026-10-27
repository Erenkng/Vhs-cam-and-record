package io.github.erenkng.vhscam.ui.restyle

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.ui.camera.label
import io.github.erenkng.vhscam.ui.camera.sample
import io.github.erenkng.vhscam.ui.components.AppSlider
import io.github.erenkng.vhscam.ui.components.ThumbnailDial
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme

@Composable
fun RestyleScreen(viewModel: RestyleViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val saved = stringResource(R.string.restyle_saved)
    val failed = stringResource(R.string.restyle_failed)
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(if (message == RestyleMessage.SAVED) saved else failed)
        }
    }
    RestyleContent(
        state = state,
        onBack = onBack,
        onLook = viewModel::setLook,
        onIntensity = viewModel::setIntensity,
        onSave = viewModel::save,
        snackbar = snackbar,
    )
}

/** Separate from the view model so it can be screenshot-tested. */
@Composable
fun RestyleContent(
    state: RestyleUiState,
    onBack: () -> Unit,
    onLook: (Look) -> Unit,
    onIntensity: (Float) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Like the camera and the viewer: dark, so the photo is what stands out.
    VhsCamTheme(darkTheme = true, dynamicColor = state.settings.dynamicColor) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Black,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.restyle)) },
                    navigationIcon = {
                        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.missing -> MissingOriginal()
                        state.preview == null -> LoadingIndicator(Modifier.size(64.dp))
                        else -> Preview(state.preview, state.rendering)
                    }
                }
                if (!state.missing) {
                    Controls(state, onLook, onIntensity, onSave)
                }
            }
        }
    }
}

@Composable
private fun Preview(preview: Bitmap, rendering: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Crossfade(targetState = preview, animationSpec = tween(220), label = "preview") { bitmap ->
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
            )
        }
        AnimatedVisibility(
            visible = rendering,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) {
            LoadingIndicator(Modifier.size(36.dp))
        }
    }
}

@Composable
private fun Controls(state: RestyleUiState, onLook: (Look) -> Unit, onIntensity: (Float) -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ThumbnailDial(
            options = Look.entries,
            selected = state.look,
            onSelect = onLook,
            image = { it.sample() },
            label = { stringResource(it.label()) },
            enabled = !state.saving,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (state.look == Look.VHS) R.string.wear else R.string.intensity),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppSlider(
                value = state.intensity,
                onValueChange = onIntensity,
                enabled = !state.saving,
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = onSave,
            enabled = !state.saving && state.preview != null,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
        ) {
            if (state.saving) {
                LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.restyle_save), style = MaterialTheme.typography.titleMediumEmphasized)
        }
    }
}

@Composable
private fun MissingOriginal() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_style),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            stringResource(R.string.restyle_no_original),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
