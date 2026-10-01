package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_entries ORDER BY timestamp DESC")
    fun getAllEntriesFlow(): Flow<List<HabitEntry>>

    @Query("SELECT * FROM habit_entries WHERE dateString = :dateString")
    suspend fun getEntriesForDate(dateString: String): List<HabitEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HabitEntry)

    @Update
    suspend fun updateEntry(entry: HabitEntry)

    @Delete
    suspend fun deleteEntry(entry: HabitEntry)

    @Query("DELETE FROM habit_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)

    @Query("DELETE FROM habit_entries WHERE dateString = :dateString")
    suspend fun deleteEntriesForDate(dateString: String)

    // Key-value preference storage
    @Query("SELECT * FROM preference_entries")
    fun getAllPreferencesFlow(): Flow<List<PreferenceEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(pref: PreferenceEntry)

    @Query("SELECT value FROM preference_entries WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceValue(key: String): String?
}
