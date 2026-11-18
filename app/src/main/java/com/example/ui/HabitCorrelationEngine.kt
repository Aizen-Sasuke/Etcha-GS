package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.HabitEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class WeekdayDropOffInsight(
    val trackerId: String,
    val trackerTitle: String,
    val trackerIcon: String,
    val weakestDayName: String,
    val weakestDayRatePercent: Int,
    val weeklyAverageRatePercent: Int,
    val dropPercentPoints: Int
)

@Immutable
data class HabitCorrelationInsight(
    val sourceTrackerId: String,
    val sourceTitle: String,
    val sourceIcon: String,
    val targetTrackerId: String,
    val targetTitle: String,
    val targetIcon: String,
    val withRatePercent: Int,
    val withoutRatePercent: Int,
    val liftPercentPoints: Int,
    val isPositive: Boolean
)

@Immutable
data class CorrelationInsightsSnapshot(
    val hasEnoughData: Boolean = false,
    val loggedDaysCount: Int = 0,
    val requiredDaysCount: Int = 14,
    val weekdayDropOff: WeekdayDropOffInsight? = null,
    val topPairings: List<HabitCorrelationInsight> = emptyList()
)

/**
 * Pure Kotlin behavioral insights calculator analyzing a 90-day window of completions
 * for weekday drop-offs and cross-habit completion lift pairings.
 */
object HabitCorrelationEngine {
    const val MIN_LOGGED_DAYS = 14
    private const val LOOKBACK_DAYS = 90L

    fun build(
        trackers: List<TrackerConfig>,
        allEntries: List<HabitEntry>,
        today: LocalDate = LocalDate.now(),
        vacation: VacationPauseState = VacationPauseState()
    ): CorrelationInsightsSnapshot {
        val activeTrackers = trackers.filter { !it.isArchived }
        if (activeTrackers.isEmpty() || allEntries.isEmpty()) {
            return CorrelationInsightsSnapshot(hasEnoughData = false, loggedDaysCount = 0)
        }

        val startDate = today.minusDays(LOOKBACK_DAYS - 1)
        val loggedDatesSet = HashSet<LocalDate>()

        // trackerId -> (date -> totalCount)
        val countsByTrackerAndDate = HashMap<String, HashMap<LocalDate, Int>>()
        for (entry in allEntries) {
            if (entry.count <= 0) continue
            val parsed = try {
                LocalDate.parse(entry.dateString)
            } catch (_: Exception) {
                null
            } ?: continue
            if (parsed.isBefore(startDate) || parsed.isAfter(today)) continue
            loggedDatesSet.add(parsed)
            val map = countsByTrackerAndDate.getOrPut(entry.trackerId) { HashMap() }
            map[parsed] = (map[parsed] ?: 0) + entry.count
        }

        val loggedDaysCount = loggedDatesSet.size
        if (loggedDaysCount < MIN_LOGGED_DAYS) {
            return CorrelationInsightsSnapshot(
                hasEnoughData = false,
                loggedDaysCount = loggedDaysCount,
                requiredDaysCount = MIN_LOGGED_DAYS
            )
        }

        val effectiveStart = loggedDatesSet.minOrNull()?.let { earliest ->
            if (earliest.isAfter(startDate)) earliest else startDate
        } ?: startDate

        val completedDaysByTracker = HashMap<String, Set<LocalDate>>()
        val eligibleDatesByTracker = HashMap<String, Set<LocalDate>>()

        for (tracker in activeTrackers) {
            val target = tracker.targetCount.coerceAtLeast(1)
            val dayCounts = countsByTrackerAndDate[tracker.id].orEmpty()
            val scheduledDays = HabitStatsCalculator.getScheduledDaysOfWeek(tracker)
            val completed = HashSet<LocalDate>()
            val eligible = HashSet<LocalDate>()

            var cur = effectiveStart
            while (!cur.isAfter(today)) {
                val isPaused = VacationPauseHelper.isPausedOnEffectiveDate(cur, tracker.pausedUntilDate, vacation)
                val isScheduled = tracker.type == "bad" ||
                    tracker.frequencyType == "weekly_quota" ||
                    tracker.frequencyType == "3x_week" ||
                    (cur.dayOfWeek in scheduledDays)
                if (!isPaused && isScheduled) {
                    eligible.add(cur)
                    val count = dayCounts[cur] ?: 0
                    val isSuccess = if (tracker.type == "bad") count == 0 else count >= target
                    if (isSuccess) {
                        completed.add(cur)
                    }
                }
                cur = cur.plusDays(1)
            }
            completedDaysByTracker[tracker.id] = completed
            eligibleDatesByTracker[tracker.id] = eligible
        }

        // 1. Weekday drop-off analysis
        var bestDropOff: WeekdayDropOffInsight? = null
        for (tracker in activeTrackers) {
            if (tracker.type == "bad") continue
            val eligible = eligibleDatesByTracker[tracker.id].orEmpty()
            val completed = completedDaysByTracker[tracker.id].orEmpty()
            if (eligible.size < MIN_LOGGED_DAYS) continue

            val overallRate = (completed.size.toDouble() / eligible.size.toDouble()) * 100.0
            if (overallRate < 25.0) continue

            var weakestDay: DayOfWeek? = null
            var weakestRate = 101.0
            for (dow in DayOfWeek.values()) {
                val eligibleOnDow = eligible.filter { it.dayOfWeek == dow }
                if (eligibleOnDow.size < 2) continue
                val doneOnDow = eligibleOnDow.count { it in completed }
                val rate = (doneOnDow.toDouble() / eligibleOnDow.size.toDouble()) * 100.0
                if (rate < weakestRate) {
                    weakestRate = rate
                    weakestDay = dow
                }
            }

            if (weakestDay != null) {
                val drop = (overallRate - weakestRate).roundToInt()
                if (drop >= 12) {
                    val candidate = WeekdayDropOffInsight(
                        trackerId = tracker.id,
                        trackerTitle = tracker.title,
                        trackerIcon = tracker.icon,
                        weakestDayName = weakestDay.getDisplayName(TextStyle.FULL, Locale.US),
                        weakestDayRatePercent = weakestRate.roundToInt().coerceIn(0, 100),
                        weeklyAverageRatePercent = overallRate.roundToInt().coerceIn(0, 100),
                        dropPercentPoints = drop
                    )
                    if (bestDropOff == null || candidate.dropPercentPoints > bestDropOff.dropPercentPoints) {
                        bestDropOff = candidate
                    }
                }
            }
        }

        // 2. Cross-habit correlation pairings (Source -> Target conditional probability lift)
        val pairings = mutableListOf<HabitCorrelationInsight>()
        if (activeTrackers.size >= 2) {
            for (source in activeTrackers) {
                val sourceCompleted = completedDaysByTracker[source.id].orEmpty()
                val sourceEligible = eligibleDatesByTracker[source.id].orEmpty()
                if (sourceCompleted.size < 4 || sourceEligible.size < 10) continue

                for (target in activeTrackers) {
                    if (source.id == target.id) continue
                    val targetEligible = eligibleDatesByTracker[target.id].orEmpty()
                    val targetCompleted = completedDaysByTracker[target.id].orEmpty()

                    val sharedEligible = sourceEligible.intersect(targetEligible)
                    if (sharedEligible.size < 12) continue

                    val daysWithSource = sharedEligible.filter { it in sourceCompleted }
                    val daysWithoutSource = sharedEligible.filter { it !in sourceCompleted }
                    if (daysWithSource.size < 4 || daysWithoutSource.size < 4) continue

                    val rateWith = (daysWithSource.count { it in targetCompleted }.toDouble() / daysWithSource.size.toDouble()) * 100.0
                    val rateWithout = (daysWithoutSource.count { it in targetCompleted }.toDouble() / daysWithoutSource.size.toDouble()) * 100.0
                    val lift = (rateWith - rateWithout).roundToInt()

                    if (kotlin.math.abs(lift) >= 12) {
                        pairings.add(
                            HabitCorrelationInsight(
                                sourceTrackerId = source.id,
                                sourceTitle = source.title,
                                sourceIcon = source.icon,
                                targetTrackerId = target.id,
                                targetTitle = target.title,
                                targetIcon = target.icon,
                                withRatePercent = rateWith.roundToInt().coerceIn(0, 100),
                                withoutRatePercent = rateWithout.roundToInt().coerceIn(0, 100),
                                liftPercentPoints = lift,
                                isPositive = lift > 0
                            )
                        )
                    }
                }
            }
        }

        val topPairings = pairings
            .sortedByDescending { kotlin.math.abs(it.liftPercentPoints) }
            .distinctBy { setOf(it.sourceTrackerId, it.targetTrackerId) }
            .take(3)

        return CorrelationInsightsSnapshot(
            hasEnoughData = true,
            loggedDaysCount = loggedDaysCount,
            requiredDaysCount = MIN_LOGGED_DAYS,
            weekdayDropOff = bestDropOff,
            topPairings = topPairings
        )
    }
}
