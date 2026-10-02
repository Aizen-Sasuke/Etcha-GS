package com.example

import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.TrackerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitBusinessLogicTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun `daily habit streak increments on consecutive days`() {
        // Monday through Wednesday
        val today = LocalDate.of(2026, 10, 7) // Wednesday
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-05", count = 1, trackerId = "reading"),
            HabitEntry(id = 2, dateString = "2026-10-06", count = 1, trackerId = "reading"),
            HabitEntry(id = 3, dateString = "2026-10-07", count = 1, trackerId = "reading")
        )
        val tracker = TrackerConfig(
            id = "reading",
            title = "Reading",
            icon = "📚",
            frequencyType = "daily",
            targetDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, freezeActive = null, freezeHist = "", tracker = tracker, today = today)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun `weekday habit preserves streak over the weekend`() {
        // Friday completed, today is Monday (not completed yet)
        val friday = LocalDate.of(2026, 10, 2) // Friday
        val monday = LocalDate.of(2026, 10, 5) // Monday
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-01", count = 1, trackerId = "gym"), // Thursday
            HabitEntry(id = 2, dateString = "2026-10-02", count = 1, trackerId = "gym")  // Friday
        )
        val tracker = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekdays",
            targetDays = "MON,TUE,WED,THU,FRI",
            targetCount = 1
        )

        // Monday morning before workout: current streak from Friday should be preserved (2 days)
        val statsBeforeMonday = invokeCalculateStats(entries, null, "", tracker, monday)
        assertEquals(2, statsBeforeMonday.currentStreak)

        // Monday after workout completed:
        val withMonday = entries + HabitEntry(id = 3, dateString = "2026-10-05", count = 1, trackerId = "gym")
        val statsAfterMonday = invokeCalculateStats(withMonday, null, "", tracker, monday)
        assertEquals(3, statsAfterMonday.currentStreak)
    }

    @Test
    fun `target count habit requires reaching target to complete streak`() {
        val today = LocalDate.of(2026, 10, 7)
        val tracker = TrackerConfig(
            id = "water",
            title = "Hydration",
            icon = "💧",
            frequencyType = "daily",
            targetDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
            targetCount = 8
        )

        // Only 4 cups logged today -> not completed
        val partialEntries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-06", count = 8, trackerId = "water"),
            HabitEntry(id = 2, dateString = "2026-10-07", count = 4, trackerId = "water")
        )
        val partialStats = invokeCalculateStats(partialEntries, null, "", tracker, today)
        // Today is not complete, but yesterday was 8/8 so streak through yesterday is 1
        assertEquals(1, partialStats.currentStreak)

        // Reached 8 cups (another entry of 4)
        val completeEntries = partialEntries + HabitEntry(id = 3, dateString = "2026-10-07", count = 4, trackerId = "water")
        val completeStats = invokeCalculateStats(completeEntries, null, "", tracker, today)
        assertEquals(2, completeStats.currentStreak)
    }

    @Test
    fun `streak freeze protects streak on missed day`() {
        val today = LocalDate.of(2026, 10, 7) // Wednesday
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-05", count = 1, trackerId = "meditation"), // Monday
            // Tuesday was missed!
            HabitEntry(id = 2, dateString = "2026-10-07", count = 1, trackerId = "meditation")  // Wednesday
        )
        val tracker = TrackerConfig(
            id = "meditation",
            title = "Meditation",
            icon = "🧘",
            frequencyType = "daily",
            targetDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
            targetCount = 1
        )

        // Without freeze: streak is broken by missing Tuesday
        val statsNoFreeze = invokeCalculateStats(entries, freezeActive = null, freezeHist = "", tracker = tracker, today = today)
        assertEquals(1, statsNoFreeze.currentStreak)

        // With Tuesday in freezeHist
        val statsWithFreeze = invokeCalculateStats(entries, freezeActive = null, freezeHist = "2026-10-06", tracker = tracker, today = today)
        assertEquals(3, statsWithFreeze.currentStreak)
    }

    @Test
    fun `bad habit streak calculates days clean since last slip`() {
        val today = LocalDate.of(2026, 10, 10)
        val tracker = TrackerConfig(
            id = "sobriety",
            title = "Sobriety",
            icon = "🚭",
            type = "bad",
            frequencyType = "daily",
            targetDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
            targetCount = 1
        )

        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-01", count = 1, trackerId = "sobriety"), // slip 1
            HabitEntry(id = 2, dateString = "2026-10-05", count = 1, trackerId = "sobriety")  // slip 2
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        // Days clean between Oct 5 and Oct 10 = 5 days
        assertEquals(5, stats.currentStreak)
    }

    // Helper to invoke calculation reflection or replica directly
    private fun invokeCalculateStats(
        list: List<HabitEntry>,
        freezeActive: String?,
        freezeHist: String,
        tracker: TrackerConfig,
        today: LocalDate
    ): com.example.ui.HabitStats {
        val method = HabitViewModel::class.java.getDeclaredMethod(
            "calculateStats",
            List::class.java,
            String::class.java,
            String::class.java,
            TrackerConfig::class.java
        )
        method.isAccessible = true

        // Subclass HabitViewModel to override getEffectiveToday
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = object : HabitViewModel(app) {
            override fun getEffectiveToday(): LocalDate = today
        }

        return method.invoke(vm, list, freezeActive, freezeHist, tracker) as com.example.ui.HabitStats
    }
}
