package io.github.erenkng.vhscam.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.LookCategory
import kotlinx.coroutines.launch

/** Every look to choose from, as a sheet: grouped by kind of camera, with a sample of each. */
@Composable
fun LookBrowserSheet(options: List<Look>, selected: Look, onSelect: (Look) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LookBrowserContent(
            options = options,
            selected = selected,
            onSelect = { look ->
                onSelect(look)
                // Let the sheet slide away before it leaves the composition.
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
        )
    }
}

/** The browser without the sheet around it, so it can be screenshot-tested. */
@Composable
fun LookBrowserContent(options: List<Look>, selected: Look, onSelect: (Look) -> Unit, modifier: Modifier = Modifier) {
    val sections = remember(options) { options.groupBy { it.category }.toSortedMap() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(152.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.navigationBarsPadding(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                Text(
                    stringResource(R.string.looks_title),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    pluralStringResource(R.plurals.looks_count, options.size, options.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        sections.forEach { (category, looks) ->
            item(span = { GridItemSpan(maxLineSpan) }, key = category.name) {
                CategoryHeader(category, count = looks.size)
            }
            items(looks, key = { it.name }) { look ->
                LookCard(look = look, selected = look == selected, onClick = { onSelect(look) })
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: LookCategory, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp, top = 10.dp).semantics { heading() },
    ) {
        Text(
            stringResource(category.label()),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
    }
}

/** A look's sample photo with its name and what it is like; the chosen one is outlined and ticked. */
@Composable
private fun LookCard(look: Look, selected: Boolean, onClick: () -> Unit) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, motion.fastSpatialSpec(), label = "press")
    val outline by animateDpAsState(if (selected) 3.dp else 0.dp, motion.fastSpatialSpec(), label = "outline")
    val outlineColor by animateColorAsState(if (selected) colors.primary else Color.Transparent, motion.defaultEffectsSpec(), label = "outlineColor")
    val shape = RoundedCornerShape(20.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
    ) {
        Box {
            Image(
                painter = painterResource(look.sample()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(shape)
                    .border(outline, outlineColor, shape),
            )
            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
            if (look.video) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_videocam),
                        contentDescription = stringResource(R.string.look_records_video),
                        modifier = Modifier.padding(4.dp).size(14.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(
                stringResource(look.label()),
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = if (selected) colors.primary else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(look.description()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun LookCategory.label(): Int = when (this) {
    LookCategory.HOME_VIDEO -> R.string.category_home_video
    LookCategory.FILM -> R.string.category_film
    LookCategory.DIGITAL -> R.string.category_digital
}

/** One line on what a look is like. */
fun Look.description(): Int = when (this) {
    Look.VHS -> R.string.look_desc_vhs
    Look.SUPER8 -> R.string.look_desc_super8
    Look.FILM -> R.string.look_desc_film
    Look.GOLD -> R.string.look_desc_gold
    Look.PORTRAIT -> R.string.look_desc_portrait
    Look.CINE -> R.string.look_desc_cine
    Look.RED_GRAIN -> R.string.look_desc_red_grain
    Look.REDSCALE -> R.string.look_desc_redscale
    Look.DISPOSABLE -> R.string.look_desc_disposable
    Look.POLAROID -> R.string.look_desc_polaroid
    Look.CCD -> R.string.look_desc_ccd
    Look.GAMEBOY -> R.string.look_desc_gameboy
    Look.WEBCAM -> R.string.look_desc_webcam
    Look.CCTV -> R.string.look_desc_cctv
}
