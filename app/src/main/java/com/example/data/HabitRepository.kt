package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class HabitRepository(
    private val appDatabase: AppDatabase,
    private val habitDao: HabitDao, 
    private val trackerDao: TrackerDao
) {
    val allEntries: Flow<List<HabitEntry>> = habitDao.getAllEntriesFlow()
    val allPreferences: Flow<List<PreferenceEntry>> = habitDao.getAllPreferencesFlow()
    val allTrackers: Flow<List<Tracker>> = trackerDao.getAllTrackersFlow()
    suspend fun getAllTrackersList(): List<Tracker> = trackerDao.getAllTrackersList()

    fun getEntriesForTracker(trackerId: String): Flow<List<HabitEntry>> = habitDao.getEntriesForTrackerFlow(trackerId)

    suspend fun insertEntry(entry: HabitEntry) = habitDao.insertEntry(entry)
    suspend fun updateEntry(entry: HabitEntry) = habitDao.updateEntry(entry)
    suspend fun deleteEntry(entry: HabitEntry) = habitDao.deleteEntry(entry)
    suspend fun deleteEntryById(id: Int) = habitDao.deleteEntryById(id)
    suspend fun deleteEntriesForTrackerAndDate(trackerId: String, dateString: String) =
        habitDao.deleteEntriesForTrackerAndDate(trackerId, dateString)
    suspend fun deleteEntriesForDate(dateString: String) = habitDao.deleteEntriesForDate(dateString)
    suspend fun getEntriesForTrackerAndDate(trackerId: String, dateString: String) =
        habitDao.getEntriesForTrackerAndDate(trackerId, dateString)
    suspend fun getEntriesForDate(dateString: String) = habitDao.getEntriesForDate(dateString)

    suspend fun insertTracker(tracker: Tracker) = trackerDao.insertTracker(tracker)
    suspend fun updateTracker(tracker: Tracker) = trackerDao.updateTracker(tracker)
    suspend fun deleteTracker(tracker: Tracker) = trackerDao.deleteTracker(tracker)
    suspend fun deleteTrackerById(id: String) = trackerDao.deleteTrackerById(id)

    suspend fun getPreference(key: String): String? = habitDao.getPreferenceValue(key)
    suspend fun setPreference(key: String, value: String) {
        habitDao.insertPreference(PreferenceEntry(key, value))
    }

    /**
     * Atomically restores database state in a single SQLite transaction without duplicating data.
     */
    suspend fun restoreDatabaseTransaction(
        trackers: List<Tracker>,
        entries: List<HabitEntry>,
        preferences: Map<String, String>
    ) {
        appDatabase.withTransaction {
            if (trackers.isNotEmpty()) {
                // Delete existing entries and trackers to prevent orphan entries and duplicates
                habitDao.deleteAllHabitEntries()
                trackerDao.deleteAllTrackers()
                trackerDao.insertTrackers(trackers)
            } else {
                habitDao.deleteAllHabitEntries()
            }

            if (entries.isNotEmpty()) {
                habitDao.insertEntries(entries)
            }

            if (preferences.isNotEmpty()) {
                habitDao.deleteAllPreferencesExceptBackups()
                val prefEntries = preferences
                    .filter { it.key != "cloud_backup_history" }
                    .map { PreferenceEntry(it.key, it.value) }
                if (prefEntries.isNotEmpty()) {
                    habitDao.insertPreferences(prefEntries)
                }
            }
        }
    }

    /**
     * Imports entries without duplicates: merges entries by (trackerId, dateString).
     */
    suspend fun importEntriesDeduplicated(importedEntries: List<HabitEntry>) {
        appDatabase.withTransaction {
            for (entry in importedEntries) {
                val existing = habitDao.getEntriesForTrackerAndDate(entry.trackerId, entry.dateString)
                if (existing.isEmpty()) {
                    habitDao.insertEntry(entry.copy(id = 0))
                } else {
                    val first = existing.first()
                    habitDao.updateEntry(first.copy(
                        count = maxOf(first.count, entry.count),
                        notes = entry.notes ?: first.notes,
                        timestamp = maxOf(first.timestamp, entry.timestamp)
                    ))
                }
            }
        }
    }
}
