package io.github.erenkng.vhscam.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Swallows haptic feedback when vibration is turned off in the settings. */
private object NoHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/** Makes [LocalHapticFeedback] silent inside [content] unless [enabled]. */
@Composable
fun ProvideHaptics(enabled: Boolean, content: @Composable () -> Unit) {
    val haptics = if (enabled) LocalHapticFeedback.current else NoHapticFeedback
    CompositionLocalProvider(LocalHapticFeedback provides haptics, content = content)
}
