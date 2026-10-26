package io.github.erenkng.vhscam.core.osd

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Order of the date fields in a stamp. */
enum class DateOrder { MONTH_DAY_YEAR, DAY_MONTH_YEAR, YEAR_MONTH_DAY }

enum class ClockStyle { H12, H24 }

/** Text of the date stamps burned into photos and videos. */
object DateStamp {

    private val MONTHS = arrayOf(
        "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
    )

    /** Camcorder date line, e.g. `JAN.01 1997`, `01.01.1997` or `1997.01.01`. */
    fun vhsDate(dateTime: LocalDateTime, order: DateOrder): String {
        val day = dateTime.dayOfMonth.twoDigits()
        val month = dateTime.monthValue.twoDigits()
        val year = dateTime.year.toString()
        return when (order) {
            DateOrder.MONTH_DAY_YEAR -> "${MONTHS[dateTime.monthValue - 1]}.$day $year"
            DateOrder.DAY_MONTH_YEAR -> "$day.$month.$year"
            DateOrder.YEAR_MONTH_DAY -> "$year.$month.$day"
        }
    }

    /** Camcorder time line, e.g. `12:00 AM` or `00:00`. */
    fun vhsTime(dateTime: LocalDateTime, style: ClockStyle): String = when (style) {
        ClockStyle.H24 -> "${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}"
        ClockStyle.H12 -> {
            val hour = (dateTime.hour % 12).let { if (it == 0) 12 else it }
            val suffix = if (dateTime.hour < 12) "AM" else "PM"
            "$hour:${dateTime.minute.twoDigits()} $suffix"
        }
    }

    /**
     * 35 mm data-back imprint, e.g. `'98 6 14`. The apostrophe marks the two-digit year and
     * month and day are not zero padded, like the seven-segment LEDs that exposed it.
     */
    fun filmDate(dateTime: LocalDateTime, order: DateOrder): String {
        val year = "'" + (dateTime.year % 100).twoDigits()
        val month = dateTime.monthValue.toString()
        val day = dateTime.dayOfMonth.toString()
        return when (order) {
            DateOrder.MONTH_DAY_YEAR -> "$month $day $year"
            DateOrder.DAY_MONTH_YEAR -> "$day $month $year"
            DateOrder.YEAR_MONTH_DAY -> "$year $month $day"
        }
    }

    /** Digital camera imprint, e.g. `2003/06/14 15:42`. */
    fun ccdDate(dateTime: LocalDateTime, order: DateOrder): String {
        val d = dateTime.dayOfMonth.twoDigits()
        val m = dateTime.monthValue.twoDigits()
        val y = dateTime.year.toString()
        val date = when (order) {
            DateOrder.MONTH_DAY_YEAR -> "$m/$d/$y"
            DateOrder.DAY_MONTH_YEAR -> "$d/$m/$y"
            DateOrder.YEAR_MONTH_DAY -> "$y/$m/$d"
        }
        return "$date ${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}"
    }

    /** Security camera stamp with seconds, always 24-hour, e.g. `14-06-1998 15:42:07`. */
    fun cctvStamp(dateTime: LocalDateTime, order: DateOrder): String {
        val d = dateTime.dayOfMonth.twoDigits()
        val m = dateTime.monthValue.twoDigits()
        val y = dateTime.year.toString()
        val date = when (order) {
            DateOrder.MONTH_DAY_YEAR -> "$m-$d-$y"
            DateOrder.DAY_MONTH_YEAR -> "$d-$m-$y"
            DateOrder.YEAR_MONTH_DAY -> "$y-$m-$d"
        }
        return "$date ${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}:${dateTime.second.twoDigits()}"
    }

    /** VCR tape counter, e.g. `0:01:05`. */
    fun tapeCounter(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis.coerceAtLeast(0L) / 1000L)
        val h = totalSeconds / 3600
        val m = (totalSeconds / 60) % 60
        val s = totalSeconds % 60
        return "$h:${m.toInt().twoDigits()}:${s.toInt().twoDigits()}"
    }

    private fun Int.twoDigits(): String = toString().padStart(2, '0')
}

/**
 * The clock of the "camcorder". Like the real thing it can be set to any date, after which it
 * keeps running from there. [offsetMillis] is the difference between camcorder and real time.
 */
data class CamcorderClock(val offsetMillis: Long = 0L) {

    fun now(epochMillis: Long, zone: ZoneId): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis + offsetMillis), zone)

    companion object {
        /** A clock that shows [setTo] at the moment [atEpochMillis]. */
        fun settingTo(setTo: LocalDateTime, atEpochMillis: Long, zone: ZoneId): CamcorderClock {
            val target = setTo.atZone(zone).toInstant().toEpochMilli()
            return CamcorderClock(offsetMillis = target - atEpochMillis)
        }
    }
}
