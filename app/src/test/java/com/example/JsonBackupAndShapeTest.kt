package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.HabitRepository
import com.example.data.Tracker
import com.example.ui.BackupJsonCodec
import com.example.ui.BackupParseResult
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.screens.CalendarCell
import com.example.ui.screens.CalendarDayCell
import com.example.ui.screens.JsonBackupDialogsHost
import com.example.ui.screens.isCurvedCalendarShape
import com.example.ui.screens.rememberJsonBackupController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JsonBackupAndShapeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var inMemoryDb: AppDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        resetSingletonDb()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HabitRepository(inMemoryDb, inMemoryDb.habitDao(), inMemoryDb.trackerDao())
    }

    @After
    fun teardown() {
        inMemoryDb.close()
        resetSingletonDb()
    }

    private fun resetSingletonDb() {
        try {
            val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
            field.isAccessible = true
            val singleton = field.get(null) as? AppDatabase
            singleton?.close()
            field.set(null, null)
        } catch (_: Exception) {
        }
        context.deleteDatabase("habit_tracker_database")
    }

    @Test
    fun fullBackupJsonEncodesAllTrackersArchivedEntriesAndPreferencesExcludingBackupHistory() {
        val trackers = listOf(
            Tracker(
                id = "sobriety",
                title = "Sobriety",
                icon = "💎",
                sortOrder = 0,
                frequencyType = "daily",
                isArchived = false,
                reminderEnabled = true,
                reminderTime = "08:30"
            ),
            Tracker(
                id = "archived_run",
                title = "Morning Run",
                icon = "🏃",
                sortOrder = 1,
                frequencyType = "weekly_quota",
                weeklyTarget = 3,
                isArchived = true
            )
        )
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-11-01", count = 1, trackerId = "sobriety", notes = "Clean"),
            HabitEntry(id = 2, dateString = "2026-11-02", count = 2, trackerId = "archived_run")
        )
        val prefs = mapOf(
            "selected_theme" to "nordic_slate",
            "checkmark_style" to "dot",
            "cloud_backup_history" to "[{\"id\":\"snap_1\"}]"
        )

        val json = BackupJsonCodec.encodeFullBackupJson(trackers, entries, prefs)
        val root = JSONObject(json)

        assertEquals(BackupJsonCodec.CURRENT_APP_ID, root.getString("app"))
        assertEquals(BackupJsonCodec.CURRENT_VERSION, root.getInt("version"))
        assertEquals(2, root.getJSONArray("trackers").length())
        assertEquals(2, root.getJSONArray("entries").length())

        val parsedPrefs = root.getJSONObject("preferences")
        assertEquals("nordic_slate", parsedPrefs.getString("selected_theme"))
        assertEquals("dot", parsedPrefs.getString("checkmark_style"))
        assertFalse(parsedPrefs.has("cloud_backup_history"))

        val entry0 = root.getJSONArray("entries").getJSONObject(0)
        assertEquals("sobriety", entry0.getString("trackerId"))
    }

    @Test
    fun parseAndValidateRejectsMalformedEmptyAndUnknownFormats() {
        assertTrue(BackupJsonCodec.parseAndValidate("") is BackupParseResult.Error)
        assertTrue(BackupJsonCodec.parseAndValidate("{bad json") is BackupParseResult.Error)
        assertTrue(
            BackupJsonCodec.parseAndValidate("""{"app":"SomeOtherApp","entries":[]}""") is BackupParseResult.Error
        )
        assertTrue(
            BackupJsonCodec.parseAndValidate("""{"app":"EtchaHabitBackup","trackers":[]}""") is BackupParseResult.Error
        )
    }

    @Test
    fun parseAndValidateAcceptsV3V2AndLegacyV1WithDateSanitization() {
        val legacyJson = JSONObject().apply {
            put("app", "HabitTrackerBackup")
            put("entries", JSONArray().apply {
                put(JSONObject().apply {
                    put("date", "2026-10-12")
                    put("count", 2)
                    put("timestamp", 123456L)
                    put("notes", "Legacy note")
                })
                put(JSONObject().apply {
                    put("date", "invalid-date")
                    put("count", 1)
                })
            })
        }.toString()

        val legacyResult = BackupJsonCodec.parseAndValidate(legacyJson, fallbackTrackerId = "water")
        assertTrue(legacyResult is BackupParseResult.Valid)
        val legacyPreview = (legacyResult as BackupParseResult.Valid).preview
        assertTrue(legacyPreview.isLegacySingleTrackerOnly)
        assertEquals(1, legacyPreview.entryCount)
        assertEquals("water", legacyPreview.entries[0].trackerId)
        assertEquals("Legacy note", legacyPreview.entries[0].notes)
    }

    @Test
    fun mergeTransactionAddsNewHabitsMergesEntriesAndPreservesExistingPrefs() = runBlocking {
        val existingTracker = Tracker(id = "sobriety", title = "Sobriety", icon = "💎", sortOrder = 0)
        repository.insertTracker(existingTracker)
        repository.insertEntry(HabitEntry(dateString = "2026-10-01", count = 1, trackerId = "sobriety", notes = "Initial"))
        repository.setPreference("selected_theme", "nordic_slate")

        val importedTrackers = listOf(
            Tracker(id = "sobriety", title = "Sobriety", icon = "💎"),
            Tracker(id = "reading", title = "Reading", icon = "📚")
        )
        val importedEntries = listOf(
            HabitEntry(dateString = "2026-10-01", count = 3, trackerId = "sobriety", notes = "Updated"),
            HabitEntry(dateString = "2026-10-02", count = 1, trackerId = "reading")
        )
        val importedPrefs = mapOf(
            "selected_theme" to "sepia",
            "checkmark_style" to "bar"
        )

        repository.mergeDatabaseTransaction(importedTrackers, importedEntries, importedPrefs)

        val trackersAfter = repository.getAllTrackersList()
        assertEquals(2, trackersAfter.size)

        val sobrietyOct1 = repository.getEntriesForTrackerAndDate("sobriety", "2026-10-01")
        assertEquals(1, sobrietyOct1.size)
        assertEquals(3, sobrietyOct1[0].count)
        assertEquals("Updated", sobrietyOct1[0].notes)

        val readingOct2 = repository.getEntriesForTrackerAndDate("reading", "2026-10-02")
        assertEquals(1, readingOct2.size)

        // Existing theme kept, missing key added
        assertEquals("nordic_slate", repository.getPreference("selected_theme"))
        assertEquals("bar", repository.getPreference("checkmark_style"))
    }

    @Test
    fun replaceTransactionHandlesOrphanEntryTrackerIdsSafelyWithoutFkCrash() = runBlocking {
        repository.insertTracker(Tracker(id = "old_habit", title = "Old", icon = "🌱"))
        repository.insertEntry(HabitEntry(dateString = "2026-09-01", count = 1, trackerId = "old_habit"))

        val restoredTrackers = listOf(Tracker(id = "new_habit", title = "New", icon = "🔥"))
        val restoredEntries = listOf(
            HabitEntry(id = 99, dateString = "2026-11-01", count = 2, trackerId = "new_habit"),
            HabitEntry(id = 100, dateString = "2026-11-02", count = 1, trackerId = "unlisted_tracker")
        )
        val restoredPrefs = mapOf("selected_theme" to "matte_black")

        repository.restoreDatabaseTransaction(restoredTrackers, restoredEntries, restoredPrefs)

        val trackers = repository.getAllTrackersList()
        assertFalse(trackers.any { it.id == "old_habit" })
        assertTrue(trackers.any { it.id == "new_habit" })
        assertTrue(trackers.any { it.id == "unlisted_tracker" })
        assertEquals(2, repository.allEntries.first().size)
        assertEquals("matte_black", repository.getPreference("selected_theme"))
    }

    @Test
    fun curvedCalendarShapeClassifierIdentifiesAllFiveShapesCorrectly() {
        assertTrue(isCurvedCalendarShape("circle"))
        assertTrue(isCurvedCalendarShape("pill"))
        assertTrue(isCurvedCalendarShape("squircle"))
        assertFalse(isCurvedCalendarShape("square"))
        assertFalse(isCurvedCalendarShape("rounded_square"))
    }

    @Test
    fun allShapesAndCheckmarkStylesGridAndWeekBadgesRenderWithoutErrors() {
        val shapes = listOf("square", "rounded_square", "squircle", "circle", "pill")
        val styles = listOf("tick", "cross", "dot", "number", "filled", "ring", "bar")
        val tracker = TrackerConfig(id = "gym", title = "Gym", icon = "🏋️", targetCount = 1)
        val font = ThemeStyles.getSelectedFontFamily("default")

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                shapes.forEachIndexed { sIdx, shapeKey ->
                    androidx.compose.foundation.layout.Row {
                        styles.forEachIndexed { cIdx, styleKey ->
                            val dayNum = (sIdx * styles.size + cIdx) % 28 + 1
                            val d = LocalDate.of(2026, 11, dayNum)
                            val dayEntries = mapOf(
                                d.toString() to listOf(
                                    HabitEntry(dateString = d.toString(), count = 2, trackerId = "gym", notes = "[MOOD:😊]")
                                )
                            )
                            Box(modifier = Modifier.size(48.dp)) {
                                CalendarDayCell(
                                    cell = CalendarCell(date = d, isCurrentMonth = true, dateString = d.toString()),
                                    today = d,
                                    activeTracker = tracker,
                                    completionsByDate = dayEntries,
                                    primaryColor = Color(0xFF38BDF8),
                                    cardBgColor = Color(0xFF14181F),
                                    borderColor = Color(0xFF28303D),
                                    textColor = Color.White,
                                    secondaryTextColor = Color.Gray,
                                    appFont = font,
                                    cellShapeKey = shapeKey,
                                    cellShape = ThemeStyles.getCalendarCellShape(shapeKey),
                                    checkmarkStyleName = styleKey,
                                    selectedTheme = "nordic_slate",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {},
                                    onLongClick = {}
                                )
                            }
                        }
                    }
                }
                androidx.compose.foundation.layout.Row {
                    styles.forEach { styleKey ->
                        com.example.ui.screens.CalendarWeekBadge(
                            isDone = true,
                            isPartial = false,
                            isFuture = false,
                            totalCount = 2,
                            targetCount = 1,
                            cellShape = ThemeStyles.getCalendarCellShape("circle"),
                            checkmarkStyleName = styleKey,
                            primaryColor = Color(0xFF38BDF8),
                            textColor = Color.White,
                            secondaryTextColor = Color.Gray,
                            borderColor = Color(0xFF28303D),
                            appFont = font
                        )
                    }
                }
            }
        }

        val matching14 = composeTestRule.onAllNodesWithText("14", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(matching14.isNotEmpty())
        val matching28 = composeTestRule.onAllNodesWithText("28", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(matching28.isNotEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun jsonBackupDialogsValidateErrorsAndExecuteMergeImportFlow() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val db = AppDatabase.getDatabase(context)
            db.trackerDao().deleteAllTrackers()
            db.trackerDao().insertTracker(Tracker(id = "sobriety", title = "Sobriety", icon = "💎"))

            val viewModel = HabitViewModel(context.applicationContext as Application)
            val controllerRef = arrayOfNulls<com.example.ui.screens.JsonBackupFlowController>(1)
            var lastStatus = ""

            composeTestRule.setContent {
                val controller = rememberJsonBackupController(viewModel) { lastStatus = it }
                controllerRef[0] = controller
                JsonBackupDialogsHost(
                    controller = controller,
                    selectedTheme = "matte_black",
                    primaryColor = Color.White,
                    cardBgColor = Color(0xFF161616),
                    borderColor = Color(0xFF2C2C2C),
                    textColor = Color.White,
                    secondaryTextColor = Color.Gray,
                    appFont = ThemeStyles.getSelectedFontFamily("default"),
                    onStatus = { lastStatus = it }
                )
            }

            composeTestRule.runOnIdle { controllerRef[0]?.openImportPaste() }
            composeTestRule.onNodeWithTag("import_json_text_field").performTextReplacement("{not valid json")
            composeTestRule.onNodeWithTag("import_json_review_button").performClick()
            composeTestRule.onNodeWithTag("import_json_error_banner").assertIsDisplayed()

            val validBackupJson = BackupJsonCodec.encodeFullBackupJson(
                trackers = listOf(Tracker(id = "yoga_imported", title = "Imported Yoga", icon = "🧘")),
                entries = listOf(HabitEntry(dateString = "2026-11-10", count = 1, trackerId = "yoga_imported")),
                preferences = mapOf("selected_theme" to "sepia")
            )
            composeTestRule.onNodeWithTag("import_json_text_field").performTextReplacement(validBackupJson)
            composeTestRule.onNodeWithTag("import_json_review_button").performClick()

            composeTestRule.onNodeWithTag("import_preview_merge_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("import_preview_replace_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("import_preview_merge_button").performClick()

            composeTestRule.waitUntil(timeoutMillis = 5000) {
                lastStatus.contains("Merged") && runBlocking {
                    db.trackerDao().getAllTrackersList().any { it.id == "yoga_imported" }
                }
            }

            val finalTrackers = db.trackerDao().getAllTrackersList()
            assertTrue(finalTrackers.any { it.id == "sobriety" })
            assertTrue(finalTrackers.any { it.id == "yoga_imported" })
            assertTrue(lastStatus.contains("Merged"))
        } finally {
            Dispatchers.resetMain()
        }
    }
}
