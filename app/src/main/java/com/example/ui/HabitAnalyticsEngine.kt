package com.example.ui

import com.example.data.HabitEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class MonthlyTrendPoint(
    val monthLabel: String,
    val fullMonthLabel: String,
    val count: Int
)

data class DayOfWeekStat(
    val dayName: String,
    val count: Int
)

data class WeeklyConsistencyPoint(
    val weekLabel: String,
    val completionPercent: Int,
    val completedDays: Int,
    val scheduledDays: Int
)

data class MoodStat(
    val emoji: String,
    val count: Int,
    val fraction: Float
)

data class TimeOfDayStat(
    val label: String,
    val icon: String,
    val count: Int,
    val fraction: Float
)

data class ReportAnalyticsSnapshot(
    val loggedDaysCount: Int = 0,
    val totalCount: Int = 0,
    val totalSpanDays: Int = 1,
    val countSubtitle: String = "No range logged",
    val avgPerMonth: Double = 0.0,
    val avgSubtitle: String = "No logs recorded",
    val currentMonthLoggedDays: Int = 0,
    val daysInCurrentMonth: Int = 30,
    val monthlyTrendAll: List<MonthlyTrendPoint> = emptyList(),
    val monthlyTrendYear: List<MonthlyTrendPoint> = emptyList(),
    val dayOfWeekAll: List<DayOfWeekStat> = emptyList(),
    val dayOfWeekYear: List<DayOfWeekStat> = emptyList(),
    val weeklyConsistency: List<WeeklyConsistencyPoint> = emptyList(),
    val moodBreakdown: List<MoodStat> = emptyList(),
    val timeOfDayBreakdown: List<TimeOfDayStat> = emptyList(),
    val annualInsights: String = ""
)

/**
 * Single-pass analytics engine for ReportScreen charts, consistency metrics, moods, and time-of-day rhythms.
 */
object HabitAnalyticsEngine {

    private val MOOD_REGEX = Regex("\\[MOOD:([^\\]]+)\\]")
    private val MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)

    fun build(
        entries: List<HabitEntry>,
        tracker: TrackerConfig,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ReportAnalyticsSnapshot {
        val currentYear = today.year
        val currentMonthYm = YearMonth.from(today)

        val parsedEntries = entries.mapNotNull { entry ->
            val date = try {
                LocalDate.parse(entry.dateString)
            } catch (_: Exception) {
                null
            }
            date?.let { it to entry }
        }

        val loggedDayKeys = HashSet<String>()
        val monthCountsAll = HashMap<YearMonth, Int>()
        val monthCountsYear = HashMap<YearMonth, Int>()
        val dowAll = IntArray(8)
        val dowYear = IntArray(8)
        val dayCountsByDate = HashMap<LocalDate, Int>()
        val moodCounts = LinkedHashMap<String, Int>()
        val todCounts = IntArray(4) // 0=Morning, 1=Afternoon, 2=Evening, 3=Night
        val yearTotals = HashMap<Int, Int>()

        var totalCount = 0
        var minDate: LocalDate? = null
        var maxDate: LocalDate? = null
        val currentMonthDates = HashSet<LocalDate>()

        for ((date, entry) in parsedEntries) {
            loggedDayKeys.add(entry.dateString)
            totalCount += entry.count
            if (minDate == null || date.isBefore(minDate)) minDate = date
            if (maxDate == null || date.isAfter(maxDate)) maxDate = date

            val ym = YearMonth.from(date)
            monthCountsAll[ym] = (monthCountsAll[ym] ?: 0) + entry.count
            dowAll[date.dayOfWeek.value] += entry.count
            dayCountsByDate[date] = (dayCountsByDate[date] ?: 0) + entry.count
            yearTotals[date.year] = (yearTotals[date.year] ?: 0) + entry.count

            if (date.year == currentYear) {
                monthCountsYear[ym] = (monthCountsYear[ym] ?: 0) + entry.count
                dowYear[date.dayOfWeek.value] += entry.count
            }
            if (ym == currentMonthYm && entry.count > 0) {
                currentMonthDates.add(date)
            }

            entry.notes?.let { note ->
                val match = MOOD_REGEX.find(note)
                val emoji = match?.groupValues?.get(1)?.trim()
                if (!emoji.isNullOrBlank()) {
                    moodCounts[emoji] = (moodCounts[emoji] ?: 0) + 1
                }
            }

            if (entry.timestamp > 0L) {
                val hour = Instant.ofEpochMilli(entry.timestamp).atZone(zoneId).hour
                val bucket = when (hour) {
                    in 5..11 -> 0
                    in 12..16 -> 1
                    in 17..21 -> 2
                    else -> 3
                }
                todCounts[bucket] += 1
            }
        }

        val totalSpanDays = if (minDate != null && maxDate != null) {
            (ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1).coerceAtLeast(1)
        } else 1

        val countSubtitle = if (minDate != null && maxDate != null) {
            "${minDate.format(MONTH_YEAR_FMT)} – ${maxDate.format(MONTH_YEAR_FMT)}"
        } else "No range logged"

        val avgPerMonth = if (parsedEntries.isEmpty()) 0.0 else {
            val months = (totalSpanDays / 30.4).coerceAtLeast(1.0)
            totalCount / months
        }

        val avgSubtitle = if (parsedEntries.isEmpty() || totalCount == 0) {
            "No logs recorded"
        } else {
            val ratio = totalSpanDays.toDouble() / totalCount.toDouble()
            "roughly every ${String.format(Locale.US, "%.1f", ratio)} days"
        }

        val trendAll = buildMonthlyTrend(monthCountsAll, minDate, today)
        val trendYear = buildMonthlyTrend(monthCountsYear, minDate?.takeIf { it.year == currentYear }, today, forceSixMonths = true)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayOfWeekAll = dayNames.mapIndexed { idx, name -> DayOfWeekStat(name, dowAll[idx + 1]) }
        val dayOfWeekYear = dayNames.mapIndexed { idx, name -> DayOfWeekStat(name, dowYear[idx + 1]) }

        val weeklyConsistency = buildWeeklyConsistency(dayCountsByDate, tracker, today, weeksBack = 12)

        val totalMoods = moodCounts.values.sum().coerceAtLeast(1)
        val moodBreakdown = moodCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (emoji, cnt) ->
                MoodStat(emoji = emoji, count = cnt, fraction = cnt.toFloat() / totalMoods.toFloat())
            }

        val totalTod = todCounts.sum().coerceAtLeast(1)
        val todMeta = listOf(
            "Morning" to "🌅",
            "Afternoon" to "☀️",
            "Evening" to "🌙",
            "Night" to "🦉"
        )
        val timeOfDayBreakdown = if (todCounts.sum() == 0) emptyList() else {
            todMeta.mapIndexed { idx, (label, icon) ->
                TimeOfDayStat(
                    label = label,
                    icon = icon,
                    count = todCounts[idx],
                    fraction = todCounts[idx].toFloat() / totalTod.toFloat()
                )
            }
        }

        val annualInsights = buildAnnualInsights(parsedEntries.size, yearTotals, today)

        return ReportAnalyticsSnapshot(
            loggedDaysCount = loggedDayKeys.size,
            totalCount = totalCount,
            totalSpanDays = totalSpanDays,
            countSubtitle = countSubtitle,
            avgPerMonth = avgPerMonth,
            avgSubtitle = avgSubtitle,
            currentMonthLoggedDays = currentMonthDates.size,
            daysInCurrentMonth = currentMonthYm.lengthOfMonth(),
            monthlyTrendAll = trendAll,
            monthlyTrendYear = trendYear,
            dayOfWeekAll = dayOfWeekAll,
            dayOfWeekYear = dayOfWeekYear,
            weeklyConsistency = weeklyConsistency,
            moodBreakdown = moodBreakdown,
            timeOfDayBreakdown = timeOfDayBreakdown,
            annualInsights = annualInsights
        )
    }

    private fun buildMonthlyTrend(
        countsByMonth: Map<YearMonth, Int>,
        firstDate: LocalDate?,
        today: LocalDate,
        forceSixMonths: Boolean = false
    ): List<MonthlyTrendPoint> {
        val currentYm = YearMonth.from(today)
        val startYm = if (forceSixMonths || firstDate == null) {
            currentYm.minusMonths(5)
        } else {
            val candidate = YearMonth.from(firstDate)
            val sixAgo = currentYm.minusMonths(5)
            if (candidate.isAfter(sixAgo)) sixAgo else candidate
        }

        val result = mutableListOf<MonthlyTrendPoint>()
        var cursor = startYm
        while (!cursor.isAfter(currentYm)) {
            val shortMonth = cursor.month.getDisplayName(TextStyle.SHORT, Locale.US)
            val label = if (cursor.monthValue == 1 || cursor == startYm) {
                "$shortMonth ${cursor.year % 100}"
            } else {
                shortMonth
            }
            val fullLabel = "$shortMonth ${cursor.year}"
            result.add(MonthlyTrendPoint(label, fullLabel, countsByMonth[cursor] ?: 0))
            cursor = cursor.plusMonths(1)
        }
        return result
    }

    private fun buildWeeklyConsistency(
        dayCountsByDate: Map<LocalDate, Int>,
        tracker: TrackerConfig,
        today: LocalDate,
        weeksBack: Int
    ): List<WeeklyConsistencyPoint> {
        val targetCount = tracker.targetCount.coerceAtLeast(1)
        val scheduledDays = HabitStatsCalculator.getScheduledDaysOfWeek(tracker)
        val isWeeklyQuota = tracker.frequencyType == "weekly_quota"
        val weeklyTarget = tracker.weeklyTarget.coerceIn(1, 7)
        val currentMonday = today.with(DayOfWeek.MONDAY)
        val startMonday = currentMonday.minusWeeks((weeksBack - 1).toLong())
        val labelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)

        val result = mutableListOf<WeeklyConsistencyPoint>()
        var weekMon = startMonday
        while (!weekMon.isAfter(currentMonday)) {
            var completedDays = 0
            var scheduledInWeek = 0

            for (d in 0..6) {
                val date = weekMon.plusDays(d.toLong())
                if (date.isAfter(today)) continue
                val count = dayCountsByDate[date] ?: 0
                if (tracker.type == "bad") {
                    scheduledInWeek++
                    if (count == 0) completedDays++
                } else if (isWeeklyQuota) {
                    if (count >= targetCount) completedDays++
                } else if (date.dayOfWeek in scheduledDays) {
                    scheduledInWeek++
                    if (count >= targetCount) completedDays++
                }
            }

            val denom = if (isWeeklyQuota) weeklyTarget else scheduledInWeek.coerceAtLeast(1)
            val pct = if (tracker.type != "bad" && !isWeeklyQuota && scheduledInWeek == 0) {
                100
            } else {
                ((completedDays.toFloat() / denom.toFloat()) * 100f).toInt().coerceIn(0, 100)
            }

            result.add(
                WeeklyConsistencyPoint(
                    weekLabel = weekMon.format(labelFmt),
                    completionPercent = pct,
                    completedDays = completedDays,
                    scheduledDays = denom
                )
            )
            weekMon = weekMon.plusWeeks(1)
        }
        return result
    }

    private fun buildAnnualInsights(
        entryCount: Int,
        yearTotals: Map<Int, Int>,
        today: LocalDate
    ): String {
        val currentYear = today.year
        if (entryCount == 0 || yearTotals.isEmpty()) {
            return "$currentYear is a fresh canvas — start logging daily to unlock multi-week patterns and insights."
        }
        val currentYearCount = yearTotals[currentYear] ?: 0
        val maxYearEntry = yearTotals.maxByOrNull { it.value }
        val builder = StringBuilder()
        if (maxYearEntry != null) {
            builder.append("${maxYearEntry.key} was your highest year — ${maxYearEntry.value} total")
            val previousYearCount = yearTotals[maxYearEntry.key - 1]
            if (previousYearCount != null) {
                builder.append(", compared to $previousYearCount in ${maxYearEntry.key - 1}.")
            } else {
                builder.append(".")
            }
        }
        val dayOfYear = today.dayOfYear.coerceAtLeast(1)
        if (currentYearCount > 0) {
            val annualized = ((currentYearCount.toDouble() / dayOfYear) * 365.0).toInt()
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append("$currentYear is pacing at ~$annualized logs annualized — steady momentum.")
        }
        return builder.toString()
    }
}
