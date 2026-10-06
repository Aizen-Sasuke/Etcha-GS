package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HabitDao
import com.example.data.HabitEntry
import com.example.data.HabitRepository
import com.example.data.Tracker
import com.example.data.TrackerDao
import com.example.ui.CloudBackupSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseBackupRestoreTest {

    private lateinit var database: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var trackerDao: TrackerDao
    private lateinit var repository: HabitRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
        trackerDao = database.trackerDao()
        repository = HabitRepository(database, habitDao, trackerDao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testCascadeDeletionWhenTrackerIsDeleted() = runBlocking {
        val tracker = Tracker(id = "reading", title = "Reading", icon = "📚")
        repository.insertTracker(tracker)

        val entry1 = HabitEntry(dateString = "2026-10-01", count = 1, trackerId = "reading")
        val entry2 = HabitEntry(dateString = "2026-10-02", count = 1, trackerId = "reading")
        repository.insertEntry(entry1)
        repository.insertEntry(entry2)

        val entriesBefore = repository.getEntriesForTracker("reading").first()
        assertEquals(2, entriesBefore.size)

        // Delete the tracker
        repository.deleteTrackerById("reading")

        // Entries must be cascade-deleted
        val entriesAfter = repository.allEntries.first()
        assertTrue(entriesAfter.isEmpty())
    }

    @Test
    fun testTransactionalRestoreDoesNotDuplicateEntries() = runBlocking {
        val tracker = Tracker(id = "gym", title = "Gym", icon = "🏋️")
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-01", count = 1, trackerId = "gym"),
            HabitEntry(id = 2, dateString = "2026-10-02", count = 2, trackerId = "gym")
        )
        val prefs = mapOf("selected_theme" to "sepia", "reminder_time" to "20:00")

        // First restore
        repository.restoreDatabaseTransaction(listOf(tracker), entries, prefs)
        val afterFirst = repository.allEntries.first()
        assertEquals(2, afterFirst.size)

        // Second restore of the exact same snapshot
        repository.restoreDatabaseTransaction(listOf(tracker), entries, prefs)
        val afterSecond = repository.allEntries.first()
        // Must NOT duplicate to 4 entries!
        assertEquals(2, afterSecond.size)
        assertEquals(1, repository.getAllTrackersList().size)
    }

    @Test
    fun testImportEntriesDeduplicatedMergesWithoutDuplicating() = runBlocking {
        val tracker = Tracker(id = "water", title = "Water", icon = "💧")
        repository.insertTracker(tracker)

        val initialEntry = HabitEntry(id = 0, dateString = "2026-10-01", count = 3, trackerId = "water", notes = "Morning")
        repository.insertEntry(initialEntry)

        // Import an entry for the same date with higher count
        val imported = listOf(
            HabitEntry(id = 0, dateString = "2026-10-01", count = 5, trackerId = "water", notes = "Morning + afternoon"),
            HabitEntry(id = 0, dateString = "2026-10-02", count = 8, trackerId = "water")
        )

        repository.importEntriesDeduplicated(imported)

        val allEntries = repository.getEntriesForTracker("water").first()
        // Must be exactly 2 entries (one for Oct 1 and one for Oct 2), not 3!
        assertEquals(2, allEntries.size)

        val oct1 = allEntries.find { it.dateString == "2026-10-01" }
        assertEquals(5, oct1?.count)
        assertEquals("Morning + afternoon", oct1?.notes)
    }

    @Test
    fun testPreferencesRestorePreservesCloudBackupHistory() = runBlocking {
        repository.setPreference("cloud_backup_history", "[{\"id\":\"snap_1\"}]")
        repository.setPreference("selected_theme", "sepia")

        val newPrefs = mapOf("selected_theme" to "nordic_slate")
        repository.restoreDatabaseTransaction(emptyList(), emptyList(), newPrefs)

        // cloud_backup_history must NOT be wiped!
        val backupHistory = repository.getPreference("cloud_backup_history")
        assertEquals("[{\"id\":\"snap_1\"}]", backupHistory)

        // selected_theme must be updated to new preference
        val theme = repository.getPreference("selected_theme")
        assertEquals("nordic_slate", theme)
    }

    @Test
    fun testUndoQuickCheckInLogicForBrandNewEntry() = runBlocking {
        val tracker = Tracker(id = "water", title = "Water", icon = "💧", targetCount = 8)
        val readingTracker = Tracker(id = "reading", title = "Reading", icon = "📚", targetCount = 1)
        repository.insertTracker(tracker)
        repository.insertTracker(readingTracker)

        // Pre-existing historical entry on another date
        val historic = HabitEntry(id = 1, dateString = "2026-10-01", count = 8, trackerId = "water")
        repository.insertEntry(historic)

        // Another tracker entry
        val otherTrackerEntry = HabitEntry(id = 2, dateString = "2026-10-02", count = 1, trackerId = "reading")
        repository.insertEntry(otherTrackerEntry)

        // User quick logs on 2026-10-02 for "water" (previously 0)
        val newEntry = HabitEntry(id = 3, dateString = "2026-10-02", count = 1, trackerId = "water", notes = "[MOOD:happy]")
        repository.insertEntry(newEntry)

        val entriesBeforeUndo = repository.getEntriesForTrackerAndDate("water", "2026-10-02")
        assertEquals(1, entriesBeforeUndo.size)
        assertEquals(1, entriesBeforeUndo[0].count)

        // Perform undo of brand new entry: delete created entry
        repository.deleteEntryById(newEntry.id)

        // Water on 2026-10-02 is now empty (reverted to 0)
        val entriesAfterUndo = repository.getEntriesForTrackerAndDate("water", "2026-10-02")
        assertTrue(entriesAfterUndo.isEmpty())

        // Historic entry on 2026-10-01 must be completely untouched
        val historicEntries = repository.getEntriesForTrackerAndDate("water", "2026-10-01")
        assertEquals(1, historicEntries.size)
        assertEquals(8, historicEntries[0].count)

        // Other tracker entry must be completely untouched
        val otherEntries = repository.getEntriesForTrackerAndDate("reading", "2026-10-02")
        assertEquals(1, otherEntries.size)
        assertEquals(1, otherEntries[0].count)
    }

    @Test
    fun testUndoQuickCheckInLogicForMultiCountIncrement() = runBlocking {
        val tracker = Tracker(id = "water", title = "Water", icon = "💧", targetCount = 8)
        repository.insertTracker(tracker)

        // Existing entry at 3 cups
        val existingEntry = HabitEntry(id = 10, dateString = "2026-10-05", count = 3, trackerId = "water", notes = "Morning")
        repository.insertEntry(existingEntry)

        // User logs +1 (now 4 cups)
        val incremented = existingEntry.copy(count = 4, notes = "Morning")
        repository.insertEntry(incremented)

        val entriesBeforeUndo = repository.getEntriesForTrackerAndDate("water", "2026-10-05")
        assertEquals(1, entriesBeforeUndo.size)
        assertEquals(4, entriesBeforeUndo[0].count)

        // User taps Undo: restores previousCount = 3 and previousNotes
        val reverted = incremented.copy(count = 3, notes = "Morning")
        repository.insertEntry(reverted)

        val entriesAfterUndo = repository.getEntriesForTrackerAndDate("water", "2026-10-05")
        assertEquals(1, entriesAfterUndo.size)
        assertEquals(3, entriesAfterUndo[0].count)
        assertEquals("Morning", entriesAfterUndo[0].notes)
    }

    @Test
    fun testLegacyMigrationAppliesCleanlyToSupportSQLiteDatabase() {
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            .create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                .name(null) // in-memory
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Version 1 schema: legacy habit_entries without trackerId
                        db.execSQL("CREATE TABLE `habit_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `count` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `notes` TEXT)")
                        db.execSQL("INSERT INTO `habit_entries` (`id`, `dateString`, `count`, `timestamp`, `notes`) VALUES (1, '2026-10-01', 1, 1000, 'Legacy')")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build())

        val db = helper.writableDatabase

        // Apply MIGRATION_1_2
        val m12 = AppDatabase::class.java.getDeclaredField("MIGRATION_1_2").apply { isAccessible = true }.get(null) as androidx.room.migration.Migration
        m12.migrate(db)

        // Apply MIGRATION_2_3
        val m23 = AppDatabase::class.java.getDeclaredField("MIGRATION_2_3").apply { isAccessible = true }.get(null) as androidx.room.migration.Migration
        m23.migrate(db)

        // Apply MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
        val m34 = AppDatabase::class.java.getDeclaredField("MIGRATION_3_4").apply { isAccessible = true }.get(null) as androidx.room.migration.Migration
        m34.migrate(db)
        val m45 = AppDatabase::class.java.getDeclaredField("MIGRATION_4_5").apply { isAccessible = true }.get(null) as androidx.room.migration.Migration
        m45.migrate(db)
        val m56 = AppDatabase::class.java.getDeclaredField("MIGRATION_5_6").apply { isAccessible = true }.get(null) as androidx.room.migration.Migration
        m56.migrate(db)
        val m67 = AppDatabase::class.java.getDeclaredField("MIGRATION_6_7").apply { isAccessible = true }.get(null) as androidx.room.migration.Migration
        m67.migrate(db)

        // Verify habit_entries migrated with default 'sobriety' trackerId
        val cursor = db.query("SELECT id, trackerId, notes FROM habit_entries WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("sobriety", cursor.getString(1))
        assertEquals("Legacy", cursor.getString(2))
        cursor.close()

        // Verify trackers table has all columns including frequencyType and weeklyTarget
        val trackersCursor = db.query("SELECT id, frequencyType, targetCount, weeklyTarget FROM trackers WHERE id = 'sobriety'")
        assertTrue(trackersCursor.moveToFirst())
        assertEquals("daily", trackersCursor.getString(1))
        assertEquals(1, trackersCursor.getInt(2))
        assertEquals(0, trackersCursor.getInt(3))
        trackersCursor.close()

        db.close()
    }

    @Test
    fun testJsonBackupAndRestorePreservesWeeklyTarget() = runBlocking {
        // Create snapshot payload JSON directly with weeklyTarget = 4
        val jsonPayload = JSONObject().apply {
            put("app", "HabitTrackerCloudBackup")
            put("version", 2)
            put("timestamp", System.currentTimeMillis())
            val arr = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "gym_weekly")
                    put("title", "Gym Quota")
                    put("icon", "🏋️")
                    put("frequencyType", "weekly_quota")
                    put("targetDays", "MON,TUE,WED,THU,FRI,SAT,SUN")
                    put("targetCount", 1)
                    put("timeOfDay", "anytime")
                    put("sortOrder", 0)
                    put("weeklyTarget", 4)
                })
            }
            put("trackers", arr)
            put("entries", JSONArray())
            put("preferences", JSONObject())
        }.toString()

        val root = JSONObject(jsonPayload)
        val trackersArr = root.getJSONArray("trackers")
        val restoredTrackers = mutableListOf<Tracker>()
        for (i in 0 until trackersArr.length()) {
            val obj = trackersArr.getJSONObject(i)
            restoredTrackers.add(
                Tracker(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    icon = obj.getString("icon"),
                    frequencyType = obj.optString("frequencyType", "daily"),
                    targetDays = obj.optString("targetDays", "MON,TUE,WED,THU,FRI,SAT,SUN"),
                    targetCount = obj.optInt("targetCount", 1),
                    timeOfDay = obj.optString("timeOfDay", "anytime"),
                    sortOrder = obj.optInt("sortOrder", i),
                    weeklyTarget = obj.optInt("weeklyTarget", 0)
                )
            )
        }

        repository.restoreDatabaseTransaction(restoredTrackers, emptyList(), emptyMap())

        val restored = repository.getAllTrackersList().find { it.id == "gym_weekly" }
        assertTrue(restored != null)
        assertEquals("weekly_quota", restored?.frequencyType)
        assertEquals(4, restored?.weeklyTarget)
    }

    @Test
    fun testLegacyJsonBackupWithoutWeeklyTargetDefaultsSafelyToZero() = runBlocking {
        // Legacy payload without weeklyTarget field
        val legacyJsonPayload = JSONObject().apply {
            put("app", "HabitTrackerCloudBackup")
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            val arr = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "legacy_gym")
                    put("title", "Gym")
                    put("icon", "🏋️")
                    put("frequencyType", "weekly")
                    put("targetDays", "MON,WED,FRI")
                    put("targetCount", 1)
                    put("timeOfDay", "morning")
                    put("sortOrder", 1)
                    // Note: weeklyTarget is absent!
                })
            }
            put("trackers", arr)
            put("entries", JSONArray())
            put("preferences", JSONObject())
        }.toString()

        val root = JSONObject(legacyJsonPayload)
        val trackersArr = root.getJSONArray("trackers")
        val restoredTrackers = mutableListOf<Tracker>()
        for (i in 0 until trackersArr.length()) {
            val obj = trackersArr.getJSONObject(i)
            restoredTrackers.add(
                Tracker(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    icon = obj.getString("icon"),
                    frequencyType = obj.optString("frequencyType", "daily"),
                    targetDays = obj.optString("targetDays", "MON,TUE,WED,THU,FRI,SAT,SUN"),
                    targetCount = obj.optInt("targetCount", 1),
                    timeOfDay = obj.optString("timeOfDay", "anytime"),
                    sortOrder = obj.optInt("sortOrder", i),
                    weeklyTarget = obj.optInt("weeklyTarget", 0)
                )
            )
        }

        repository.restoreDatabaseTransaction(restoredTrackers, emptyList(), emptyMap())

        val restored = repository.getAllTrackersList().find { it.id == "legacy_gym" }
        assertTrue(restored != null)
        assertEquals("weekly", restored?.frequencyType)
        assertEquals("MON,WED,FRI", restored?.targetDays)
        assertEquals(0, restored?.weeklyTarget)
    }
}
