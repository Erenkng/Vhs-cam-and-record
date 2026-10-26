package io.github.erenkng.vhscam.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.ui.theme.CameraColors
import io.github.erenkng.vhscam.ui.theme.Vt323
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Real proportions of a VHS cassette, 187 x 103 mm. */
private const val CASSETTE_ASPECT = 187f / 103f

/**
 * A VHS cassette drawn from scratch: label, window, and two reels that turn while the tape
 * winds from one to the other. It drops in with a spring when [active] turns true.
 */
@Composable
fun CassetteArt(active: Boolean, modifier: Modifier = Modifier) {
    val spin = rememberInfiniteTransition(label = "reels")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1_600, easing = LinearEasing)), label = "angle")
    val wind by spin.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse), label = "wind")
    val blink by spin.animateFloat(1f, 0.25f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "blink")
    val drop = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active) {
            drop.snapTo(1f)
            drop.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        }
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Vt323, fontSize = 20.sp, color = Color(0xFF2A2730))
    val glow = MaterialTheme.colorScheme.primary

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
                        translationY = drop.value * 140.dp.toPx()
                        rotationZ = -8f * drop.value
                        alpha = 1f - drop.value * 0.8f
                    },
            ) {
                drawCassette(angle, wind)
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
            }
        }
        Text(
            text = "PLAY ▶",
            style = TextStyle(fontFamily = Vt323, fontSize = 30.sp, letterSpacing = 3.sp),
            color = Color.White.copy(alpha = blink),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
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

private class LookCard(val image: Int, val label: Int)

private val LookCards = listOf(
    LookCard(R.drawable.onboarding_vhs, R.string.look_vhs),
    LookCard(R.drawable.onboarding_ccd, R.string.look_ccd),
    LookCard(R.drawable.onboarding_film, R.string.look_film),
    LookCard(R.drawable.onboarding_instant, R.string.look_polaroid),
    LookCard(R.drawable.onboarding_gameboy, R.string.look_gameboy),
    LookCard(R.drawable.onboarding_webcam, R.string.look_webcam),
    LookCard(R.drawable.onboarding_cctv, R.string.look_cctv),
)

/**
 * The same beach through every look, as a fanned stack of prints that turns like a carousel.
 * Tapping brings the next one forward; while [active] it also turns by itself.
 */
@Composable
fun LookCardFan(active: Boolean, modifier: Modifier = Modifier) {
    var front by remember { mutableIntStateOf(0) }
    var turns by remember { mutableIntStateOf(0) }
    // A tap restarts the timer, so the stack never turns twice in a row.
    LaunchedEffect(active, turns) {
        if (!active) return@LaunchedEffect
        while (true) {
            delay(1_800)
            front = (front + 1) % LookCards.size
        }
    }
    val count = LookCards.size
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 340.dp, height = 280.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) {
                    front = (front + 1) % count
                    turns++
                },
            contentAlignment = Alignment.Center,
        ) {
            LookCards.forEachIndexed { index, card ->
                val distance = (index - front + count) % count
                val side = if (distance == 0) 0f else if (distance <= count / 2) 1f else -1f
                val depth = if (distance <= count / 2) distance else count - distance
                val spec = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                val x by animateFloatAsState(side * FanX[depth], spec, label = "x")
                val tilt by animateFloatAsState(side * FanTilt[depth], spec, label = "tilt")
                val scale by animateFloatAsState(FanScale[depth], spec, label = "scale")
                val dim by animateFloatAsState(FanDim[depth], spec, label = "dim")
                Box(
                    Modifier
                        .zIndex(10f - depth)
                        .graphicsLayer {
                            translationX = x * density
                            translationY = depth * 9f * density
                            rotationZ = tilt
                            scaleX = scale
                            scaleY = scale
                        }
                        .width(210.dp)
                        .shadow(14.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    val painter = painterResource(card.image)
                    val ratio = painter.intrinsicSize.width / painter.intrinsicSize.height
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (ratio.isFinite() && ratio > 0f) ratio else 4f / 3f),
                    )
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dim)))
                }
            }
        }
        AnimatedContent(
            targetState = front,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "lookName",
            modifier = Modifier.padding(top = 18.dp),
        ) { index ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    text = stringResource(LookCards[index].label),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private val FanX = floatArrayOf(0f, 62f, 104f, 134f)
private val FanTilt = floatArrayOf(0f, 7f, 13f, 19f)
private val FanScale = floatArrayOf(1f, 0.88f, 0.78f, 0.7f)
private val FanDim = floatArrayOf(0f, 0.28f, 0.48f, 0.62f)

private class EffectSample(val image: Int, val label: Int, val icon: Int)

private val EffectSamples = listOf(
    EffectSample(R.drawable.onboarding_vhs, R.string.effect_none, R.drawable.ic_block),
    EffectSample(R.drawable.onboarding_vhs_night, R.string.effect_night, R.drawable.ic_nightlight),
    EffectSample(R.drawable.onboarding_vhs_sepia, R.string.effect_sepia, R.drawable.ic_filter_vintage),
    EffectSample(R.drawable.onboarding_vhs_mono, R.string.effect_mono, R.drawable.ic_filter_b_and_w),
    EffectSample(R.drawable.onboarding_vhs_mosaic, R.string.effect_mosaic, R.drawable.ic_auto_awesome_mosaic),
)

/** A camcorder viewfinder that steps through the digital effects, with the effect buttons under it. */
@Composable
fun EffectsShowcase(active: Boolean, modifier: Modifier = Modifier) {
    var current by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    LaunchedEffect(active, taps) {
        if (!active) return@LaunchedEffect
        while (true) {
            delay(1_500)
            current = (current + 1) % EffectSamples.size
        }
    }
    val blink = rememberInfiniteTransition(label = "rec")
    val dot by blink.animateFloat(1f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier
                .width(300.dp)
                .aspectRatio(4f / 3f)
                .shadow(20.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black),
        ) {
            Crossfade(targetState = current, animationSpec = tween(380), label = "effect") { index ->
                Image(
                    painter = painterResource(EffectSamples[index].image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(9.dp).graphicsLayer { alpha = dot }.background(CameraColors.Record, CircleShape))
                Text("REC", style = TextStyle(fontFamily = Vt323, fontSize = 18.sp), color = Color.White)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            EffectSamples.forEachIndexed { index, sample ->
                FilterChip(
                    selected = index == current,
                    onClick = {
                        current = index
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
 * What comes out of the camera: a photobooth strip printing, an instant photo developing and
 * a GIF sticker popping on.
 */
@Composable
fun ShareShowcase(active: Boolean, modifier: Modifier = Modifier) {
    val print = remember { Animatable(0f) }
    val develop = remember { Animatable(0f) }
    val sticker = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        print.snapTo(0f)
        develop.snapTo(0f)
        sticker.snapTo(0f)
        launch { print.animateTo(1f, tween(1_500, easing = FastOutSlowInEasing)) }
        launch { develop.animateTo(1f, tween(2_800, delayMillis = 300, easing = LinearEasing)) }
        launch {
            delay(900)
            sticker.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val wobble = rememberInfiniteTransition(label = "sticker")
    val pulse by wobble.animateFloat(-9f, -4f, infiniteRepeatable(tween(1_400), RepeatMode.Reverse), label = "pulse")

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
                .rotate(7f)
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
