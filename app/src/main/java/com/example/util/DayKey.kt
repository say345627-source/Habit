package com.example.util

import java.util.Calendar
import java.util.Locale

/**
 * Pure Kotlin proleptic Gregorian calendar date keyed by [epochDay] (days since 1970-01-01).
 * Safe on minSdk 24 without requiring core library desugaring for java.time.
 */
@ConsistentCopyVisibility
data class DayKey private constructor(
    val epochDay: Long,
    val year: Int,
    val month: Int, // 1..12
    val dayOfMonth: Int // 1..31
) : Comparable<DayKey> {

    /** 1 = Monday .. 7 = Sunday (ISO-8601 convention). */
    val dayOfWeekIso: Int
        get() {
            // 1970-01-01 was Thursday (4)
            val mod = ((epochDay + 3L) % 7L + 7L) % 7L
            return mod.toInt() + 1
        }

    val isoString: String
        get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayOfMonth)

    fun plusDays(days: Long): DayKey = fromEpochDay(epochDay + days)

    fun minusDays(days: Long): DayKey = fromEpochDay(epochDay - days)

    override fun compareTo(other: DayKey): Int = epochDay.compareTo(other.epochDay)

    companion object {
        private const val DAYS_0000_TO_1970 = 719528L
        private const val DAYS_PER_CYCLE = 146097L

        fun today(): DayKey {
            val cal = Calendar.getInstance()
            return fromYearMonthDay(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun fromYearMonthDay(year: Int, month: Int, dayOfMonth: Int): DayKey {
            val y = year.toLong()
            val m = month.toLong()
            val d = dayOfMonth.toLong()
            var total = 365L * y
            if (y >= 0) {
                total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
            } else {
                total -= y / -4 - y / -100 + y / -400
            }
            total += (367 * m - 362) / 12
            total += d - 1
            if (m > 2) {
                total--
                if (!isLeapYear(year)) {
                    total--
                }
            }
            val epochDay = total - DAYS_0000_TO_1970
            return DayKey(epochDay, year, month, dayOfMonth)
        }

        fun fromEpochDay(epochDay: Long): DayKey {
            var zeroDay = epochDay + DAYS_0000_TO_1970
            zeroDay -= 60L
            var adjust = 0L
            if (zeroDay < 0) {
                val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                adjust = adjustCycles * 400L
                zeroDay += -adjustCycles * DAYS_PER_CYCLE
            }
            var yearEst = (400L * zeroDay + 591L) / DAYS_PER_CYCLE
            var doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L)
            if (doyEst < 0) {
                yearEst--
                doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L)
            }
            yearEst += adjust
            val marchDoy0 = doyEst.toInt()
            val marchMonth0 = (marchDoy0 * 5 + 2) / 153
            val month = (marchMonth0 + 2) % 12 + 1
            val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
            yearEst += (marchMonth0 / 10).toLong()
            return DayKey(epochDay, yearEst.toInt(), month, dom)
        }

        fun isLeapYear(year: Int): Boolean {
            return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        }

        fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }

        fun previousMonth(year: Int, month: Int): Pair<Int, Int> {
            return if (month == 1) (year - 1) to 12 else year to (month - 1)
        }

        fun nextMonth(year: Int, month: Int): Pair<Int, Int> {
            return if (month == 12) (year + 1) to 1 else year to (month + 1)
        }

        fun arabicMonthName(month: Int): String = when (month) {
            1 -> "يناير"
            2 -> "فبراير"
            3 -> "مارس"
            4 -> "أبريل"
            5 -> "مايو"
            6 -> "يونيو"
            7 -> "يوليو"
            8 -> "أغسطس"
            9 -> "سبتمبر"
            10 -> "أكتوبر"
            11 -> "نوفمبر"
            12 -> "ديسمبر"
            else -> ""
        }

        fun englishMonthName(month: Int): String = when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> ""
        }

        fun arabicWeekdayShort(dayOfWeekIso: Int): String = when (dayOfWeekIso) {
            1 -> "إثنين"
            2 -> "ثلاثاء"
            3 -> "أربعاء"
            4 -> "خميس"
            5 -> "جمعة"
            6 -> "سبت"
            7 -> "أحد"
            else -> ""
        }

        fun arabicWeekdayInitial(dayOfWeekIso: Int): String = when (dayOfWeekIso) {
            6 -> "س"
            7 -> "ح"
            1 -> "ن"
            2 -> "ث"
            3 -> "ر"
            4 -> "خ"
            5 -> "ج"
            else -> ""
        }

        fun englishWeekdayShort(dayOfWeekIso: Int): String = when (dayOfWeekIso) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> ""
        }
    }
}
