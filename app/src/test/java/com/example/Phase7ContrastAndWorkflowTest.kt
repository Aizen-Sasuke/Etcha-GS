package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PreferenceEntry
import com.example.data.Tracker
import com.example.ui.HabitViewModel
import com.example.ui.ThemeData
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.VacationPauseState
import com.example.ui.screens.PreferencesScreen
import com.example.ui.screens.ThemePickerModal
import com.example.ui.screens.UnifiedHabitSettingsSheet
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase7ContrastAndWorkflowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private val createdViewModels = mutableListOf<HabitViewModel>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        resetSingletonDb()
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

    private fun createViewModel(): HabitViewModel {
        val app = context.applicationContext as Application
        return HabitViewModel(app).also { createdViewModels.add(it) }
    }

    @Test
    fun allThemesPassWcagContrastInResolvedPaletteAndArgb() {
        for (theme in ThemeData.ALL_THEMES) {
            val palette = ThemeStyles.resolvePalette(theme.id)
            assertTrue(
                "onPrimary contrast failed for ${theme.id}",
                ThemeStyles.contrastRatio(palette.onPrimary, palette.primary) >= 4.5f
            )
            assertTrue(
                "textPrimary contrast failed for ${theme.id}",
                ThemeStyles.contrastRatio(palette.textPrimary, palette.modalSurface) >= 4.5f
            )
            assertTrue(
                "textSecondary contrast failed for ${theme.id}",
                ThemeStyles.contrastRatio(palette.textSecondary, palette.modalSurface) >= 4.5f
            )
            assertTrue(
                "accentOnSurface contrast failed for ${theme.id}",
                ThemeStyles.contrastRatio(palette.accentOnSurface, palette.modalSurface) >= 4.5f
            )

            val argb = ThemeStyles.resolveArgbPalette(theme.id)
            assertTrue(
                "argb onPrimary contrast failed for ${theme.id}",
                ThemeStyles.contrastRatio(Color(argb.onPrimary), Color(argb.primary)) >= 4.5f
            )
            assertTrue(
                "argb accentOnCard contrast failed for ${theme.id}",
                ThemeStyles.contrastRatio(Color(argb.accentOnCard), Color(argb.card)) >= 4.5f
            )
        }

        val extremeCustoms = listOf(
            "#FFFFFF" to "#FFFFFF",
            "#FFFF00" to "#FFF9C4",
            "#000000" to "#0A0A0A",
            "#00E5FF" to "#F5F7FA"
        )
        for ((primaryHex, bgHex) in extremeCustoms) {
            val customPalette = ThemeStyles.resolvePalette("custom", primaryHex, bgHex)
            assertTrue(
                "custom onPrimary contrast failed for $primaryHex on $bgHex",
                ThemeStyles.contrastRatio(customPalette.onPrimary, customPalette.primary) >= 4.5f
            )
            assertTrue(
                "custom accentOnSurface contrast failed for $primaryHex on $bgHex",
                ThemeStyles.contrastRatio(customPalette.accentOnSurface, customPalette.modalSurface) >= 4.5f
            )
            assertTrue(
                "custom textPrimary contrast failed for $primaryHex on $bgHex",
                ThemeStyles.contrastRatio(customPalette.textPrimary, customPalette.modalSurface) >= 4.5f
            )
        }
    }

    @Test
    fun frequencyAndTargetDaysNormalizationAndBadHabitCheckmarkIsolation() {
        assertEquals("custom_days", HabitViewModel.normalizeFrequencyType("weekly"))
        assertEquals("weekly_quota", HabitViewModel.normalizeFrequencyType("3x_week"))
        assertEquals("daily", HabitViewModel.normalizeFrequencyType("DAILY"))
        assertEquals("MON,FRI,SUN", HabitViewModel.normalizeTargetDays("SUN,FRI,MON,mon,invalid"))

        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.habitDao().insertPreference(PreferenceEntry("checkmark_style", "dot"))
        }
        val viewModel = createViewModel()
        runBlocking { viewModel.awaitInitialized() }

        assertFalse(viewModel.ambientMotionEnabled.value)

        viewModel.addTracker(
            title = "No Late Caffeine",
            icon = "☕",
            accentColor = "#FF0000",
            type = "bad",
            frequencyType = "weekly",
            targetDays = "FRI,MON",
            targetCount = 1,
            timeOfDay = "evening",
            weeklyTarget = 0,
            defaultFocusMinutes = 25
        )

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking {
                db.trackerDao().getAllTrackersList().any { it.title == "No Late Caffeine" }
            }
        }

        val added = runBlocking {
            db.trackerDao().getAllTrackersList().first { it.title == "No Late Caffeine" }
        }
        assertEquals("custom_days", added.frequencyType)
        assertEquals("MON,FRI", added.targetDays)
        assertEquals("dot", runBlocking { db.habitDao().getPreferenceValue("checkmark_style") })

        viewModel.setAmbientMotionEnabled(true)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking {
                db.habitDao().getPreferenceValue("ambient_motion_enabled") == "true"
            }
        }
        assertEquals("true", runBlocking { db.habitDao().getPreferenceValue("ambient_motion_enabled") })
    }

    @Test
    fun unifiedHabitSettingsSheetQuickActionsTabsPauseAndSaveFlow() {
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.trackerDao().getAllTrackersList()
            db.trackerDao().deleteAllTrackers()
            db.trackerDao().insertTracker(
                Tracker(
                    id = "read",
                    title = "Read",
                    icon = "📚",
                    type = "good",
                    frequencyType = "weekly",
                    targetDays = "FRI,MON",
                    targetCount = 1,
                    timeOfDay = "evening"
                )
            )
        }

        val viewModel = createViewModel()
        runBlocking { viewModel.awaitInitialized() }

        val tracker = TrackerConfig(
            id = "read",
            title = "Read",
            icon = "📚",
            type = "good",
            frequencyType = "weekly",
            targetDays = "FRI,MON",
            targetCount = 1,
            timeOfDay = "evening"
        )
        val palette = ThemeStyles.resolvePalette(ThemeStyles.DEFAULT_THEME_ID)

        var pausedDate: String? = null
        var dismissedCount = 0

        composeTestRule.setContent {
            UnifiedHabitSettingsSheet(
                tracker = tracker,
                initialTab = 0,
                today = LocalDate.of(2026, 11, 16),
                vacationState = VacationPauseState(),
                canDelete = true,
                onDismiss = { dismissedCount++ },
                onPauseUntil = { _, untilDate -> pausedDate = untilDate },
                onArchive = {},
                onDelete = {},
                viewModel = viewModel,
                appFont = FontFamily.Default,
                primaryColor = palette.primary,
                textColor = palette.textPrimary,
                secondaryTextColor = palette.textSecondary,
                cardBgColor = palette.modalSurface,
                borderColor = palette.border,
                selectedTheme = ThemeStyles.DEFAULT_THEME_ID
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("unified_habit_settings_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("habit_settings_quick_pause").assertIsDisplayed()
        composeTestRule.onNodeWithTag("habit_settings_quick_archive").assertIsDisplayed()
        composeTestRule.onNodeWithTag("habit_settings_quick_delete").assertIsDisplayed()

        // Basics tab title input
        composeTestRule.onNodeWithTag("tracker_title_input").performTextReplacement("Deep Reading")

        // Schedule tab + inline pause preset
        composeTestRule.onNodeWithTag("habit_settings_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("habit_settings_pause_1")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals("2026-11-16", pausedDate)

        // Alerts & Focus tab
        composeTestRule.onNodeWithTag("habit_settings_tab_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("habit_reminder_switch").performScrollTo().assertIsDisplayed()

        // Save Changes normalizes legacy "weekly" & sorts "FRI,MON" -> "MON,FRI"
        composeTestRule.onNodeWithTag("edit_tracker_save_button").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking {
                db.trackerDao().getAllTrackersList().find { it.id == "read" }?.title == "Deep Reading"
            }
        }

        val updated = runBlocking { db.trackerDao().getAllTrackersList().find { it.id == "read" } }
        assertNotNull(updated)
        assertEquals("custom_days", updated!!.frequencyType)
        assertEquals("MON,FRI", updated.targetDays)
        assertEquals(1, dismissedCount)
    }

    @Test
    fun ambientMotionTogglesInPreferencesScreen() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createViewModel()
        runBlocking { viewModel.awaitInitialized() }
        assertFalse(viewModel.ambientMotionEnabled.value)

        composeTestRule.setContent {
            PreferencesScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preferences_lazy_column")
            .performScrollToNode(hasTestTag("pref_ambient_motion_switch"))
        composeTestRule.onNodeWithTag("pref_ambient_motion_switch").assertIsDisplayed().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            viewModel.ambientMotionEnabled.value
        }
        assertTrue(viewModel.ambientMotionEnabled.value)
    }

    @Test
    fun ambientMotionTogglesInThemeStudio() {
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.habitDao().insertPreference(PreferenceEntry("ambient_motion_enabled", "true"))
        }
        val viewModel = createViewModel()
        runBlocking { viewModel.awaitInitialized() }

        composeTestRule.setContent {
            ThemePickerModal(viewModel = viewModel, onClose = {}, onOpenDonate = {})
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            viewModel.ambientMotionEnabled.value
        }

        composeTestRule.onNodeWithTag("theme_studio_motion_toggle").assertIsDisplayed().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            !viewModel.ambientMotionEnabled.value
        }
        assertFalse(viewModel.ambientMotionEnabled.value)
    }
}
