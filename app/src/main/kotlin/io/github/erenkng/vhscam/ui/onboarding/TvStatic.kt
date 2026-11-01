package io.github.erenkng.vhscam.ui.onboarding

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random

/** A tile of grey TV snow. Drawn large with blocky pixels at a new place each frame, it seems to boil. */
@Composable
fun rememberSnow(): ImageBitmap = remember {
    val random = Random(SNOW_SEED)
    val pixels = IntArray(SNOW_SIZE * SNOW_SIZE) {
        val v = random.nextInt(256)
        android.graphics.Color.rgb(v, v, v)
    }
    Bitmap.createBitmap(pixels, SNOW_SIZE, SNOW_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * Static over the whole area at [amount] (0..1), with a few bright lines where the picture
 * tears. [seed] picks where the snow sits, so a new seed each frame keeps it moving. With
 * [BlendMode.SrcAtop] on a layer of its own it only covers what is already drawn.
 */
fun DrawScope.drawStatic(snow: ImageBitmap, amount: Float, seed: Int, blendMode: BlendMode = BlendMode.SrcOver) {
    if (amount <= 0.004f) return
    val random = Random(seed)
    val cell = 2.dp.toPx().coerceAtLeast(1f)
    val tile = IntSize((snow.width * cell).roundToInt(), (snow.height * cell).roundToInt())
    var y = -random.nextInt(tile.height)
    while (y < size.height) {
        var x = -random.nextInt(tile.width)
        while (x < size.width) {
            drawImage(
                snow,
                dstOffset = IntOffset(x, y),
                dstSize = tile,
                alpha = amount.coerceAtMost(1f),
                blendMode = blendMode,
                filterQuality = FilterQuality.None,
            )
            x += tile.width
        }
        y += tile.height
    }
    repeat(TEAR_LINES) {
        val top = random.nextFloat() * size.height
        val height = (1 + random.nextInt(3)) * cell
        drawRect(
            Color.White.copy(alpha = (amount * 0.7f).coerceAtMost(1f)),
            topLeft = Offset(0f, top),
            size = Size(size.width, height),
            blendMode = blendMode,
        )
    }
}

private const val SNOW_SIZE = 96
private const val SNOW_SEED = 1998
private const val TEAR_LINES = 3
