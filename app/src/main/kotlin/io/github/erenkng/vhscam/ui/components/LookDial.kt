package io.github.erenkng.vhscam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * A camera-style dial: the options scroll sideways and the one in the middle is chosen.
 * Flinging snaps to an option; tapping one scrolls it in. With [live] the choice follows the
 * finger while dragging (for values with an instant preview), otherwise it lands when the
 * scrolling stops.
 */
@Composable
fun <T> SnapDial(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    itemWidth: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    live: Boolean = false,
    item: @Composable (option: T, selected: Boolean, onClick: () -> Unit) -> Unit,
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val fling = rememberSnapFlingBehavior(listState, SnapPosition.Center)
    val haptics = LocalHapticFeedback.current
    val currentSelected by rememberUpdatedState(selected)
    val currentOptions by rememberUpdatedState(options)
    val currentOnSelect by rememberUpdatedState(onSelect)
    // Scrolling we start ourselves (to show a choice made elsewhere) must not choose anything.
    var programmatic by remember { mutableStateOf(false) }

    // Indices rather than options: an option may itself be null (a setting's "auto").
    fun middleIndex(): Int? {
        val info = listState.layoutInfo
        val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
        return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - centre) }?.index
    }

    fun choose(index: Int?) {
        if (index == null || programmatic || index !in currentOptions.indices) return
        val option = currentOptions[index]
        if (option == currentSelected) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        currentOnSelect(option)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            // Only when a scroll ends: the first value is just the dial at rest.
            .drop(1)
            .filter { inProgress -> !inProgress }
            .collect { choose(middleIndex()) }
    }
    if (live) {
        LaunchedEffect(listState) {
            snapshotFlow { if (listState.isScrollInProgress) middleIndex() else null }
                .distinctUntilChanged()
                .collect { choose(it) }
        }
    }
    // A choice made elsewhere (or by tapping) scrolls into the middle.
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress) {
            programmatic = true
            try {
                listState.animateScrollToItem(selectedIndex)
            } finally {
                programmatic = false
            }
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Fade the options out towards both edges.
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.16f to Color.Black,
                        0.84f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val side = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
        LazyRow(
            state = listState,
            flingBehavior = fling,
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(horizontal = side),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        ) {
            itemsIndexed(options) { index, option ->
                Box(Modifier.width(itemWidth), contentAlignment = Alignment.Center) {
                    item(option, index == selectedIndex) {
                        if (enabled && option != selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(option)
                        }
                    }
                }
            }
        }
    }
}

/** A text dial with a pill under the chosen option: the camera's mode switcher. */
@Composable
fun <T> LookDial(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SnapDial(options, selected, onSelect, itemWidth = 88.dp, height = 40.dp, modifier = modifier, enabled = enabled) { option, chosen, onClick ->
        DialItem(text = label(option), selected = chosen, enabled = enabled, onClick = onClick)
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
            .fillMaxWidth()
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
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
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

/**
 * Round thumbnails of what each option looks like, with its name under it, like a camera's
 * filter picker. The chosen one grows and gets a ring.
 */
@Composable
fun <T> ThumbnailDial(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    image: (T) -> Int,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SnapDial(options, selected, onSelect, itemWidth = 68.dp, height = 74.dp, modifier = modifier, enabled = enabled) { option, chosen, onClick ->
        val motion = MaterialTheme.motionScheme
        val colors = MaterialTheme.colorScheme
        val grow by animateFloatAsState(if (chosen) 1f else 0f, motion.fastSpatialSpec(), label = "grow")
        val ring by animateColorAsState(if (chosen) colors.primary else Color.White.copy(alpha = 0.25f), motion.defaultEffectsSpec(), label = "ring")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .semantics {
                    role = Role.RadioButton
                    this.selected = chosen
                },
        ) {
            Image(
                painter = painterResource(image(option)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .graphicsLayer {
                        val scale = 0.84f + 0.16f * grow
                        scaleX = scale
                        scaleY = scale
                        alpha = if (enabled) 1f else 0.5f
                    }
                    .size(48.dp)
                    .border(if (chosen) 2.5.dp else 1.dp, ring, CircleShape)
                    .padding(if (chosen) 4.dp else 0.dp)
                    .clip(CircleShape),
            )
            Text(
                text = label(option),
                style = if (chosen) MaterialTheme.typography.labelMediumEmphasized else MaterialTheme.typography.labelMedium,
                color = if (chosen) colors.primary else Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A ruler for a manual setting, the way the iPhone's camera dials look: fine ticks that follow
 * the finger past a fixed pointer, numbers only at the [major] stops, and the chosen value
 * large above the middle.
 */
@Composable
fun <T> RulerDial(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    major: (index: Int, option: T) -> Boolean = { _, _ -> true },
) {
    val accent = MaterialTheme.colorScheme.primary
    // Long scales get fine ticks; a handful of presets get room for every name.
    val dense = options.size > DENSE_OPTIONS
    Column(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label(selected),
            style = MaterialTheme.typography.titleMediumEmphasized.copy(fontFeatureSettings = "tnum"),
            color = accent,
            maxLines = 1,
        )
        Box(contentAlignment = Alignment.TopCenter) {
            SnapDial(
                options = options,
                selected = selected,
                onSelect = onSelect,
                itemWidth = if (dense) 20.dp else 64.dp,
                height = 40.dp,
                enabled = enabled,
                live = true,
            ) { option, chosen, onClick ->
                val index = options.indexOf(option)
                val isMajor = major(index, option)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clickable(enabled = enabled, onClick = onClick)
                        .padding(top = 6.dp),
                ) {
                    Box(
                        Modifier
                            .width(if (isMajor) 2.dp else 1.dp)
                            .height(if (isMajor) 14.dp else 8.dp)
                            .background(Color.White.copy(alpha = if (isMajor) 0.85f else 0.45f)),
                    )
                    if (isMajor) {
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (chosen) accent else Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                        )
                    }
                }
            }
            // The fixed pointer in the middle.
            Box(
                Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(accent, CircleShape),
            )
        }
    }
}

private const val DENSE_OPTIONS = 8
