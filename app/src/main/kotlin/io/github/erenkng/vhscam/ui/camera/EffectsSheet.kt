package io.github.erenkng.vhscam.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.camera.LookFactory
import io.github.erenkng.vhscam.core.look.FadeColor
import io.github.erenkng.vhscam.core.look.IntervalSetting
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.look.VhsAdjustments
import io.github.erenkng.vhscam.core.osd.CamcorderClock
import io.github.erenkng.vhscam.core.osd.ClockStyle
import io.github.erenkng.vhscam.core.osd.DateOrder
import io.github.erenkng.vhscam.core.osd.DateStamp
import io.github.erenkng.vhscam.settings.WhiteBalance
import io.github.erenkng.vhscam.ui.components.AppSlider
import io.github.erenkng.vhscam.ui.components.ConnectedToggleGroup
import io.github.erenkng.vhscam.ui.components.groupToggleColors
import io.github.erenkng.vhscam.ui.components.groupedRowColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Bottom sheet with everything about the look and the recording: tape, intensity, fader,
 * interval REC, OSD, camcorder clock, grid and pro controls.
 */
@Composable
fun EffectsSheet(state: CameraUiState, actions: CameraActions, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        EffectsSheetContent(state, actions)
    }
}

/** The sheet's content, separate from the sheet so it can be screenshot-tested. */
@Composable
fun EffectsSheetContent(state: CameraUiState, actions: CameraActions, modifier: Modifier = Modifier) {
    val settings = state.settings
    val look = state.activeLook
    val change = actions.onSettingsChange
    var showAdvanced by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf<LocalDate?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (look == Look.VHS) {
            section(R.string.sheet_tape)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.tape_speed), style = MaterialTheme.typography.titleSmallEmphasized)
                    ConnectedToggleGroup(
                        options = TapeSpeed.entries,
                        selected = settings.tapeSpeed,
                        onSelect = { speed -> change { it.copy(tapeSpeed = speed) } },
                        colors = groupToggleColors(),
                        fill = true,
                        modifier = Modifier.fillMaxWidth(),
                    ) { speed -> Text(stringResource(speed.label())) }
                    Text(
                        stringResource(R.string.tape_speed_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                LabeledSlider(R.string.wear, settings.vhs.wear, 0f..1f) { v ->
                    change { it.copy(vhs = it.vhs.copy(wear = v)) }
                }
            }
            item {
                val arrow by animateFloatAsState(if (showAdvanced) 90f else 0f, label = "arrow")
                ListItem(
                    onClick = { showAdvanced = !showAdvanced },
                    colors = groupedRowColors(),
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.rotate(arrow))
                    },
                ) {
                    Text(stringResource(R.string.advanced))
                }
            }
            item {
                AnimatedVisibility(visible = showAdvanced) {
                    AdvancedVhs(settings.vhs) { transform -> change { it.copy(vhs = transform(it.vhs)) } }
                }
            }
        } else {
            section(R.string.sheet_look)
            item {
                LabeledSlider(R.string.intensity, LookFactory.intensity(settings, look), 0f..1f) { v ->
                    change { LookFactory.withIntensity(it, look, v) }
                }
            }
        }

        if (state.isFilming) {
            section(R.string.sheet_recording)
            switchGroup(
                buildList {
                    add(SwitchRow(R.string.fade, settings.fade, help = R.string.fade_help) { v -> change { it.copy(fade = v) } })
                    if (look == Look.VHS) {
                        add(
                            SwitchRow(R.string.blue_intro, settings.blueIntro, help = R.string.blue_intro_help) { v ->
                                change { it.copy(blueIntro = v) }
                            },
                        )
                    }
                },
            )
            item {
                AnimatedVisibility(visible = settings.fade) {
                    ChoiceRow(
                        title = R.string.fade_color,
                        options = FadeColor.entries,
                        selected = settings.fadeColor,
                        onSelect = { color -> change { it.copy(fadeColor = color) } },
                    ) { color -> Text(stringResource(if (color == FadeColor.WHITE) R.string.fade_white else R.string.fade_black)) }
                }
            }
            item {
                // The interval loop is set up when recording starts.
                ChoiceRow(
                    title = R.string.interval_rec,
                    help = R.string.interval_help,
                    options = IntervalSetting.entries,
                    selected = settings.interval,
                    onSelect = { interval -> change { it.copy(interval = interval) } },
                    enabled = !state.isRecording,
                ) { interval -> Text(stringResource(interval.label()), maxLines = 1) }
            }
        }

        if (look in OSD_LOOKS) {
            section(R.string.sheet_osd)
            switchGroup(
                buildList {
                    add(SwitchRow(R.string.date_stamp, settings.dateStamp) { v -> change { it.copy(dateStamp = v) } })
                    if (look == Look.VHS) {
                        add(
                            SwitchRow(R.string.time_stamp, settings.timeStamp, enabled = settings.dateStamp) { v ->
                                change { it.copy(timeStamp = v) }
                            },
                        )
                        add(SwitchRow(R.string.play_osd, settings.playOsd) { v -> change { it.copy(playOsd = v) } })
                        add(SwitchRow(R.string.counter_osd, settings.counterOsd) { v -> change { it.copy(counterOsd = v) } })
                    }
                    add(
                        SwitchRow(R.string.clock_24h, settings.clockStyle == ClockStyle.H24) { v ->
                            change { it.copy(clockStyle = if (v) ClockStyle.H24 else ClockStyle.H12) }
                        },
                    )
                },
            )
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.date_format), style = MaterialTheme.typography.titleSmallEmphasized)
                    val sample = LookFactory.clockTime(settings)
                    ConnectedToggleGroup(
                        options = DateOrder.entries,
                        selected = settings.dateOrder,
                        onSelect = { order -> change { it.copy(dateOrder = order) } },
                        colors = groupToggleColors(),
                        buttonSize = ToggleButtonSize.ExtraSmall,
                        fill = true,
                        modifier = Modifier.fillMaxWidth(),
                    ) { order -> Text(DateStamp.vhsDate(sample, order), maxLines = 1) }
                }
            }
            item {
                val clockText = if (settings.clockOffsetMillis == null) {
                    stringResource(R.string.camcorder_clock_real)
                } else {
                    stringResource(R.string.camcorder_clock_custom, DateStamp.vhsDate(LookFactory.clockTime(settings), settings.dateOrder))
                }
                ListItem(
                    onClick = { pickDate = true },
                    colors = groupedRowColors(),
                    leadingContent = { Icon(painterResource(R.drawable.ic_calendar_month), contentDescription = null) },
                    supportingContent = { Text(clockText) },
                    trailingContent = if (settings.clockOffsetMillis != null) {
                        {
                            TextButton(onClick = { change { it.copy(clockOffsetMillis = null) } }) {
                                Text(stringResource(R.string.use_real_time))
                            }
                        }
                    } else {
                        null
                    },
                ) {
                    Text(stringResource(R.string.camcorder_clock))
                }
            }
        }

        section(R.string.sheet_viewfinder)
        switchGroup(listOf(SwitchRow(R.string.grid, settings.grid) { v -> change { it.copy(grid = v) } }))

        section(R.string.sheet_pro)
        item { ManualFocusControl(state, actions) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.white_balance), style = MaterialTheme.typography.titleSmallEmphasized)
                ConnectedToggleGroup(
                    options = WhiteBalance.entries,
                    selected = state.whiteBalance,
                    onSelect = actions.onWhiteBalance,
                    colors = groupToggleColors(),
                    buttonSize = ToggleButtonSize.ExtraSmall,
                    fill = true,
                    modifier = Modifier.fillMaxWidth(),
                ) { wb -> Icon(painterResource(wb.icon()), contentDescription = stringResource(wb.label())) }
            }
        }
    }

    if (pickDate) {
        val current = LookFactory.clockTime(settings)
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis
                    pickDate = false
                    if (millis != null) pickTime = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    pickTime?.let { date ->
        val current = LookFactory.clockTime(settings)
        val timeState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = settings.clockStyle == ClockStyle.H24,
        )
        AlertDialog(
            onDismissRequest = { pickTime = null },
            confirmButton = {
                TextButton(onClick = {
                    val target = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                    val clock = CamcorderClock.settingTo(target, System.currentTimeMillis(), ZoneId.systemDefault())
                    change { it.copy(clockOffsetMillis = clock.offsetMillis) }
                    pickTime = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickTime = null }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

private fun LazyListScope.section(title: Int) {
    item {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
    }
}

/** Looks that print a date or time into the picture. */
private val OSD_LOOKS = Look.entries.toSet() - setOf(Look.SUPER8, Look.POLAROID, Look.GAMEBOY, Look.WEBCAM)

private class SwitchRow(
    val title: Int,
    val checked: Boolean,
    val enabled: Boolean = true,
    val help: Int? = null,
    val onChange: (Boolean) -> Unit,
)

/** Switch rows drawn as one rounded M3E segmented group. */
private fun LazyListScope.switchGroup(rows: List<SwitchRow>) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            rows.forEachIndexed { index, row ->
                SegmentedListItem(
                    checked = row.checked,
                    onCheckedChange = row.onChange,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = rows.size),
                    enabled = row.enabled,
                    colors = groupedRowColors(),
                    supportingContent = if (row.help != null) {
                        { Text(stringResource(row.help)) }
                    } else {
                        null
                    },
                    trailingContent = { Switch(checked = row.checked, onCheckedChange = null, enabled = row.enabled) },
                ) {
                    Text(stringResource(row.title))
                }
            }
        }
    }
}

/** A titled single-choice button group. */
@Composable
private fun <T> ChoiceRow(
    title: Int,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    help: Int? = null,
    enabled: Boolean = true,
    label: @Composable (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmallEmphasized)
        if (help != null) {
            Text(
                stringResource(help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ConnectedToggleGroup(
            options = options,
            selected = selected,
            onSelect = onSelect,
            colors = groupToggleColors(),
            buttonSize = ToggleButtonSize.ExtraSmall,
            enabled = enabled,
            fill = true,
            modifier = Modifier.fillMaxWidth(),
            label = label,
        )
    }
}

private fun IntervalSetting.label(): Int = when (this) {
    IntervalSetting.OFF -> R.string.interval_off
    IntervalSetting.S10 -> R.string.interval_10s
    IntervalSetting.S30 -> R.string.interval_30s
    IntervalSetting.M1 -> R.string.interval_1m
}

@Composable
private fun LabeledSlider(
    title: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                "${((value - range.start) / (range.endInclusive - range.start) * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppSlider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AdvancedVhs(vhs: VhsAdjustments, onChange: ((VhsAdjustments) -> VhsAdjustments) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val range = 0f..2f
        LabeledSlider(R.string.color_bleed, vhs.colorBleed, range) { v -> onChange { it.copy(colorBleed = v) } }
        LabeledSlider(R.string.noise, vhs.noise, range) { v -> onChange { it.copy(noise = v) } }
        LabeledSlider(R.string.tracking, vhs.tracking, range) { v -> onChange { it.copy(tracking = v) } }
        LabeledSlider(R.string.jitter, vhs.jitter, range) { v -> onChange { it.copy(jitter = v) } }
        LabeledSlider(R.string.sharpen, vhs.sharpen, range) { v -> onChange { it.copy(sharpen = v) } }
        LabeledSlider(R.string.head_switch, vhs.headSwitch, range) { v -> onChange { it.copy(headSwitch = v) } }
        LabeledSlider(R.string.scanlines, vhs.scanlines, range) { v -> onChange { it.copy(scanlines = v) } }
        LabeledSlider(R.string.vignette, vhs.vignette, range) { v -> onChange { it.copy(vignette = v) } }
        Spacer(Modifier.height(4.dp))
        FilledTonalButton(
            onClick = { onChange { VhsAdjustments(wear = it.wear) } },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(painterResource(R.drawable.ic_restart_alt), contentDescription = null)
            Spacer(Modifier.padding(start = 6.dp))
            Text(stringResource(R.string.reset))
        }
    }
}

@Composable
private fun ManualFocusControl(state: CameraUiState, actions: CameraActions) {
    val capabilities = state.capabilities
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ListItem(
            checked = state.manualFocus != null,
            onCheckedChange = { on -> actions.onManualFocus(if (on) capabilities.minFocusDiopters / 4f else null) },
            enabled = capabilities.manualFocus,
            colors = groupedRowColors(),
            supportingContent = if (!capabilities.manualFocus) {
                { Text(stringResource(R.string.manual_focus_unavailable)) }
            } else {
                null
            },
            trailingContent = {
                Switch(checked = state.manualFocus != null, onCheckedChange = null, enabled = capabilities.manualFocus)
            },
        ) {
            Text(stringResource(R.string.manual_focus))
        }
        val focus = state.manualFocus
        if (focus != null && capabilities.manualFocus) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_filter_hdr), contentDescription = stringResource(R.string.focus_far))
                // Diopters: 0 is infinity, larger is closer. Show far on the left like a lens barrel.
                AppSlider(
                    value = focus,
                    onValueChange = { actions.onManualFocus(it) },
                    valueRange = 0f..capabilities.minFocusDiopters,
                    modifier = Modifier.weight(1f),
                )
                Icon(painterResource(R.drawable.ic_local_florist), contentDescription = stringResource(R.string.focus_near))
            }
        }
    }
}

private fun WhiteBalance.icon(): Int = when (this) {
    WhiteBalance.AUTO -> R.drawable.ic_wb_auto
    WhiteBalance.DAYLIGHT -> R.drawable.ic_wb_sunny
    WhiteBalance.CLOUDY -> R.drawable.ic_wb_cloudy
    WhiteBalance.INCANDESCENT -> R.drawable.ic_wb_incandescent
    WhiteBalance.FLUORESCENT -> R.drawable.ic_wb_iridescent
}

private fun WhiteBalance.label(): Int = when (this) {
    WhiteBalance.AUTO -> R.string.wb_auto
    WhiteBalance.DAYLIGHT -> R.string.wb_daylight
    WhiteBalance.CLOUDY -> R.string.wb_cloudy
    WhiteBalance.INCANDESCENT -> R.string.wb_incandescent
    WhiteBalance.FLUORESCENT -> R.string.wb_fluorescent
}
