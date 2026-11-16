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
    fun getEntriesFromDate(startDateString: String): Flow<List<HabitEntry>> = habitDao.getEntriesFromDateFlow(startDateString)

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
    suspend fun setTrackerArchived(id: String, isArchived: Boolean) = trackerDao.setArchived(id, isArchived)
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

                val knownIds = trackers.map { it.id }.toMutableSet()
                var nextOrder = (trackers.maxOfOrNull { it.sortOrder } ?: -1) + 1
                val extraTrackers = mutableListOf<Tracker>()
                for (entry in entries) {
                    if (entry.trackerId !in knownIds) {
                        knownIds.add(entry.trackerId)
                        extraTrackers.add(
                            Tracker(
                                id = entry.trackerId,
                                title = entry.trackerId.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                icon = "⭐",
                                sortOrder = nextOrder++
                            )
                        )
                    }
                }
                trackerDao.insertTrackers(trackers + extraTrackers)
            } else {
                habitDao.deleteAllHabitEntries()
                val existingIds = trackerDao.getAllTrackersList().map { it.id }.toMutableSet()
                val missingTrackers = mutableListOf<Tracker>()
                var nextOrder = existingIds.size
                for (entry in entries) {
                    if (entry.trackerId !in existingIds) {
                        existingIds.add(entry.trackerId)
                        missingTrackers.add(
                            Tracker(
                                id = entry.trackerId,
                                title = entry.trackerId.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                icon = "⭐",
                                sortOrder = nextOrder++
                            )
                        )
                    }
                }
                if (missingTrackers.isNotEmpty()) {
                    trackerDao.insertTrackers(missingTrackers)
                }
            }

            if (entries.isNotEmpty()) {
                habitDao.insertEntries(entries.map { it.copy(id = 0) })
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
     * Merges imported trackers, entries, and missing preferences non-destructively into existing data.
     * Ensures every referenced trackerId exists before inserting entries and deduplicates by (trackerId, dateString).
     */
    suspend fun mergeDatabaseTransaction(
        trackers: List<Tracker>,
        entries: List<HabitEntry>,
        preferences: Map<String, String>
    ) {
        appDatabase.withTransaction {
            val existingTrackers = trackerDao.getAllTrackersList()
            val knownIds = existingTrackers.map { it.id }.toMutableSet()
            var nextSortOrder = (existingTrackers.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val trackersToInsert = mutableListOf<Tracker>()

            for (t in trackers) {
                if (t.id !in knownIds) {
                    knownIds.add(t.id)
                    trackersToInsert.add(t.copy(sortOrder = nextSortOrder++))
                }
            }

            for (entry in entries) {
                if (entry.trackerId !in knownIds) {
                    knownIds.add(entry.trackerId)
                    trackersToInsert.add(
                        Tracker(
                            id = entry.trackerId,
                            title = entry.trackerId.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            icon = "⭐",
                            sortOrder = nextSortOrder++
                        )
                    )
                }
            }

            if (trackersToInsert.isNotEmpty()) {
                trackerDao.insertTrackers(trackersToInsert)
            }

            for (entry in entries) {
                val existing = habitDao.getEntriesForTrackerAndDate(entry.trackerId, entry.dateString)
                if (existing.isEmpty()) {
                    habitDao.insertEntry(entry.copy(id = 0))
                } else {
                    val first = existing.first()
                    val mergedNotes = when {
                        entry.notes.isNullOrBlank() -> first.notes
                        first.notes.isNullOrBlank() -> entry.notes
                        first.notes == entry.notes -> first.notes
                        else -> entry.notes
                    }
                    habitDao.updateEntry(
                        first.copy(
                            count = maxOf(first.count, entry.count),
                            notes = mergedNotes,
                            timestamp = maxOf(first.timestamp, entry.timestamp)
                        )
                    )
                }
            }

            for ((key, value) in preferences) {
                if (key == "cloud_backup_history") continue
                if (habitDao.getPreferenceValue(key) == null) {
                    habitDao.insertPreference(PreferenceEntry(key, value))
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
