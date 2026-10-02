package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.ACTION_WIDGET_CHECK_IN") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val trackers = db.trackerDao().getAllTrackersList()
                    val activeId = db.habitDao().getPreferenceValue("selected_tracker_id") ?: "sobriety"
                    val tracker = trackers.find { it.id == activeId } ?: trackers.firstOrNull()
                    if (tracker != null) {
                        val todayString = LocalDate.now().toString()
                        val existing = db.habitDao().getEntriesForTrackerAndDate(tracker.id, todayString)
                        if (existing.isEmpty()) {
                            db.habitDao().insertEntry(
                                HabitEntry(
                                    dateString = todayString,
                                    count = 1,
                                    trackerId = tracker.id
                                )
                            )
                        } else {
                            val last = existing.last()
                            db.habitDao().updateEntry(last.copy(count = last.count + 1))
                        }
                    }
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, HabitAppWidgetProvider::class.java)
                    )
                    updateWidgetsInternal(context, appWidgetManager, ids)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (intent.action == "com.example.ACTION_UPDATE_HABIT_WIDGET") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, HabitAppWidgetProvider::class.java)
            )
            updateWidgetsInternal(context, appWidgetManager, ids)
        }
    }

    private fun updateWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val checkInIntent = Intent(context, HabitAppWidgetProvider::class.java).apply {
            action = "com.example.ACTION_WIDGET_CHECK_IN"
        }
        val checkInPendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val trackers = db.trackerDao().getAllTrackersList()
                val activeId = db.habitDao().getPreferenceValue("selected_tracker_id") ?: "sobriety"
                val activeTracker = trackers.find { it.id == activeId } ?: trackers.firstOrNull()

                val todayString = LocalDate.now().toString()
                val entries = if (activeTracker != null) {
                    db.habitDao().getEntriesForTrackerAndDate(activeTracker.id, todayString)
                } else emptyList()

                val totalCount = entries.sumOf { it.count }
                val targetCount = activeTracker?.targetCount ?: 1
                val isDoneToday = totalCount >= targetCount

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_habit_today).apply {
                        if (activeTracker != null) {
                            setTextViewText(R.id.widget_habit_icon, activeTracker.icon)
                            setTextViewText(R.id.widget_habit_title, activeTracker.title)
                        } else {
                            setTextViewText(R.id.widget_habit_icon, "✨")
                            setTextViewText(R.id.widget_habit_title, "Daily Habit")
                        }

                        if (isDoneToday) {
                            val statusText = if (targetCount > 1) "Completed today ($totalCount/$targetCount) ✓" else "Completed today ✓"
                            setTextViewText(R.id.widget_habit_status, statusText)
                            setTextViewText(R.id.widget_btn_action, "✓ Done Today · Open App")
                            setOnClickPendingIntent(R.id.widget_btn_action, openAppPendingIntent)
                        } else {
                            val statusText = if (targetCount > 1) "$totalCount of $targetCount logged today" else "Pending check-in"
                            val btnText = if (targetCount > 1) "⚡ Log +1 (${totalCount}/$targetCount)" else "⚡ Tap to Check In"
                            setTextViewText(R.id.widget_habit_status, statusText)
                            setTextViewText(R.id.widget_btn_action, btnText)
                            setOnClickPendingIntent(R.id.widget_btn_action, checkInPendingIntent)
                        }

                        setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                // In case of error, still wire click intent
                for (widgetId in appWidgetIds) {
                    val fallback = RemoteViews(context.packageName, R.layout.widget_habit_today).apply {
                        setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_action, openAppPendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, fallback)
                }
            }
        }
    }
}
