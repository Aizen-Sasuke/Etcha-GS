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
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
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
                    db.habitDao().getEntriesForDate(todayString).filter { it.trackerId == activeTracker.id }
                } else emptyList()

                val isDoneToday = entries.sumOf { it.count } > 0

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
                            setTextViewText(R.id.widget_habit_status, "Completed today ✓")
                            setTextViewText(R.id.widget_btn_action, "✓ Done Today · View App")
                        } else {
                            setTextViewText(R.id.widget_habit_status, "Pending check-in")
                            setTextViewText(R.id.widget_btn_action, "⚡ Tap to Check In")
                        }

                        setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_action, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                // In case of error, still wire click intent
                for (widgetId in appWidgetIds) {
                    val fallback = RemoteViews(context.packageName, R.layout.widget_habit_today).apply {
                        setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        setOnClickPendingIntent(R.id.widget_btn_action, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, fallback)
                }
            }
        }
    }
}
