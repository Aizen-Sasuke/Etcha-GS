package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM trackers ORDER BY sortOrder ASC")
    fun getAllTrackersFlow(): Flow<List<Tracker>>

    @Query("SELECT * FROM trackers ORDER BY sortOrder ASC")
    suspend fun getAllTrackersList(): List<Tracker>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: Tracker)

    @Update
    suspend fun updateTracker(tracker: Tracker)

    @Delete
    suspend fun deleteTracker(tracker: Tracker)
    
    @Query("DELETE FROM trackers WHERE id = :id")
    suspend fun deleteTrackerById(id: String)
}
