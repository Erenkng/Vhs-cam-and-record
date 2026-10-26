package io.github.erenkng.vhscam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

private val ItemWidth = 88.dp

/**
 * A camera-style mode dial: the options scroll sideways and the one in the middle is chosen,
 * with a pill that grows under it. Flinging snaps to an option; tapping one scrolls it in.
 */
@Composable
fun <T> LookDial(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val fling = rememberSnapFlingBehavior(listState, SnapPosition.Center)
    val haptics = LocalHapticFeedback.current
    val currentSelected by rememberUpdatedState(selected)
    val currentOptions by rememberUpdatedState(options)
    val currentOnSelect by rememberUpdatedState(onSelect)

    // Whatever ends up in the middle once scrolling stops becomes the selection.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { inProgress -> !inProgress }
            .collect {
                val info = listState.layoutInfo
                val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
                val middle = info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - centre) } ?: return@collect
                val option = currentOptions.getOrNull(middle.index) ?: return@collect
                if (option != currentSelected) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    currentOnSelect(option)
                }
            }
    }
    // A selection made elsewhere (or by tapping) scrolls into the middle.
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress) listState.animateScrollToItem(selectedIndex)
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Fade the options out towards both edges.
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.18f to Color.Black,
                        0.82f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val side = ((maxWidth - ItemWidth) / 2).coerceAtLeast(0.dp)
        LazyRow(
            state = listState,
            flingBehavior = fling,
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(horizontal = side),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(options) { index, option ->
                DialItem(
                    text = label(option),
                    selected = index == selectedIndex,
                    enabled = enabled,
                    onClick = {
                        if (option != selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(option)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DialItem(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val pill by animateFloatAsState(if (selected) 1f else 0f, motion.fastSpatialSpec(), label = "pill")
    val textColor by animateColorAsState(
        if (selected) colors.onPrimaryContainer else Color.White.copy(alpha = if (enabled) 0.82f else 0.4f),
        motion.defaultEffectsSpec(),
        label = "text",
    )
    Box(
        modifier = Modifier
            .width(ItemWidth)
            .height(40.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = 0.6f + 0.4f * pill
                    scaleY = 0.6f + 0.4f * pill
                    alpha = pill
                }
                .width(ItemWidth - 8.dp)
                .height(34.dp)
                .background(colors.primaryContainer, CircleShape),
        )
        Text(
            text = text,
            color = textColor,
            style = if (selected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
