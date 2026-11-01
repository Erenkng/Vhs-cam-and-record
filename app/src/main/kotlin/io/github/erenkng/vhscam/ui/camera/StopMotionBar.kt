package io.github.erenkng.vhscam.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.core.media.StopMotion

/**
 * Stop-motion controls, in place of the looks while an animation is being shot: take back the
 * last frame, how long it plays, a menu with the frame rate and throwing it all away, and
 * making it into a video.
 */
@Composable
fun StopMotionBar(
    frames: Int,
    rate: Int,
    busy: Boolean,
    onUndo: () -> Unit,
    onRate: (Int) -> Unit,
    onDiscard: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val seconds = String.format(locale, "%.1f", StopMotion.durationMillis(frames, rate) / 1_000f)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onUndo, enabled = frames > 0 && !busy, shapes = IconButtonDefaults.shapes()) {
            Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.reel_undo))
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.reel_length, seconds),
                style = MaterialTheme.typography.titleSmallEmphasized.copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
            Text(
                stringResource(R.string.reel_rate, rate),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, enabled = !busy, shapes = IconButtonDefaults.shapes()) {
                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.reel_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(
                    stringResource(R.string.reel_rate_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                StopMotion.RATES.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reel_rate, option)) },
                        onClick = {
                            menuOpen = false
                            onRate(option)
                        },
                        trailingIcon = if (option == rate) {
                            { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reel_discard)) },
                    onClick = {
                        menuOpen = false
                        confirmDiscard = true
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                )
            }
        }
        Button(
            onClick = onFinish,
            enabled = frames >= CameraViewModel.MIN_ANIMATION_FRAMES && !busy,
            shapes = ButtonDefaults.shapes(),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.reel_save))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            title = { Text(stringResource(R.string.reel_discard_title)) },
            text = { Text(pluralStringResource(R.plurals.reel_discard_body, frames, frames)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscard()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
