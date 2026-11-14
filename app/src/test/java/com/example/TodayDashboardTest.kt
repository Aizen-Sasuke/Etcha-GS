package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.PreferenceEntry
import com.example.data.Tracker
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TodayDashboardCalculator
import com.example.ui.TodayDashboardState
import com.example.ui.TodayHabitStatus
import com.example.ui.TrackerConfig
import com.example.ui.screens.HomeThemeConfig
import com.example.ui.screens.UnifiedTodayDashboardCard
import com.example.worker.ReminderManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodayDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        resetSingletonDb()
    }

    @After
    fun teardown() {
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
    fun emptyTrackersProduceCleanEmptyDashboardState() {
        val monday = LocalDateTime.of(2026, 11, 9, 10, 0)
        val state = TodayDashboardCalculator.build(
            trackers = emptyList(),
            recentEntries = emptyList(),
            preferences = emptyMap(),
            now = monday
        )

        assertEquals(0, state.totalActiveCount)
        assertEquals(0, state.dueTotalCount)
        assertEquals(0, state.dueCompletedCount)
        assertEquals(0f, state.overallProgress, 0.001f)
        assertTrue(state.attentionItems.isEmpty())
        assertTrue(state.completedItems.isEmpty())
        assertTrue(state.avoidanceItems.isEmpty())
        assertTrue(state.restAndProtectedItems.isEmpty())
        assertNull(state.nextReminderText)
    }

    @Test
    fun overallDailyProgressAndPartialMultiCountSorting() {
        val monday = LocalDateTime.of(2026, 11, 9, 11, 0)
        val todayStr = "2026-11-09"

        val trackers = listOf(
            TrackerConfig(id = "read", title = "Read", icon = "📚", targetCount = 1),
            TrackerConfig(id = "water", title = "Water", icon = "💧", targetCount = 4),
            TrackerConfig(id = "stretch", title = "Stretch", icon = "🤸", targetCount = 1)
        )

        val entries = listOf(
            HabitEntry(dateString = todayStr, count = 1, trackerId = "read"),
            HabitEntry(dateString = todayStr, count = 2, trackerId = "water")
        )

        val state = TodayDashboardCalculator.build(
            trackers = trackers,
            recentEntries = entries,
            preferences = emptyMap(),
            now = monday
        )

        assertEquals(3, state.dueTotalCount)
        assertEquals(1, state.dueCompletedCount)
        // Fractional progress: (1.0 + 0.5 + 0.0) / 3 = 0.5
        assertEquals(0.5f, state.overallProgress, 0.001f)

        // In-progress partial habit MUST appear before untouched NEEDS_ATTENTION habit
        assertEquals(2, state.attentionItems.size)
        assertEquals("water", state.attentionItems[0].tracker.id)
        assertEquals(TodayHabitStatus.IN_PROGRESS, state.attentionItems[0].status)
        assertEquals(2, state.attentionItems[0].todayCount)
        assertEquals(4, state.attentionItems[0].targetCount)
        assertEquals("2 of 4 logged", state.attentionItems[0].statusLabel)

        assertEquals("stretch", state.attentionItems[1].tracker.id)
        assertEquals(TodayHabitStatus.NEEDS_ATTENTION, state.attentionItems[1].status)

        assertEquals(1, state.completedItems.size)
        assertEquals("read", state.completedItems[0].tracker.id)

        // Completing water moves it out of attentionItems into completedItems
        val completedEntries = entries + HabitEntry(dateString = todayStr, count = 2, trackerId = "water")
        val updatedState = TodayDashboardCalculator.build(
            trackers = trackers,
            recentEntries = completedEntries,
            preferences = emptyMap(),
            now = monday
        )
        assertEquals(2, updatedState.dueCompletedCount)
        assertEquals(1, updatedState.attentionItems.size)
        assertEquals(2, updatedState.completedItems.size)
        assertEquals(2f / 3f, updatedState.overallProgress, 0.001f)
    }

    @Test
    fun weeklyQuotaShowsProgressAndSuppressesMissedWarningOnceGoalMet() {
        // Wednesday of week Mon 2026-11-09 .. Sun 2026-11-15
        val wednesday = LocalDateTime.of(2026, 11, 11, 14, 0)
        val gym = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3
        )

        // 2 of 3 completed earlier in week -> still needs attention today
        val twoDaysEntries = listOf(
            HabitEntry(dateString = "2026-11-09", count = 1, trackerId = "gym"),
            HabitEntry(dateString = "2026-11-10", count = 1, trackerId = "gym")
        )
        val inProgressWeek = TodayDashboardCalculator.build(
            trackers = listOf(gym),
            recentEntries = twoDaysEntries,
            preferences = emptyMap(),
            now = wednesday
        )
        assertEquals(1, inProgressWeek.attentionItems.size)
        assertEquals(2, inProgressWeek.attentionItems[0].weeklyCompletedDays)
        assertEquals(3, inProgressWeek.attentionItems[0].weeklyTarget)
        assertEquals("2 of 3 days this week", inProgressWeek.attentionItems[0].statusLabel)
        assertEquals(1, inProgressWeek.dueTotalCount)

        // 3 of 3 completed across Mon, Tue, and an extra count on Mon doesn't double-count; need distinct Fri or earlier day:
        // Simulate Saturday with Mon, Tue, Wed already completed (3/3 met)
        val saturday = LocalDateTime.of(2026, 11, 14, 14, 0)
        val threeDaysEntries = listOf(
            HabitEntry(dateString = "2026-11-09", count = 1, trackerId = "gym"),
            HabitEntry(dateString = "2026-11-10", count = 1, trackerId = "gym"),
            HabitEntry(dateString = "2026-11-11", count = 1, trackerId = "gym")
        )
        val metState = TodayDashboardCalculator.build(
            trackers = listOf(gym),
            recentEntries = threeDaysEntries,
            preferences = emptyMap(),
            now = saturday
        )
        assertTrue(metState.attentionItems.isEmpty())
        assertEquals(0, metState.dueTotalCount)
        assertEquals(1, metState.restAndProtectedItems.size)
        assertEquals(TodayHabitStatus.QUOTA_MET, metState.restAndProtectedItems[0].status)
        assertEquals("Weekly goal met (3/3)", metState.restAndProtectedItems[0].statusLabel)

        // If user also works out on Saturday (bonus 4th day), it moves to completedItems cleanly
        val bonusState = TodayDashboardCalculator.build(
            trackers = listOf(gym),
            recentEntries = threeDaysEntries + HabitEntry(dateString = "2026-11-14", count = 1, trackerId = "gym"),
            preferences = emptyMap(),
            now = saturday
        )
        assertEquals(1, bonusState.completedItems.size)
        assertEquals(TodayHabitStatus.COMPLETED, bonusState.completedItems[0].status)
    }

    @Test
    fun restDaysAreExcludedFromDueHabitsAndNeverShownAsMissed() {
        // 2026-11-14 is Saturday
        val saturday = LocalDateTime.of(2026, 11, 14, 10, 0)
        val trackers = listOf(
            TrackerConfig(id = "work", title = "Deep Work", icon = "💻", frequencyType = "weekdays"),
            TrackerConfig(id = "piano", title = "Piano", icon = "🎹", frequencyType = "custom_days", targetDays = "MON,WED,FRI"),
            TrackerConfig(id = "meditate", title = "Meditate", icon = "🧘", frequencyType = "daily")
        )

        val state = TodayDashboardCalculator.build(
            trackers = trackers,
            recentEntries = emptyList(),
            preferences = emptyMap(),
            now = saturday
        )

        // Only daily Meditate is due on Saturday
        assertEquals(1, state.dueTotalCount)
        assertEquals(1, state.attentionItems.size)
        assertEquals("meditate", state.attentionItems[0].tracker.id)

        assertEquals(2, state.restAndProtectedItems.size)
        assertTrue(state.restAndProtectedItems.all { it.status == TodayHabitStatus.REST_DAY && it.isRestDay })

        // Bonus completion on rest day counts as COMPLETED without polluting missing items
        val bonusState = TodayDashboardCalculator.build(
            trackers = trackers,
            recentEntries = listOf(HabitEntry(dateString = "2026-11-14", count = 1, trackerId = "work")),
            preferences = emptyMap(),
            now = saturday
        )
        assertEquals(1, bonusState.completedItems.size)
        assertEquals("work", bonusState.completedItems[0].tracker.id)
        assertEquals("Bonus logged on rest day", bonusState.completedItems[0].statusLabel)
        assertEquals(1, bonusState.dueTotalCount)
    }

    @Test
    fun badHabitsUseCleanAndSlipSemanticsOutsidePositiveDueCounts() {
        val monday = LocalDateTime.of(2026, 11, 9, 12, 0)
        val noSugar = TrackerConfig(id = "no_sugar", title = "No Sugar", icon = "🍬", type = "bad")
        val dailyRead = TrackerConfig(id = "read", title = "Read", icon = "📚", type = "good")

        // Clean today
        val cleanState = TodayDashboardCalculator.build(
            trackers = listOf(noSugar, dailyRead),
            recentEntries = emptyList(),
            preferences = emptyMap(),
            now = monday
        )
        assertEquals(1, cleanState.dueTotalCount)
        assertEquals(1, cleanState.avoidanceItems.size)
        assertEquals(TodayHabitStatus.BAD_CLEAN, cleanState.avoidanceItems[0].status)
        assertEquals("Clean today — stay disciplined", cleanState.avoidanceItems[0].statusLabel)

        // Slip recorded today
        val slipState = TodayDashboardCalculator.build(
            trackers = listOf(noSugar, dailyRead),
            recentEntries = listOf(HabitEntry(dateString = "2026-11-09", count = 1, trackerId = "no_sugar")),
            preferences = emptyMap(),
            now = monday
        )
        assertEquals(1, slipState.avoidanceItems.size)
        assertEquals(TodayHabitStatus.BAD_SLIP, slipState.avoidanceItems[0].status)
        assertEquals("Slip recorded today · Keep moving forward", slipState.avoidanceItems[0].statusLabel)
        assertEquals(1, slipState.dueTotalCount)
    }

    @Test
    fun frozenDayProtectsUncompletedGoodHabitsAndPreservesCompletedOnes() {
        val monday = LocalDateTime.of(2026, 11, 9, 12, 0)
        val trackers = listOf(
            TrackerConfig(id = "run", title = "Run", icon = "🏃", type = "good"),
            TrackerConfig(id = "read", title = "Read", icon = "📚", type = "good")
        )
        val entries = listOf(HabitEntry(dateString = "2026-11-09", count = 1, trackerId = "run"))

        val state = TodayDashboardCalculator.build(
            trackers = trackers,
            recentEntries = entries,
            preferences = mapOf("streak_freeze_date" to "2026-11-09"),
            now = monday
        )

        assertTrue(state.isTodayFrozen)
        assertTrue(state.attentionItems.isEmpty())
        assertEquals(1, state.completedItems.size)
        assertEquals("run", state.completedItems[0].tracker.id)
        assertEquals(1, state.restAndProtectedItems.size)
        assertEquals("read", state.restAndProtectedItems[0].tracker.id)
        assertEquals(TodayHabitStatus.FROZEN, state.restAndProtectedItems[0].status)
        assertEquals("Frozen today · Streak protected", state.restAndProtectedItems[0].statusLabel)
    }

    @Test
    fun archivedHabitsAreExcludedAndUpcomingRemindersSurfaceNextTrigger() {
        val mondayMorning = LocalDateTime.of(2026, 11, 9, 9, 0)
        val trackers = listOf(
            TrackerConfig(
                id = "archived_early",
                title = "Archived Habit",
                icon = "📦",
                isArchived = true,
                reminderEnabled = true,
                reminderTime = "09:30"
            ),
            TrackerConfig(
                id = "water",
                title = "Hydrate",
                icon = "💧",
                reminderEnabled = true,
                reminderTime = "13:00"
            ),
            TrackerConfig(
                id = "read",
                title = "Read",
                icon = "📚",
                reminderEnabled = true,
                reminderTime = "20:00"
            )
        )

        val state = TodayDashboardCalculator.build(
            trackers = trackers,
            recentEntries = emptyList(),
            preferences = emptyMap(),
            now = mondayMorning
        )

        assertEquals(2, state.totalActiveCount)
        assertEquals(2, state.dueTotalCount)
        assertFalse(state.allItems.any { it.tracker.id == "archived_early" })
        assertEquals("Next: 💧 Hydrate at 13:00", state.nextReminderText)
        assertTrue(state.allItems.first { it.tracker.id == "water" }.isReminderUpcomingToday)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModelTodayDashboardReactsToCheckInUndoEditArchiveAndUnarchive() = runBlocking {
        val singletonDb = AppDatabase.getDatabase(context)
        val now = LocalDateTime.now()
        val effectiveToday = ReminderManager.getEffectiveToday(0, now).toString()

        singletonDb.habitDao().insertPreference(PreferenceEntry("day_rollover_hour", "0"))
        singletonDb.trackerDao().deleteAllTrackers()
        singletonDb.trackerDao().insertTracker(
            Tracker(id = "water", title = "Water", icon = "💧", targetCount = 2, sortOrder = 0)
        )
        singletonDb.trackerDao().insertTracker(
            Tracker(id = "no_junk", title = "No Junk", icon = "🍔", type = "bad", sortOrder = 1)
        )
        singletonDb.trackerDao().insertTracker(
            Tracker(id = "old_habit", title = "Old", icon = "📦", isArchived = true, sortOrder = 2)
        )

        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        val app = context.applicationContext as Application
        val vm = HabitViewModel(app)
        val collectJob = launch(testDispatcher) {
            vm.todayDashboard.collect { }
        }

        try {
            val initial = withTimeout(5000) {
                vm.todayDashboard.first { state ->
                    state.totalActiveCount == 2 && state.attentionItems.any { it.tracker.id == "water" }
                }
            }
            assertEquals(1, initial.dueTotalCount)
            assertEquals(0, initial.dueCompletedCount)
            assertEquals(1, initial.avoidanceItems.size)
            assertFalse(initial.allItems.any { it.tracker.id == "old_habit" })

            // 1. Check in 1/2 -> partial IN_PROGRESS
            val record1 = CompletableDeferred<com.example.ui.QuickCheckInRecord>()
            vm.quickIncrementForTracker("water", effectiveToday, null) { record1.complete(it) }
            val afterFirst = withTimeout(5000) {
                vm.todayDashboard.first { state ->
                    state.attentionItems.any { it.tracker.id == "water" && it.todayCount == 1 }
                }
            }
            assertEquals(TodayHabitStatus.IN_PROGRESS, afterFirst.attentionItems.first().status)
            assertEquals(0.5f, afterFirst.overallProgress, 0.01f)
            assertEquals(1, record1.await().newCount)

            // 2. Check in 2/2 -> COMPLETED
            val record2 = CompletableDeferred<com.example.ui.QuickCheckInRecord>()
            vm.quickIncrementForTracker("water", effectiveToday, null) { record2.complete(it) }
            val afterSecond = withTimeout(5000) {
                vm.todayDashboard.first { state ->
                    state.completedItems.any { it.tracker.id == "water" && it.todayCount == 2 }
                }
            }
            assertEquals(1, afterSecond.dueCompletedCount)
            assertEquals(1f, afterSecond.overallProgress, 0.01f)

            // 3. Undo second increment -> reverts cleanly back to 1/2 IN_PROGRESS
            val secondRecord = record2.await()
            vm.undoQuickCheckIn(secondRecord.token)
            val afterUndo = withTimeout(5000) {
                vm.todayDashboard.first { state ->
                    state.attentionItems.any { it.tracker.id == "water" && it.todayCount == 1 }
                }
            }
            assertEquals(0, afterUndo.dueCompletedCount)
            assertEquals(0.5f, afterUndo.overallProgress, 0.01f)

            // 4. Edit tracker targetCount 2 -> 4 updates dashboard immediately
            val waterConfig = afterUndo.attentionItems.first { it.tracker.id == "water" }.tracker
            vm.updateTracker(
                id = waterConfig.id,
                title = waterConfig.title,
                icon = waterConfig.icon,
                accentColor = waterConfig.accentColor,
                type = waterConfig.type,
                frequencyType = waterConfig.frequencyType,
                targetDays = waterConfig.targetDays,
                targetCount = 4,
                timeOfDay = waterConfig.timeOfDay,
                weeklyTarget = waterConfig.weeklyTarget
            )
            val afterEdit = withTimeout(5000) {
                vm.todayDashboard.first { state ->
                    state.attentionItems.any { it.tracker.id == "water" && it.targetCount == 4 }
                }
            }
            assertEquals(0.25f, afterEdit.overallProgress, 0.01f)

            // 5. Archive active habit removes it; Unarchive old habit adds it
            vm.archiveTracker("water")
            val afterArchive = withTimeout(5000) {
                vm.todayDashboard.first { state ->
                    state.allItems.none { it.tracker.id == "water" }
                }
            }
            assertEquals(1, afterArchive.totalActiveCount)

            vm.unarchiveTracker("old_habit")
            val afterUnarchive = withTimeout(5000) {
                vm.todayDashboard.first { state ->
                    state.allItems.any { it.tracker.id == "old_habit" }
                }
            }
            assertEquals(2, afterUnarchive.totalActiveCount)
        } finally {
            collectJob.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun composeTodayDashboardCardRendersGroupsAndCollapsedCompletedDrawer() {
        val monday = LocalDateTime.of(2026, 11, 9, 9, 0)
        val trackers = listOf(
            TrackerConfig(id = "water", title = "Drink Water", icon = "💧", targetCount = 4),
            TrackerConfig(id = "read", title = "Daily Read", icon = "📚", targetCount = 1),
            TrackerConfig(id = "no_sugar", title = "No Sugar", icon = "🍬", type = "bad")
        )
        val entries = listOf(
            HabitEntry(dateString = "2026-11-09", count = 2, trackerId = "water"),
            HabitEntry(dateString = "2026-11-09", count = 1, trackerId = "read")
        )
        val state: TodayDashboardState = TodayDashboardCalculator.build(
            trackers = trackers,
            recentEntries = entries,
            preferences = emptyMap(),
            now = monday
        )
        val themeConfig = HomeThemeConfig(
            selectedTheme = "matte_black",
            primaryColor = ThemeStyles.getPrimaryColor("matte_black", "#FFFFFF"),
            bgColor = ThemeStyles.getBackgroundColor("matte_black", "#111111"),
            cardBgColor = ThemeStyles.getCardBackgroundColor("matte_black", "#111111"),
            borderColor = ThemeStyles.getBorderColor("matte_black"),
            textColor = ThemeStyles.getTextColor("matte_black"),
            secondaryTextColor = ThemeStyles.getSecondaryTextColor("matte_black"),
            appFont = ThemeStyles.getSelectedFontFamily("sans"),
            cellShape = ThemeStyles.getCalendarCellShape("rounded_square"),
            checkmarkStyleName = "tick"
        )

        var incrementedId: String? = null
        composeTestRule.setContent {
            UnifiedTodayDashboardCard(
                dashboard = state,
                selectedTrackerId = "water",
                selectedRoutineFilter = "all",
                themeConfig = themeConfig,
                onQuickIncrement = { incrementedId = it },
                onOpenHabitDetail = {},
                onAddTrackerClick = {}
            )
        }

        composeTestRule.waitForIdle()
        try {
            composeTestRule.onNodeWithTag("today_dashboard_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("today_overall_percentage", useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("Drink Water").assertIsDisplayed()
            composeTestRule.onNodeWithText("No Sugar").assertIsDisplayed()

            // Action button on Drink Water invokes quick increment callback
            composeTestRule.onNodeWithTag("dashboard_action_water").performClick()
            assertEquals("water", incrementedId)

            // Completed drawer starts collapsed, tapping expands it to reveal Daily Read
            composeTestRule.onNodeWithTag("dashboard_completed_toggle").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Daily Read").assertIsDisplayed()
        } catch (e: AssertionError) {
            val treeDump = composeTestRule.onRoot(useUnmergedTree = true).printToString()
            throw AssertionError("${e.message}\nSemantics tree:\n$treeDump", e)
        }
    }
}
