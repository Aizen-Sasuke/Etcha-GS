package com.example.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.PreferenceEntry
import com.example.data.SnapshotFileStore
import com.example.ui.BackupJsonCodec
import com.example.ui.CloudBackupSnapshot
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val allTrackers = db.trackerDao().getAllTrackersList()
            if (allTrackers.isEmpty()) return Result.success()

            val allEntries = db.habitDao().getAllEntriesFlow().first()
            val prefsMap = db.habitDao().getAllPreferencesFlow().first().associate { it.key to it.value }
            val json = BackupJsonCodec.encodeFullBackupJson(
                trackers = allTrackers,
                entries = allEntries,
                preferences = prefsMap,
                prettyIndentSpaces = 0
            )
            val nowMs = System.currentTimeMillis()
            val displayDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a"))
            val snapshot = CloudBackupSnapshot(
                id = "auto_snap_$nowMs",
                timestamp = nowMs,
                displayDate = displayDate,
                deviceName = "${Build.MODEL ?: "Android"} (Auto)",
                totalTrackers = allTrackers.size,
                totalEntries = allEntries.size,
                dataJson = json
            )
            SnapshotFileStore(applicationContext).saveSnapshot(snapshot)
            db.habitDao().insertPreference(PreferenceEntry("last_cloud_backup_time", displayDate))
            Result.success()
        } catch (e: Exception) {
            Log.w("AutoBackupWorker", "Automatic snapshot failed", e)
            Result.retry()
        }
    }
}

object AutoBackupScheduler {
    const val KEY_AUTO_BACKUP_CADENCE = "auto_backup_cadence"
    const val CADENCE_OFF = "off"
    const val CADENCE_DAILY = "daily"
    const val CADENCE_WEEKLY = "weekly"

    private const val UNIQUE_WORK_NAME = "habit_auto_local_backup"

    fun syncSchedule(context: Context, cadence: String) {
        try {
            val workManager = WorkManager.getInstance(context.applicationContext)
            val intervalDays = when (cadence.lowercase()) {
                CADENCE_DAILY -> 1L
                CADENCE_WEEKLY -> 7L
                else -> {
                    workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                    return
                }
            }
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalDays,
                TimeUnit.DAYS
            ).build()
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } catch (e: Exception) {
            Log.w("AutoBackupScheduler", "Unable to sync periodic backup work", e)
        }
    }
}
