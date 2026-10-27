package io.github.erenkng.vhscam.core.exposure

import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Sensor exposure: ISO and shutter time. */
data class Exposure(val iso: Int, val timeNanos: Long)

/** Pro mode's manual exposure: the dial stops and filling in whatever is left on auto. */
object ManualExposure {

    /** ISO in thirds of a stop. */
    val ISO_STOPS: List<Int> = listOf(
        25, 32, 40, 50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250,
        1600, 2000, 2500, 3200, 4000, 5000, 6400, 8000, 10000, 12800,
    )

    /** Shutter times in half stops, from 1/8000 s to 8 s. */
    val SHUTTER_STOPS: List<Long> = listOf(
        8000, 6000, 4000, 3000, 2000, 1500, 1000, 750, 500, 350, 250, 180, 125, 90, 60, 45, 30, 20,
        15, 10, 8, 6, 4,
    ).map { NANOS_PER_SECOND / it } + listOf(1.0 / 3, 0.5, 0.7, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0).map {
        (it * NANOS_PER_SECOND).roundToLong()
    }

    fun isoStops(range: IntRange): List<Int> = ISO_STOPS.filter { it in range }

    fun shutterStops(range: LongRange): List<Long> = SHUTTER_STOPS.filter { it in range }

    /**
     * The exposure to set when the user picked [iso], [timeNanos] or both. Whatever is left on auto
     * is worked out from what auto exposure last [metered], so the picture keeps its brightness:
     * ISO priority or shutter priority, like the A and S modes of a camera.
     */
    fun complete(iso: Int?, timeNanos: Long?, metered: Exposure?, isoRange: IntRange, timeRange: LongRange): Exposure {
        val base = metered?.takeIf { it.iso > 0 && it.timeNanos > 0 } ?: Exposure(DEFAULT_ISO, DEFAULT_TIME_NANOS)
        val light = base.iso.toDouble() * base.timeNanos
        val time = timeNanos ?: (light / (iso ?: base.iso)).roundToLong()
        val sensitivity = iso ?: (light / time).roundToInt()
        return Exposure(sensitivity.coerceIn(isoRange), time.coerceIn(timeRange))
    }

    /** The nearest stop to [value] in [stops], for showing a metered value on the dial. */
    fun <T : Comparable<T>> nearest(stops: List<T>, value: T, distance: (T, T) -> Double): T? =
        stops.minByOrNull { distance(it, value) }

    /** Shutter time as cameras print it: `1/250`, `0.5"`, `2"`. */
    fun formatShutter(nanos: Long): String {
        val seconds = nanos.toDouble() / NANOS_PER_SECOND
        if (seconds < 0.29) return "1/${(1.0 / seconds).roundToInt()}"
        val text = String.format(Locale.US, "%.1f", seconds).removeSuffix(".0")
        return "$text\""
    }

    const val NANOS_PER_SECOND = 1_000_000_000L
    private const val DEFAULT_ISO = 100
    private const val DEFAULT_TIME_NANOS = NANOS_PER_SECOND / 60
}
