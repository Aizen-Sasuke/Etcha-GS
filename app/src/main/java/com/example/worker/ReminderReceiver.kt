package com.example.worker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.Tracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_PREFS_NAME = "habit_notification_actions"
        private const val KEY_PROCESSED_TOKENS = "processed_action_tokens"
        private const val MAX_TOKENS_RETAINED = 30
        private val tokenLock = Any()

        internal fun consumeActionTokenIfUnused(context: Context, token: String?): Boolean {
            if (token.isNullOrBlank()) return false
            synchronized(tokenLock) {
                val prefs = context.getSharedPreferences(ACTION_PREFS_NAME, Context.MODE_PRIVATE)
                val existing = prefs.getString(KEY_PROCESSED_TOKENS, "") ?: ""
                val list = existing.split("|").filter { it.isNotBlank() }.toMutableList()
                if (token in list) {
                    return false
                }
                list.add(token)
                val trimmed = if (list.size > MAX_TOKENS_RETAINED) {
                    list.takeLast(MAX_TOKENS_RETAINED)
                } else {
                    list
                }
                prefs.edit { putString(KEY_PROCESSED_TOKENS, trimmed.joinToString("|")) }
                return true
            }
        }

        internal data class ActionOutcome(
            val executed: Boolean,
            val tracker: Tracker? = null,
            val dateString: String? = null,
            val previousCount: Int = 0,
            val newCount: Int = 0,
            val entryId: Int? = null,
            val previousNotes: String? = null,
            val alreadyCompleted: Boolean = false
        )

        internal suspend fun handleCheckInAction(
            context: Context,
            db: AppDatabase,
            trackerId: String,
            actionToken: String?,
            isSlipAction: Boolean,
            now: LocalDateTime = LocalDateTime.now()
        ): ActionOutcome {
            if (!consumeActionTokenIfUnused(context, actionToken)) {
                return ActionOutcome(executed = false)
            }

            val tracker = db.trackerDao().getAllTrackersList().find { it.id == trackerId }
            if (tracker == null || tracker.isArchived) {
                return ActionOutcome(executed = false, tracker = null)
            }

            // Guard mismatched positive action on bad habits
            if (tracker.type == "bad" && !isSlipAction) {
                return ActionOutcome(executed = false, tracker = tracker)
            }

            val rolloverHour = db.habitDao().getPreferenceValue("day_rollover_hour")?.toIntOrNull() ?: 0
            val effectiveDate = ReminderManager.getEffectiveToday(rolloverHour, now).toString()
            val existing = db.habitDao().getEntriesForTrackerAndDate(tracker.id, effectiveDate)
            val currentTotal = existing.sumOf { it.count }
            val target = tracker.targetCount.coerceAtLeast(1)

            // Prevent duplicate check-ins when a good habit has already reached today's target
            if (tracker.type == "good" && currentTotal >= target) {
                return ActionOutcome(
                    executed = false,
                    tracker = tracker,
                    dateString = effectiveDate,
                    previousCount = currentTotal,
                    newCount = currentTotal,
                    alreadyCompleted = true
                )
            }

            // Prevent duplicate slip log on bad habit if already logged via single notification tap
            if (tracker.type == "bad" && isSlipAction && currentTotal > 0) {
                return ActionOutcome(
                    executed = false,
                    tracker = tracker,
                    dateString = effectiveDate,
                    previousCount = currentTotal,
                    newCount = currentTotal,
                    alreadyCompleted = true
                )
            }

            val previousNotes: String?
            val previousEntryCount: Int
            val newTotal: Int
            val entryId: Int?

            if (existing.isEmpty()) {
                previousNotes = null
                previousEntryCount = 0
                val newEntry = HabitEntry(
                    dateString = effectiveDate,
                    count = 1,
                    trackerId = tracker.id
                )
                db.habitDao().insertEntry(newEntry)
                val inserted = db.habitDao().getEntriesForTrackerAndDate(tracker.id, effectiveDate).lastOrNull()
                entryId = inserted?.id
                newTotal = 1
            } else {
                val last = existing.last()
                previousNotes = last.notes
                previousEntryCount = last.count
                val updated = last.copy(count = last.count + 1)
                db.habitDao().updateEntry(updated)
                entryId = last.id
                newTotal = currentTotal + 1
            }

            notifyWidgetUpdate(context)

            return ActionOutcome(
                executed = true,
                tracker = tracker,
                dateString = effectiveDate,
                previousCount = previousEntryCount,
                newCount = newTotal,
                entryId = entryId,
                previousNotes = previousNotes
            )
        }

        internal suspend fun handleUndoAction(
            context: Context,
            db: AppDatabase,
            trackerId: String,
            dateString: String,
            entryId: Int,
            previousCount: Int,
            previousNotes: String?,
            actionToken: String?
        ): Boolean {
            if (!consumeActionTokenIfUnused(context, actionToken)) return false
            val tracker = db.trackerDao().getAllTrackersList().find { it.id == trackerId }
            if (tracker == null || tracker.isArchived) return false

            if (previousCount <= 0) {
                if (entryId > 0) {
                    db.habitDao().deleteEntryById(entryId)
                } else {
                    db.habitDao().deleteEntriesForTrackerAndDate(trackerId, dateString)
                }
            } else {
                val existing = db.habitDao().getEntriesForTrackerAndDate(trackerId, dateString)
                val target = if (entryId > 0) {
                    existing.find { it.id == entryId } ?: existing.lastOrNull()
                } else {
                    existing.lastOrNull()
                }
                if (target != null) {
                    db.habitDao().insertEntry(
                        target.copy(
                            count = previousCount,
                            notes = previousNotes
                        )
                    )
                }
            }

            notifyWidgetUpdate(context)
            return true
        }

        private fun notifyWidgetUpdate(context: Context) {
            try {
                val widgetIntent = Intent("com.example.ACTION_UPDATE_HABIT_WIDGET").apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(widgetIntent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val pendingResult: PendingResult? = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val now = LocalDateTime.now()

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                        ReminderManager.rescheduleAllFromDatabase(context, db, now)
                    }

                    ReminderManager.ACTION_HABIT_REMINDER -> {
                        val trackerId = intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
                        val isSnooze = intent.getBooleanExtra(ReminderManager.EXTRA_IS_SNOOZE, false)
                        if (!trackerId.isNullOrBlank()) {
                            val tracker = db.trackerDao().getAllTrackersList().find { it.id == trackerId }
                            if (tracker == null || tracker.isArchived || !tracker.reminderEnabled) {
                                ReminderManager.cancelTrackerReminder(context, trackerId)
                            } else {
                                if (ReminderManager.shouldShowNotificationNow(tracker, db, now)) {
                                    showPerHabitNotification(context, db, tracker, now)
                                }
                                if (!isSnooze) {
                                    // Advance recurring alarm past current firing window
                                    ReminderManager.scheduleTrackerReminder(
                                        context = context,
                                        tracker = tracker,
                                        now = now,
                                        minTriggerAfter = now.plusMinutes(1)
                                    )
                                }
                            }
                        }
                    }

                    ReminderManager.ACTION_NOTIFICATION_CHECK_IN,
                    ReminderManager.ACTION_NOTIFICATION_LOG_SLIP -> {
                        val trackerId = intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
                        val token = intent.getStringExtra(ReminderManager.EXTRA_ACTION_TOKEN)
                        if (!trackerId.isNullOrBlank()) {
                            val isSlip = action == ReminderManager.ACTION_NOTIFICATION_LOG_SLIP
                            val outcome = handleCheckInAction(context, db, trackerId, token, isSlip, now)
                            val tracker = outcome.tracker
                            if (tracker == null || tracker.isArchived) {
                                cancelHabitNotification(context, trackerId)
                            } else if (outcome.executed) {
                                val target = tracker.targetCount.coerceAtLeast(1)
                                if (tracker.type != "bad" && outcome.newCount < target) {
                                    // Multi-count progress remains incomplete: refresh notification with updated count & fresh token
                                    showPerHabitNotification(context, db, tracker, now)
                                } else {
                                    showActionConfirmationNotification(context, tracker, outcome)
                                }
                            } else if (outcome.alreadyCompleted) {
                                cancelHabitNotification(context, trackerId)
                            }
                        }
                    }

                    ReminderManager.ACTION_NOTIFICATION_SNOOZE -> {
                        val trackerId = intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
                        if (!trackerId.isNullOrBlank()) {
                            cancelHabitNotification(context, trackerId)
                            val tracker = db.trackerDao().getAllTrackersList().find { it.id == trackerId }
                            if (tracker != null && !tracker.isArchived && tracker.reminderEnabled) {
                                ReminderManager.scheduleTrackerSnooze(context, tracker, delayMinutes = 60L, now = now)
                            }
                        }
                    }

                    ReminderManager.ACTION_NOTIFICATION_DISMISS -> {
                        val trackerId = intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
                        if (!trackerId.isNullOrBlank()) {
                            cancelHabitNotification(context, trackerId)
                        }
                    }

                    ReminderManager.ACTION_NOTIFICATION_UNDO -> {
                        val trackerId = intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
                        val dateString = intent.getStringExtra(ReminderManager.EXTRA_DATE_STRING)
                        val entryId = intent.getIntExtra(ReminderManager.EXTRA_ENTRY_ID, -1)
                        val previousCount = intent.getIntExtra(ReminderManager.EXTRA_PREVIOUS_COUNT, 0)
                        val previousNotes = intent.getStringExtra(ReminderManager.EXTRA_PREVIOUS_NOTES)
                        val token = intent.getStringExtra(ReminderManager.EXTRA_ACTION_TOKEN)
                        if (!trackerId.isNullOrBlank() && !dateString.isNullOrBlank()) {
                            handleUndoAction(
                                context = context,
                                db = db,
                                trackerId = trackerId,
                                dateString = dateString,
                                entryId = entryId,
                                previousCount = previousCount,
                                previousNotes = previousNotes,
                                actionToken = token
                            )
                            cancelHabitNotification(context, trackerId)
                        }
                    }

                    else -> {
                        // Legacy / Global Daily Reminder
                        val isGlobalEnabled = db.habitDao().getPreferenceValue("reminders_enabled") == "true"
                        if (isGlobalEnabled) {
                            val activeTrackers = db.trackerDao().getAllTrackersList().filter { !it.isArchived }
                            // Exclude habits that already have their own individual reminder to prevent duplicate alerts
                            val unremindedActive = activeTrackers.filter { !it.reminderEnabled }
                            if (unremindedActive.isNotEmpty()) {
                                val habitNames = unremindedActive.take(2).joinToString(", ") { "${it.icon} ${it.title}" }
                                val msg = if (unremindedActive.size > 2) {
                                    "$habitNames and ${unremindedActive.size - 2} more"
                                } else {
                                    habitNames
                                }
                                showGlobalNotification(context, "Time to check in", "Don't break the chain! $msg")
                            }
                            val timeStr = intent.getStringExtra(ReminderManager.EXTRA_TIME_STR)
                                ?: db.habitDao().getPreferenceValue("reminder_time")
                                ?: "21:00"
                            ReminderManager.scheduleReminder(context, timeStr, now.plusMinutes(1))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ReminderReceiver", "Error handling reminder broadcast: $action", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureNotificationChannel(context: Context): NotificationManager {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderManager.CHANNEL_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Scheduled habit and daily check-in reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }
        return notificationManager
    }

    private fun cancelHabitNotification(context: Context, trackerId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(
                ReminderManager.habitNotificationTag(trackerId),
                ReminderManager.HABIT_NOTIFICATION_ID
            )
        } catch (_: Exception) {
        }
    }

    private suspend fun showPerHabitNotification(
        context: Context,
        db: AppDatabase,
        tracker: Tracker,
        now: LocalDateTime
    ) {
        if (!canPostNotifications(context)) return
        val notificationManager = ensureNotificationChannel(context)

        val rolloverHour = db.habitDao().getPreferenceValue("day_rollover_hour")?.toIntOrNull() ?: 0
        val effectiveToday = ReminderManager.getEffectiveToday(rolloverHour, now)
        val todayCount = db.habitDao().getEntriesForTrackerAndDate(tracker.id, effectiveToday.toString())
            .sumOf { it.count }
        val target = tracker.targetCount.coerceAtLeast(1)

        val title = "${tracker.icon} ${tracker.title}"
        val body = when {
            tracker.type == "bad" -> "Daily check-in · Stay mindful and keep your clean streak going."
            tracker.frequencyType == "weekly_quota" || tracker.frequencyType == "3x_week" -> {
                val quota = tracker.weeklyTarget.coerceIn(1, 7)
                val weeklyDone = ReminderManager.getWeeklyCompletedDays(db, tracker, effectiveToday)
                if (target > 1) {
                    "$weeklyDone of $quota days this week · Today: $todayCount/$target"
                } else {
                    "$weeklyDone of $quota days this week · Ready to build momentum?"
                }
            }
            target > 1 -> "Progress today: $todayCount/$target · Log +1 to stay on track"
            tracker.type == "misc" -> "Ready to log ${tracker.title} today?"
            else -> "Don't break the chain! Tap to check in today."
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderManager.EXTRA_TRACKER_ID, tracker.id)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            ReminderManager.habitRequestCode(tracker.id),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actionToken = UUID.randomUUID().toString()
        val builder = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPi)
            .setAutoCancel(true)

        if (tracker.type == "bad") {
            val stillCleanPi = buildActionPendingIntent(
                context = context,
                trackerId = tracker.id,
                action = ReminderManager.ACTION_NOTIFICATION_DISMISS,
                requestCode = ReminderManager.actionRequestCode(tracker.id, 1),
                token = actionToken
            )
            val logSlipPi = buildActionPendingIntent(
                context = context,
                trackerId = tracker.id,
                action = ReminderManager.ACTION_NOTIFICATION_LOG_SLIP,
                requestCode = ReminderManager.actionRequestCode(tracker.id, 2),
                token = actionToken
            )
            val snoozePi = buildActionPendingIntent(
                context = context,
                trackerId = tracker.id,
                action = ReminderManager.ACTION_NOTIFICATION_SNOOZE,
                requestCode = ReminderManager.actionRequestCode(tracker.id, 3),
                token = actionToken
            )
            builder.addAction(0, "Still Clean", stillCleanPi)
            builder.addAction(0, "Log Slip", logSlipPi)
            builder.addAction(0, "Snooze 1h", snoozePi)
        } else {
            val checkInLabel = if (target > 1 || tracker.type == "misc") "Log +1" else "Check In"
            val checkInPi = buildActionPendingIntent(
                context = context,
                trackerId = tracker.id,
                action = ReminderManager.ACTION_NOTIFICATION_CHECK_IN,
                requestCode = ReminderManager.actionRequestCode(tracker.id, 1),
                token = actionToken
            )
            val snoozePi = buildActionPendingIntent(
                context = context,
                trackerId = tracker.id,
                action = ReminderManager.ACTION_NOTIFICATION_SNOOZE,
                requestCode = ReminderManager.actionRequestCode(tracker.id, 3),
                token = actionToken
            )
            val dismissPi = buildActionPendingIntent(
                context = context,
                trackerId = tracker.id,
                action = ReminderManager.ACTION_NOTIFICATION_DISMISS,
                requestCode = ReminderManager.actionRequestCode(tracker.id, 4),
                token = actionToken
            )
            builder.addAction(0, checkInLabel, checkInPi)
            builder.addAction(0, "Snooze 1h", snoozePi)
            builder.addAction(0, "Dismiss", dismissPi)
        }

        try {
            notificationManager.notify(
                ReminderManager.habitNotificationTag(tracker.id),
                ReminderManager.HABIT_NOTIFICATION_ID,
                builder.build()
            )
        } catch (e: SecurityException) {
            Log.w("ReminderReceiver", "Notification permission missing when posting per-habit reminder", e)
        }
    }

    private fun showActionConfirmationNotification(
        context: Context,
        tracker: Tracker,
        outcome: ActionOutcome
    ) {
        if (!canPostNotifications(context)) return
        val notificationManager = ensureNotificationChannel(context)

        val target = tracker.targetCount.coerceAtLeast(1)
        val title = if (tracker.type == "bad") {
            "${tracker.icon} ${tracker.title} · Slip Logged"
        } else if (target > 1) {
            "${tracker.icon} ${tracker.title} · ${outcome.newCount}/$target"
        } else {
            "${tracker.icon} ${tracker.title} · Checked In ✓"
        }
        val text = if (tracker.type == "bad") {
            "Recorded for ${outcome.dateString}. Tap Undo if logged by mistake."
        } else {
            "Logged for ${outcome.dateString}."
        }

        val undoToken = UUID.randomUUID().toString()
        val undoIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderManager.ACTION_NOTIFICATION_UNDO
            setPackage(context.packageName)
            data = "etchags://notification-undo/${Uri.encode(tracker.id)}".toUri()
            putExtra(ReminderManager.EXTRA_TRACKER_ID, tracker.id)
            putExtra(ReminderManager.EXTRA_DATE_STRING, outcome.dateString ?: "")
            putExtra(ReminderManager.EXTRA_ENTRY_ID, outcome.entryId ?: -1)
            putExtra(ReminderManager.EXTRA_PREVIOUS_COUNT, outcome.previousCount)
            putExtra(ReminderManager.EXTRA_PREVIOUS_NOTES, outcome.previousNotes)
            putExtra(ReminderManager.EXTRA_ACTION_TOKEN, undoToken)
        }
        val undoPi = PendingIntent.getBroadcast(
            context,
            ReminderManager.actionRequestCode(tracker.id, 5),
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderManager.EXTRA_TRACKER_ID, tracker.id)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            ReminderManager.habitRequestCode(tracker.id),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setTimeoutAfter(15_000L)
            .addAction(0, "Undo", undoPi)
            .build()

        try {
            notificationManager.notify(
                ReminderManager.habitNotificationTag(tracker.id),
                ReminderManager.HABIT_NOTIFICATION_ID,
                notification
            )
        } catch (e: SecurityException) {
            Log.w("ReminderReceiver", "Notification permission missing when posting confirmation", e)
        }
    }

    private fun buildActionPendingIntent(
        context: Context,
        trackerId: String,
        action: String,
        requestCode: Int,
        token: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            setPackage(context.packageName)
            data = "etchags://notification-action/${Uri.encode(action)}/${Uri.encode(trackerId)}".toUri()
            putExtra(ReminderManager.EXTRA_TRACKER_ID, trackerId)
            putExtra(ReminderManager.EXTRA_ACTION_TOKEN, token)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showGlobalNotification(context: Context, title: String, message: String) {
        if (!canPostNotifications(context)) return
        val notificationManager = ensureNotificationChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_habit)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(ReminderManager.GLOBAL_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w("ReminderReceiver", "Notification permission missing when posting global reminder", e)
        }
    }
}
