package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_entries ORDER BY timestamp DESC")
    fun getAllEntriesFlow(): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE trackerId = :trackerId ORDER BY timestamp DESC")
    fun getEntriesForTrackerFlow(trackerId: String): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE trackerId = :trackerId AND dateString = :dateString ORDER BY timestamp ASC")
    suspend fun getEntriesForTrackerAndDate(trackerId: String, dateString: String): List<HabitEntry>

    @Query("SELECT * FROM habit_entries WHERE dateString = :dateString")
    suspend fun getEntriesForDate(dateString: String): List<HabitEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HabitEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<HabitEntry>)

    @Update
    suspend fun updateEntry(entry: HabitEntry)

    @Delete
    suspend fun deleteEntry(entry: HabitEntry)

    @Query("DELETE FROM habit_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)

    @Query("DELETE FROM habit_entries WHERE trackerId = :trackerId AND dateString = :dateString")
    suspend fun deleteEntriesForTrackerAndDate(trackerId: String, dateString: String)

    @Query("DELETE FROM habit_entries WHERE trackerId = :trackerId")
    suspend fun deleteEntriesForTracker(trackerId: String)

    @Query("DELETE FROM habit_entries WHERE dateString = :dateString")
    suspend fun deleteEntriesForDate(dateString: String)

    @Query("DELETE FROM habit_entries")
    suspend fun deleteAllHabitEntries()

    // Key-value preference storage
    @Query("SELECT * FROM preference_entries")
    fun getAllPreferencesFlow(): Flow<List<PreferenceEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(pref: PreferenceEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(prefs: List<PreferenceEntry>)

    @Query("SELECT value FROM preference_entries WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceValue(key: String): String?

    @Query("DELETE FROM preference_entries WHERE `key` != 'cloud_backup_history'")
    suspend fun deleteAllPreferencesExceptBackups()
}
