package com.example.ui

import android.os.Build
import com.example.data.HabitEntry
import com.example.data.Tracker
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class BackupImportMode {
    MERGE,
    REPLACE
}

data class BackupPreview(
    val formatLabel: String,
    val version: Int,
    val trackers: List<Tracker>,
    val entries: List<HabitEntry>,
    val preferences: Map<String, String>,
    val isLegacySingleTrackerOnly: Boolean = false
) {
    val habitCount: Int get() = trackers.size
    val entryCount: Int get() = entries.size
    val preferenceCount: Int get() = preferences.size
}

sealed class BackupParseResult {
    data class Valid(val preview: BackupPreview) : BackupParseResult()
    data class Error(val message: String) : BackupParseResult()
}

object BackupJsonCodec {
    const val CURRENT_APP_ID = "EtchaHabitBackup"
    const val CLOUD_BACKUP_APP_ID = "HabitTrackerCloudBackup"
    const val LEGACY_BACKUP_APP_ID = "HabitTrackerBackup"
    const val CURRENT_VERSION = 3

    private val ISO_DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun encodeFullBackupJson(
        trackers: List<Tracker>,
        entries: List<HabitEntry>,
        preferences: Map<String, String>,
        deviceName: String = Build.MODEL ?: "Android Device",
        prettyIndentSpaces: Int = 2
    ): String {
        val root = JSONObject()
        root.put("app", CURRENT_APP_ID)
        root.put("version", CURRENT_VERSION)
        root.put("exported_at", System.currentTimeMillis())
        root.put("deviceName", deviceName)

        val trackersArr = JSONArray()
        trackers.sortedBy { it.sortOrder }.forEachIndexed { idx, t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("icon", t.icon)
            obj.put("accentColor", t.accentColor ?: "")
            obj.put("type", t.type)
            obj.put("frequencyType", t.frequencyType)
            obj.put("targetDays", t.targetDays)
            obj.put("targetCount", t.targetCount)
            obj.put("timeOfDay", t.timeOfDay)
            obj.put("sortOrder", t.sortOrder.takeIf { it >= 0 } ?: idx)
            obj.put("weeklyTarget", t.weeklyTarget)
            obj.put("isArchived", t.isArchived)
            obj.put("reminderEnabled", t.reminderEnabled)
            obj.put("reminderTime", t.reminderTime)
            obj.put("pausedUntilDate", t.pausedUntilDate ?: "")
            obj.put("defaultFocusMinutes", t.defaultFocusMinutes)
            trackersArr.put(obj)
        }
        root.put("trackers", trackersArr)

        val entriesArr = JSONArray()
        entries.forEach { e ->
            val obj = JSONObject()
            obj.put("trackerId", e.trackerId)
            obj.put("date", e.dateString)
            obj.put("count", e.count)
            obj.put("timestamp", e.timestamp)
            obj.put("notes", e.notes ?: "")
            entriesArr.put(obj)
        }
        root.put("entries", entriesArr)

        val prefsObj = JSONObject()
        preferences.toSortedMap().forEach { (k, v) ->
            if (k != "cloud_backup_history") {
                prefsObj.put(k, v)
            }
        }
        root.put("preferences", prefsObj)

        return if (prettyIndentSpaces > 0) root.toString(prettyIndentSpaces) else root.toString()
    }

    fun parseAndValidate(
        jsonString: String,
        fallbackTrackerId: String = "sobriety"
    ): BackupParseResult {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return BackupParseResult.Error("Backup JSON is empty.")
        }

        val root = try {
            JSONObject(trimmed)
        } catch (e: Exception) {
            return BackupParseResult.Error("Invalid JSON syntax: ${e.localizedMessage ?: "Malformed document"}")
        }

        val appId = root.optString("app", "")
        if (appId != CURRENT_APP_ID && appId != CLOUD_BACKUP_APP_ID && appId != LEGACY_BACKUP_APP_ID) {
            return BackupParseResult.Error("Unrecognized backup format (expected Etcha / HabitTracker JSON).")
        }

        val version = root.optInt("version", if (appId == LEGACY_BACKUP_APP_ID) 1 else 2)
        val isLegacyV1 = (appId == LEGACY_BACKUP_APP_ID && !root.has("trackers"))

        // 1. Parse trackers
        val parsedTrackers = linkedMapOf<String, Tracker>()
        val trackersArr = root.optJSONArray("trackers")
        if (trackersArr != null) {
            for (i in 0 until trackersArr.length()) {
                val obj = trackersArr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val title = obj.optString("title", "").trim()
                if (id.isEmpty() || title.isEmpty()) continue
                val icon = obj.optString("icon", "⭐").ifBlank { "⭐" }
                val accent = obj.optString("accentColor", "").trim().takeIf { it.isNotEmpty() }
                val type = obj.optString("type", "good").takeIf { it in setOf("good", "bad", "misc") } ?: "good"
                val freq = obj.optString("frequencyType", "daily").ifBlank { "daily" }
                val targetDays = obj.optString("targetDays", "MON,TUE,WED,THU,FRI,SAT,SUN").ifBlank { "MON,TUE,WED,THU,FRI,SAT,SUN" }
                val targetCount = obj.optInt("targetCount", 1).coerceAtLeast(1)
                val timeOfDay = obj.optString("timeOfDay", "anytime").ifBlank { "anytime" }
                val sortOrder = obj.optInt("sortOrder", i)
                val weeklyTarget = obj.optInt("weeklyTarget", 0).coerceIn(0, 7)
                val isArchived = obj.optBoolean("isArchived", false)
                val reminderEnabled = obj.optBoolean("reminderEnabled", false)
                val reminderTime = obj.optString("reminderTime", "21:00").ifBlank { "21:00" }
                val pausedUntilDate = obj.optString("pausedUntilDate", "").trim().takeIf { it.isNotEmpty() }
                val defaultFocusMinutes = obj.optInt("defaultFocusMinutes", 25).coerceIn(5, 180)

                parsedTrackers[id] = Tracker(
                    id = id,
                    title = title,
                    icon = icon,
                    sortOrder = sortOrder,
                    accentColor = accent,
                    type = type,
                    frequencyType = freq,
                    targetDays = targetDays,
                    targetCount = targetCount,
                    timeOfDay = timeOfDay,
                    weeklyTarget = weeklyTarget,
                    isArchived = isArchived,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    pausedUntilDate = pausedUntilDate,
                    defaultFocusMinutes = defaultFocusMinutes
                )
            }
        }

        // 2. Parse entries
        val entriesArr = root.optJSONArray("entries")
            ?: return BackupParseResult.Error("Backup JSON is missing the 'entries' array.")

        val parsedEntries = mutableListOf<HabitEntry>()
        for (i in 0 until entriesArr.length()) {
            val obj = entriesArr.optJSONObject(i) ?: continue
            val dateStr = obj.optString("date", obj.optString("dateString", "")).trim()
            if (!ISO_DATE_REGEX.matches(dateStr)) continue
            val validDate = try {
                LocalDate.parse(dateStr)
                true
            } catch (_: Exception) {
                false
            }
            if (!validDate) continue

            val count = obj.optInt("count", 1).coerceAtLeast(0)
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            val notesRaw = obj.optString("notes", "")
            val trackerId = obj.optString("trackerId", fallbackTrackerId).trim().ifEmpty { fallbackTrackerId }

            parsedEntries.add(
                HabitEntry(
                    id = 0,
                    dateString = dateStr,
                    count = count,
                    timestamp = timestamp,
                    notes = notesRaw.ifEmpty { null },
                    trackerId = trackerId
                )
            )
        }

        // 3. Parse preferences
        val parsedPrefs = mutableMapOf<String, String>()
        val prefsObj = root.optJSONObject("preferences")
        if (prefsObj != null) {
            val keys = prefsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k != "cloud_backup_history") {
                    val v = prefsObj.opt(k)?.toString() ?: ""
                    parsedPrefs[k] = v
                }
            }
        }

        val formatLabel = when {
            isLegacyV1 -> "Legacy Entry Export (v1)"
            appId == CLOUD_BACKUP_APP_ID -> "Local Snapshot (v$version)"
            else -> "Full Etcha Backup (v$version)"
        }

        return BackupParseResult.Valid(
            BackupPreview(
                formatLabel = formatLabel,
                version = version,
                trackers = parsedTrackers.values.toList(),
                entries = parsedEntries,
                preferences = parsedPrefs,
                isLegacySingleTrackerOnly = isLegacyV1
            )
        )
    }
}
