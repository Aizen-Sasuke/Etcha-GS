package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trackers")
data class Tracker(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String,
    val sortOrder: Int = 0,
    val accentColor: String? = null,
    val type: String = "good", // "good", "misc", "bad"
    val frequencyType: String = "daily", // "daily", "weekdays", "custom_days", "times_per_week"
    val targetDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val targetCount: Int = 1,
    val timeOfDay: String = "anytime" // "anytime", "morning", "afternoon", "evening"
)
