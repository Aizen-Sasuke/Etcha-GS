package com.example.ui

import com.example.data.HabitEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * Pure Kotlin calculator for habit streaks, strength score, and monthly summary stats.
 * Extracted from HabitViewModel for modularity and direct unit testing.
 */
object HabitStatsCalculator {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getScheduledDaysOfWeek(tracker: TrackerConfig): Set<DayOfWeek> {
        return when (tracker.frequencyType) {
            "weekdays" -> setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )
            "custom_days", "weekly" -> {
                val parts = tracker.targetDays.split(",").map { it.trim().uppercase() }
                val set = mutableSetOf<DayOfWeek>()
                for (p in parts) {
                    when (p) {
                        "MON" -> set.add(DayOfWeek.MONDAY)
                        "TUE" -> set.add(DayOfWeek.TUESDAY)
                        "WED" -> set.add(DayOfWeek.WEDNESDAY)
                        "THU" -> set.add(DayOfWeek.THURSDAY)
                        "FRI" -> set.add(DayOfWeek.FRIDAY)
                        "SAT" -> set.add(DayOfWeek.SATURDAY)
                        "SUN" -> set.add(DayOfWeek.SUNDAY)
                    }
                }
                if (set.isEmpty()) DayOfWeek.values().toSet() else set
            }
            else -> DayOfWeek.values().toSet()
        }
    }

    fun calculateStats(
        list: List<HabitEntry>,
        freezeActive: String?,
        freezeHist: String,
        tracker: TrackerConfig,
        today: LocalDate,
        vacation: VacationPauseState = VacationPauseState()
    ): HabitStats {
        if (list.isEmpty()) return HabitStats()

        val currentYear = today.year
        val currentMonth = today.monthValue
        val lastMonthDate = today.minusMonths(1)

        val totalSum = list.sumOf { it.count }
        val (strengthScore, strengthStatus) = calculateHabitStrength(list, today, tracker, vacation)

        var thisYearSum = 0
        var thisMonthSum = 0
        var lastMonthSum = 0

        for (entry in list) {
            val date = parseLocalDateOrNull(entry.dateString) ?: continue
            if (date.year == currentYear) {
                thisYearSum += entry.count
            }
            if (date.year == currentYear && date.monthValue == currentMonth) {
                thisMonthSum += entry.count
            }
            if (date.year == lastMonthDate.year && date.monthValue == lastMonthDate.monthValue) {
                lastMonthSum += entry.count
            }
        }

        val percentageChange: Double
        val percentageIsDecrease: Boolean
        val percentageText: String

        if (lastMonthSum == 0) {
            percentageChange = if (thisMonthSum > 0) 100.0 else 0.0
            percentageIsDecrease = false
            percentageText = if (thisMonthSum > 0) "↑100%" else "0%"
        } else {
            val diff = thisMonthSum - lastMonthSum
            percentageChange = (diff.toDouble() / lastMonthSum.toDouble()) * 100.0
            percentageIsDecrease = diff < 0
            val absVal = abs(percentageChange)
            val sign = if (diff < 0) "↓" else "↑"
            percentageText = String.format(Locale.US, "%s%.1f%%", sign, absVal)
        }

        if (tracker.type == "misc") {
            return HabitStats(
                totalCount = totalSum,
                thisYearCount = thisYearSum,
                thisMonthCount = thisMonthSum,
                lastMonthCount = lastMonthSum,
                percentageChange = percentageChange,
                percentageText = percentageText,
                percentageIsDecrease = percentageIsDecrease,
                currentStreak = 0,
                bestStreak = 0,
                bestStreakTimeline = null,
                habitStrengthScore = strengthScore,
                habitStrengthStatus = strengthStatus
            )
        }

        if (tracker.type == "bad") {
            val allDates = list.mapNotNull { parseLocalDateOrNull(it.dateString) }.sorted()
            if (allDates.isEmpty()) {
                return HabitStats(
                    totalCount = totalSum,
                    thisYearCount = thisYearSum,
                    thisMonthCount = thisMonthSum,
                    lastMonthCount = lastMonthSum,
                    percentageChange = percentageChange,
                    percentageText = percentageText,
                    percentageIsDecrease = percentageIsDecrease,
                    currentStreak = 0,
                    bestStreak = 0,
                    bestStreakTimeline = null,
                    habitStrengthScore = strengthScore,
                    habitStrengthStatus = strengthStatus
                )
            }

            val firstDate = allDates.first()
            val upperBoundary = if (allDates.last().isAfter(today)) allDates.last() else today
            val activeDates = list.filter { it.count > 0 }.mapNotNull { parseLocalDateOrNull(it.dateString) }.sorted()

            val currentStreak = if (activeDates.isNotEmpty()) {
                val slipDiff = ChronoUnit.DAYS.between(activeDates.last(), upperBoundary).toInt()
                maxOf(0, slipDiff)
            } else {
                ChronoUnit.DAYS.between(firstDate, upperBoundary).toInt() + 1
            }

            val cleanIntervals = mutableListOf<Pair<LocalDate, LocalDate>>()
            if (activeDates.isEmpty()) {
                if (!firstDate.isAfter(upperBoundary)) {
                    cleanIntervals.add(firstDate to upperBoundary)
                }
            } else {
                if (firstDate.isBefore(activeDates.first())) {
                    cleanIntervals.add(firstDate to activeDates.first().minusDays(1))
                }
                for (i in 1 until activeDates.size) {
                    val s1 = activeDates[i - 1]
                    val s2 = activeDates[i]
                    if (s1.plusDays(1).isBefore(s2)) {
                        cleanIntervals.add(s1.plusDays(1) to s2.minusDays(1))
                    }
                }
                if (activeDates.last().isBefore(upperBoundary)) {
                    cleanIntervals.add(activeDates.last().plusDays(1) to upperBoundary)
                }
            }

            var maxLen = 0
            var bestStart: LocalDate? = null
            var bestEnd: LocalDate? = null
            cleanIntervals.forEach { (start, end) ->
                val len = ChronoUnit.DAYS.between(start, end).toInt() + 1
                if (len > maxLen) {
                    maxLen = len
                    bestStart = start
                    bestEnd = end
                }
            }

            return HabitStats(
                totalCount = totalSum,
                thisYearCount = thisYearSum,
                thisMonthCount = thisMonthSum,
                lastMonthCount = lastMonthSum,
                percentageChange = percentageChange,
                percentageText = percentageText,
                percentageIsDecrease = percentageIsDecrease,
                currentStreak = currentStreak,
                bestStreak = maxLen,
                bestStreakTimeline = formatStreakTimeline(bestStart, bestEnd),
                habitStrengthScore = strengthScore,
                habitStrengthStatus = strengthStatus
            )
        }

        if (tracker.frequencyType == "weekly_quota") {
            val streakRes = calculateWeeklyQuotaStreaks(list, today, tracker, vacation)
            return HabitStats(
                totalCount = totalSum,
                thisYearCount = thisYearSum,
                thisMonthCount = thisMonthSum,
                lastMonthCount = lastMonthSum,
                percentageChange = percentageChange,
                percentageText = percentageText,
                percentageIsDecrease = percentageIsDecrease,
                currentStreak = streakRes.currentStreak,
                bestStreak = streakRes.bestStreak,
                bestStreakTimeline = formatStreakTimeline(streakRes.bestStreakStart, streakRes.bestStreakEnd),
                habitStrengthScore = strengthScore,
                habitStrengthStatus = strengthStatus
            )
        }

        val frozenDates = freezeHist.split(",").filter { it.isNotBlank() }.toSet()
        val streakRes = calculateStreaks(list, frozenDates, freezeActive, today, tracker, vacation)

        return HabitStats(
            totalCount = totalSum,
            thisYearCount = thisYearSum,
            thisMonthCount = thisMonthSum,
            lastMonthCount = lastMonthSum,
            percentageChange = percentageChange,
            percentageText = percentageText,
            percentageIsDecrease = percentageIsDecrease,
            currentStreak = streakRes.currentStreak,
            bestStreak = streakRes.bestStreak,
            bestStreakTimeline = formatStreakTimeline(streakRes.bestStreakStart, streakRes.bestStreakEnd),
            habitStrengthScore = strengthScore,
            habitStrengthStatus = strengthStatus
        )
    }

    fun calculateHabitStrength(
        list: List<HabitEntry>,
        today: LocalDate,
        tracker: TrackerConfig,
        vacation: VacationPauseState = VacationPauseState()
    ): Pair<Int, String> {
        if (list.isEmpty()) return Pair(0, "Starting 🌱")

        val target = tracker.targetCount.coerceAtLeast(1)
        val completionDates = list.groupBy { it.dateString }
            .filter { (_, entries) -> entries.sumOf { it.count } >= target }
            .keys
            .mapNotNull { parseLocalDateOrNull(it) }
            .toSet()

        val sortedDates = list.mapNotNull { parseLocalDateOrNull(it.dateString) }.sorted()
        if (sortedDates.isEmpty()) return Pair(0, "Starting 🌱")

        if (tracker.frequencyType == "weekly_quota") {
            val weeklyTarget = tracker.weeklyTarget.coerceIn(1, 7)
            val startMonday = sortedDates.first().with(DayOfWeek.MONDAY)
            val currentMonday = today.with(DayOfWeek.MONDAY)

            var strength = 0.0
            val alpha = 0.15
            val beta = 0.12

            var weekMon = startMonday
            while (!weekMon.isAfter(currentMonday)) {
                var distinctCompletedDays = 0
                var pausedDaysInWeek = 0
                for (d in 0..6) {
                    val day = weekMon.plusDays(d.toLong())
                    if (day in completionDates) {
                        distinctCompletedDays++
                    }
                    if (VacationPauseHelper.isPausedOnEffectiveDate(day, tracker.pausedUntilDate, vacation)) {
                        pausedDaysInWeek++
                    }
                }
                val isSuccessfulWeek = distinctCompletedDays >= weeklyTarget
                val isProtectedWeek = pausedDaysInWeek >= 4
                val isCurrentWeek = weekMon == currentMonday

                if (isSuccessfulWeek) {
                    strength += alpha * (1.0 - strength)
                } else if (!isCurrentWeek && !isProtectedWeek) {
                    strength *= (1.0 - beta)
                }
                weekMon = weekMon.plusWeeks(1)
            }

            val score = (strength * 100.0).toInt().coerceIn(0, 100)
            return Pair(score, strengthStatusForScore(score))
        }

        val startDate = sortedDates.first().coerceAtLeast(today.minusDays(180))
        val allowedDaysSet = getScheduledDaysOfWeek(tracker)

        var strength = 0.0
        val alpha = 0.05
        val beta = 0.035

        var cur = startDate
        while (!cur.isAfter(today)) {
            val isPaused = VacationPauseHelper.isPausedOnEffectiveDate(cur, tracker.pausedUntilDate, vacation)
            val isTargetDay = cur.dayOfWeek in allowedDaysSet
            if (tracker.type == "bad") {
                val hadSlip = cur in completionDates
                if (!hadSlip && !isPaused) {
                    strength += alpha * (1.0 - strength)
                } else if (hadSlip) {
                    strength *= (1.0 - beta * 2.5)
                }
            } else if (isTargetDay) {
                val done = cur in completionDates
                if (done) {
                    strength += alpha * (1.0 - strength)
                } else if (cur.isBefore(today) && !isPaused) {
                    strength *= (1.0 - beta)
                }
            }
            cur = cur.plusDays(1)
        }

        val score = (strength * 100.0).toInt().coerceIn(0, 100)
        return Pair(score, strengthStatusForScore(score))
    }

    private fun strengthStatusForScore(score: Int): String = when {
        score >= 85 -> "Ironclad 💎"
        score >= 65 -> "Established 🏆"
        score >= 45 -> "Building 🚀"
        score >= 20 -> "Developing ⚡"
        else -> "Starting 🌱"
    }

    fun calculateWeeklyQuotaStreaks(
        list: List<HabitEntry>,
        today: LocalDate,
        tracker: TrackerConfig,
        vacation: VacationPauseState = VacationPauseState()
    ): StreakResult {
        val targetCount = tracker.targetCount.coerceAtLeast(1)
        val weeklyTarget = tracker.weeklyTarget.coerceIn(1, 7)

        val completedDates = list
            .groupBy { it.dateString }
            .filter { (_, entries) -> entries.sumOf { it.count } >= targetCount }
            .keys
            .mapNotNull { parseLocalDateOrNull(it) }
            .toSet()

        if (completedDates.isEmpty()) return StreakResult(0, 0, null, null)

        val startMonday = completedDates.minOrNull()!!.with(DayOfWeek.MONDAY)
        val currentMonday = today.with(DayOfWeek.MONDAY)

        val completedWeeks = mutableSetOf<LocalDate>()
        val protectedWeeks = mutableSetOf<LocalDate>()
        var weekCursor = startMonday
        while (!weekCursor.isAfter(currentMonday)) {
            var distinctCompletedDays = 0
            var pausedDays = 0
            for (d in 0..6) {
                val day = weekCursor.plusDays(d.toLong())
                if (day in completedDates) {
                    distinctCompletedDays++
                }
                if (VacationPauseHelper.isPausedOnEffectiveDate(day, tracker.pausedUntilDate, vacation)) {
                    pausedDays++
                }
            }
            if (distinctCompletedDays >= weeklyTarget) {
                completedWeeks.add(weekCursor)
            } else if (pausedDays >= 4) {
                protectedWeeks.add(weekCursor)
            }
            weekCursor = weekCursor.plusWeeks(1)
        }

        var currentStreak = 0
        var checkWeek = if (currentMonday in completedWeeks) {
            currentStreak++
            currentMonday.minusWeeks(1)
        } else {
            currentMonday.minusWeeks(1)
        }

        while (!checkWeek.isBefore(startMonday)) {
            if (checkWeek in completedWeeks) {
                currentStreak++
                checkWeek = checkWeek.minusWeeks(1)
            } else if (checkWeek in protectedWeeks) {
                checkWeek = checkWeek.minusWeeks(1)
            } else {
                break
            }
        }

        var bestStreak = 0
        var bestStart: LocalDate? = null
        var bestEnd: LocalDate? = null
        var tempStreak = 0
        var tempStart: LocalDate? = null

        var iterWeek = startMonday
        while (!iterWeek.isAfter(currentMonday)) {
            if (iterWeek in completedWeeks) {
                if (tempStreak == 0) tempStart = iterWeek
                tempStreak++
                val tempEnd = iterWeek.plusDays(6)
                if (tempStreak > bestStreak) {
                    bestStreak = tempStreak
                    bestStart = tempStart
                    bestEnd = tempEnd
                }
            } else if (iterWeek !in protectedWeeks) {
                tempStreak = 0
                tempStart = null
            }
            iterWeek = iterWeek.plusWeeks(1)
        }

        if (bestStreak < currentStreak) bestStreak = currentStreak
        return StreakResult(currentStreak, bestStreak, bestStart, bestEnd)
    }

    fun calculateStreaks(
        list: List<HabitEntry>,
        frozenDates: Set<String>,
        activeFreeze: String?,
        today: LocalDate,
        tracker: TrackerConfig,
        vacation: VacationPauseState = VacationPauseState()
    ): StreakResult {
        val target = tracker.targetCount.coerceAtLeast(1)
        val completedDates = list
            .groupBy { it.dateString }
            .filter { (_, entries) -> entries.sumOf { it.count } >= target }
            .keys
            .toSet()

        val dates = completedDates + frozenDates + listOfNotNull(activeFreeze)
        val scheduledDays = getScheduledDaysOfWeek(tracker)
        if (dates.isEmpty()) return StreakResult(0, 0, null, null)

        val parsedDates = dates.mapNotNull { parseLocalDateOrNull(it) }.sorted()
        if (parsedDates.isEmpty()) return StreakResult(0, 0, null, null)

        val dateSet = parsedDates.toSet()
        val firstDate = parsedDates.first()

        var currentStreak = 0
        var checkDate = if (today.dayOfWeek in scheduledDays && today in dateSet) {
            currentStreak++
            today.minusDays(1)
        } else {
            today.minusDays(1)
        }

        while (!checkDate.isBefore(firstDate.minusDays(7))) {
            val isPaused = VacationPauseHelper.isPausedOnEffectiveDate(checkDate, tracker.pausedUntilDate, vacation)
            if (checkDate.dayOfWeek in scheduledDays) {
                if (checkDate in dateSet) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                } else if (isPaused) {
                    checkDate = checkDate.minusDays(1)
                } else {
                    break
                }
            } else {
                checkDate = checkDate.minusDays(1)
            }
        }

        var bestStreak = 0
        var bestStart: LocalDate? = null
        var bestEnd: LocalDate? = null
        var tempStreak = 0
        var tempStart: LocalDate? = null

        var cur = firstDate
        while (!cur.isAfter(today)) {
            val isPaused = VacationPauseHelper.isPausedOnEffectiveDate(cur, tracker.pausedUntilDate, vacation)
            if (cur.dayOfWeek in scheduledDays) {
                if (cur in dateSet) {
                    if (tempStreak == 0) tempStart = cur
                    tempStreak++
                    if (tempStreak > bestStreak) {
                        bestStreak = tempStreak
                        bestStart = tempStart
                        bestEnd = cur
                    }
                } else if (!isPaused) {
                    tempStreak = 0
                    tempStart = null
                }
            }
            cur = cur.plusDays(1)
        }

        if (bestStreak < currentStreak) bestStreak = currentStreak
        return StreakResult(currentStreak, bestStreak, bestStart, bestEnd)
    }

    fun formatStreakTimeline(start: LocalDate?, end: LocalDate?): String? {
        if (start == null || end == null) return null
        val startDay = start.dayOfMonth
        val startMonth = start.month.getDisplayName(TextStyle.SHORT, Locale.US).lowercase()
        val startYear = start.year
        val endDay = end.dayOfMonth
        val endMonth = end.month.getDisplayName(TextStyle.SHORT, Locale.US).lowercase()
        val endYear = end.year

        return if (startYear == endYear) {
            if (startMonth == endMonth) {
                if (startDay == endDay) "$startDay $startMonth $startYear"
                else "$startDay–$endDay $startMonth $startYear"
            } else {
                "$startDay $startMonth – $endDay $endMonth $startYear"
            }
        } else {
            "$startDay $startMonth $startYear – $endDay $endMonth $endYear"
        }
    }

    private fun parseLocalDateOrNull(dateString: String): LocalDate? = try {
        LocalDate.parse(dateString, DATE_FORMATTER)
    } catch (_: Exception) {
        null
    }
}
