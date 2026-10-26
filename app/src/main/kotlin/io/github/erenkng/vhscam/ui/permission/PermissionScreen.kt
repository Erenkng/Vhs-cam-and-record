package io.github.erenkng.vhscam.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.R

/** First-run screen asking for the camera (and optionally the microphone). */
@Composable
fun PermissionScreen(onGranted: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.CAMERA] == true) onGranted() else denied = true
    }
    PermissionContent(
        denied = denied,
        onRequest = {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        },
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        modifier = modifier,
    )
}

@Composable
fun PermissionContent(
    denied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spin = rememberInfiniteTransition(label = "hero")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "angle",
    )
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(220.dp)
                        .rotate(angle)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.SoftBurst.toShape()),
                )
                Box(
                    Modifier
                        .size(120.dp)
                        .rotate(-angle * 1.5f)
                        .background(MaterialTheme.colorScheme.tertiary, MaterialShapes.Cookie9Sided.toShape()),
                )
                Icon(
                    painterResource(R.drawable.ic_videocam),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(56.dp),
                )
            }
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.displaySmallEmphasized,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(if (denied) R.string.permission_denied else R.string.permission_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRequest,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.MediumContentPadding,
            ) {
                Text(stringResource(R.string.permission_grant), style = MaterialTheme.typography.titleMediumEmphasized)
            }
            if (denied) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.permission_settings))
                }
            }
        }
    }
}
