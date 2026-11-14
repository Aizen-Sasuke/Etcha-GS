package com.example.ui

import com.example.data.HabitEntry
import com.example.data.Tracker
import com.example.worker.ReminderManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

enum class TodayHabitStatus {
    NEEDS_ATTENTION,
    IN_PROGRESS,
    COMPLETED,
    REST_DAY,
    QUOTA_MET,
    FROZEN,
    BAD_CLEAN,
    BAD_SLIP
}

data class TodayHabitItem(
    val tracker: TrackerConfig,
    val status: TodayHabitStatus,
    val todayCount: Int,
    val targetCount: Int,
    val progressFraction: Float,
    val weeklyCompletedDays: Int,
    val weeklyTarget: Int,
    val isFrozenToday: Boolean,
    val isRestDay: Boolean,
    val isWeeklyQuota: Boolean,
    val reminderSummary: String?,
    val isReminderUpcomingToday: Boolean,
    val statusLabel: String
)

data class TodayDashboardState(
    val effectiveToday: LocalDate = LocalDate.now(),
    val attentionItems: List<TodayHabitItem> = emptyList(),
    val avoidanceItems: List<TodayHabitItem> = emptyList(),
    val completedItems: List<TodayHabitItem> = emptyList(),
    val restAndProtectedItems: List<TodayHabitItem> = emptyList(),
    val dueCompletedCount: Int = 0,
    val dueTotalCount: Int = 0,
    val overallProgress: Float = 0f,
    val nextReminderText: String? = null,
    val isTodayFrozen: Boolean = false,
    val totalActiveCount: Int = 0,
    val allItems: List<TodayHabitItem> = emptyList()
)

object TodayDashboardCalculator {

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

    fun build(
        trackers: List<TrackerConfig>,
        recentEntries: List<HabitEntry>,
        preferences: Map<String, String>,
        now: LocalDateTime = LocalDateTime.now()
    ): TodayDashboardState {
        val dayRolloverHour = preferences["day_rollover_hour"]?.toIntOrNull() ?: 0
        val effectiveToday = ReminderManager.getEffectiveToday(dayRolloverHour, now)
        val todayStr = effectiveToday.toString()

        val freezeActive = preferences["streak_freeze_date"]?.takeIf { it.isNotBlank() }
        val freezeHist = preferences["streak_freeze_history"] ?: ""
        val frozenDates = freezeHist.split(",").filter { it.isNotBlank() }.toSet()
        val isTodayFrozen = (freezeActive == todayStr) || (todayStr in frozenDates)

        val activeTrackers = trackers.filter { !it.isArchived }
        if (activeTrackers.isEmpty()) {
            return TodayDashboardState(
                effectiveToday = effectiveToday,
                isTodayFrozen = isTodayFrozen,
                totalActiveCount = 0
            )
        }

        val globalRemindersEnabled = preferences["reminders_enabled"] == "true"
        val globalReminderTime = preferences["reminder_time"] ?: "21:00"

        // Pre-index recent entries once per recomputation: trackerId -> (dateString -> sum(count))
        val countsByTrackerAndDate: Map<String, Map<String, Int>> = recentEntries
            .groupBy { it.trackerId }
            .mapValues { (_, list) ->
                list.groupBy { it.dateString }.mapValues { (_, dayList) -> dayList.sumOf { it.count } }
            }

        val monday = effectiveToday.with(DayOfWeek.MONDAY)
        val currentWeekDates = (0..6).map { monday.plusDays(it.toLong()).toString() }

        val attentionItems = mutableListOf<TodayHabitItem>()
        val avoidanceItems = mutableListOf<TodayHabitItem>()
        val completedItems = mutableListOf<TodayHabitItem>()
        val restAndProtectedItems = mutableListOf<TodayHabitItem>()
        val allItems = mutableListOf<TodayHabitItem>()

        var dueTotalCount = 0
        var dueCompletedCount = 0
        var fractionalSum = 0f

        var earliestUpcomingTrigger: Pair<LocalDateTime, TrackerConfig>? = null
        var hasActionableHabitUsingGlobal = false

        for (tracker in activeTrackers) {
            val trackerDayCounts = countsByTrackerAndDate[tracker.id].orEmpty()
            val todayCount = trackerDayCounts[todayStr] ?: 0
            val targetCount = tracker.targetCount.coerceAtLeast(1)
            val progressFraction = (todayCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
            val isWeeklyQuota = tracker.frequencyType == "weekly_quota" || tracker.frequencyType == "3x_week"
            val weeklyTarget = if (isWeeklyQuota) tracker.weeklyTarget.coerceIn(1, 7) else 0
            val weeklyCompletedDays = if (isWeeklyQuota) {
                currentWeekDates.count { dateKey -> (trackerDayCounts[dateKey] ?: 0) >= targetCount }
            } else {
                0
            }

            val scheduledDays = getScheduledDaysOfWeek(tracker)
            val isRestDay = !isWeeklyQuota && tracker.type != "bad" && (effectiveToday.dayOfWeek !in scheduledDays)

            val status = when {
                tracker.type == "bad" -> {
                    if (todayCount > 0) TodayHabitStatus.BAD_SLIP else TodayHabitStatus.BAD_CLEAN
                }
                isWeeklyQuota -> {
                    when {
                        todayCount >= targetCount -> TodayHabitStatus.COMPLETED
                        weeklyCompletedDays >= weeklyTarget -> TodayHabitStatus.QUOTA_MET
                        isTodayFrozen && tracker.type == "good" -> TodayHabitStatus.FROZEN
                        todayCount > 0 -> TodayHabitStatus.IN_PROGRESS
                        else -> TodayHabitStatus.NEEDS_ATTENTION
                    }
                }
                isRestDay -> {
                    if (todayCount >= targetCount) TodayHabitStatus.COMPLETED else TodayHabitStatus.REST_DAY
                }
                todayCount >= targetCount -> TodayHabitStatus.COMPLETED
                isTodayFrozen && tracker.type == "good" -> TodayHabitStatus.FROZEN
                todayCount > 0 -> TodayHabitStatus.IN_PROGRESS
                else -> TodayHabitStatus.NEEDS_ATTENTION
            }

            // Overall due-habit progress:
            // Only actionable due good/misc habits contribute to the daily due denominator.
            // Rest days, already-met weekly quotas, frozen days, and avoidance habits are tracked cleanly outside due failures.
            val countsAsDueToday = when {
                tracker.type == "bad" -> false
                isRestDay -> false
                status == TodayHabitStatus.QUOTA_MET -> false
                status == TodayHabitStatus.FROZEN -> false
                else -> true
            }

            if (countsAsDueToday) {
                dueTotalCount++
                fractionalSum += progressFraction
                if (status == TodayHabitStatus.COMPLETED) {
                    dueCompletedCount++
                }
            }

            // Reminder calculation reusing ReminderManager
            var reminderSummary: String? = null
            var isReminderUpcomingToday = false
            val needsReminderAction = status == TodayHabitStatus.NEEDS_ATTENTION ||
                status == TodayHabitStatus.IN_PROGRESS ||
                status == TodayHabitStatus.BAD_CLEAN

            if (tracker.reminderEnabled) {
                val nextTrigger = ReminderManager.calculateNextTriggerDateTime(
                    tracker = tracker.toEntity(),
                    now = now
                )
                if (nextTrigger != null) {
                    if (needsReminderAction && nextTrigger.toLocalDate() == effectiveToday) {
                        isReminderUpcomingToday = true
                        reminderSummary = "🔔 ${tracker.reminderTime}"
                        val currentEarliest = earliestUpcomingTrigger
                        if (currentEarliest == null || nextTrigger.isBefore(currentEarliest.first)) {
                            earliestUpcomingTrigger = nextTrigger to tracker
                        }
                    } else {
                        val dayShort = nextTrigger.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
                        reminderSummary = "🔔 $dayShort ${tracker.reminderTime}"
                    }
                }
            } else if (needsReminderAction) {
                hasActionableHabitUsingGlobal = true
            }

            val statusLabel = when (status) {
                TodayHabitStatus.NEEDS_ATTENTION -> when {
                    isWeeklyQuota -> "$weeklyCompletedDays of $weeklyTarget days this week"
                    tracker.type == "misc" -> "Ready to log today"
                    targetCount > 1 -> "0 of $targetCount logged"
                    else -> "Due today"
                }
                TodayHabitStatus.IN_PROGRESS -> if (isWeeklyQuota) {
                    "$todayCount/$targetCount today · $weeklyCompletedDays/$weeklyTarget this wk"
                } else {
                    "$todayCount of $targetCount logged"
                }
                TodayHabitStatus.COMPLETED -> when {
                    isWeeklyQuota -> "Done today · $weeklyCompletedDays/$weeklyTarget this week"
                    isRestDay -> "Bonus logged on rest day"
                    targetCount > 1 -> "Completed ($todayCount/$targetCount)"
                    else -> "Completed today"
                }
                TodayHabitStatus.REST_DAY -> if (todayCount > 0) {
                    "Rest day · $todayCount/$targetCount bonus"
                } else {
                    "Rest day · Not scheduled today"
                }
                TodayHabitStatus.QUOTA_MET -> "Weekly goal met ($weeklyCompletedDays/$weeklyTarget)"
                TodayHabitStatus.FROZEN -> "Frozen today · Streak protected"
                TodayHabitStatus.BAD_CLEAN -> "Clean today — stay disciplined"
                TodayHabitStatus.BAD_SLIP -> "Slip recorded today · Keep moving forward"
            }

            val item = TodayHabitItem(
                tracker = tracker,
                status = status,
                todayCount = todayCount,
                targetCount = targetCount,
                progressFraction = progressFraction,
                weeklyCompletedDays = weeklyCompletedDays,
                weeklyTarget = weeklyTarget,
                isFrozenToday = isTodayFrozen && tracker.type == "good",
                isRestDay = isRestDay,
                isWeeklyQuota = isWeeklyQuota,
                reminderSummary = reminderSummary,
                isReminderUpcomingToday = isReminderUpcomingToday,
                statusLabel = statusLabel
            )

            allItems.add(item)
            when (status) {
                TodayHabitStatus.IN_PROGRESS,
                TodayHabitStatus.NEEDS_ATTENTION -> attentionItems.add(item)
                TodayHabitStatus.BAD_CLEAN,
                TodayHabitStatus.BAD_SLIP -> avoidanceItems.add(item)
                TodayHabitStatus.COMPLETED -> completedItems.add(item)
                TodayHabitStatus.REST_DAY,
                TodayHabitStatus.QUOTA_MET,
                TodayHabitStatus.FROZEN -> restAndProtectedItems.add(item)
            }
        }

        // Order attention items: partial in-progress habits first (closest to target), then remaining due items
        val sortedAttention = attentionItems.sortedWith(
            compareByDescending<TodayHabitItem> { it.status == TodayHabitStatus.IN_PROGRESS }
                .thenByDescending { it.progressFraction }
        )

        val overallProgress = when {
            dueTotalCount > 0 -> (fractionalSum / dueTotalCount.toFloat()).coerceIn(0f, 1f)
            activeTrackers.isNotEmpty() -> 1f
            else -> 0f
        }

        val nextReminderText = when {
            earliestUpcomingTrigger != null -> {
                val (triggerDt, tr) = earliestUpcomingTrigger
                val timeFmt = String.format("%02d:%02d", triggerDt.hour, triggerDt.minute)
                "Next: ${tr.icon} ${tr.title} at $timeFmt"
            }
            globalRemindersEnabled && hasActionableHabitUsingGlobal -> {
                val parsed = ReminderManager.parseTime(globalReminderTime)
                val isStillUpcoming = parsed != null &&
                    effectiveToday.atTime(parsed.first, parsed.second).isAfter(now)
                if (isStillUpcoming) "Global reminder at $globalReminderTime" else null
            }
            else -> null
        }

        return TodayDashboardState(
            effectiveToday = effectiveToday,
            attentionItems = sortedAttention,
            avoidanceItems = avoidanceItems,
            completedItems = completedItems,
            restAndProtectedItems = restAndProtectedItems,
            dueCompletedCount = dueCompletedCount,
            dueTotalCount = dueTotalCount,
            overallProgress = overallProgress,
            nextReminderText = nextReminderText,
            isTodayFrozen = isTodayFrozen,
            totalActiveCount = activeTrackers.size,
            allItems = allItems
        )
    }

    private fun TrackerConfig.toEntity(): Tracker = Tracker(
        id = id,
        title = title,
        icon = icon,
        accentColor = accentColor,
        type = type,
        frequencyType = frequencyType,
        targetDays = targetDays,
        targetCount = targetCount,
        timeOfDay = timeOfDay,
        weeklyTarget = weeklyTarget,
        isArchived = isArchived,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime
    )
}
