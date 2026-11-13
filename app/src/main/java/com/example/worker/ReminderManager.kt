package com.example.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.data.AppDatabase
import com.example.data.Tracker
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderManager {
    const val CHANNEL_ID = "habit_reminder_channel"
    const val GLOBAL_NOTIFICATION_ID = 1
    const val HABIT_NOTIFICATION_ID = 1001

    const val ACTION_GLOBAL_REMINDER = "com.example.ACTION_GLOBAL_REMINDER"
    const val ACTION_HABIT_REMINDER = "com.example.ACTION_HABIT_REMINDER"
    const val ACTION_NOTIFICATION_CHECK_IN = "com.example.ACTION_NOTIFICATION_CHECK_IN"
    const val ACTION_NOTIFICATION_LOG_SLIP = "com.example.ACTION_NOTIFICATION_LOG_SLIP"
    const val ACTION_NOTIFICATION_SNOOZE = "com.example.ACTION_NOTIFICATION_SNOOZE"
    const val ACTION_NOTIFICATION_DISMISS = "com.example.ACTION_NOTIFICATION_DISMISS"
    const val ACTION_NOTIFICATION_UNDO = "com.example.ACTION_NOTIFICATION_UNDO"

    const val EXTRA_TRACKER_ID = "extra_tracker_id"
    const val EXTRA_TIME_STR = "timeStr"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val EXTRA_ACTION_TOKEN = "extra_action_token"
    const val EXTRA_DATE_STRING = "extra_date_string"
    const val EXTRA_ENTRY_ID = "extra_entry_id"
    const val EXTRA_PREVIOUS_COUNT = "extra_previous_count"
    const val EXTRA_PREVIOUS_NOTES = "extra_previous_notes"

    private const val GLOBAL_REQUEST_CODE = 0

    fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    fun habitRequestCode(trackerId: String): Int {
        val positive = trackerId.hashCode() and 0x7FFFFFFF
        return 10_000 + (positive % 900_000)
    }

    fun snoozeRequestCode(trackerId: String): Int {
        val positive = trackerId.hashCode() and 0x7FFFFFFF
        return 1_000_000 + (positive % 900_000)
    }

    fun actionRequestCode(trackerId: String, offset: Int): Int {
        val positive = trackerId.hashCode() and 0x7FFFFFFF
        return 2_000_000 + (positive % 900_000) * 10 + offset
    }

    fun habitNotificationTag(trackerId: String): String = "habit_$trackerId"

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    fun getEffectiveToday(dayRolloverHour: Int, now: LocalDateTime = LocalDateTime.now()): LocalDate {
        return if (now.hour < dayRolloverHour) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
    }

    fun getScheduledDaysOfWeek(tracker: Tracker): Set<DayOfWeek> {
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

    fun isDateEligibleForReminder(tracker: Tracker, date: LocalDate): Boolean {
        if (tracker.isArchived || !tracker.reminderEnabled) return false
        return when (tracker.frequencyType) {
            "weekdays", "custom_days", "weekly" -> date.dayOfWeek in getScheduledDaysOfWeek(tracker)
            "weekly_quota", "3x_week", "daily" -> true
            else -> true
        }
    }

    fun calculateNextTriggerDateTime(
        tracker: Tracker,
        now: LocalDateTime = LocalDateTime.now(),
        minTriggerAfter: LocalDateTime = now
    ): LocalDateTime? {
        if (tracker.isArchived || !tracker.reminderEnabled) return null
        val (hour, minute) = parseTime(tracker.reminderTime) ?: return null
        val startDate = now.toLocalDate()
        for (offset in 0..7) {
            val candidateDate = startDate.plusDays(offset.toLong())
            val candidate = candidateDate.atTime(hour, minute)
            if (candidate.isAfter(minTriggerAfter) && isDateEligibleForReminder(tracker, candidateDate)) {
                return candidate
            }
        }
        return null
    }

    suspend fun getWeeklyCompletedDays(
        db: AppDatabase,
        tracker: Tracker,
        effectiveToday: LocalDate
    ): Int {
        val targetCount = tracker.targetCount.coerceAtLeast(1)
        val monday = effectiveToday.with(DayOfWeek.MONDAY)
        var completedDays = 0
        for (d in 0..6) {
            val dateStr = monday.plusDays(d.toLong()).toString()
            val dayEntries = db.habitDao().getEntriesForTrackerAndDate(tracker.id, dateStr)
            if (dayEntries.sumOf { it.count } >= targetCount) {
                completedDays++
            }
        }
        return completedDays
    }

    suspend fun shouldShowNotificationNow(
        tracker: Tracker,
        db: AppDatabase,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        if (tracker.isArchived || !tracker.reminderEnabled) return false
        val dayRolloverHour = db.habitDao().getPreferenceValue("day_rollover_hour")?.toIntOrNull() ?: 0
        val effectiveToday = getEffectiveToday(dayRolloverHour, now)
        if (!isDateEligibleForReminder(tracker, effectiveToday)) return false

        val todayEntries = db.habitDao().getEntriesForTrackerAndDate(tracker.id, effectiveToday.toString())
        val todayTotal = todayEntries.sumOf { it.count }
        val target = tracker.targetCount.coerceAtLeast(1)

        if (tracker.type == "bad") {
            // Avoid nagging if user already logged a slip today
            if (todayTotal > 0) return false
        } else {
            if (todayTotal >= target) return false
        }

        if (tracker.frequencyType == "weekly_quota" || tracker.frequencyType == "3x_week") {
            val quota = tracker.weeklyTarget.coerceIn(1, 7)
            val completedDays = getWeeklyCompletedDays(db, tracker, effectiveToday)
            // Rest days once weekly quota is achieved should not trigger nagging reminders
            if (completedDays >= quota) return false
        }

        return true
    }

    fun buildHabitAlarmPendingIntent(
        context: Context,
        trackerId: String,
        isSnooze: Boolean = false
    ): PendingIntent {
        val uriScheme = if (isSnooze) "etchags://reminder-snooze/" else "etchags://reminder/"
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            data = Uri.parse("$uriScheme${Uri.encode(trackerId)}")
            putExtra(EXTRA_TRACKER_ID, trackerId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        val reqCode = if (isSnooze) snoozeRequestCode(trackerId) else habitRequestCode(trackerId)
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleTrackerReminder(
        context: Context,
        tracker: Tracker,
        now: LocalDateTime = LocalDateTime.now(),
        minTriggerAfter: LocalDateTime = now
    ): Long? {
        if (tracker.isArchived || !tracker.reminderEnabled) {
            cancelTrackerReminder(context, tracker.id)
            return null
        }
        val nextTime = calculateNextTriggerDateTime(tracker, now, minTriggerAfter) ?: return null
        val triggerAtMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = buildHabitAlarmPendingIntent(context, tracker.id, isSnooze = false)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
        return triggerAtMillis
    }

    fun scheduleTrackerSnooze(
        context: Context,
        tracker: Tracker,
        delayMinutes: Long = 60L,
        now: LocalDateTime = LocalDateTime.now()
    ): Long? {
        if (tracker.isArchived || !tracker.reminderEnabled) return null
        val snoozeTime = now.plusMinutes(delayMinutes.coerceAtLeast(1L))
        val triggerAtMillis = snoozeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = buildHabitAlarmPendingIntent(context, tracker.id, isSnooze = true)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)
        return triggerAtMillis
    }

    fun cancelTrackerReminder(context: Context, trackerId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val regularPi = buildHabitAlarmPendingIntent(context, trackerId, isSnooze = false)
        alarmManager.cancel(regularPi)
        regularPi.cancel()

        val snoozePi = buildHabitAlarmPendingIntent(context, trackerId, isSnooze = true)
        alarmManager.cancel(snoozePi)
        snoozePi.cancel()

        try {
            NotificationManagerCompat.from(context)
                .cancel(habitNotificationTag(trackerId), HABIT_NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    fun syncTrackerReminder(
        context: Context,
        tracker: Tracker,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        if (tracker.isArchived || !tracker.reminderEnabled) {
            cancelTrackerReminder(context, tracker.id)
        } else {
            scheduleTrackerReminder(context, tracker, now)
        }
    }

    suspend fun rescheduleAllFromDatabase(
        context: Context,
        db: AppDatabase,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val isGlobalEnabled = db.habitDao().getPreferenceValue("reminders_enabled") == "true"
        if (isGlobalEnabled) {
            val timeStr = db.habitDao().getPreferenceValue("reminder_time") ?: "21:00"
            scheduleReminder(context, timeStr, now)
        } else {
            cancelReminder(context)
        }

        val trackers = db.trackerDao().getAllTrackersList()
        for (tracker in trackers) {
            syncTrackerReminder(context, tracker, now)
        }
    }

    // Legacy / Global Daily Reminder (preserves existing Settings -> Daily Reminder Notifications compatibility)
    fun scheduleReminder(
        context: Context,
        timeStr: String,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        try {
            val (hour, minute) = parseTime(timeStr) ?: return
            var nextTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!nextTime.isAfter(now)) {
                nextTime = nextTime.plusDays(1)
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_GLOBAL_REMINDER
                data = Uri.parse("etchags://reminder-global")
                putExtra(EXTRA_TIME_STR, timeStr)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                GLOBAL_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val timeMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            scheduleExactOrFallback(alarmManager, timeMillis, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_GLOBAL_REMINDER
            data = Uri.parse("etchags://reminder-global")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GLOBAL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        // Also cancel legacy URI-less pending intent if any existed from older app versions
        val legacyIntent = Intent(context, ReminderReceiver::class.java)
        val legacyPi = PendingIntent.getBroadcast(
            context,
            GLOBAL_REQUEST_CODE,
            legacyIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (legacyPi != null) {
            alarmManager.cancel(legacyPi)
            legacyPi.cancel()
        }
    }

    private fun scheduleExactOrFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } catch (_: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
