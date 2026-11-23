package com.example.util

import com.example.data.model.RecurringRule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object ArabicDateUtils {
    private val arabicMonths = listOf(
        "كانون الثاني",
        "شباط",
        "آذار",
        "نيسان",
        "أيار",
        "حزيران",
        "تموز",
        "آب",
        "أيلول",
        "تشرين الأول",
        "تشرين الثاني",
        "كانون الأول"
    )

    private val arabicDays = mapOf(
        DayOfWeek.SATURDAY to "السبت",
        DayOfWeek.SUNDAY to "الأحد",
        DayOfWeek.MONDAY to "الاثنين",
        DayOfWeek.TUESDAY to "الثلاثاء",
        DayOfWeek.WEDNESDAY to "الأربعاء",
        DayOfWeek.THURSDAY to "الخميس",
        DayOfWeek.FRIDAY to "الجمعة"
    )

    private val arabicShortDays = mapOf(
        DayOfWeek.SATURDAY to "سبت",
        DayOfWeek.SUNDAY to "أحد",
        DayOfWeek.MONDAY to "اثنين",
        DayOfWeek.TUESDAY to "ثلاثاء",
        DayOfWeek.WEDNESDAY to "أربعاء",
        DayOfWeek.THURSDAY to "خميس",
        DayOfWeek.FRIDAY to "جمعة"
    )

    fun toLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()

    fun toZonedDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId)

    fun currentMonthYear(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): String {
        val ym = YearMonth.from(toLocalDate(nowMillis, zoneId))
        return formatYearMonthKey(ym)
    }

    fun formatYearMonthKey(ym: YearMonth): String =
        String.format(Locale.US, "%04d-%02d", ym.year, ym.monthValue)

    fun parseYearMonthKey(key: String): YearMonth = YearMonth.parse(key)

    fun formatMonthYearLabel(ym: YearMonth): String =
        "${arabicMonths[ym.monthValue - 1]} ${ym.year}"

    fun formatMonthYearLabel(key: String): String =
        formatMonthYearLabel(parseYearMonthKey(key))

    fun monthYearFromMillis(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        formatYearMonthKey(YearMonth.from(toLocalDate(epochMillis, zoneId)))

    fun formatDayHeader(
        date: LocalDate,
        today: LocalDate = LocalDate.now()
    ): String {
        val dayName = arabicDays[date.dayOfWeek].orEmpty()
        val monthName = arabicMonths[date.monthValue - 1]
        val fullDate = "$dayName، ${date.dayOfMonth} $monthName ${date.year}"
        return when (date) {
            today -> "اليوم — $fullDate"
            today.minusDays(1) -> "أمس — $fullDate"
            else -> fullDate
        }
    }

    fun formatShortDayName(date: LocalDate): String =
        arabicShortDays[date.dayOfWeek].orEmpty()

    fun formatShortDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dt = toLocalDate(epochMillis, zoneId)
        return String.format(Locale.US, "%02d/%02d/%04d", dt.dayOfMonth, dt.monthValue, dt.year)
    }

    fun formatDateTimeDisplay(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zdt = toZonedDateTime(epochMillis, zoneId)
        val monthName = arabicMonths[zdt.monthValue - 1]
        val hour24 = zdt.hour
        val period = if (hour24 < 12) "ص" else "م"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val minute = String.format(Locale.US, "%02d", zdt.minute)
        return "${zdt.dayOfMonth} $monthName ${zdt.year} • $hour12:$minute $period"
    }

    fun formatTimeOnly(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zdt = toZonedDateTime(epochMillis, zoneId)
        val hour24 = zdt.hour
        val period = if (hour24 < 12) "ص" else "م"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val minute = String.format(Locale.US, "%02d", zdt.minute)
        return "$hour12:$minute $period"
    }

    fun formatIsoDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
            .withZone(zoneId)
            .format(Instant.ofEpochMilli(epochMillis))
    }

    fun startOfDayMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun endOfDayMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L

    fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

    fun nextOccurrenceMillis(
        epochMillis: Long,
        rule: RecurringRule,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val zdt = toZonedDateTime(epochMillis, zoneId)
        return when (rule) {
            RecurringRule.NONE -> null
            RecurringRule.WEEKLY -> zdt.plusWeeks(1).toInstant().toEpochMilli()
            RecurringRule.MONTHLY -> zdt.plusMonths(1).toInstant().toEpochMilli()
        }
    }
}
