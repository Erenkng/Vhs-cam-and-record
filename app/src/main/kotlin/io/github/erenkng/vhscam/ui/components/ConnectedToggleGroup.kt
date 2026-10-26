package io.github.erenkng.vhscam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Material 3 Expressive connected button group used as a single-choice selector: the outer
 * buttons keep their rounded ends, the selected one morphs into a pill.
 */
@Composable
fun <T> ConnectedToggleGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = ToggleButtonDefaults.size,
    colors: ToggleButtonColors = ToggleButtonDefaults.colors(),
    enabled: Boolean = true,
    fill: Boolean = false,
    icon: (@Composable (T) -> Unit)? = null,
    label: @Composable (T) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            val iconContent: (@Composable () -> Unit)? = if (icon != null) {
                { icon(option) }
            } else {
                null
            }
            ToggleButton(
                checked = option == selected,
                onCheckedChange = {
                    if (option != selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(option)
                },
                modifier = (if (fill) Modifier.weight(1f) else Modifier).semantics { role = Role.RadioButton },
                buttonSize = buttonSize,
                enabled = enabled,
                icon = iconContent,
                shapes = shapes,
                colors = colors,
            ) {
                label(option)
            }
        }
    }
}
