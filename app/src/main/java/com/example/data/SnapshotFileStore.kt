package com.example.data

import android.content.Context
import android.util.Log
import com.example.ui.CloudBackupSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * File-backed local device snapshot store under app-private filesDir/snapshots/.
 * Avoids SQLite CursorWindow bloat from storing multi-habit JSON backups inside preference_entries.
 */
class SnapshotFileStore(private val context: Context) {

    companion object {
        private const val TAG = "SnapshotFileStore"
        private const val DIR_NAME = "snapshots"
        private const val INDEX_FILE = "index.json"
        private const val MAX_SNAPSHOTS = 10
    }

    private val lock = Any()

    private fun rootDir(): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun indexFile(): File = File(rootDir(), INDEX_FILE)

    private fun snapshotDataFile(id: String): File {
        val safeId = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(rootDir(), "$safeId.json")
    }

    fun loadAllSnapshots(): List<CloudBackupSnapshot> = synchronized(lock) {
        val idxFile = indexFile()
        if (!idxFile.exists()) return emptyList()
        return try {
            val raw = idxFile.readText(Charsets.UTF_8)
            val arr = JSONArray(raw)
            val list = mutableListOf<CloudBackupSnapshot>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val dataFile = snapshotDataFile(id)
                val dataJson = if (dataFile.exists()) {
                    dataFile.readText(Charsets.UTF_8)
                } else {
                    obj.optString("dataJson", "{}")
                }
                list.add(
                    CloudBackupSnapshot(
                        id = id,
                        timestamp = obj.optLong("timestamp", 0L),
                        displayDate = obj.optString("displayDate", ""),
                        deviceName = obj.optString("deviceName", "Android Device"),
                        totalTrackers = obj.optInt("totalTrackers", 0),
                        totalEntries = obj.optInt("totalEntries", 0),
                        dataJson = dataJson
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read snapshot index", e)
            emptyList()
        }
    }

    fun saveSnapshot(snapshot: CloudBackupSnapshot): List<CloudBackupSnapshot> = synchronized(lock) {
        val current = loadAllSnapshots().filter { it.id != snapshot.id }.toMutableList()
        current.add(0, snapshot)
        val trimmed = current.take(MAX_SNAPSHOTS)
        val removed = current.drop(MAX_SNAPSHOTS)
        removed.forEach { old ->
            snapshotDataFile(old.id).delete()
        }
        snapshotDataFile(snapshot.id).writeText(snapshot.dataJson, Charsets.UTF_8)
        writeIndex(trimmed)
        trimmed
    }

    fun deleteSnapshot(snapshotId: String): List<CloudBackupSnapshot> = synchronized(lock) {
        val remaining = loadAllSnapshots().filter { it.id != snapshotId }
        snapshotDataFile(snapshotId).delete()
        writeIndex(remaining)
        remaining
    }

    fun migrateLegacyPreferenceJson(legacyRawJson: String?): List<CloudBackupSnapshot> = synchronized(lock) {
        val existing = loadAllSnapshots()
        if (legacyRawJson.isNullOrBlank()) return existing
        return try {
            val arr = JSONArray(legacyRawJson)
            val legacyList = mutableListOf<CloudBackupSnapshot>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val dataJson = obj.optString("dataJson", "{}")
                val snap = CloudBackupSnapshot(
                    id = id,
                    timestamp = obj.optLong("timestamp", 0L),
                    displayDate = obj.optString("displayDate", ""),
                    deviceName = obj.optString("deviceName", "Android Device"),
                    totalTrackers = obj.optInt("totalTrackers", 0),
                    totalEntries = obj.optInt("totalEntries", 0),
                    dataJson = dataJson
                )
                legacyList.add(snap)
            }
            val mergedById = LinkedHashMap<String, CloudBackupSnapshot>()
            (existing + legacyList)
                .sortedByDescending { it.timestamp }
                .take(MAX_SNAPSHOTS)
                .forEach { snap ->
                    mergedById[snap.id] = snap
                    val f = snapshotDataFile(snap.id)
                    if (!f.exists() && snap.dataJson.isNotBlank()) {
                        f.writeText(snap.dataJson, Charsets.UTF_8)
                    }
                }
            val result = mergedById.values.toList()
            writeIndex(result)
            result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to migrate legacy preference snapshots", e)
            existing
        }
    }

    private fun writeIndex(snapshots: List<CloudBackupSnapshot>) {
        val arr = JSONArray()
        for (s in snapshots) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("timestamp", s.timestamp)
                put("displayDate", s.displayDate)
                put("deviceName", s.deviceName)
                put("totalTrackers", s.totalTrackers)
                put("totalEntries", s.totalEntries)
            }
            arr.put(obj)
        }
        indexFile().writeText(arr.toString(), Charsets.UTF_8)
    }
}
