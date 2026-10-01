package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_entries",
    foreignKeys = [
        ForeignKey(
            entity = Tracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerId"])]
)
data class HabitEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // Format: "YYYY-MM-DD"
    val count: Int = 1,     // Support multiple completions per day (cumulative or separate entries)
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val trackerId: String
)
