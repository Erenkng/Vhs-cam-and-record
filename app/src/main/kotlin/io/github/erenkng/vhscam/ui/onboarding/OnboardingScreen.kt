package io.github.erenkng.vhscam.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

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
                    // Neighbours stay composed, so their pictures are decoded before a swipe.
                    beyondViewportPageCount = 1,
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

/**
 * Soft lights drifting slowly behind the art. They take each page's colour as the pages slide,
 * and slide a little against them.
 */
@Composable
private fun Backdrop(pager: PagerState) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF3DDC84),
        MaterialTheme.colorScheme.secondary,
    )
    val drift = rememberInfiniteTransition(label = "backdrop")
    val turn by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(BACKDROP_MILLIS, easing = LinearEasing)), label = "turn")
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val position = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, (colors.size - 1).toFloat())
                val from = floor(position).toInt()
                val to = (from + 1).coerceAtMost(colors.lastIndex)
                val main = lerp(colors[from], colors[to], position - from)
                val other = lerp(colors[(from + 1) % colors.size], colors[(to + 1) % colors.size], position - from)
                val a = turn * 2f * PI.toFloat()
                val w = size.width
                val h = size.height
                val slide = position * w * 0.06f
                light(main, 0.34f, Offset(w * 0.5f + cos(a) * w * 0.14f - slide, h * 0.34f + sin(a) * h * 0.04f), w * 0.8f)
                light(other, 0.2f, Offset(w * 0.15f + sin(2f * a) * w * 0.1f - slide * 0.5f, h * 0.6f + cos(a) * h * 0.05f), w * 0.6f)
                light(
                    lerp(main, Color.White, 0.3f),
                    0.12f,
                    Offset(w * 0.88f + cos(a + 2f) * w * 0.08f - slide * 1.5f, h * 0.16f + sin(1.5f * a) * h * 0.04f),
                    w * 0.5f,
                )
            },
    )
}

private fun DrawScope.light(color: Color, alpha: Float, center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
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

/**
 * A page: its art sliding a little slower than the text, for depth. Like changing channels on
 * an old TV, the art fills with static while it slides and tunes in with a burst of it when it
 * settles, the channel number showing in green; the title and text then spring into place.
 */
@Composable
private fun PageContent(page: Int, pager: PagerState) {
    val active = pager.settledPage == page
    val distance = { pageDistance(pager.currentPage, pager.currentPageOffsetFraction, page) }
    val snow = rememberSnow()
    val tune = remember { Animatable(0f) }
    val titleRise = remember { Animatable(1f) }
    val bodyRise = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active) {
            launch {
                tune.snapTo(1f)
                tune.animateTo(0f, tween(TUNE_MILLIS, easing = LinearEasing))
            }
            launch { titleRise.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
            launch {
                delay(BODY_DELAY_MILLIS)
                bodyRise.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
            }
        } else {
            // Off screen by now: ready to rise again next time.
            tune.snapTo(0f)
            titleRise.snapTo(1f)
            bodyRise.snapTo(1f)
        }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    val d = distance()
                    val shown = pageVisibility(d)
                    val noise = channelNoise(d, tune.value)
                    // The picture jumps sideways while the static is on.
                    val jitter = if (noise > MIN_STATIC) (Random(staticSeed(d, tune.value)).nextFloat() - 0.5f) * noise * Jitter.toPx() else 0f
                    translationX = d * size.width * 0.45f + jitter
                    alpha = shown
                    scaleX = 0.88f + 0.12f * shown
                    scaleY = 0.88f + 0.12f * shown
                    // Static covers only the art, which takes a layer of its own while it shows.
                    compositingStrategy = if (noise > MIN_STATIC) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                }
                .drawWithContent {
                    drawContent()
                    val d = distance()
                    drawStatic(snow, channelNoise(d, tune.value), staticSeed(d, tune.value), BlendMode.SrcAtop)
                },
            contentAlignment = Alignment.Center,
        ) {
            when (page) {
                0 -> CassetteArt(active, scrub = distance)
                1 -> LookCoverFlow(active)
                2 -> EffectsShowcase(active)
                else -> ShareShowcase(active)
            }
            ChannelOsd(channel = page + 1, active = active, modifier = Modifier.align(Alignment.TopEnd))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    val d = distance()
                    translationX = d * size.width * 0.15f
                    alpha = pageVisibility(d)
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Pages[page].title),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    translationY = titleRise.value * TitleRise.toPx()
                    alpha = 1f - titleRise.value * 0.6f
                },
            )
            Text(
                text = stringResource(Pages[page].body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 3,
                modifier = Modifier.graphicsLayer {
                    translationY = bodyRise.value * BodyRise.toPx()
                    alpha = 1f - bodyRise.value * 0.7f
                },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** Static over a page: some while it slides, most halfway, and a burst as it tunes in. */
private fun channelNoise(distance: Float, tune: Float): Float {
    val sliding = sin(PI.toFloat() * abs(distance).coerceAtMost(1f))
    return max(sliding * SLIDE_STATIC, tune * TUNE_STATIC)
}

/** A different seed for every frame of the slide and of the burst, so the static moves. */
private fun staticSeed(distance: Float, tune: Float): Int = (abs(distance) * 4_000f).toInt() * 31 + (tune * 1_000f).toInt()

/** The channel number an old TV shows in green for a moment after switching. */
@Composable
private fun ChannelOsd(channel: Int, active: Boolean, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            shown.snapTo(1f)
            delay(CHANNEL_MILLIS)
            shown.animateTo(0f, tween(CHANNEL_FADE_MILLIS))
        } else {
            shown.snapTo(0f)
        }
    }
    Text(
        text = String.format(Locale.ROOT, "CH %02d", channel),
        style = TextStyle(
            fontFamily = Vt323,
            fontSize = 34.sp,
            letterSpacing = 2.sp,
            shadow = Shadow(OsdGreen.copy(alpha = 0.7f), blurRadius = 14f),
        ),
        color = OsdGreen,
        modifier = modifier
            .padding(top = 4.dp, end = 24.dp)
            .graphicsLayer { alpha = shown.value },
    )
}

private val OsdGreen = Color(0xFF6CFF7A)
private val Jitter = 14.dp
private val TitleRise = 26.dp
private val BodyRise = 34.dp

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
private const val BACKDROP_MILLIS = 18_000
private const val TUNE_MILLIS = 450
private const val BODY_DELAY_MILLIS = 70L
private const val CHANNEL_MILLIS = 2_200L
private const val CHANNEL_FADE_MILLIS = 400
private const val SLIDE_STATIC = 0.55f
private const val TUNE_STATIC = 0.5f
private const val MIN_STATIC = 0.005f
