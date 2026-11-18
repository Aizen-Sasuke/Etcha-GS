package com.example.ui

import com.example.data.HabitEntry
import com.example.data.Tracker
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pure Kotlin RFC-4180 CSV spreadsheet exporter for single-habit or all-habits data portability.
 */
object CsvExportCodec {
    private val MOOD_REGEX = Regex("\\[MOOD:([^\\]]+)\\]")
    private val ISO_INSTANT_FMT = DateTimeFormatter.ISO_INSTANT

    fun encodeEntriesCsv(
        trackers: List<Tracker>,
        entries: List<HabitEntry>,
        filterTrackerId: String? = null
    ): String {
        val trackerMap = trackers.associateBy { it.id }
        val filtered = if (filterTrackerId.isNullOrBlank()) {
            entries
        } else {
            entries.filter { it.trackerId == filterTrackerId }
        }.sortedWith(compareBy<HabitEntry> { it.dateString }.thenBy { it.timestamp })

        val sb = StringBuilder()
        sb.appendLine("date,tracker_id,habit_name,type,count,target_count,mood,notes,logged_at_utc")

        for (entry in filtered) {
            val tracker = trackerMap[entry.trackerId]
            val habitName = tracker?.title ?: entry.trackerId
            val type = tracker?.type ?: "good"
            val targetCount = tracker?.targetCount ?: 1
            val rawNote = entry.notes.orEmpty()
            val mood = MOOD_REGEX.find(rawNote)?.groupValues?.get(1)?.trim().orEmpty()
            val cleanNote = rawNote.replace(MOOD_REGEX, "").trim()
            val loggedAtIso = if (entry.timestamp > 0L) {
                ISO_INSTANT_FMT.format(Instant.ofEpochMilli(entry.timestamp).atOffset(ZoneOffset.UTC))
            } else {
                ""
            }

            sb.append(csvCell(entry.dateString)).append(',')
            sb.append(csvCell(entry.trackerId)).append(',')
            sb.append(csvCell(habitName)).append(',')
            sb.append(csvCell(type)).append(',')
            sb.append(entry.count).append(',')
            sb.append(targetCount).append(',')
            sb.append(csvCell(mood)).append(',')
            sb.append(csvCell(cleanNote)).append(',')
            sb.appendLine(csvCell(loggedAtIso))
        }
        return sb.toString()
    }

    internal fun csvCell(raw: String): String {
        // Prevent CSV formula injection when opened in Excel / Google Sheets
        val sanitized = if (raw.isNotEmpty() && raw.first() in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            "'$raw"
        } else {
            raw
        }
        val needsQuotes = sanitized.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) {
            "\"" + sanitized.replace("\"", "\"\"") + "\""
        } else {
            sanitized
        }
    }
}
