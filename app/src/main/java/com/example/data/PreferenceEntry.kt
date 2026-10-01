package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preference_entries")
data class PreferenceEntry(
    @PrimaryKey val key: String,
    val value: String
)
