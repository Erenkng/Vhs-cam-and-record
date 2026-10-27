package io.github.erenkng.vhscam.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.ui.theme.CameraColors
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme
import io.github.erenkng.vhscam.ui.theme.Vt323
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.floor

private class Page(val title: Int, val body: Int)

private val Pages = listOf(
    Page(R.string.onboarding_1_title, R.string.onboarding_1_body),
    Page(R.string.onboarding_2_title, R.string.onboarding_2_body),
    Page(R.string.onboarding_3_title, R.string.onboarding_3_body),
    Page(R.string.onboarding_4_title, R.string.onboarding_4_body),
)

/** Introduces the app on first launch. [onDone] runs once the screen has switched itself off. */
@Composable
fun OnboardingScreen(dynamicColor: Boolean, onDone: () -> Unit) {
    VhsCamTheme(darkTheme = true, dynamicColor = dynamicColor) {
        OnboardingContent(onDone = onDone)
    }
}

/** The onboarding pager, separate from the theme so it can be screenshot-tested. */
@Composable
fun OnboardingContent(onDone: () -> Unit, modifier: Modifier = Modifier, initialPage: Int = 0) {
    val pager = rememberPagerState(initialPage = initialPage) { Pages.size }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val last = pager.currentPage == Pages.lastIndex
    // Leaving switches the screen off like an old TV: down to a line, then to a dot.
    val powerOff = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    val finish = {
        if (!leaving) {
            leaving = true
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            scope.launch {
                powerOff.animateTo(1f, tween(POWER_OFF_MILLIS, easing = FastOutLinearInEasing))
                onDone()
            }
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = powerOff.value
                    val collapse = (p / 0.6f).coerceIn(0f, 1f)
                    val shrink = ((p - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    scaleY = 1f - collapse * 0.994f
                    scaleX = 1f - shrink
                }
                .background(Background),
        ) {
            Backdrop(pager)
            // Faint CRT lines behind everything, so text and buttons stay clean.
            Scanlines()
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                TopRow(showSkip = !last, onSkip = finish)
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = !leaving,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { page ->
                    PageContent(page, pager)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PageIndicator(pager)
                    NextButton(
                        last = last,
                        onClick = {
                            if (last) {
                                finish()
                            } else {
                                scope.launch {
                                    pager.animateScrollToPage(
                                        pager.currentPage + 1,
                                        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                                    )
                                }
                            }
                        },
                    )
                }
            }
            // The picture flares white as the tube switches off.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (powerOff.value * 1.6f).coerceIn(0f, 0.9f) }
                    .background(Color.White),
            )
        }
    }
}

private val Background = Color(0xFF0C0B0F)

/** A soft light behind the art that takes each page's colour as the pages slide. */
@Composable
private fun Backdrop(pager: PagerState) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF3DDC84),
        MaterialTheme.colorScheme.secondary,
    )
    val breathe = rememberInfiniteTransition(label = "backdrop")
    val pulse by breathe.animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(4_000), RepeatMode.Reverse), label = "pulse")
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val position = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, (colors.size - 1).toFloat())
                val from = floor(position).toInt()
                val to = (from + 1).coerceAtMost(colors.lastIndex)
                val color = lerp(colors[from], colors[to], position - from)
                drawRect(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.36f),
                        radius = size.width * 0.75f * pulse,
                    ),
                )
            },
    )
}

/** Faint CRT lines. */
@Composable
private fun Scanlines() {
    Canvas(Modifier.fillMaxSize()) {
        val pitch = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawRect(Color.Black.copy(alpha = 0.12f), topLeft = Offset(0f, y), size = Size(size.width, pitch / 3f))
            y += pitch
        }
    }
}

@Composable
private fun TopRow(showSkip: Boolean, onSkip: () -> Unit) {
    val blink = rememberInfiniteTransition(label = "rec")
    val dot by blink.animateFloat(1f, 0.2f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot")
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).graphicsLayer { alpha = dot }.background(CameraColors.Record, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "VHS CAM",
            style = TextStyle(fontFamily = Vt323, fontSize = 24.sp, letterSpacing = 2.sp),
            color = Color.White,
        )
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(visible = showSkip, enter = fadeIn(), exit = fadeOut()) {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
        }
    }
}

/** A page: its art sliding a little slower than the text, for depth. */
@Composable
private fun PageContent(page: Int, pager: PagerState) {
    val active = pager.settledPage == page
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    val distance = pageDistance(pager.currentPage, pager.currentPageOffsetFraction, page)
                    val shown = pageVisibility(distance)
                    translationX = distance * size.width * 0.45f
                    alpha = shown
                    scaleX = 0.88f + 0.12f * shown
                    scaleY = 0.88f + 0.12f * shown
                },
            contentAlignment = Alignment.Center,
        ) {
            when (page) {
                0 -> CassetteArt(active)
                1 -> LookCardFan(active)
                2 -> EffectsShowcase(active)
                else -> ShareShowcase(active)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    val distance = pageDistance(pager.currentPage, pager.currentPageOffsetFraction, page)
                    translationX = distance * size.width * 0.15f
                    alpha = pageVisibility(distance)
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Pages[page].title),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Pages[page].body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 3,
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** Dots that stretch into a pill under the current page, following the finger. */
@Composable
private fun PageIndicator(pager: PagerState) {
    val accent = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(Pages.size) { page ->
            val shown = pageVisibility(pageDistance(pager.currentPage, pager.currentPageOffsetFraction, page))
            Box(
                Modifier
                    .height(8.dp)
                    .width(lerp(8.dp, 28.dp, shown))
                    .background(lerp(Color.White.copy(alpha = 0.28f), accent, shown), CircleShape),
            )
        }
    }
}

@Composable
private fun NextButton(last: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.height(56.dp).animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
    ) {
        AnimatedContent(
            targetState = last,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "next",
        ) { isLast ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Icon(
                    painterResource(if (isLast) R.drawable.ic_play_arrow else R.drawable.ic_arrow_forward),
                    contentDescription = null,
                )
            }
        }
    }
}

private const val POWER_OFF_MILLIS = 460
