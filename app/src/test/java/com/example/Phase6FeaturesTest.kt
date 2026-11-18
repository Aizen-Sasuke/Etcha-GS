package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.PreferenceEntry
import com.example.data.Tracker
import com.example.ui.BackupJsonCodec
import com.example.ui.BackupParseResult
import com.example.ui.CorrelationInsightsSnapshot
import com.example.ui.CsvExportCodec
import com.example.ui.FocusMode
import com.example.ui.FocusPhase
import com.example.ui.FocusSessionState
import com.example.ui.HabitCorrelationEngine
import com.example.ui.HabitCorrelationInsight
import com.example.ui.HabitStatsCalculator
import com.example.ui.HabitViewModel
import com.example.ui.TodayDashboardCalculator
import com.example.ui.TodayHabitStatus
import com.example.ui.TrackerConfig
import com.example.ui.VacationPauseHelper
import com.example.ui.VacationPauseState
import com.example.ui.WeekdayDropOffInsight
import com.example.ui.screens.BehavioralInsightsSection
import com.example.ui.screens.FocusTimerSheet
import com.example.ui.screens.MainContainer
import com.example.ui.screens.PreferencesScreen
import com.example.worker.AutoBackupScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase6FeaturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private val createdViewModels = mutableListOf<HabitViewModel>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        resetSingletonDb()
        context.getSharedPreferences("habit_widget_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun teardown() {
        runBlocking {
            createdViewModels.forEach { vm ->
                try {
                    vm.awaitInitialized()
                } catch (_: Exception) {
                }
                vm.clearForTest()
            }
            createdViewModels.clear()
        }
        resetSingletonDb()
        context.getSharedPreferences("habit_widget_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun createTestViewModel(): HabitViewModel {
        val app = context.applicationContext as Application
        return HabitViewModel(app).also { createdViewModels.add(it) }
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
    fun vacationAndPerHabitPausePreserveStreakAndStrengthAndRouteTodayDashboard() {
        val today = LocalDate.of(2026, 11, 16)
        val pausedTracker = TrackerConfig(
            id = "meditate",
            title = "Meditate",
            icon = "🧘",
            type = "good",
            frequencyType = "daily",
            targetCount = 1,
            pausedUntilDate = today.toString()
        )
        val activeTracker = TrackerConfig(
            id = "walk",
            title = "Morning Walk",
            icon = "🚶",
            type = "good",
            frequencyType = "daily",
            targetCount = 1
        )
        // Completed Nov 11..13, then Nov 14..16 are covered by global vacation
        val entries = listOf(
            HabitEntry(dateString = "2026-11-11", count = 1, trackerId = "walk"),
            HabitEntry(dateString = "2026-11-12", count = 1, trackerId = "walk"),
            HabitEntry(dateString = "2026-11-13", count = 1, trackerId = "walk")
        )
        val vacation = VacationPauseState(
            globalEnabled = true,
            globalStartDate = "2026-11-14",
            globalEndDate = "2026-11-17"
        )

        val statsOnVacation = HabitStatsCalculator.calculateStats(
            list = entries,
            freezeActive = null,
            freezeHist = "",
            tracker = activeTracker,
            today = today,
            vacation = vacation
        )
        assertEquals(3, statsOnVacation.currentStreak)
        assertEquals(3, statsOnVacation.bestStreak)

        val statsWithoutVacation = HabitStatsCalculator.calculateStats(
            list = entries,
            freezeActive = null,
            freezeHist = "",
            tracker = activeTracker,
            today = today,
            vacation = VacationPauseState()
        )
        assertEquals(0, statsWithoutVacation.currentStreak)
        assertTrue(statsOnVacation.habitStrengthScore >= statsWithoutVacation.habitStrengthScore)

        // Today dashboard with per-habit pause (global off, pausedTracker pausedUntil today)
        val dashboardPerHabit = TodayDashboardCalculator.build(
            trackers = listOf(pausedTracker, activeTracker),
            recentEntries = emptyList(),
            preferences = mapOf("reminders_enabled" to "true", "reminder_time" to "23:00"),
            now = LocalDateTime.of(2026, 11, 16, 10, 0)
        )
        assertFalse(dashboardPerHabit.isVacationActive)
        assertEquals(1, dashboardPerHabit.dueTotalCount)
        assertEquals(1, dashboardPerHabit.attentionItems.size)
        assertEquals("walk", dashboardPerHabit.attentionItems.first().tracker.id)
        val pausedItem = dashboardPerHabit.restAndProtectedItems.find { it.tracker.id == "meditate" }
        assertNotNull(pausedItem)
        assertEquals(TodayHabitStatus.PAUSED, pausedItem!!.status)
        assertTrue(pausedItem.isPausedToday)
        assertFalse(pausedItem.isReminderUpcomingToday)

        // Today dashboard with global vacation enabled -> both routed to paused, reminders suppressed
        val dashboardGlobal = TodayDashboardCalculator.build(
            trackers = listOf(pausedTracker, activeTracker),
            recentEntries = emptyList(),
            preferences = mapOf(
                VacationPauseHelper.KEY_VACATION_ENABLED to "true",
                VacationPauseHelper.KEY_VACATION_START to "2026-11-15",
                VacationPauseHelper.KEY_VACATION_END to "2026-11-18",
                "reminders_enabled" to "true",
                "reminder_time" to "23:00"
            ),
            now = LocalDateTime.of(2026, 11, 16, 10, 0)
        )
        assertTrue(dashboardGlobal.isVacationActive)
        assertEquals(0, dashboardGlobal.dueTotalCount)
        assertEquals(2, dashboardGlobal.restAndProtectedItems.size)
        assertEquals("🌴 Vacation mode · Reminders paused", dashboardGlobal.nextReminderText)
    }

    @Test
    fun habitCorrelationEngineThresholdsDropOffAndLiftPairings() {
        val today = LocalDate.of(2026, 11, 16) // Monday
        val sleepTracker = TrackerConfig(id = "sleep", title = "Sleep Early", icon = "😴")
        val focusTracker = TrackerConfig(id = "focus", title = "Deep Work", icon = "🧠")

        // Below 14 distinct logged days -> gated
        val sparseEntries = (1..10).map { d ->
            HabitEntry(dateString = today.minusDays(d.toLong()).toString(), count = 1, trackerId = "sleep")
        }
        val gatedSnapshot = HabitCorrelationEngine.build(
            trackers = listOf(sleepTracker, focusTracker),
            allEntries = sparseEntries,
            today = today
        )
        assertFalse(gatedSnapshot.hasEnoughData)
        assertEquals(10, gatedSnapshot.loggedDaysCount)
        assertEquals(14, gatedSnapshot.requiredDaysCount)

        // Construct 28-day window where Mondays consistently miss both,
        // and sleep+focus strongly correlate (sleep done -> focus done; sleep missed -> focus missed)
        val richEntries = mutableListOf<HabitEntry>()
        for (i in 0L until 28L) {
            val day = today.minusDays(i)
            val isMonday = day.dayOfWeek == DayOfWeek.MONDAY
            // Log sleep on non-Mondays, and miss on half of Sundays too so variance exists
            val completeSleep = !isMonday && (i % 5L != 0L)
            if (completeSleep) {
                richEntries.add(HabitEntry(dateString = day.toString(), count = 1, trackerId = "sleep"))
                richEntries.add(HabitEntry(dateString = day.toString(), count = 1, trackerId = "focus"))
            }
        }

        val richSnapshot = HabitCorrelationEngine.build(
            trackers = listOf(sleepTracker, focusTracker),
            allEntries = richEntries,
            today = today
        )
        assertTrue(richSnapshot.hasEnoughData)
        assertTrue(richSnapshot.loggedDaysCount >= 14)
        assertNotNull(richSnapshot.weekdayDropOff)
        assertEquals("Monday", richSnapshot.weekdayDropOff!!.weakestDayName)
        assertTrue(richSnapshot.weekdayDropOff!!.dropPercentPoints >= 12)

        assertTrue(richSnapshot.topPairings.isNotEmpty())
        val top = richSnapshot.topPairings.first()
        assertTrue(top.isPositive)
        assertTrue(top.liftPercentPoints >= 12)
        assertTrue(top.withRatePercent > top.withoutRatePercent)
    }

    @Test
    fun csvExportCodecEscapesFormulasQuotesAndFiltersByTracker() {
        val t1 = Tracker(id = "t1", title = "Read \"Books\", Daily", icon = "📚", type = "good", targetCount = 2)
        val t2 = Tracker(id = "t2", title = "Hydrate", icon = "💧", type = "good", targetCount = 1)
        val e1 = HabitEntry(
            dateString = "2026-11-15",
            count = 2,
            timestamp = 1700000000000L,
            notes = "[MOOD:Energized] =SUM(1,2) with \"quotes\", comma",
            trackerId = "t1"
        )
        val e2 = HabitEntry(
            dateString = "2026-11-16",
            count = 1,
            timestamp = 1700050000000L,
            notes = "+cmd_injection",
            trackerId = "t2"
        )

        val allCsv = CsvExportCodec.encodeEntriesCsv(listOf(t1, t2), listOf(e1, e2), filterTrackerId = null)
        val lines = allCsv.trim().lines()
        assertEquals(3, lines.size)
        assertEquals("date,tracker_id,habit_name,type,count,target_count,mood,notes,logged_at_utc", lines[0])
        assertTrue(lines[1].contains("\"Read \"\"Books\"\", Daily\""))
        assertTrue(lines[1].contains("Energized"))
        assertTrue(lines[1].contains("\"'=SUM(1,2) with \"\"quotes\"\", comma\""))
        assertTrue(lines[2].contains("'+cmd_injection"))

        val filteredCsv = CsvExportCodec.encodeEntriesCsv(listOf(t1, t2), listOf(e1, e2), filterTrackerId = "t2")
        val filteredLines = filteredCsv.trim().lines()
        assertEquals(2, filteredLines.size)
        assertTrue(filteredLines[1].contains(",t2,Hydrate,"))
    }

    @Test
    fun focusSessionStateElapsedRemainingAndJsonRoundtrip() {
        val baseStart = 1_700_000_000_000L
        val session = FocusSessionState(
            isSheetVisible = true,
            trackerId = "deep_work",
            trackerTitle = "Deep Work",
            trackerIcon = "🧠",
            mode = FocusMode.POMODORO,
            phase = FocusPhase.FOCUS_RUNNING,
            targetDurationMinutes = 25,
            breakDurationMinutes = 5,
            completedPomodoroCycles = 1,
            startedAtEpochMs = baseStart,
            accumulatedElapsedMs = 60_000L // 1 minute already elapsed before resume
        )

        // 4 minutes after resume = 5 minutes total elapsed -> 20 minutes remaining
        val nowMs = baseStart + 240_000L
        assertEquals(300_000L, session.elapsedMillis(nowMs))
        assertEquals(1_200_000L, session.remainingMillis(nowMs))
        assertEquals(0.2f, session.progressFraction(nowMs), 0.001f)
        assertFalse(session.hasReachedPhaseTarget(nowMs))

        // At 25m -> reached phase target
        val reachedMs = baseStart + 1_440_000L
        assertTrue(session.hasReachedPhaseTarget(reachedMs))
        assertEquals("25:00", FocusSessionState.formatClockMillis(25 * 60_000L))
        assertEquals("1:05:00", FocusSessionState.formatClockMillis(3900_000L))

        // Stopwatch ignores countdown target
        val stopwatch = session.copy(mode = FocusMode.STOPWATCH)
        assertEquals(300_000L, stopwatch.remainingMillis(nowMs))
        assertFalse(stopwatch.hasReachedPhaseTarget(reachedMs))

        // JSON persistence roundtrip restores active/paused session with isSheetVisible = false
        val json = session.toPersistedJson()
        val restored = FocusSessionState.fromPersistedJson(json)
        assertNotNull(restored)
        assertFalse(restored!!.isSheetVisible)
        assertEquals("deep_work", restored.trackerId)
        assertEquals(FocusMode.POMODORO, restored.mode)
        assertEquals(FocusPhase.FOCUS_RUNNING, restored.phase)
        assertEquals(1, restored.completedPomodoroCycles)
        assertEquals(baseStart, restored.startedAtEpochMs)
        assertEquals(60_000L, restored.accumulatedElapsedMs)

        // Idle sessions do not restore
        val idleJson = session.copy(phase = FocusPhase.IDLE).toPersistedJson()
        assertNull(FocusSessionState.fromPersistedJson(idleJson))
    }

    @Test
    fun backupJsonCodecPreservesPausedUntilAndDefaultFocusMinutes() {
        val tracker = Tracker(
            id = "focus_habit",
            title = "Study Kotlin",
            icon = "💻",
            sortOrder = 0,
            pausedUntilDate = "2026-11-20",
            defaultFocusMinutes = 45
        )
        val entry = HabitEntry(dateString = "2026-11-16", count = 1, trackerId = "focus_habit", notes = "Focused 45m")
        val prefs = mapOf(
            VacationPauseHelper.KEY_VACATION_ENABLED to "true",
            VacationPauseHelper.KEY_VACATION_START to "2026-11-16",
            VacationPauseHelper.KEY_VACATION_END to "2026-11-18"
        )

        val json = BackupJsonCodec.encodeFullBackupJson(listOf(tracker), listOf(entry), prefs)
        val parsed = BackupJsonCodec.parseAndValidate(json)
        assertTrue(parsed is BackupParseResult.Valid)
        val valid = parsed as BackupParseResult.Valid
        assertEquals(BackupJsonCodec.CURRENT_VERSION, valid.preview.version)
        val restoredTracker = valid.preview.trackers.single()
        assertEquals("2026-11-20", restoredTracker.pausedUntilDate)
        assertEquals(45, restoredTracker.defaultFocusMinutes)
        assertEquals("true", valid.preview.preferences[VacationPauseHelper.KEY_VACATION_ENABLED])
    }

    @Test
    fun mainContainerSwitchesBetweenNavigationBarAndNavigationRailByWindowSize() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        composeTestRule.setContent {
            MainContainer(
                viewModel = viewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Compact
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("main_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main_navigation_rail").assertDoesNotExist()
    }

    @Test
    fun mainContainerExpandedLayoutShowsNavigationRailAndTwoPaneHome() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        composeTestRule.setContent {
            MainContainer(
                viewModel = viewModel,
                windowWidthSizeClass = WindowWidthSizeClass.Expanded
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("main_navigation_rail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main_navigation_bar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_two_pane_layout").assertIsDisplayed()

        // Switch via rail to Analytics and back
        composeTestRule.onNodeWithTag("nav_analytics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_two_pane_layout").assertIsDisplayed()
    }

    @Test
    fun focusTimerSheetAndInsightsAndPreferencesControlsRenderAndInteract() {
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.trackerDao().getAllTrackersList()
            db.habitDao().insertPreference(PreferenceEntry("has_seen_onboarding", "true"))
        }
        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        val active = TrackerConfig(id = "study", title = "Deep Study", icon = "📚", defaultFocusMinutes = 25)
        viewModel.openFocusSheet(active)

        val insights = CorrelationInsightsSnapshot(
            hasEnoughData = true,
            loggedDaysCount = 20,
            requiredDaysCount = 14,
            weekdayDropOff = WeekdayDropOffInsight(
                trackerId = "study",
                trackerTitle = "Deep Study",
                trackerIcon = "📚",
                weakestDayName = "Monday",
                weakestDayRatePercent = 25,
                weeklyAverageRatePercent = 78,
                dropPercentPoints = 53
            ),
            topPairings = listOf(
                HabitCorrelationInsight(
                    sourceTrackerId = "walk",
                    sourceTitle = "Walk",
                    sourceIcon = "🚶",
                    targetTrackerId = "study",
                    targetTitle = "Deep Study",
                    targetIcon = "📚",
                    withRatePercent = 90,
                    withoutRatePercent = 40,
                    liftPercentPoints = 50,
                    isPositive = true
                )
            )
        )

        var adjustedTrackerId: String? = null
        composeTestRule.setContent {
            val sessionState by viewModel.focusSession.collectAsState()
            FocusTimerSheet(
                session = sessionState,
                appFont = FontFamily.Default,
                primaryColor = Color(0xFFC32021),
                textColor = Color(0xFF141414),
                secondaryTextColor = Color(0xFF555555),
                cardBgColor = Color(0xFFFDFBF7),
                borderColor = Color(0xFFCCCCCC),
                selectedTheme = "revolutionary_cream",
                onDismiss = { viewModel.dismissFocusSheet() },
                onSelectMode = { viewModel.setFocusMode(it) },
                onSelectDuration = { viewModel.setFocusDurationMinutes(it) },
                onStartOrResume = { viewModel.startOrResumeFocus() },
                onPause = { viewModel.pauseFocus() },
                onReset = { viewModel.resetFocus() },
                onCompleteAndLog = { viewModel.completeFocusAndLog() }
            )
            BehavioralInsightsSection(
                insights = insights,
                appFont = FontFamily.Default,
                primaryColor = Color(0xFFC32021),
                cardBgColor = Color(0xFFFDFBF7),
                borderColor = Color(0xFFCCCCCC),
                cardStroke = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                textColor = Color(0xFF141414),
                secondaryTextColor = Color(0xFF555555),
                isLight = true,
                isGlass = false,
                onAdjustReminder = { adjustedTrackerId = it }
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("behavioral_insights_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("insights_adjust_reminder_button").performClick()
        assertEquals("study", adjustedTrackerId)

        composeTestRule.onNodeWithTag("focus_timer_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("focus_mode_countdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(FocusMode.COUNTDOWN, viewModel.focusSession.value.mode)

        composeTestRule.onNodeWithTag("focus_preset_45m").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(45, viewModel.focusSession.value.targetDurationMinutes)
        composeTestRule.onNodeWithTag("focus_clock_readout").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun preferencesScreenRendersCsvExportAutoBackupVacationAndQuickTileCards() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        composeTestRule.setContent {
            PreferencesScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preferences_lazy_column")
            .performScrollToNode(hasTestTag("pref_export_csv_all"))
        composeTestRule.onNodeWithTag("pref_export_csv_all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pref_export_csv_active").assertIsDisplayed()

        composeTestRule.onNodeWithTag("preferences_lazy_column")
            .performScrollToNode(hasTestTag("pref_autobackup_daily"))
        composeTestRule.onNodeWithTag("pref_autobackup_daily").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            viewModel.autoBackupCadence.value == AutoBackupScheduler.CADENCE_DAILY
        }

        composeTestRule.onNodeWithTag("preferences_lazy_column")
            .performScrollToNode(hasTestTag("pref_vacation_mode_card"))
        composeTestRule.onNodeWithTag("pref_vacation_mode_card").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("global_vacation_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vacation_preset_1").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            viewModel.vacationState.value.globalEnabled
        }

        composeTestRule.onNodeWithTag("preferences_lazy_column")
            .performScrollToNode(hasTestTag("pref_quick_tile_card"))
        composeTestRule.onNodeWithTag("pref_quick_tile_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preferences_lazy_column")
            .performScrollToNode(hasTestTag("pref_pin_quick_tile_button"))
        composeTestRule.onNodeWithTag("pref_pin_quick_tile_button").assertIsDisplayed()
    }

    @Test
    fun habitViewModelFocusVacationAutoBackupAndCsvEndToEnd() = runBlocking {
        val db = AppDatabase.getDatabase(context)
        db.trackerDao().deleteAllTrackers()
        db.habitDao().deleteAllHabitEntries()
        db.trackerDao().insertTracker(
            Tracker(id = "code", title = "Code Session", icon = "💻", defaultFocusMinutes = 30)
        )

        val viewModel = createTestViewModel()
        viewModel.awaitInitialized()

        // 1. Vacation + per-habit pause
        viewModel.setGlobalVacation(true, "2026-11-16", "2026-11-19")
        viewModel.setTrackerPausedUntil("code", "2026-11-22")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking {
                db.habitDao().getPreferenceValue(VacationPauseHelper.KEY_VACATION_ENABLED) == "true" &&
                    db.trackerDao().getAllTrackersList().find { it.id == "code" }?.pausedUntilDate == "2026-11-22"
            }
        }

        // 2. Quick tile + auto backup cadence
        viewModel.setAutoBackupCadence(AutoBackupScheduler.CADENCE_WEEKLY)
        viewModel.setQuickTileTrackerId("code")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking {
                db.habitDao().getPreferenceValue(AutoBackupScheduler.KEY_AUTO_BACKUP_CADENCE) == AutoBackupScheduler.CADENCE_WEEKLY &&
                    db.habitDao().getPreferenceValue("quick_tile_tracker_id") == "code"
            }
        }

        // 3. Focus session start & completion logs +1 with Focused Xm note
        val config = TrackerConfig(id = "code", title = "Code Session", icon = "💻", defaultFocusMinutes = 30)
        viewModel.openFocusSheet(config)
        assertEquals(30, viewModel.focusSession.value.targetDurationMinutes)
        viewModel.startOrResumeFocus()
        assertTrue(viewModel.focusSession.value.isRunning)

        var loggedMinutes = 0
        viewModel.completeFocusAndLog(manualElapsedMinutes = 30) { mins ->
            loggedMinutes = mins
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            loggedMinutes == 30
        }
        val todayEntries = db.habitDao().getEntriesForTrackerAndDate("code", viewModel.getEffectiveToday().toString())
        assertEquals(1, todayEntries.size)
        assertEquals(1, todayEntries.first().count)
        assertTrue(todayEntries.first().notes.orEmpty().contains("Focused 30m"))

        // 4. CSV export contains the newly logged focus entry
        var exportedCsv = ""
        var suggestedFile = ""
        viewModel.prepareCsvExport("code") { csv, name ->
            exportedCsv = csv
            suggestedFile = name
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            exportedCsv.isNotEmpty()
        }
        assertTrue(suggestedFile.startsWith("etchags-code-session-"))
        assertTrue(suggestedFile.endsWith(".csv"))
        assertTrue(exportedCsv.contains("Focused 30m"))
    }
}
