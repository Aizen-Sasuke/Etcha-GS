package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao, private val trackerDao: TrackerDao) {
    val allEntries: Flow<List<HabitEntry>> = habitDao.getAllEntriesFlow()
    val allPreferences: Flow<List<PreferenceEntry>> = habitDao.getAllPreferencesFlow()
    val allTrackers: Flow<List<Tracker>> = trackerDao.getAllTrackersFlow()
    suspend fun getAllTrackersList(): List<Tracker> = trackerDao.getAllTrackersList()

    suspend fun insertEntry(entry: HabitEntry) = habitDao.insertEntry(entry)
    suspend fun updateEntry(entry: HabitEntry) = habitDao.updateEntry(entry)
    suspend fun deleteEntry(entry: HabitEntry) = habitDao.deleteEntry(entry)
    suspend fun deleteEntryById(id: Int) = habitDao.deleteEntryById(id)
    suspend fun deleteEntriesForDate(dateString: String) = habitDao.deleteEntriesForDate(dateString)
    suspend fun getEntriesForDate(dateString: String) = habitDao.getEntriesForDate(dateString)

    suspend fun insertTracker(tracker: Tracker) = trackerDao.insertTracker(tracker)
    suspend fun updateTracker(tracker: Tracker) = trackerDao.updateTracker(tracker)
    suspend fun deleteTracker(tracker: Tracker) = trackerDao.deleteTracker(tracker)
    suspend fun deleteTrackerById(id: String) = trackerDao.deleteTrackerById(id)

    suspend fun getPreference(key: String): String? = habitDao.getPreferenceValue(key)
    suspend fun setPreference(key: String, value: String) {
        habitDao.insertPreference(PreferenceEntry(key, value))
    }
}
