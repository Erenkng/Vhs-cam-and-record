package io.github.erenkng.vhscam.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.core.media.FilmLab
import io.github.erenkng.vhscam.media.LabRoll
import io.github.erenkng.vhscam.media.RollState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The single-use camera's roll: what is on it, the rolls at the lab, and when prints come back. */
@Composable
fun RollSheet(state: CameraUiState, actions: CameraActions, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        RollContent(
            roll = state.roll,
            nextMorning = state.settings.disposableNextMorning,
            onSend = actions.onSendRoll,
            onDevelopNow = actions.onDevelopNow,
            onNextMorning = { on -> actions.onSettingsChange { it.copy(disposableNextMorning = on) } },
        )
    }
}

/** The roll sheet without the sheet around it, so it can be screenshot-tested. */
@Composable
fun RollContent(
    roll: RollState,
    nextMorning: Boolean,
    onSend: () -> Unit,
    onDevelopNow: () -> Unit,
    onNextMorning: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    Column(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.roll_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val left = pluralStringResource(R.plurals.roll_left, roll.left, roll.left)
        FilmStrip(exposed = roll.inCamera, modifier = Modifier.fillMaxWidth().height(58.dp).semantics { contentDescription = left })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                left,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSend, enabled = roll.inCamera > 0, shapes = ButtonDefaults.shapes()) {
                Icon(painterResource(R.drawable.ic_camera_roll), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.roll_send))
            }
        }

        if (roll.atLab.isNotEmpty()) {
            Text(
                stringResource(R.string.roll_at_lab),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
            roll.atLab.forEach { LabRollRow(it, nowMillis) }
            TextButton(onClick = onDevelopNow) { Text(stringResource(R.string.roll_develop_now)) }
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.roll_next_morning)) },
            supportingContent = { Text(stringResource(R.string.roll_next_morning_help)) },
            trailingContent = { Switch(checked = nextMorning, onCheckedChange = onNextMorning) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun LabRollRow(roll: LabRoll, nowMillis: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
            Icon(
                painterResource(R.drawable.ic_camera_roll),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(9.dp),
            )
        }
        Column {
            Text(
                pluralStringResource(R.plurals.roll_prints, roll.shots, roll.shots),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                readyLabel(roll.readyAtMillis, nowMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Ready tomorrow at 09:00", or today, or on a date; "ready" once the time has come. */
@Composable
fun readyLabel(readyAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (readyAtMillis <= nowMillis) return stringResource(R.string.roll_ready_now)
    val zone = ZoneId.systemDefault()
    val ready = Instant.ofEpochMilli(readyAtMillis).atZone(zone)
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val time = ready.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return when (ready.toLocalDate()) {
        today -> stringResource(R.string.roll_ready_today, time)
        today.plusDays(1) -> stringResource(R.string.roll_ready_tomorrow, time)
        else -> stringResource(R.string.roll_ready_on, ready.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)))
    }
}

/** The roll as a strip of negative: sprocket holes along both edges, the shot frames orange. */
@Composable
private fun FilmStrip(exposed: Int, modifier: Modifier = Modifier) {
    val unexposed = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier) {
        val frames = FilmLab.ROLL_SIZE
        val holeBand = size.height * 0.2f
        drawRoundRect(BASE, cornerRadius = CornerRadius(8.dp.toPx()))
        // Two holes per frame on each edge.
        val holes = frames * 2
        val holeStep = size.width / holes
        for (i in 0 until holes) {
            val x = i * holeStep + holeStep * 0.3f
            for (y in listOf(holeBand * 0.25f, size.height - holeBand * 0.75f)) {
                drawRoundRect(HOLE, Offset(x, y), Size(holeStep * 0.4f, holeBand * 0.5f), CornerRadius(1.5.dp.toPx()))
            }
        }
        val gap = 2.dp.toPx()
        val frameWidth = (size.width - gap * (frames + 1)) / frames
        for (i in 0 until frames) {
            drawRoundRect(
                color = if (i < exposed) EXPOSED else unexposed,
                topLeft = Offset(gap + i * (frameWidth + gap), holeBand),
                size = Size(frameWidth, size.height - holeBand * 2),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

private val BASE = Color(0xFF2A1A12)
private val HOLE = Color(0xFF0E0906)
private val EXPOSED = Color(0xFFE07A2E)
