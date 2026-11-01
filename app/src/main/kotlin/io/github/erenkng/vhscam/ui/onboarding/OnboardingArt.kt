package io.github.erenkng.vhscam.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.ui.camera.label
import io.github.erenkng.vhscam.ui.camera.sample
import io.github.erenkng.vhscam.ui.theme.CameraColors
import io.github.erenkng.vhscam.ui.theme.Vt323
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/** Real proportions of a VHS cassette, 187 x 103 mm. */
private const val CASSETTE_ASPECT = 187f / 103f

/** What the deck is doing with the tape, the way its display says it. */
private enum class Deck(val label: String) {
    PLAY("PLAY ▶"),
    FAST_FORWARD("▶▶ FF"),
    REWIND("◀◀ REW"),
}

/**
 * A VHS cassette drawn from scratch: label, window, and two reels winding the tape from one to
 * the other, with the deck's display and tape counter under it. The first time [active] turns
 * true it flips down onto the table with a spring; after that light glints across its shell
 * now and then. [scrub] is how far the pages have been dragged from this one: dragging on
 * fast-forwards the tape, dragging back rewinds it.
 */
@Composable
fun CassetteArt(active: Boolean, modifier: Modifier = Modifier, scrub: () -> Float = { 0f }) {
    val currentScrub by rememberUpdatedState(scrub)
    var angle by remember { mutableFloatStateOf(0f) }
    var played by remember { mutableFloatStateOf(0f) }
    var deck by remember { mutableStateOf(Deck.PLAY) }
    LaunchedEffect(Unit) {
        var last = -1L
        var lastScrub = currentScrub()
        var speed = 1f
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                val dt = if (last < 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
                val position = currentScrub()
                val pagesPerSecond = if (dt > 0f) (position - lastScrub) / dt else 0f
                lastScrub = position
                // Ease towards the new speed so a flick winds up and down like a motor.
                speed += (1f + pagesPerSecond * SHUTTLE_GAIN - speed) * (dt * 10f).coerceAtMost(1f)
                deck = when {
                    speed > 3f -> Deck.FAST_FORWARD
                    speed < -1f -> Deck.REWIND
                    else -> Deck.PLAY
                }
                angle = (angle + dt * REEL_DEGREES_PER_SECOND * speed) % 360f
                played = (played + dt * speed).coerceAtLeast(0f)
            }
        }
    }
    val drop = remember { Animatable(1f) }
    val sheen = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        if (drop.value > 0f) drop.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        while (true) {
            sheen.snapTo(0f)
            sheen.animateTo(1f, tween(SHEEN_MILLIS, easing = FastOutSlowInEasing))
            delay(SHEEN_PAUSE_MILLIS)
        }
    }
    val blink = rememberInfiniteTransition(label = "display")
    val shown by blink.animateFloat(1f, 0.25f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "blink")
    val seconds by remember { derivedStateOf { played.toInt() } }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Vt323, fontSize = 20.sp, color = Color(0xFF2A2730))
    val glow = MaterialTheme.colorScheme.primary
    val display = TextStyle(fontFamily = Vt323, fontSize = 30.sp, letterSpacing = 3.sp)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // A soft light behind the tape, in the theme's colour.
            Box(
                Modifier
                    .size(320.dp)
                    .background(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent))),
            )
            Canvas(
                Modifier
                    .width(280.dp)
                    .aspectRatio(CASSETTE_ASPECT)
                    .graphicsLayer {
                        val d = drop.value
                        translationY = d * 120.dp.toPx()
                        rotationX = d * 70f
                        rotationZ = -6f * d
                        alpha = (1f - d).coerceIn(0f, 1f)
                        cameraDistance = 12f * density
                    },
            ) {
                // The tape winds over to the other reel and back, faster while it is wound on.
                val phase = (played / WIND_SECONDS) % 2f
                drawCassette(angle, if (phase < 1f) phase else 2f - phase)
                drawText(
                    measurer,
                    "VHS CAM",
                    topLeft = Offset(size.width * 0.21f, size.height * 0.1f),
                    style = labelStyle.copy(fontSize = (size.height * 0.11f).toSp()),
                )
                drawText(
                    measurer,
                    "T-120",
                    topLeft = Offset(size.width * 0.66f, size.height * 0.12f),
                    style = labelStyle.copy(fontSize = (size.height * 0.08f).toSp(), color = Color(0xFF6B6570)),
                )
                drawSheen(sheen.value)
            }
        }
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = deck.label,
                style = display.copy(textAlign = TextAlign.End),
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .width(118.dp)
                    .graphicsLayer { alpha = if (deck == Deck.PLAY) shown else 1f },
            )
            Text(
                text = tapeCounter(seconds),
                style = display,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                modifier = Modifier.width(118.dp),
            )
        }
    }
}

/** A tape counter, hours:minutes:seconds. */
private fun tapeCounter(seconds: Int): String =
    String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)

/** A glint of light sweeping across the shell as [progress] runs 0..1. */
private fun DrawScope.drawSheen(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val w = size.width
    val h = size.height
    val x = -w * 0.3f + progress * w * 1.6f
    drawRoundRect(
        Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = 0.2f),
            1f to Color.Transparent,
            start = Offset(x - w * 0.15f, 0f),
            end = Offset(x + w * 0.15f, h * 0.3f),
        ),
        cornerRadius = CornerRadius(h * 0.06f),
    )
}

private fun DrawScope.drawCassette(angle: Float, wind: Float) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(h * 0.06f)
    // Shell, lit from above.
    drawRoundRect(Color(0xFF1E1D23), cornerRadius = corner)
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), endY = h * 0.5f),
        cornerRadius = corner,
    )
    drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = corner, style = Stroke(h * 0.012f))
    // Grip ridges at both ends.
    for (i in 0 until 6) {
        val y = h * (0.3f + i * 0.08f)
        drawLine(Color.Black.copy(alpha = 0.5f), Offset(w * 0.03f, y), Offset(w * 0.08f, y), strokeWidth = h * 0.012f)
        drawLine(Color.Black.copy(alpha = 0.5f), Offset(w * 0.92f, y), Offset(w * 0.97f, y), strokeWidth = h * 0.012f)
    }
    // Label with the classic stripes.
    val labelLeft = w * 0.17f
    val labelTop = h * 0.07f
    val labelSize = Size(w * 0.66f, h * 0.36f)
    drawRoundRect(Color(0xFFF2E8D7), Offset(labelLeft, labelTop), labelSize, CornerRadius(h * 0.03f))
    val stripe = labelSize.height * 0.11f
    listOf(Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835)).forEachIndexed { i, color ->
        drawRect(color, Offset(labelLeft, labelTop + labelSize.height * 0.58f + i * stripe), Size(labelSize.width, stripe))
    }
    // Window onto the reels.
    val windowTopLeft = Offset(w * 0.26f, h * 0.5f)
    val windowSize = Size(w * 0.48f, h * 0.38f)
    drawRoundRect(Color(0xFF0E0D12), windowTopLeft, windowSize, CornerRadius(h * 0.05f))
    val left = Offset(w * 0.37f, h * 0.69f)
    val right = Offset(w * 0.63f, h * 0.69f)
    val small = h * 0.1f
    val big = h * 0.165f
    val leftPack = big + (small - big) * wind
    val rightPack = small + (big - small) * wind
    // Tape running along the bottom of the window between the packs.
    drawLine(
        Color(0xFF3A2A21),
        Offset(left.x, left.y + leftPack),
        Offset(right.x, right.y + rightPack),
        strokeWidth = h * 0.012f,
    )
    drawReel(left, leftPack, angle)
    drawReel(right, rightPack, angle)
    drawRoundRect(
        Color.White.copy(alpha = 0.07f),
        windowTopLeft,
        windowSize,
        CornerRadius(h * 0.05f),
        style = Stroke(h * 0.012f),
    )
    // Window glare.
    drawLine(
        Color.White.copy(alpha = 0.12f),
        Offset(windowTopLeft.x + windowSize.width * 0.12f, windowTopLeft.y + windowSize.height * 0.15f),
        Offset(windowTopLeft.x + windowSize.width * 0.3f, windowTopLeft.y + windowSize.height * 0.15f),
        strokeWidth = h * 0.02f,
    )
    // Screws.
    listOf(Offset(w * 0.06f, h * 0.12f), Offset(w * 0.94f, h * 0.12f), Offset(w * 0.06f, h * 0.88f), Offset(w * 0.94f, h * 0.88f))
        .forEach { drawCircle(Color(0xFF34333A), radius = h * 0.028f, center = it) }
}

private fun DrawScope.drawReel(center: Offset, pack: Float, angle: Float) {
    val h = size.height
    // The wound tape, with a few rings.
    drawCircle(Color(0xFF3B2A22), radius = pack, center = center)
    for (r in listOf(0.92f, 0.8f, 0.68f)) {
        drawCircle(Color.Black.copy(alpha = 0.18f), radius = pack * r, center = center, style = Stroke(h * 0.004f))
    }
    // Hub with six teeth, turning.
    val hub = h * 0.065f
    drawCircle(Color(0xFFE9E3D8), radius = hub, center = center)
    rotate(angle, pivot = center) {
        for (i in 0 until 6) {
            val a = Math.toRadians(i * 60.0)
            val tip = Offset(center.x + cos(a).toFloat() * hub * 0.85f, center.y + sin(a).toFloat() * hub * 0.85f)
            val base = Offset(center.x + cos(a).toFloat() * hub * 0.45f, center.y + sin(a).toFloat() * hub * 0.45f)
            drawLine(Color(0xFF6E6873), base, tip, strokeWidth = h * 0.018f)
        }
    }
    drawCircle(Color(0xFF1E1D23), radius = hub * 0.32f, center = center)
}

/** The looks the carousel shows: a few from each kind of camera, in the browser's order. */
private val FlowLooks = listOf(
    Look.VHS, Look.SUPER8, Look.MINIDV, Look.NIGHTSHOT,
    Look.FILM, Look.AEROCHROME, Look.SLIDE, Look.POLAROID,
    Look.CCD, Look.GAMEBOY, Look.THERMAL,
)

/**
 * The same beach through looks from every kind of camera, as prints in a 3D carousel standing
 * on a glossy floor. Tapping brings the next one forward; while [active] it also turns by
 * itself. Under it, the kind of camera and the look's name.
 */
@Composable
fun LookCoverFlow(active: Boolean, modifier: Modifier = Modifier) {
    val count = FlowLooks.size
    var target by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    val position = remember { Animatable(0f) }
    LaunchedEffect(target) {
        position.animateTo(target.toFloat(), spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }
    // A tap restarts the timer, so the carousel never turns twice in a row.
    LaunchedEffect(active, taps) {
        if (!active) return@LaunchedEffect
        while (true) {
            delay(FLOW_STEP_MILLIS)
            target++
        }
    }
    val front = target.mod(count)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 360.dp, height = 248.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) {
                    target++
                    taps++
                },
        ) {
            FlowLooks.forEachIndexed { index, look ->
                FlowCard(
                    look = look,
                    offset = { wrapped(index - position.value, count) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Stack by where the carousel is going, so the next print comes over the rest.
                        .zIndex(10f - abs(wrapped((index - target).toFloat(), count))),
                )
            }
        }
        AnimatedContent(
            targetState = front,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "lookName",
            modifier = Modifier.padding(top = 8.dp),
        ) { index ->
            val look = FlowLooks[index]
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(look.category.label()),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = stringResource(look.label()),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** [d] moved by whole turns of a carousel of [count] into -count/2 ..< count/2. */
private fun wrapped(d: Float, count: Int): Float {
    val half = count / 2f
    return ((d + half) % count + count) % count - half
}

/**
 * One print in the carousel, [offset] places from the front (fractional while it turns): the
 * front one faces us, the rest turn towards it, stack up behind and fall into shadow.
 */
@Composable
private fun FlowCard(look: Look, offset: () -> Float, modifier: Modifier = Modifier) {
    val painter = painterResource(look.sample())
    val intrinsic = painter.intrinsicSize
    val ratio = (intrinsic.width / intrinsic.height).takeIf { it.isFinite() && it > 0f } ?: (4f / 3f)
    val width = if (ratio >= 1f) CardSide else CardSide * ratio
    val height = if (ratio >= 1f) CardSide / ratio else CardSide
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier.graphicsLayer {
            val d = offset()
            val near = d.coerceIn(-1f, 1f)
            val far = (abs(d) - 1f).coerceAtLeast(0f)
            translationX = (near * FLOW_SPREAD + sign(d) * far * FLOW_STACK).dp.toPx()
            rotationY = near * FLOW_TILT
            val scale = 1f - 0.1f * abs(d).coerceAtMost(2.5f)
            scaleX = scale
            scaleY = scale
            alpha = ((2.6f - abs(d)) / 0.6f).coerceIn(0f, 1f)
            cameraDistance = 14f * density
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(width, height).shadow(14.dp, shape).clip(shape)) {
            Image(painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind { drawRect(Color.Black.copy(alpha = (abs(offset()) * 0.3f).coerceAtMost(0.6f))) },
            )
        }
        // Its reflection, fading away from the print.
        Box(Modifier.padding(top = 3.dp).size(width, Reflection).clipToBounds()) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .size(width, height)
                    .graphicsLayer {
                        scaleY = -1f
                        alpha = 0.3f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        // Upside down, so the bottom of the picture is next to the print.
                        drawRect(
                            Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
    }
}

private class EffectSample(val image: Int, val label: Int, val icon: Int)

private val EffectSamples = listOf(
    EffectSample(R.drawable.onboarding_fx_none, R.string.effect_none, R.drawable.ic_block),
    EffectSample(R.drawable.onboarding_fx_night, R.string.effect_night, R.drawable.ic_nightlight),
    EffectSample(R.drawable.onboarding_fx_sepia, R.string.effect_sepia, R.drawable.ic_filter_vintage),
    EffectSample(R.drawable.onboarding_fx_mono, R.string.effect_mono, R.drawable.ic_filter_b_and_w),
    EffectSample(R.drawable.onboarding_fx_mosaic, R.string.effect_mosaic, R.drawable.ic_auto_awesome_mosaic),
)

/**
 * A camcorder viewfinder with its display live over the picture (recording, the tape counter,
 * the date and the zoom bar as the digital zoom drifts in and out), stepping through the
 * digital effects with a burst of bad tracking between them and the effect's name in the
 * middle. The effect buttons under it switch by hand.
 */
@Composable
fun EffectsShowcase(active: Boolean, modifier: Modifier = Modifier) {
    val images = EffectSamples.map { ImageBitmap.imageResource(it.image) }
    val names = EffectSamples.map { stringResource(it.label) }
    var current by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    val glitch = remember { Animatable(0f) }
    val banner = remember { Animatable(0f) }
    LaunchedEffect(target) {
        if (target == current) return@LaunchedEffect
        banner.snapTo(if (target == 0) 0f else 1f)
        glitch.snapTo(0f)
        glitch.animateTo(1f, tween(GLITCH_MILLIS, easing = LinearEasing))
        current = target
        glitch.snapTo(0f)
        delay(BANNER_MILLIS)
        banner.animateTo(0f, tween(BANNER_FADE_MILLIS))
    }
    // A tap restarts the timer, so a chosen effect stays a while.
    LaunchedEffect(active, taps) {
        if (!active) return@LaunchedEffect
        while (true) {
            delay(EFFECT_STEP_MILLIS)
            target = (target + 1) % EffectSamples.size
        }
    }
    val loop = rememberInfiniteTransition(label = "viewfinder")
    val dot by loop.animateFloat(1f, 0.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot")
    val zoom by loop.animateFloat(
        1f,
        MAX_ZOOM,
        infiniteRepeatable(tween(ZOOM_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "zoom",
    )
    val clock by loop.animateFloat(
        0f,
        CLOCK_SECONDS,
        infiniteRepeatable(tween(CLOCK_SECONDS.toInt() * 1_000, easing = LinearEasing)),
        label = "clock",
    )
    val snow = rememberSnow()
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val osd = TextStyle(
        fontFamily = Vt323,
        fontSize = 17.sp,
        color = Color.White,
        shadow = Shadow(Color.Black.copy(alpha = 0.75f), offset = Offset(2f, 2f)),
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Canvas(
            Modifier
                .width(300.dp)
                .aspectRatio(4f / 3f)
                .shadow(20.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black),
        ) {
            val progress = glitch.value
            val tear = sin(PI.toFloat() * progress)
            val seed = (progress * 1_000f).toInt()
            drawPicture(images[if (progress < 0.5f) current else target], zoom, tear, seed)
            drawStatic(snow, tear * 0.4f, seed)
            drawCamcorderOsd(measurer, osd, dot, clock.toInt(), zoom)
            if (banner.value > 0f) drawBanner(measurer, osd, names[target], banner.value)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            EffectSamples.forEachIndexed { index, sample ->
                FilterChip(
                    selected = index == target,
                    onClick = {
                        target = index
                        taps++
                    },
                    label = { Text(stringResource(sample.label)) },
                    leadingIcon = { Icon(painterResource(sample.icon), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                )
            }
        }
    }
}

/**
 * [image] filling the viewfinder at digital [zoom]. [tear] (0..1) tears it the way a tape
 * with bad tracking does: bands of lines pushed sideways and colours slipping apart.
 */
private fun DrawScope.drawPicture(image: ImageBitmap, zoom: Float, tear: Float, seed: Int) {
    val w = size.width.roundToInt()
    val h = size.height.roundToInt()
    val srcW = (image.width / zoom).roundToInt()
    val srcH = (image.height / zoom).roundToInt()
    val srcX = (image.width - srcW) / 2
    val srcY = (image.height - srcH) / 2
    if (tear <= 0.01f) {
        drawImage(image, srcOffset = IntOffset(srcX, srcY), srcSize = IntSize(srcW, srcH), dstSize = IntSize(w, h))
        return
    }
    val random = Random(seed)
    for (band in 0 until TEAR_BANDS) {
        val top = h * band / TEAR_BANDS
        val bottom = h * (band + 1) / TEAR_BANDS
        val srcTop = srcH * band / TEAR_BANDS
        val srcBottom = srcH * (band + 1) / TEAR_BANDS
        val shift = if (random.nextFloat() < 0.5f) ((random.nextFloat() - 0.5f) * 2f * tear * w * 0.14f).roundToInt() else 0
        drawImage(
            image,
            srcOffset = IntOffset(srcX, srcY + srcTop),
            srcSize = IntSize(srcW, max(1, srcBottom - srcTop)),
            dstOffset = IntOffset(shift, top),
            dstSize = IntSize(w, max(1, bottom - top)),
        )
    }
    val fringe = (tear * 9.dp.toPx()).roundToInt()
    for ((dx, filter) in listOf(-fringe to RedOnly, fringe to BlueOnly)) {
        drawImage(
            image,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
            dstOffset = IntOffset(dx, 0),
            dstSize = IntSize(w, h),
            alpha = 0.45f * tear,
            colorFilter = filter,
            blendMode = BlendMode.Screen,
        )
    }
}

private val RedOnly = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

private val BlueOnly = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/** The camcorder's display: recording, the tape counter, the time and date, and the zoom bar. */
private fun DrawScope.drawCamcorderOsd(measurer: TextMeasurer, style: TextStyle, dot: Float, seconds: Int, zoom: Float) {
    val pad = 16.dp.toPx()
    val w = size.width
    val h = size.height
    val rec = measurer.measure("REC", style)
    val radius = 4.5.dp.toPx()
    drawCircle(CameraColors.Record.copy(alpha = dot), radius = radius, center = Offset(pad + radius, pad + rec.size.height / 2f))
    drawText(rec, topLeft = Offset(pad + radius * 2f + 5.dp.toPx(), pad))
    val counter = measurer.measure("SP " + tapeCounter(seconds), style)
    drawText(counter, topLeft = Offset(w - pad - counter.size.width, pad))
    val date = measurer.measure(OSD_DATE, style)
    val time = measurer.measure(OSD_TIME, style)
    val lastLine = h - pad - date.size.height
    drawText(time, topLeft = Offset(pad, lastLine - time.size.height))
    drawText(date, topLeft = Offset(pad, lastLine))
    // The zoom bar, W to T, its marker following the zoom.
    val wide = measurer.measure("W", style)
    val tele = measurer.measure("T", style)
    val gap = 5.dp.toPx()
    val barWidth = w * 0.22f
    val barRight = w - pad - tele.size.width - gap
    val barLeft = barRight - barWidth
    val lineY = lastLine + date.size.height / 2f
    drawText(wide, topLeft = Offset(barLeft - gap - wide.size.width, lastLine))
    drawText(tele, topLeft = Offset(barRight + gap, lastLine))
    val shade = Color.Black.copy(alpha = 0.6f)
    val drop = Offset(1.5f, 1.5f)
    val bar = Size(barWidth, 2.dp.toPx())
    val barTop = Offset(barLeft, lineY - bar.height / 2f)
    drawRect(shade, topLeft = barTop + drop, size = bar)
    drawRect(Color.White, topLeft = barTop, size = bar)
    val marker = Size(4.dp.toPx(), 12.dp.toPx())
    val markerX = barLeft + barWidth * ((zoom - 1f) / (MAX_ZOOM - 1f)).coerceIn(0f, 1f)
    val markerTop = Offset(markerX - marker.width / 2f, lineY - marker.height / 2f)
    drawRect(shade, topLeft = markerTop + drop, size = marker)
    drawRect(Color.White, topLeft = markerTop, size = marker)
}

/** The effect's name in the middle of the picture, as the camcorder shows it when switching. */
private fun DrawScope.drawBanner(measurer: TextMeasurer, style: TextStyle, text: String, alpha: Float) {
    val layout = measurer.measure(text, style.copy(fontSize = 30.sp, letterSpacing = 2.sp))
    drawText(
        layout,
        topLeft = Offset((size.width - layout.size.width) / 2f, (size.height - layout.size.height) / 2f),
        alpha = alpha.coerceIn(0f, 1f),
    )
}

/**
 * What comes out of the camera: a photobooth strip fed out a picture at a time, an instant
 * photo pushed out, shaken and developing, and a GIF sticker slapped on with confetti.
 */
@Composable
fun ShareShowcase(active: Boolean, modifier: Modifier = Modifier) {
    val print = remember { Animatable(0f) }
    val eject = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val develop = remember { Animatable(0f) }
    val sticker = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(active) {
        // Off screen it starts over, so it plays again next time.
        listOf(print, eject, shake, develop, sticker, burst).forEach { it.snapTo(0f) }
        if (!active) return@LaunchedEffect
        launch {
            print.animateTo(
                1f,
                keyframes {
                    durationMillis = PRINT_MILLIS
                    0.25f at 300
                    0.25f at 480
                    0.5f at 780
                    0.5f at 960
                    0.75f at 1_260
                    0.75f at 1_440
                },
            )
        }
        launch { eject.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)) }
        launch {
            delay(SHAKE_DELAY_MILLIS)
            shake.animateTo(1f, tween(SHAKE_MILLIS, easing = LinearEasing))
        }
        launch { develop.animateTo(1f, tween(2_800, delayMillis = 300, easing = LinearEasing)) }
        launch {
            delay(STICKER_DELAY_MILLIS)
            sticker.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
        launch {
            delay(STICKER_DELAY_MILLIS + 80L)
            burst.animateTo(1f, tween(BURST_MILLIS, easing = LinearEasing))
        }
    }
    val wobble = rememberInfiniteTransition(label = "sticker")
    val pulse by wobble.animateFloat(-9f, -4f, infiniteRepeatable(tween(1_400), RepeatMode.Reverse), label = "pulse")
    val confetti = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFFDD835),
        CameraColors.Record,
    )

    Box(modifier.size(width = 330.dp, height = 330.dp)) {
        // The strip comes out of the booth's slot, then hangs a little crooked.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = 36.dp)
                .graphicsLayer {
                    rotationZ = -5f * print.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .clip(RectangleShape)
                .graphicsLayer { translationY = (print.value - 1f) * 330.dp.toPx() },
        ) {
            PhotoStrip()
        }
        // The instant photo develops from a murky teal into colour.
        val saturation = develop.value
        val matrix = remember(saturation) { ColorMatrix().apply { setToSaturation(saturation) } }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .graphicsLayer {
                    translationX = (1f - eject.value) * 240.dp.toPx()
                    val s = shake.value
                    rotationZ = 7f + sin(s * 6f * PI.toFloat()) * 8f * (1f - s)
                }
                .width(158.dp)
                .shadow(16.dp, RoundedCornerShape(4.dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding_instant),
                contentDescription = null,
                colorFilter = ColorFilter.colorMatrix(matrix),
                modifier = Modifier.fillMaxWidth().aspectRatio(713f / 866f),
            )
            // The picture area of the card (79 mm inside an 88 x 107 mm card) starts out dark.
            Box(
                Modifier
                    .padding(start = 158.dp * (4.5f / 88f), top = 158.dp * (6f / 88f))
                    .size(158.dp * (79f / 88f))
                    .background(Color(0xFF223A3E).copy(alpha = 0.92f * (1f - develop.value))),
            )
        }
        Canvas(Modifier.matchParentSize()) {
            drawConfetti(burst.value, confetti, origin = Offset(size.width / 2f, size.height - 64.dp.toPx()))
        }
        // A GIF sticker slapped on top.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .graphicsLayer {
                    scaleX = sticker.value
                    scaleY = sticker.value
                    rotationZ = pulse
                }
                .size(92.dp)
                .shadow(10.dp, MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "GIF",
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** Confetti thrown out from [origin] as [progress] runs 0..1, tumbling, falling a little and fading. */
private fun DrawScope.drawConfetti(progress: Float, colors: List<Color>, origin: Offset) {
    if (progress <= 0f || progress >= 1f) return
    val travel = FastOutSlowInEasing.transform(progress)
    val piece = Size(8.dp.toPx(), 4.dp.toPx())
    for (i in 0 until CONFETTI) {
        val angle = i * (2f * PI.toFloat() / CONFETTI) + (i % 3) * 0.35f
        val reach = (64 + (i * 37) % 56).dp.toPx() * travel
        val center = Offset(
            origin.x + cos(angle) * reach,
            origin.y + sin(angle) * reach * 0.8f + progress * progress * 48.dp.toPx(),
        )
        rotate(degrees = i * 47f + progress * 520f, pivot = center) {
            drawRect(
                colors[i % colors.size].copy(alpha = 1f - progress),
                topLeft = Offset(center.x - piece.width / 2f, center.y - piece.height / 2f),
                size = piece,
            )
        }
    }
}

/** Four booth shots on photo paper, with the date under them. */
@Composable
private fun PhotoStrip() {
    Column(
        Modifier
            .width(104.dp)
            .background(Color(0xFFF4F1EA))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(
            R.drawable.onboarding_vhs,
            R.drawable.onboarding_film,
            R.drawable.onboarding_webcam,
            R.drawable.onboarding_cctv,
        ).forEach { image ->
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
        }
        Text(
            "VHS CAM 14.08.1998",
            style = TextStyle(fontFamily = Vt323, fontSize = 10.sp, color = Color(0xFF3B3836)),
            maxLines = 1,
        )
    }
}

/** How far a page is from the middle of the pager: 0 on screen, ±1 one page away. */
fun pageDistance(current: Int, fraction: Float, page: Int): Float = (current - page) + fraction

/** 1 when the page is on screen, falling to 0 a page away. */
fun pageVisibility(distance: Float): Float = (1f - abs(distance)).coerceIn(0f, 1f)

private const val SHUTTLE_GAIN = 8f
private const val REEL_DEGREES_PER_SECOND = 225f
private const val WIND_SECONDS = 40f
private const val SHEEN_MILLIS = 1_100
private const val SHEEN_PAUSE_MILLIS = 4_200L

private const val FLOW_STEP_MILLIS = 1_900L
private const val FLOW_SPREAD = 118f
private const val FLOW_STACK = 34f
private const val FLOW_TILT = 50f
private val CardSide = 186.dp
private val Reflection = 46.dp

private const val GLITCH_MILLIS = 420
private const val BANNER_MILLIS = 900L
private const val BANNER_FADE_MILLIS = 300
private const val EFFECT_STEP_MILLIS = 1_900L
private const val MAX_ZOOM = 1.25f
private const val ZOOM_MILLIS = 5_000
private const val CLOCK_SECONDS = 3_600f
private const val TEAR_BANDS = 18
private const val OSD_TIME = "6:42 PM"
private const val OSD_DATE = "AUG.14 1998"

private const val PRINT_MILLIS = 1_700
private const val SHAKE_DELAY_MILLIS = 350L
private const val SHAKE_MILLIS = 1_000
private const val STICKER_DELAY_MILLIS = 1_000L
private const val BURST_MILLIS = 900
private const val CONFETTI = 16
