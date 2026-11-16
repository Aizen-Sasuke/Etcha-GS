package com.example

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.PreferenceEntry
import com.example.data.SnapshotFileStore
import com.example.data.Tracker
import com.example.ui.CloudBackupSnapshot
import com.example.ui.HabitAnalyticsEngine
import com.example.ui.HabitViewModel
import com.example.ui.MonthlyTrendPoint
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.screens.CustomColorPicker
import com.example.ui.screens.DonateModal
import com.example.ui.screens.InteractiveMonthlyTrendChart
import com.example.ui.screens.WeeklyConsistencyChartCard
import com.example.ui.screens.reorderTrackersByKey
import com.example.widget.HabitAppWidgetProvider
import kotlinx.coroutines.runBlocking
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProjectImprovementsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        resetSingletonDb()
        File(context.filesDir, "snapshots").deleteRecursively()
        context.getSharedPreferences("habit_widget_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun teardown() {
        resetSingletonDb()
        File(context.filesDir, "snapshots").deleteRecursively()
        context.getSharedPreferences("habit_widget_prefs", Context.MODE_PRIVATE).edit().clear().commit()
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
    fun migration9To10AddsHabitEntryIndicesWithoutDataLoss() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `trackers` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `icon` TEXT NOT NULL, `accentColor` TEXT NOT NULL, `type` TEXT NOT NULL, `frequencyType` TEXT NOT NULL, `targetDays` TEXT NOT NULL, `targetCount` INTEGER NOT NULL, `timeOfDay` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `weeklyTarget` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderTime` TEXT NOT NULL, PRIMARY KEY(`id`))")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `count` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `notes` TEXT, `trackerId` TEXT NOT NULL)")
                        db.execSQL("INSERT INTO `trackers` VALUES ('read', 'Read', '📚', '#FFFFFF', 'good', 'daily', 'MON,TUE,WED,THU,FRI,SAT,SUN', 1, 'anytime', 0, 0, 0, 0, '21:00')")
                        db.execSQL("INSERT INTO `habit_entries` (`id`, `dateString`, `count`, `timestamp`, `notes`, `trackerId`) VALUES (7, '2026-11-10', 1, 123456, 'chapter 3', 'read')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = helper.writableDatabase
        val m910 = AppDatabase::class.java.getDeclaredField("MIGRATION_9_10")
            .apply { isAccessible = true }
            .get(null) as androidx.room.migration.Migration
        m910.migrate(db)

        val indexNames = mutableSetOf<String>()
        db.query("PRAGMA index_list('habit_entries')").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIdx >= 0) indexNames.add(cursor.getString(nameIdx))
            }
        }
        assertTrue(indexNames.contains("index_habit_entries_dateString"))
        assertTrue(indexNames.contains("index_habit_entries_trackerId_dateString"))

        db.query("SELECT id, dateString, notes, trackerId FROM habit_entries WHERE id = 7").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("2026-11-10", cursor.getString(1))
            assertEquals("chapter 3", cursor.getString(2))
            assertEquals("read", cursor.getString(3))
        }
        db.close()
    }

    @Test
    fun snapshotFileStoreEnforcesCapDeletesAndMigratesLegacyPreferences() {
        val store = SnapshotFileStore(context)

        for (i in 1..12) {
            store.saveSnapshot(
                CloudBackupSnapshot(
                    id = "snap_$i",
                    timestamp = i * 1000L,
                    displayDate = "2026-11-${"%02d".format(i)}",
                    deviceName = "Pixel Test",
                    totalTrackers = 2,
                    totalEntries = i,
                    dataJson = """{"id":$i}"""
                )
            )
        }

        val loaded = store.loadAllSnapshots()
        assertEquals(10, loaded.size)
        assertEquals("snap_12", loaded.first().id)
        assertEquals("""{"id":12}""", loaded.first().dataJson)
        assertFalse(File(context.filesDir, "snapshots/snap_1.json").exists())
        assertTrue(File(context.filesDir, "snapshots/snap_12.json").exists())

        val afterDelete = store.deleteSnapshot("snap_12")
        assertEquals(9, afterDelete.size)
        assertFalse(File(context.filesDir, "snapshots/snap_12.json").exists())

        // Legacy migration adds older JSON array snapshots without overwriting existing newer ones
        val legacyJson = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "legacy_99")
                put("timestamp", 99_000L)
                put("displayDate", "2026-12-01")
                put("deviceName", "Old Device")
                put("totalTrackers", 3)
                put("totalEntries", 44)
                put("dataJson", """{"legacy":true}""")
            })
        }.toString()

        val migrated = store.migrateLegacyPreferenceJson(legacyJson)
        assertEquals(10, migrated.size)
        assertEquals("legacy_99", migrated.first().id)
        assertEquals("""{"legacy":true}""", migrated.first().dataJson)
        assertTrue(File(context.filesDir, "snapshots/legacy_99.json").exists())
    }

    @Test
    fun analyticsEngineAggregatesTrendsMoodsRhythmAndDynamicYearInsights() {
        val today = LocalDate.of(2028, 3, 15)
        val gym = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1
        )

        // Empty check first: dynamic year, never hardcoded 2026
        val emptySnapshot = HabitAnalyticsEngine.build(
            entries = emptyList(),
            tracker = gym,
            today = today,
            zoneId = ZoneOffset.UTC
        )
        assertTrue(emptySnapshot.annualInsights.contains("2028"))
        assertFalse(emptySnapshot.annualInsights.contains("2026"))

        val monMorning = LocalDateTime.of(2028, 3, 13, 8, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        val tueAfternoon = LocalDateTime.of(2028, 3, 14, 14, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val wedEvening = LocalDateTime.of(2028, 3, 15, 19, 15).toInstant(ZoneOffset.UTC).toEpochMilli()
        val lastWeekFri = LocalDateTime.of(2028, 3, 10, 23, 30).toInstant(ZoneOffset.UTC).toEpochMilli()

        val entries = listOf(
            HabitEntry(dateString = "2028-03-10", count = 1, timestamp = lastWeekFri, notes = "[MOOD:🔥] strong session", trackerId = "gym"),
            HabitEntry(dateString = "2028-03-13", count = 1, timestamp = monMorning, notes = "[MOOD:😊] energetic", trackerId = "gym"),
            HabitEntry(dateString = "2028-03-14", count = 1, timestamp = tueAfternoon, notes = "[MOOD:😊] steady", trackerId = "gym"),
            HabitEntry(dateString = "2028-03-15", count = 1, timestamp = wedEvening, notes = null, trackerId = "gym")
        )

        val snapshot = HabitAnalyticsEngine.build(
            entries = entries,
            tracker = gym,
            today = today,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(4, snapshot.loggedDaysCount)
        assertEquals(4, snapshot.totalCount)
        assertEquals(4, snapshot.currentMonthLoggedDays)
        assertEquals(31, snapshot.daysInCurrentMonth)
        assertEquals(12, snapshot.weeklyConsistency.size)
        // Current week has 3 completed days for weeklyTarget=3 -> 100%
        assertEquals(100, snapshot.weeklyConsistency.last().completionPercent)

        assertEquals(2, snapshot.moodBreakdown.size)
        assertEquals("😊", snapshot.moodBreakdown[0].emoji)
        assertEquals(2, snapshot.moodBreakdown[0].count)

        assertEquals(4, snapshot.timeOfDayBreakdown.size)
        assertTrue(snapshot.timeOfDayBreakdown.all { it.count == 1 })
        assertTrue(snapshot.annualInsights.contains("2028"))
    }

    @Test
    fun reorderTrackersByKeyIgnoresHeaderFooterAndFixesIndexOffset() {
        val list = listOf(
            TrackerConfig(id = "t0", title = "Zero", icon = "0"),
            TrackerConfig(id = "t1", title = "One", icon = "1"),
            TrackerConfig(id = "t2", title = "Two", icon = "2")
        )

        // Swapping by stable tracker key shifts exact elements cleanly
        val reordered = reorderTrackersByKey(list, fromKey = "t2", toKey = "t0")
        assertEquals(listOf("t2", "t0", "t1"), reordered.map { it.id })

        // Dropping onto non-habit LazyRow items is a safe no-op
        val unchangedFilter = reorderTrackersByKey(list, fromKey = "t1", toKey = "routine_filter_pill")
        val unchangedAdd = reorderTrackersByKey(list, fromKey = "t1", toKey = "add_tracker_btn")
        assertEquals(list, unchangedFilter)
        assertEquals(list, unchangedAdd)
    }

    @Test
    fun widgetProviderCyclesSafelyProtectsBadHabitsAndHonorsRolloverHour() = runBlocking {
        val manager = AppWidgetManager.getInstance(context)
        val shadowManager = Shadows.shadowOf(manager)
        val widgetId = shadowManager.createWidget(
            HabitAppWidgetProvider::class.java,
            R.layout.widget_habit_today
        )
        // Let the initial AppWidgetProvider.onUpdate coroutine triggered by createWidget finish cleanly
        kotlinx.coroutines.delay(250)

        val db = AppDatabase.getDatabase(context)
        db.trackerDao().getAllTrackersList()
        db.trackerDao().deleteAllTrackers()
        db.habitDao().deleteAllHabitEntries()
        db.trackerDao().insertTracker(Tracker(id = "habit_water", title = "Water", icon = "💧", type = "good", targetCount = 2, sortOrder = 0))
        db.trackerDao().insertTracker(Tracker(id = "no_sugar", title = "No Sugar", icon = "🍬", type = "bad", sortOrder = 1))
        db.trackerDao().insertTracker(Tracker(id = "archived_t", title = "Archived", icon = "📦", isArchived = true, sortOrder = 2))
        db.habitDao().insertPreference(PreferenceEntry("day_rollover_hour", "4"))

        // Start cycling from habit_water -> no_sugar -> wraps back to habit_water (skips archived)
        HabitAppWidgetProvider.setSelectedWidgetTrackerId(context, "habit_water")
            val next = HabitAppWidgetProvider.cycleSelectedTracker(context, db, step = 1)
            assertEquals("no_sugar", next?.id)

            // Safety rule: widget quick check-in NEVER logs a slip on a bad habit
            val badCheckInAllowed = HabitAppWidgetProvider.handleWidgetCheckIn(
                context,
                db,
                now = LocalDateTime.of(2026, 11, 14, 2, 0)
            )
            assertFalse(badCheckInAllowed)
            assertTrue(db.habitDao().getEntriesForTrackerAndDate("no_sugar", "2026-11-13").isEmpty())

            // Wrap backward back to "habit_water"
            val prev = HabitAppWidgetProvider.cycleSelectedTracker(context, db, step = -1)
            assertEquals("habit_water", prev?.id)

            // 02:00 AM with rolloverHour=4 maps to previous calendar day 2026-11-13
            val firstTap = HabitAppWidgetProvider.handleWidgetCheckIn(
                context,
                db,
                now = LocalDateTime.of(2026, 11, 14, 2, 0)
            )
            assertTrue(firstTap)
            val secondTap = HabitAppWidgetProvider.handleWidgetCheckIn(
                context,
                db,
                now = LocalDateTime.of(2026, 11, 14, 2, 5)
            )
            assertTrue(secondTap)
            // Target 2 already reached -> third duplicate tap must not over-log
            val thirdTap = HabitAppWidgetProvider.handleWidgetCheckIn(
                context,
                db,
                now = LocalDateTime.of(2026, 11, 14, 2, 10)
            )
            assertFalse(thirdTap)
            val entries = db.habitDao().getEntriesForTrackerAndDate("habit_water", "2026-11-13")
            assertEquals(2, entries.sumOf { it.count })

        // performWidgetUpdate completes safely and renders RemoteViews state
        HabitAppWidgetProvider.performWidgetUpdate(
            context = context,
            appWidgetManager = manager,
            appWidgetIds = intArrayOf(widgetId),
            db = db,
            now = LocalDateTime.of(2026, 11, 14, 2, 10)
        )
        val renderedView = shadowManager.getViewFor(widgetId)
        val titleView = renderedView.findViewById<android.widget.TextView>(R.id.widget_habit_title)
        val headerView = renderedView.findViewById<android.widget.TextView>(R.id.widget_app_label)
        assertEquals("Water", titleView.text.toString())
        assertEquals("TODAY · ALL 1 DONE ✓", headerView.text.toString())
    }

    @Test
    fun composeCustomColorPickerDoesNotMutateOnNullInitialHex() {
        var callbackCount = 0
        composeTestRule.setContent {
            CustomColorPicker(
                appFont = FontFamily.SansSerif,
                initialHex = null,
                onColorChanged = { callbackCount++ }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hex Color").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brightness").assertIsDisplayed()
        assertEquals(0, callbackCount)
    }

    @Test
    fun composeDonateModalDisplaysHonestOfflineVaultCopy() {
        val singletonDb = AppDatabase.getDatabase(context)
        runBlocking {
            singletonDb.habitDao().insertPreference(PreferenceEntry("is_supporter", "false"))
        }
        val app = context.applicationContext as Application
        val viewModel = HabitViewModel(app)

        composeTestRule.setContent {
            DonateModal(viewModel = viewModel, onDismiss = {})
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("donate_modal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supporter Edition & Vault").assertIsDisplayed()
        composeTestRule.onNodeWithText("INCLUDED IN SUPPORTER EDITION (NO CHARGE · 100% OFFLINE):").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("✨ Activate Supporter Edition & Vault"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("✨ Activate Supporter Edition & Vault").assertIsDisplayed()
    }

    @Test
    fun composeInteractiveReportChartsRenderAndRespondToSelection() {
        val points = listOf(
            MonthlyTrendPoint("Jan 28", "Jan 2028", 12),
            MonthlyTrendPoint("Feb", "Feb 2028", 18),
            MonthlyTrendPoint("Mar", "Mar 2028", 24)
        )
        val weeks = listOf(
            com.example.ui.WeeklyConsistencyPoint("Mar 6", 67, 2, 3),
            com.example.ui.WeeklyConsistencyPoint("Mar 13", 100, 3, 3)
        )

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                InteractiveMonthlyTrendChart(
                    appFont = FontFamily.SansSerif,
                    dataPoints = points,
                    lineColor = Color(0xFF4CAF50),
                    textColor = Color.White,
                    secondaryTextColor = Color.LightGray,
                    gridColor = Color.DarkGray,
                    cardBgColor = Color.Black
                )
                WeeklyConsistencyChartCard(
                    appFont = FontFamily.SansSerif,
                    points = weeks,
                    primaryColor = Color(0xFF4CAF50),
                    textColor = Color.White,
                    secondaryTextColor = Color.LightGray,
                    cardBgColor = Color.Black,
                    borderColor = Color.DarkGray
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("monthly_trend_chart").assertIsDisplayed()
        composeTestRule.onNodeWithTag("weekly_consistency_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("12-WEEK SCHEDULE ADHERENCE").assertIsDisplayed()

        composeTestRule.onNodeWithTag("weekly_bar_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Week of Mar 13: 3/3 scheduled (100%)").assertIsDisplayed()
    }
}
