package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateCalendarUtils {

    private val monthNamesPt = arrayOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    private val shortWeekdaysPt = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    fun todayEpochDay(): Long {
        val cal = Calendar.getInstance()
        return toEpochDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return formatYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun formatYearMonth(year: Int, month1To12: Int): String {
        return String.format(Locale.US, "%04d-%02d", year, month1To12)
    }

    fun yearMonthFromEpochDay(epochDay: Long): String {
        val (y, m, _) = fromEpochDay(epochDay)
        return formatYearMonth(y, m)
    }

    fun formatYearMonthDisplay(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return yearMonth
        val year = parts[0].toIntOrNull() ?: 2026
        val month = (parts[1].toIntOrNull() ?: 1).coerceIn(1, 12)
        return "${monthNamesPt[month - 1]} $year"
    }

    fun shiftYearMonth(yearMonth: String, deltaMonths: Int): String {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return formatYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun toEpochDay(year: Int, month1To12: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month1To12 - 1, dayOfMonth, 0, 0, 0)
        }
        return cal.timeInMillis / 86_400_000L
    }

    fun fromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * 86_400_000L
        }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formatEpochDayPt(epochDay: Long): String {
        val (y, m, d) = fromEpochDay(epochDay)
        val monthShort = monthNamesPt[(m - 1).coerceIn(0, 11)].take(3)
        return String.format(Locale("pt", "BR"), "%02d de %s, %04d", d, monthShort, y)
    }

    fun formatIsoDate(epochDay: Long): String {
        val (y, m, d) = fromEpochDay(epochDay)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun todayIsoDate(): String = formatIsoDate(todayEpochDay())

    fun currentTimeHHmm(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun weekdayNamesShort(): List<String> = shortWeekdaysPt.toList()

    fun daysInMonth(year: Int, month1To12: Int): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, (month1To12 - 1).coerceIn(0, 11), 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Generates a list of calendar cells for a given YYYY-MM month.
     * Null entries represent leading empty days before day 1 of the month.
     */
    fun buildMonthCalendarGrid(yearMonth: String): List<Long?> {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }
        val firstDayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0 = Sunday .. 6 = Saturday
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<Long?>()
        repeat(firstDayOfWeekIndex.coerceAtLeast(0)) {
            cells.add(null)
        }
        for (day in 1..daysInMonth) {
            cells.add(toEpochDay(year, month, day))
        }
        return cells
    }

    fun epochMillisForAlarm(epochDay: Long, hour: Int, minute: Int): Long {
        val (y, m, d) = fromEpochDay(epochDay)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun nextDueAlarmMillis(dueDayOfMonth: Int, reminderDaysBefore: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dueDayOfMonth.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, -reminderDaysBefore.coerceIn(0, 15))
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.MONTH, 1)
        }
        return target.timeInMillis
    }

    fun currentTimestampLabel(): String {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
        return sdf.format(Date())
    }
}
