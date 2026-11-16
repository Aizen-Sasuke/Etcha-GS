package com.example

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.graphics.toColorInt
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.PreferenceEntry
import com.example.data.SampleTrackerSeeder
import com.example.data.Tracker
import com.example.ui.HabitViewModel
import com.example.ui.ThemeData
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.components.LiquidAmbientBackdrop
import com.example.ui.components.ThemeEffectPreviewCanvas
import com.example.ui.components.recessedWell
import com.example.ui.components.surface3DCard
import com.example.ui.screens.DemoTrackerBannerCard
import com.example.ui.screens.DonateModal
import com.example.ui.screens.FontPickerModal
import com.example.ui.screens.HEATMAP_PERIOD_ROLLING_12M
import com.example.ui.screens.HomeThemeConfig
import com.example.ui.screens.ThemePickerModal
import com.example.ui.screens.buildAvailableHeatmapPeriods
import com.example.ui.screens.buildHeatmapGridModel
import com.example.ui.screens.completedCellCornerGlow
import com.example.ui.screens.isCurvedCalendarShape
import com.example.widget.HabitAppWidgetProvider
import com.example.widget.WidgetDayState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.flow.first
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase5FeaturesTest {

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
    fun defaultThemeIsRevolutionaryCreamAndFontIsSpaceGrotesk() = runBlocking {
        assertEquals("revolutionary_cream", ThemeStyles.DEFAULT_THEME_ID)
        assertEquals("space_grotesk", ThemeStyles.DEFAULT_FONT_ID)

        val viewModel = createTestViewModel()
        viewModel.awaitInitialized()
        assertEquals("revolutionary_cream", viewModel.selectedTheme.value)
        assertEquals("space_grotesk", viewModel.selectedFont.value)

        val cream = ThemeStyles.resolveArgbPalette("revolutionary_cream", "#C32021", "#EBE4D3")
        assertFalse(cream.isDark)
        assertEquals("#EBE4D3".toColorInt(), cream.background)
        assertEquals("#C32021".toColorInt(), cream.primary)
        assertEquals(Color.WHITE, cream.onPrimary)

        val matte = ThemeStyles.resolveArgbPalette("matte_black", "#FFFFFF", "#000000")
        assertTrue(matte.isDark)
        assertEquals(Color.WHITE, matte.textPrimary)
    }

    @Test
    fun sampleTrackerSeederGeneratesRealistic45DayWindowWithOrganicGapsMoodsAndNotes() = runBlocking {
        val today = LocalDate.of(2026, 11, 14)
        val deterministicEntries = SampleTrackerSeeder.buildSampleEntries(today, Random(42))

        val distinctDates = deterministicEntries.map { it.dateString }.distinct()
        assertTrue("Expected organic coverage in 28..44 days, got ${distinctDates.size}", distinctDates.size in 28..44)
        assertEquals(today.minusDays(45).toString(), distinctDates.first())
        assertEquals(today.minusDays(1).toString(), distinctDates.last())
        // Recent 4 days preserved for live streak
        for (d in 1L..4L) {
            assertTrue(distinctDates.contains(today.minusDays(d).toString()))
        }
        assertTrue(deterministicEntries.all { !it.notes.isNullOrBlank() && it.notes!!.contains("[MOOD:") })
        assertTrue(deterministicEntries.any { it.count > 1 })

        // Trigger fresh DB onCreate
        val db = AppDatabase.getDatabase(context)
        val trackers = db.trackerDao().getAllTrackersList()
        assertTrue(trackers.any { it.id == SampleTrackerSeeder.SAMPLE_TRACKER_ID && it.title == "Morning Walk" })
        assertEquals("revolutionary_cream", db.habitDao().getPreferenceValue("selected_theme"))
        assertEquals("space_grotesk", db.habitDao().getPreferenceValue("selected_font"))
        assertEquals(SampleTrackerSeeder.SAMPLE_TRACKER_ID, db.habitDao().getPreferenceValue("selected_tracker_id"))

        val seededSampleEntries = db.habitDao().getEntriesForTrackerFlow(SampleTrackerSeeder.SAMPLE_TRACKER_ID).first()
        val seededDaysCount = seededSampleEntries.map { it.dateString }.distinct().size
        assertTrue("Seeded days should reflect 45-day realistic window, got $seededDaysCount", seededDaysCount in 26..45)
    }

    @Test
    fun widgetFollowsSelectedAppThemeAndGeneratesSizedBitmaps() = runBlocking {
        val bitmap = HabitAppWidgetProvider.createRoundedRectBitmap(200, 80, 16f, Color.RED, Color.BLACK, 2f)
        assertEquals(200, bitmap.width)
        assertEquals(80, bitmap.height)

        val prog = HabitAppWidgetProvider.createProgressBarBitmap(160, 12, 50, Color.GRAY, Color.BLUE)
        assertEquals(160, prog.width)
        assertEquals(12, prog.height)

        val manager = AppWidgetManager.getInstance(context)
        val shadowManager = Shadows.shadowOf(manager)
        val widgetId = shadowManager.createWidget(HabitAppWidgetProvider::class.java, R.layout.widget_habit_today)
        kotlinx.coroutines.delay(250)

        val db = AppDatabase.getDatabase(context)
        db.trackerDao().deleteAllTrackers()
        db.trackerDao().insertTracker(Tracker(id = "walk", title = "Walk", icon = "🌱"))

        // Light Cream theme -> dark espresso title text (#131311)
        db.habitDao().insertPreference(PreferenceEntry("selected_theme", "revolutionary_cream"))
        HabitAppWidgetProvider.performWidgetUpdate(context, manager, intArrayOf(widgetId), db, LocalDateTime.of(2026, 11, 14, 10, 0))
        val creamView = shadowManager.getViewFor(widgetId)
        val creamTitle = creamView.findViewById<TextView>(R.id.widget_habit_title)
        assertEquals("#131311".toColorInt(), creamTitle.currentTextColor)

        // Dark theme -> crisp white title text (#FFFFFF)
        db.habitDao().insertPreference(PreferenceEntry("selected_theme", "matte_black"))
        HabitAppWidgetProvider.performWidgetUpdate(context, manager, intArrayOf(widgetId), db, LocalDateTime.of(2026, 11, 14, 10, 0))
        val darkView = shadowManager.getViewFor(widgetId)
        val darkTitle = darkView.findViewById<TextView>(R.id.widget_habit_title)
        assertEquals(Color.WHITE, darkTitle.currentTextColor)
    }

    @Test
    fun heatmapGridModelAlignsWeekColumnsMonthHeadersAndPeriodStats() {
        val today = LocalDate.of(2026, 11, 14)
        val tracker = TrackerConfig(id = "walk", title = "Walk", icon = "🌱", targetCount = 1)
        val entries = listOf(
            HabitEntry(dateString = "2025-06-10", count = 2, trackerId = "walk"),
            HabitEntry(dateString = "2026-11-11", count = 1, trackerId = "walk"),
            HabitEntry(dateString = "2026-11-12", count = 3, trackerId = "walk"),
            HabitEntry(dateString = "2026-11-13", count = 1, trackerId = "walk")
        )

        val periods = buildAvailableHeatmapPeriods(entries, today)
        assertEquals(HEATMAP_PERIOD_ROLLING_12M, periods.first().first)
        assertTrue(periods.any { it.first == "2026" })
        assertTrue(periods.any { it.first == "2025" })

        val rollingModel = buildHeatmapGridModel(entries, tracker, null, "", HEATMAP_PERIOD_ROLLING_12M, today)
        assertEquals(DayOfWeek.SUNDAY, rollingModel.gridStart.dayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, rollingModel.gridEnd.dayOfWeek)
        assertTrue(rollingModel.weeks in 52..54)
        assertEquals(3, rollingModel.summary.activeDays)
        assertEquals(5, rollingModel.summary.totalSignals)
        assertEquals(3, rollingModel.summary.bestStreak)
        assertEquals("Nov 12 · 3", rollingModel.summary.bestDayLabel)
        assertTrue(rollingModel.monthHeaders.isNotEmpty())
        assertTrue(rollingModel.monthHeaders.all { (w, _) -> w in 0 until rollingModel.weeks })

        val year2025Model = buildHeatmapGridModel(entries, tracker, null, "", "2025", today)
        assertEquals(LocalDate.of(2025, 1, 1), year2025Model.rangeStart)
        assertEquals(LocalDate.of(2025, 12, 31), year2025Model.rangeEnd)
        assertEquals(1, year2025Model.summary.activeDays)
        assertEquals(2, year2025Model.summary.totalSignals)
        assertEquals("Jun 10 · 2", year2025Model.summary.bestDayLabel)
    }

    @Test
    fun supporterVaultFreezeUpdatesPreferencesAndPreventsDoubleFreeze() {
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.habitDao().insertPreference(PreferenceEntry("is_supporter", "true"))
            db.habitDao().insertPreference(PreferenceEntry("supporter_vault_freezes", "3"))
        }

        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        var firstDone = false
        var firstOk = false
        viewModel.useVaultFreeze { ok, _ ->
            firstOk = ok
            firstDone = true
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            firstDone
        }
        assertTrue(firstOk)
        runBlocking {
            assertEquals("2", db.habitDao().getPreferenceValue("supporter_vault_freezes"))
            val todayStr = viewModel.getEffectiveToday().toString()
            assertEquals(todayStr, db.habitDao().getPreferenceValue("streak_freeze_date"))
        }

        // Second freeze on the exact same day is rejected without burning another freeze
        var secondDone = false
        var secondOk = true
        viewModel.useVaultFreeze { ok, _ ->
            secondOk = ok
            secondDone = true
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            secondDone
        }
        assertFalse(secondOk)
        runBlocking {
            assertEquals("2", db.habitDao().getPreferenceValue("supporter_vault_freezes"))
        }

        viewModel.refillVaultFreezes()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            runBlocking { db.habitDao().getPreferenceValue("supporter_vault_freezes") == "5" }
        }
    }

    @Test
    fun composeDonateModalActiveSupporterVaultRendersInteractiveControls() {
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            db.habitDao().insertPreference(PreferenceEntry("is_supporter", "true"))
            db.habitDao().insertPreference(PreferenceEntry("supporter_vault_freezes", "2"))
            db.habitDao().insertPreference(PreferenceEntry("streak_freeze_date", ""))
        }
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            DonateModal(viewModel = viewModel, onDismiss = {})
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithText("Supporter Edition Active!").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("🛡️ Vault Freezes: 2 available"))
        composeTestRule.onNodeWithText("🛡️ Vault Freezes: 2 available").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("use_vault_freeze_button"))
        composeTestRule.onNodeWithTag("use_vault_freeze_button").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("refill_vault_button"))
        composeTestRule.onNodeWithTag("refill_vault_button").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("Supporter Emblem Badge"))
        composeTestRule.onNodeWithText("Supporter Emblem Badge").assertIsDisplayed()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("refill_vault_button"))
        composeTestRule.onNodeWithTag("refill_vault_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("Vault refilled to 5 freezes."))
        composeTestRule.onNodeWithText("Vault refilled to 5 freezes.").assertIsDisplayed()
    }

    @Test
    fun customThemeLuminanceContrastAndCategorizedGlassThemesWork() {
        assertFalse(ThemeStyles.isLightTheme("custom", "#121318"))
        assertEquals(androidx.compose.ui.graphics.Color.White, ThemeStyles.getTextColor("custom", "#121318"))
        assertTrue(ThemeStyles.isLightTheme("custom", "#F4EFE6"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF141414), ThemeStyles.getTextColor("custom", "#F4EFE6"))

        val validCategoryIds = ThemeData.CATEGORIES.map { it.id }.toSet()
        assertTrue(ThemeData.ALL_THEMES.all { it.category in validCategoryIds })

        val glassThemes = ThemeData.ALL_THEMES.filter { it.category == "glass" }
        assertTrue("Expected at least 5 Glass & Liquid themes", glassThemes.size >= 5)
        assertTrue(glassThemes.all { it.isGlass })

        val aurora = "liquid_aurora_glass"
        assertTrue(ThemeStyles.isGlassTheme(aurora))
        assertTrue(ThemeStyles.getCardBackgroundColor(aurora).alpha < 0.9f)
        assertTrue(ThemeStyles.getOpaqueCardColor(aurora).alpha >= 0.95f)
    }

    @Test
    fun demoTrackerBannerDismissAndClearLifecycleWorks() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() } // Ensure onCreate seed runs

        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        val themeConfig = HomeThemeConfig(
            selectedTheme = "frosted_glacier",
            bgColor = ThemeStyles.getBackgroundColor("frosted_glacier"),
            primaryColor = ThemeStyles.getPrimaryColor("frosted_glacier"),
            cardBgColor = ThemeStyles.getCardBackgroundColor("frosted_glacier"),
            borderColor = ThemeStyles.getBorderColor("frosted_glacier"),
            textColor = ThemeStyles.getTextColor("frosted_glacier"),
            secondaryTextColor = ThemeStyles.getSecondaryTextColor("frosted_glacier"),
            appFont = FontFamily.Default,
            cellShape = RoundedCornerShape(10),
            checkmarkStyleName = "tick",
            modalBgColor = ThemeStyles.getOpaqueCardColor("frosted_glacier")
        )

        composeTestRule.setContent {
            val isDemoLoaded = viewModel.isDemoTrackerLoaded.collectAsState().value
            val showBanner = viewModel.showDemoBanner.collectAsState().value
            val trackersList = viewModel.trackers.collectAsState().value
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(
                    "demo=$isDemoLoaded;hasSample=${trackersList.any { it.id == SampleTrackerSeeder.SAMPLE_TRACKER_ID }}"
                )
                if (showBanner) {
                    DemoTrackerBannerCard(
                        themeConfig = themeConfig,
                        onDismiss = { viewModel.dismissDemoBanner() },
                        onClearDemo = { viewModel.clearDemoTracker() }
                    )
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("demo_tracker_banner").assertIsDisplayed()
                composeTestRule.onNodeWithText("demo=true;hasSample=true").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithTag("demo_dismiss_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("demo_tracker_banner").assertDoesNotExist()
                composeTestRule.onNodeWithText("demo=true;hasSample=true").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        viewModel.clearDemoTracker()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithText("demo=false;hasSample=false").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        viewModel.loadDemoTracker()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("demo_tracker_banner").assertIsDisplayed()
                composeTestRule.onNodeWithText("demo=true;hasSample=true").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun composeDemoTrackerBannerAndThemePickerCategoriesRenderCleanly() {
        var cleared = false
        var dismissed = false
        val themeConfig = HomeThemeConfig(
            selectedTheme = "frosted_glacier",
            bgColor = ThemeStyles.getBackgroundColor("frosted_glacier"),
            primaryColor = ThemeStyles.getPrimaryColor("frosted_glacier"),
            cardBgColor = ThemeStyles.getCardBackgroundColor("frosted_glacier"),
            borderColor = ThemeStyles.getBorderColor("frosted_glacier"),
            textColor = ThemeStyles.getTextColor("frosted_glacier"),
            secondaryTextColor = ThemeStyles.getSecondaryTextColor("frosted_glacier"),
            appFont = FontFamily.Default,
            cellShape = RoundedCornerShape(10),
            checkmarkStyleName = "tick",
            modalBgColor = ThemeStyles.getOpaqueCardColor("frosted_glacier")
        )

        composeTestRule.setContent {
            DemoTrackerBannerCard(
                themeConfig = themeConfig,
                onDismiss = { dismissed = true },
                onClearDemo = { cleared = true }
            )
        }

        composeTestRule.onNodeWithTag("demo_tracker_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample Demo Tracker Loaded").assertIsDisplayed()
        composeTestRule.onNodeWithTag("demo_clear_button").performClick()
        composeTestRule.onNodeWithTag("demo_dismiss_button").performClick()
        assertTrue(cleared)
        assertTrue(dismissed)
    }

    @Test
    fun composeThemePickerShowsCategoriesAndFiltersGlassThemes() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            ThemePickerModal(viewModel = viewModel, onClose = {}, onOpenDonate = {})
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("theme_filter_glass").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithTag("theme_filter_glass").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_picker_grid").performScrollToNode(hasTestTag("theme_section_glass"))
        composeTestRule.onNodeWithTag("theme_section_glass").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_picker_grid").performScrollToNode(hasTestTag("theme_card_liquid_aurora_glass"))
        composeTestRule.onNodeWithTag("theme_card_liquid_aurora_glass").assertIsDisplayed()
    }

    @Test
    fun diamondCellShapeAndElementalShaderThemesWorkCleanly() {
        val diamondShape = ThemeStyles.getCalendarCellShape("diamond")
        assertTrue(diamondShape !== CircleShape)
        assertTrue(diamondShape !== ThemeStyles.getCalendarCellShape("rounded_square"))
        assertTrue(isCurvedCalendarShape("diamond"))

        assertTrue(ThemeData.CATEGORIES.any { it.id == "elemental" })
        val elementalThemes = ThemeData.ALL_THEMES.filter { it.category == "elemental" }
        assertTrue("Expected frozen_liquid theme", elementalThemes.any { it.effectStyle == "frozen_liquid" })
        assertTrue("Expected volcano_magma theme", elementalThemes.any { it.effectStyle == "volcano_magma" })
        assertTrue(elementalThemes.all { it.isGlass })

        val glacier = ThemeStyles.getAppTheme("cryo_glacier_ice")
        assertEquals("frozen_liquid", glacier.effectStyle)
        assertTrue(ThemeStyles.getCardBackgroundColor(glacier.id).alpha < 0.9f)
        assertTrue(ThemeStyles.getOpaqueCardColor(glacier.id).alpha >= 0.95f)

        val magma = ThemeStyles.getAppTheme("volcanic_magma_core")
        assertEquals("volcano_magma", magma.effectStyle)
        assertTrue(ThemeStyles.getCardBackgroundColor(magma.id).alpha < 0.9f)
    }

    @Test
    fun composeThemeStudioFiltersElementalAndUpdatesHeroPreview() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            ThemePickerModal(viewModel = viewModel, onClose = {}, onOpenDonate = {})
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("theme_hero_preview").assertIsDisplayed()
                composeTestRule.onNodeWithTag("theme_filter_elemental").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithTag("theme_filter_elemental").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_picker_grid").performScrollToNode(hasTestTag("theme_section_elemental"))
        composeTestRule.onNodeWithTag("theme_section_elemental").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_picker_grid").performScrollToNode(hasTestTag("theme_card_cryo_glacier_ice"))
        composeTestRule.onNodeWithTag("theme_card_cryo_glacier_ice").assertIsDisplayed()

        composeTestRule.onNodeWithTag("theme_card_cryo_glacier_ice").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_picker_grid").performScrollToNode(hasTestTag("theme_hero_preview"))
        composeTestRule.onNodeWithTag("theme_hero_preview").assertIsDisplayed()
    }

    @Test
    fun favoriteThemesPersistAndFilterInThemeStudio() {
        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        composeTestRule.setContent {
            ThemePickerModal(viewModel = viewModel, onClose = {}, onOpenDonate = {})
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("theme_filter_favorites").assertIsDisplayed()
                composeTestRule.onNodeWithTag("hero_favorite_button").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        // Toggle favorite via card heart button on revolutionary_cream
        composeTestRule.onNodeWithTag("theme_picker_grid")
            .performScrollToNode(hasTestTag("theme_favorite_revolutionary_cream"))
        composeTestRule.onNodeWithTag("theme_favorite_revolutionary_cream").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            viewModel.favoriteThemes.value.contains("revolutionary_cream") &&
                runBlocking {
                    db.habitDao().getPreferenceValue("favorite_themes")?.contains("revolutionary_cream") == true
                }
        }

        // Filter to Favorites tab -> shows favorites section & pinned theme card
        composeTestRule.onNodeWithTag("theme_filter_favorites").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_picker_grid")
            .performScrollToNode(hasTestTag("theme_section_favorites"))
        composeTestRule.onNodeWithTag("theme_section_favorites").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_picker_grid")
            .performScrollToNode(hasTestTag("theme_card_revolutionary_cream"))
        composeTestRule.onNodeWithTag("theme_card_revolutionary_cream").assertIsDisplayed()

        // Unfavorite via Hero Preview heart button -> empty state renders
        composeTestRule.onNodeWithTag("theme_picker_grid")
            .performScrollToNode(hasTestTag("hero_favorite_button"))
        composeTestRule.onNodeWithTag("hero_favorite_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            !viewModel.favoriteThemes.value.contains("revolutionary_cream")
        }
        composeTestRule.onNodeWithTag("theme_picker_grid")
            .performScrollToNode(hasTestTag("favorites_empty_state"))
        composeTestRule.onNodeWithTag("favorites_empty_state").assertIsDisplayed()
    }

    @Test
    fun curatedFontPackExcludesGenericFontsAndResolvesFallbacks() {
        ThemeStyles.init(context)
        val expectedKeys = listOf(
            "space_grotesk",
            "plus_jakarta_sans",
            "outfit",
            "inter",
            "sora",
            "jetbrains_mono",
            "serif",
            "default"
        )
        assertEquals(expectedKeys, ThemeStyles.FONT_OPTIONS.map { it.first })

        val retiredKeys = listOf("comfortaa", "nunito", "cursive", "monospace", "sans_serif")
        retiredKeys.forEach { retired ->
            assertFalse("Retired font $retired should not be in FONT_OPTIONS", expectedKeys.contains(retired))
            assertNotNull(ThemeStyles.getSelectedFontFamily(retired))
        }

        assertEquals("Plus Jakarta Sans", ThemeStyles.getFontDisplayName("plus_jakarta_sans"))
        assertEquals("Outfit", ThemeStyles.getFontDisplayName("outfit"))
        assertEquals("Inter", ThemeStyles.getFontDisplayName("inter"))
        assertEquals("Sora", ThemeStyles.getFontDisplayName("sora"))
        assertEquals("Space Grotesk", ThemeStyles.getFontDisplayName("comfortaa"))
        expectedKeys.forEach { key ->
            assertNotNull(ThemeStyles.getSelectedFontFamily(key))
        }

        val db = AppDatabase.getDatabase(context)
        runBlocking { db.trackerDao().getAllTrackersList() }
        val viewModel = createTestViewModel()
        runBlocking { viewModel.awaitInitialized() }

        composeTestRule.setContent {
            FontPickerModal(viewModel = viewModel, onClose = {})
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            try {
                composeTestRule.onNodeWithTag("font_specimen_preview").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        composeTestRule.onNodeWithTag("font_picker_list")
            .performScrollToNode(hasTestTag("font_row_plus_jakarta_sans"))
        composeTestRule.onNodeWithTag("font_row_plus_jakarta_sans").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            viewModel.selectedFont.value == "plus_jakarta_sans"
        }

        composeTestRule.onNodeWithTag("font_picker_list")
            .performScrollToNode(hasTestTag("font_row_outfit"))
        composeTestRule.onNodeWithTag("font_row_outfit").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            viewModel.selectedFont.value == "outfit"
        }
    }

    @Test
    fun backdropAnd3DSurfacesRenderOnElementalAndCreamThemes() {
        val themes = listOf("cryo_glacier_ice", "volcanic_magma_core", "revolutionary_cream")
        composeTestRule.setContent {
            Column {
                themes.forEach { themeId ->
                    val appTheme = ThemeStyles.getAppTheme(themeId)
                    val isLight = ThemeStyles.isLightTheme(themeId)
                    LiquidAmbientBackdrop(
                        selectedTheme = themeId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .surface3DCard(
                                    shape = RoundedCornerShape(12.dp),
                                    isLight = isLight,
                                    primaryColor = appTheme.primary,
                                    isGlass = appTheme.isGlass,
                                    elevationDp = 6.dp
                                )
                                .recessedWell(
                                    shape = RoundedCornerShape(8.dp),
                                    isLight = isLight
                                )
                        ) {
                            ThemeEffectPreviewCanvas(
                                theme = appTheme,
                                animated = false,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Text("3D_BACKDROP_OK")
            }
        }

        composeTestRule.onNodeWithText("3D_BACKDROP_OK").assertIsDisplayed()
    }

    @Test
    fun multiHabitWidgetRendersRingStreakStripAndChecklistCheckIn() = runBlocking {
        val manager = AppWidgetManager.getInstance(context)
        val shadowManager = Shadows.shadowOf(manager)
        val widgetId = shadowManager.createWidget(HabitAppWidgetProvider::class.java, R.layout.widget_habit_today)
        kotlinx.coroutines.delay(250)

        val db = AppDatabase.getDatabase(context)
        db.trackerDao().deleteAllTrackers()
        db.habitDao().deleteAllHabitEntries()

        val walk = Tracker(id = "habit_walk", title = "Morning Walk", icon = "🚶", type = "good", targetCount = 1, sortOrder = 0)
        val water = Tracker(id = "habit_water", title = "Drink Water", icon = "💧", type = "good", targetCount = 2, sortOrder = 1)
        val read = Tracker(id = "habit_read", title = "Read 15m", icon = "📚", type = "good", targetCount = 1, sortOrder = 2)
        val sugar = Tracker(id = "no_sugar", title = "No Sugar", icon = "🍬", type = "bad", sortOrder = 3)
        db.trackerDao().insertTracker(walk)
        db.trackerDao().insertTracker(water)
        db.trackerDao().insertTracker(read)
        db.trackerDao().insertTracker(sugar)

        val now = LocalDateTime.of(2026, 11, 14, 10, 0)
        val today = now.toLocalDate()
        db.habitDao().insertEntry(HabitEntry(dateString = today.minusDays(2).toString(), count = 2, trackerId = "habit_water"))
        db.habitDao().insertEntry(HabitEntry(dateString = today.toString(), count = 1, trackerId = "habit_water"))
        db.habitDao().insertPreference(PreferenceEntry("streak_freeze_date", today.minusDays(1).toString()))
        HabitAppWidgetProvider.setSelectedWidgetTrackerId(context, "habit_walk")

        val slots = HabitAppWidgetProvider.buildSevenDaySlots(
            tracker = water,
            trackerEntries = db.habitDao().getEntriesForTrackerFlow("habit_water").first(),
            freezeActiveDate = today.minusDays(1).toString(),
            freezeHistory = "",
            effectiveToday = today
        )
        assertEquals(7, slots.size)
        assertEquals(WidgetDayState.DONE, slots[4].state)
        assertEquals(WidgetDayState.FROZEN, slots[5].state)
        assertEquals(WidgetDayState.PARTIAL, slots[6].state)
        assertTrue(slots[6].isToday)

        val ringBmp = HabitAppWidgetProvider.createTodayRingBitmap(72, 65, Color.GRAY, Color.RED, Color.BLACK)
        assertEquals(72, ringBmp.width)
        assertEquals(72, ringBmp.height)
        val stripBmp = HabitAppWidgetProvider.createSevenDayStripBitmap(
            widthPx = 400,
            heightPx = 48,
            slots = slots,
            capsuleFill = Color.DKGRAY,
            capsuleStroke = Color.LTGRAY,
            primaryColor = Color.CYAN,
            textColor = Color.WHITE,
            secondaryTextColor = Color.LTGRAY,
            isDark = true
        )
        assertEquals(400, stripBmp.width)
        assertEquals(48, stripBmp.height)

        // Direct per-row check-in for habit_read without mutating selected hero tracker
        val directReadOk = HabitAppWidgetProvider.handleWidgetCheckIn(context, db, now = now, trackerId = "habit_read")
        assertTrue(directReadOk)
        assertEquals(1, db.habitDao().getEntriesForTrackerAndDate("habit_read", today.toString()).sumOf { it.count })
        assertEquals("habit_walk", HabitAppWidgetProvider.getSelectedWidgetTrackerId(context))

        // Safety: explicit trackerId on a bad habit still refuses silent slip check-in
        val directBadOk = HabitAppWidgetProvider.handleWidgetCheckIn(context, db, now = now, trackerId = "no_sugar")
        assertFalse(directBadOk)

        // Full 4x3 widget render shows Today Ring, 7-Day Strip, Hero & Multi-Habit Checklist rows
        HabitAppWidgetProvider.performWidgetUpdate(context, manager, intArrayOf(widgetId), db, now)
        val view = shadowManager.getViewFor(widgetId)
        assertEquals("Morning Walk", view.findViewById<TextView>(R.id.widget_habit_title).text.toString())
        assertEquals(View.VISIBLE, view.findViewById<ImageView>(R.id.widget_today_ring).visibility)
        assertEquals(View.VISIBLE, view.findViewById<ImageView>(R.id.widget_streak_strip).visibility)
        assertEquals(View.VISIBLE, view.findViewById<LinearLayout>(R.id.widget_checklist_container).visibility)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_row_0).visibility)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_row_1).visibility)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_row_2).visibility)
        assertEquals("Drink Water", view.findViewById<TextView>(R.id.widget_row_0_title).text.toString())
        assertEquals("1/2", view.findViewById<TextView>(R.id.widget_row_0_meta).text.toString())

        // Compact 3x2 resize hides checklist rows cleanly
        val compactOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        }
        manager.updateAppWidgetOptions(widgetId, compactOptions)
        HabitAppWidgetProvider.performWidgetUpdate(context, manager, intArrayOf(widgetId), db, now)
        val compactView = shadowManager.getViewFor(widgetId)
        assertEquals(View.GONE, compactView.findViewById<LinearLayout>(R.id.widget_checklist_container).visibility)
    }

    @Test
    fun uniformSheenSurfaceModifiersRenderAcrossAllCellShapesCleanly() {
        val shapes = listOf(
            CircleShape,
            RoundedCornerShape(12.dp),
            ThemeStyles.getCalendarCellShape("diamond")
        )
        composeTestRule.setContent {
            Column {
                shapes.forEach { shape ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .surface3DCard(
                                shape = shape,
                                isLight = false,
                                primaryColor = androidx.compose.ui.graphics.Color.Cyan,
                                isGlass = true,
                                elevationDp = 6.dp
                            )
                            .completedCellCornerGlow(androidx.compose.ui.graphics.Color.Cyan)
                            .recessedWell(
                                shape = shape,
                                isLight = false
                            )
                    )
                }
                Text("UNIFORM_SHEEN_OK")
            }
        }
        composeTestRule.onNodeWithText("UNIFORM_SHEEN_OK").assertIsDisplayed()
    }
}
