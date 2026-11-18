package com.example.worker

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.Tracker
import com.example.ui.VacationPauseHelper
import com.example.ui.VacationPauseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.N)
class HabitQuickSettingsTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val pinnedId = db.habitDao().getPreferenceValue(KEY_QUICK_TILE_TRACKER_ID)
                    ?.takeIf { it.isNotBlank() }
                val selectedId = db.habitDao().getPreferenceValue("selected_tracker_id")
                val activeTrackers = db.trackerDao().getAllTrackersList().filter { !it.isArchived }
                val targetTracker = resolveTileTracker(activeTrackers, pinnedId, selectedId) ?: run {
                    refreshTileState()
                    return@launch
                }

                // Safety rule: never silently log a bad-habit slip from a quick settings tap
                if (targetTracker.type == "bad") {
                    refreshTileState()
                    return@launch
                }

                val rolloverHour = db.habitDao().getPreferenceValue("day_rollover_hour")?.toIntOrNull() ?: 0
                val effectiveTodayStr = ReminderManager.getEffectiveToday(rolloverHour, LocalDateTime.now()).toString()
                val existing = db.habitDao().getEntriesForTrackerAndDate(targetTracker.id, effectiveTodayStr)
                val currentTotal = existing.sumOf { it.count }
                val target = targetTracker.targetCount.coerceAtLeast(1)

                if (targetTracker.type == "good" && currentTotal >= target) {
                    // Already met target today — undo the last tap cleanly if single-step, or keep completed state
                    if (target == 1 && existing.size == 1 && existing.first().count == 1 && existing.first().notes.isNullOrBlank()) {
                        db.habitDao().deleteEntryById(existing.first().id)
                    }
                } else {
                    if (existing.isEmpty()) {
                        db.habitDao().insertEntry(
                            HabitEntry(
                                dateString = effectiveTodayStr,
                                count = 1,
                                trackerId = targetTracker.id
                            )
                        )
                    } else {
                        val last = existing.last()
                        db.habitDao().updateEntry(last.copy(count = last.count + 1))
                    }
                }

                notifyWidgetUpdate(applicationContext)
                updateTileFromDb(db)
            } catch (e: Exception) {
                Log.w("HabitQsTileService", "Failed handling tile click", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun refreshTileState() {
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                updateTileFromDb(db)
            } catch (e: Exception) {
                Log.w("HabitQsTileService", "Failed refreshing tile state", e)
            }
        }
    }

    private suspend fun updateTileFromDb(db: AppDatabase) {
        val tile = qsTile ?: return
        val activeTrackers = db.trackerDao().getAllTrackersList().filter { !it.isArchived }
        if (activeTrackers.isEmpty()) {
            tile.label = getString(R.string.qs_tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.qs_tile_default_subtitle)
            }
            tile.state = Tile.STATE_UNAVAILABLE
            tile.updateTile()
            return
        }

        val pinnedId = db.habitDao().getPreferenceValue(KEY_QUICK_TILE_TRACKER_ID)
            ?.takeIf { it.isNotBlank() }
        val selectedId = db.habitDao().getPreferenceValue("selected_tracker_id")
        val tracker = resolveTileTracker(activeTrackers, pinnedId, selectedId) ?: activeTrackers.first()

        val rolloverHour = db.habitDao().getPreferenceValue("day_rollover_hour")?.toIntOrNull() ?: 0
        val effectiveToday = ReminderManager.getEffectiveToday(rolloverHour, LocalDateTime.now())
        val todayEntries = db.habitDao().getEntriesForTrackerAndDate(tracker.id, effectiveToday.toString())
        val todayCount = todayEntries.sumOf { it.count }
        val target = tracker.targetCount.coerceAtLeast(1)

        val vacationState = VacationPauseState(
            globalEnabled = db.habitDao().getPreferenceValue(VacationPauseHelper.KEY_VACATION_ENABLED) == "true",
            globalStartDate = db.habitDao().getPreferenceValue(VacationPauseHelper.KEY_VACATION_START),
            globalEndDate = db.habitDao().getPreferenceValue(VacationPauseHelper.KEY_VACATION_END)
        )
        val isPaused = VacationPauseHelper.isPausedOnEffectiveDate(effectiveToday, tracker.pausedUntilDate, vacationState)

        val isDone = when {
            tracker.type == "bad" -> todayCount == 0
            else -> todayCount >= target
        }

        tile.label = "${tracker.icon} ${tracker.title}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                isPaused -> "Paused today"
                tracker.type == "bad" -> if (todayCount == 0) "Clean today" else "Slip logged"
                isDone -> getString(R.string.qs_tile_done_subtitle)
                target > 1 -> "$todayCount / $target today"
                tracker.type == "misc" && todayCount > 0 -> "$todayCount logged today"
                else -> getString(R.string.qs_tile_default_subtitle)
            }
        }
        tile.state = when {
            isDone -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    private fun resolveTileTracker(
        activeTrackers: List<Tracker>,
        pinnedId: String?,
        selectedId: String?
    ): Tracker? {
        if (activeTrackers.isEmpty()) return null
        if (!pinnedId.isNullOrBlank()) {
            activeTrackers.find { it.id == pinnedId }?.let { return it }
        }
        if (!selectedId.isNullOrBlank()) {
            activeTrackers.find { it.id == selectedId }?.let { return it }
        }
        return activeTrackers.firstOrNull { it.type != "bad" } ?: activeTrackers.first()
    }

    private fun notifyWidgetUpdate(context: Context) {
        try {
            val intent = Intent("com.example.ACTION_UPDATE_HABIT_WIDGET").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val KEY_QUICK_TILE_TRACKER_ID = "quick_tile_tracker_id"

        fun requestListeningRefresh(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestListeningState(
                        context.applicationContext,
                        ComponentName(context.applicationContext, HabitQuickSettingsTileService::class.java)
                    )
                } catch (_: Exception) {
                }
            }
        }

        fun requestPinTile(context: Context, trackerLabel: String, onResult: (String) -> Unit) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onResult("Open Android Quick Settings and drag '${context.getString(R.string.qs_tile_label)}' into your active tiles.")
                return
            }
            try {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                val componentName = ComponentName(context, HabitQuickSettingsTileService::class.java)
                statusBarManager.requestAddTileService(
                    componentName,
                    trackerLabel.ifBlank { context.getString(R.string.qs_tile_label) },
                    Icon.createWithResource(context, R.drawable.ic_notification_habit),
                    Executors.newSingleThreadExecutor()
                ) { resultCode ->
                    val message = when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            "Quick Settings tile added!"
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            "Quick Settings tile is already active in your shade."
                        else ->
                            "You can add the tile anytime from Quick Settings edit mode."
                    }
                    onResult(message)
                }
            } catch (e: Exception) {
                onResult("Use Android Quick Settings edit mode to add the Habit Check-In tile.")
            }
        }
    }
}
