package io.github.erenkng.vhscam.ui.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A value/callback slider on top of the state-based Material 3 Expressive sliders.
 * External value changes are applied unless the user is dragging.
 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    vertical: Boolean = false,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val state = remember(valueRange, steps) {
        SliderState(value = value.coerceIn(valueRange), steps = steps, trackRange = valueRange)
    }
    LaunchedEffect(value) {
        if (!state.isDragging) state.value = value.coerceIn(valueRange)
    }
    val onChange: (Float) -> Unit = {
        state.value = it
        onValueChange(it)
    }
    if (vertical) {
        VerticalSlider(
            state = state,
            onValueChange = onChange,
            modifier = modifier,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
            topToBottom = false,
        )
    } else {
        Slider(
            state = state,
            onValueChange = onChange,
            modifier = modifier,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}
