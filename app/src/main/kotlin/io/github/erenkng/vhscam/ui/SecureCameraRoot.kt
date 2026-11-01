package io.github.erenkng.vhscam.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.VhsCamApp
import io.github.erenkng.vhscam.launch.LaunchDestination
import io.github.erenkng.vhscam.ui.camera.CameraScreen
import io.github.erenkng.vhscam.ui.launch.CrtPowerOn
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme

/**
 * The lock screen camera: the camera alone, the gallery and settings behind [onUnlock]. Setting
 * the app up (the onboarding, the camera permission) also needs the phone unlocked first.
 */
@Composable
fun SecureCameraRoot(onUnlock: (LaunchDestination) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VhsCamApp
    val settings by app.container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val hasCamera = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    var poweringOn by rememberSaveable { mutableStateOf(true) }

    val loaded = settings
    VhsCamTheme(darkTheme = true, dynamicColor = loaded?.dynamicColor ?: true) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when {
                loaded == null -> Unit
                !loaded.onboardingDone || !hasCamera -> UnlockToSetUp(onUnlock = { onUnlock(LaunchDestination.CAMERA) })
                else -> CameraScreen(
                    onOpenGallery = { onUnlock(LaunchDestination.GALLERY) },
                    onOpenSettings = { onUnlock(LaunchDestination.SETTINGS) },
                    secure = true,
                )
            }
            if (poweringOn) CrtPowerOn(onFinished = { poweringOn = false })
        }
    }
}

@Composable
private fun UnlockToSetUp(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            stringResource(R.string.secure_setup_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.secure_setup_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onUnlock, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.secure_unlock))
        }
    }
}
