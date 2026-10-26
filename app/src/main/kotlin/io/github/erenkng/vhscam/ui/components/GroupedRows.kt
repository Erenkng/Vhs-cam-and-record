package io.github.erenkng.vhscam.ui.components

import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable

/**
 * Rows of grouped lists keep one card colour whatever their state: the switch or chevron in the
 * row shows the rest. (By default a checked row is tinted, which reads as a different card.)
 */
@Composable
fun groupedRowColors(): ListItemColors {
    val base = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    return ListItemDefaults.segmentedColors(
        containerColor = base.containerColor,
        disabledContainerColor = base.containerColor,
        selectedContainerColor = base.containerColor,
        selectedContentColor = base.contentColor,
        selectedLeadingContentColor = base.leadingContentColor,
        selectedTrailingContentColor = base.trailingContentColor,
        selectedOverlineContentColor = base.overlineContentColor,
        selectedSupportingContentColor = base.supportingContentColor,
    )
}

/** Connected toggle buttons on a sheet or page, where the default unchecked colour fades into the background. */
@Composable
fun groupToggleColors(): ToggleButtonColors =
    ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
