package io.github.erenkng.vhscam.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.core.exposure.ManualExposure
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.settings.WhiteBalance
import io.github.erenkng.vhscam.ui.components.RulerDial
import io.github.erenkng.vhscam.ui.components.ThumbnailDial
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** The settings of Pro mode, in the order they sit along the bottom. */
enum class ProSetting { LOOK, ISO, SPEED, EV, FOCUS, WB }

/**
 * Pro mode's manual controls, laid out like a phone's pro camera: a row of settings with
 * their values, and a ruler for the one being changed. Settings on auto show what the camera
 * chose, dimmed.
 */
@Composable
fun ProPanel(state: CameraUiState, actions: CameraActions, open: ProSetting?, onOpen: (ProSetting) -> Unit) {
    val caps = state.capabilities
    val exposure = state.exposure
    val auto = stringResource(R.string.pro_auto)
    val af = stringResource(R.string.pro_af)
    val awb = stringResource(R.string.pro_awb)
    val isoRange = caps.isoRange
    val timeRange = caps.exposureTimeRange
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AnimatedContent(
            targetState = open,
            transitionSpec = {
                (fadeIn() + expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) togetherWith
                    fadeOut() + shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()))
                    .using(SizeTransform(clip = false))
            },
            label = "proDial",
        ) { setting ->
            when (setting) {
                null -> Spacer(Modifier.fillMaxWidth().height(0.dp))
                ProSetting.LOOK -> ThumbnailDial(
                    options = Look.entries,
                    selected = state.settings.photoLook,
                    onSelect = { look -> actions.onSettingsChange { it.copy(photoLook = look) } },
                    image = { it.sample() },
                    label = { stringResource(it.label()) },
                )
                ProSetting.ISO -> RulerDial(
                    options = listOf<Int?>(null) + (isoRange?.let { ManualExposure.isoStops(it) } ?: emptyList()),
                    selected = state.iso,
                    onSelect = actions.onIso,
                    label = { it?.toString() ?: auto },
                )
                ProSetting.SPEED -> RulerDial(
                    options = listOf<Long?>(null) + (timeRange?.let { ManualExposure.shutterStops(it) } ?: emptyList()),
                    selected = state.shutterNanos,
                    onSelect = actions.onShutterSpeed,
                    label = { it?.let(ManualExposure::formatShutter) ?: auto },
                )
                ProSetting.EV -> RulerDial(
                    options = (exposure.min..exposure.max).toList(),
                    selected = exposure.index,
                    onSelect = actions.onExposure,
                    label = { formatEv(it * exposure.step) },
                    enabled = !state.manualExposure,
                )
                ProSetting.FOCUS -> RulerDial(
                    options = listOf<Float?>(null) + FocusStops.filter { it <= caps.minFocusDiopters },
                    selected = state.manualFocus,
                    onSelect = actions.onManualFocus,
                    label = { focusLabel(it, af) },
                )
                ProSetting.WB -> RulerDial(
                    options = WhiteBalanceOrder,
                    selected = state.whiteBalance,
                    onSelect = actions.onWhiteBalance,
                    label = { whiteBalanceLabel(it, awb) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProCell(
                title = stringResource(R.string.pro_look),
                value = stringResource(state.settings.photoLook.label()),
                manual = false,
                open = open == ProSetting.LOOK,
                onClick = { onOpen(ProSetting.LOOK) },
            )
            ProCell(
                title = stringResource(R.string.pro_iso),
                value = state.iso?.toString() ?: auto,
                hint = state.meter?.iso?.toString()?.takeIf { state.iso == null },
                manual = state.iso != null,
                open = open == ProSetting.ISO,
                enabled = caps.manualExposure,
                onClick = { onOpen(ProSetting.ISO) },
            )
            ProCell(
                title = stringResource(R.string.pro_speed),
                value = state.shutterNanos?.let(ManualExposure::formatShutter) ?: auto,
                hint = state.meter?.timeNanos?.let(ManualExposure::formatShutter)?.takeIf { state.shutterNanos == null },
                manual = state.shutterNanos != null,
                open = open == ProSetting.SPEED,
                enabled = caps.manualExposure,
                onClick = { onOpen(ProSetting.SPEED) },
            )
            ProCell(
                title = stringResource(R.string.pro_ev),
                value = formatEv(exposure.index * exposure.step),
                manual = exposure.index != 0,
                open = open == ProSetting.EV,
                // Exposure compensation steers auto exposure, so it does nothing in manual.
                enabled = exposure.supported && !state.manualExposure,
                onClick = { onOpen(ProSetting.EV) },
            )
            ProCell(
                title = stringResource(R.string.pro_focus),
                value = focusLabel(state.manualFocus, af),
                manual = state.manualFocus != null,
                open = open == ProSetting.FOCUS,
                enabled = caps.manualFocus,
                onClick = { onOpen(ProSetting.FOCUS) },
            )
            ProCell(
                title = stringResource(R.string.pro_wb),
                value = whiteBalanceLabel(state.whiteBalance, awb),
                manual = state.whiteBalance != WhiteBalance.AUTO,
                open = open == ProSetting.WB,
                onClick = { onOpen(ProSetting.WB) },
            )
        }
    }
}

/** One setting: its name, its value (in the accent colour when set by hand) and what auto chose. */
@Composable
private fun ProCell(
    title: String,
    value: String,
    manual: Boolean,
    open: Boolean,
    onClick: () -> Unit,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val motion = MaterialTheme.motionScheme
    val accent = MaterialTheme.colorScheme.primary
    val valueColor by animateColorAsState(if (manual || open) accent else Color.White, motion.defaultEffectsSpec(), label = "value")
    val marker by animateFloatAsState(if (open) 1f else 0f, motion.fastSpatialSpec(), label = "marker")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .widthIn(min = 52.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Tab
                selected = open
            }
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Color.White.copy(alpha = 0.6f),
        )
        Text(text = value, style = MaterialTheme.typography.labelLargeEmphasized, color = valueColor, maxLines = 1)
        Text(
            text = hint ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            maxLines = 1,
        )
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = marker
                    alpha = marker
                }
                .width(16.dp)
                .height(3.dp)
                .background(accent, CircleShape),
        )
    }
}

/** Focus distances on the ruler, in diopters: infinity, 5 m, 2 m, 1 m, 50, 30, 20 and 10 cm. */
private val FocusStops = listOf(0f, 0.2f, 0.5f, 1f, 2f, 10f / 3f, 5f, 10f)

/** White balance presets from warm light to shade, as their colour temperatures. */
private val WhiteBalanceOrder = listOf(
    WhiteBalance.AUTO,
    WhiteBalance.INCANDESCENT,
    WhiteBalance.FLUORESCENT,
    WhiteBalance.DAYLIGHT,
    WhiteBalance.CLOUDY,
)

private fun focusLabel(diopters: Float?, autofocus: String): String = when {
    diopters == null -> autofocus
    diopters <= 0f -> "∞"
    diopters <= 1f -> "${(1f / diopters).roundToInt()}m"
    else -> "${(100f / diopters).roundToInt()}cm"
}

private fun whiteBalanceLabel(whiteBalance: WhiteBalance, auto: String): String = when (whiteBalance) {
    WhiteBalance.AUTO -> auto
    WhiteBalance.INCANDESCENT -> "2800K"
    WhiteBalance.FLUORESCENT -> "4000K"
    WhiteBalance.DAYLIGHT -> "5500K"
    WhiteBalance.CLOUDY -> "6500K"
}

/** Exposure compensation as cameras show it: `+0.7`, `0.0`, `−1.3`. */
private fun formatEv(ev: Float): String {
    val rounded = (ev * 10f).roundToInt() / 10f
    return when {
        abs(rounded) < 0.05f -> "0.0"
        rounded > 0f -> "+" + String.format(Locale.US, "%.1f", rounded)
        else -> "−" + String.format(Locale.US, "%.1f", -rounded)
    }
}
