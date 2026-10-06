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

    @Test
    fun `calendar completion state semantics for targetCount greater than 1`() {
        val targetCount = 8

        fun evaluateCalendarState(totalSum: Int): Pair<Boolean, Boolean> {
            val isCompleted = totalSum >= targetCount
            val isPartial = totalSum in 1 until targetCount
            return Pair(isCompleted, isPartial)
        }

        // 0 of 8: neither completed nor partial
        val (c0, p0) = evaluateCalendarState(0)
        assertEquals(false, c0)
        assertEquals(false, p0)

        // 1 of 8: partial progress, not completed
        val (c1, p1) = evaluateCalendarState(1)
        assertEquals(false, c1)
        assertEquals(true, p1)

        // 3 of 8: partial progress, not completed
        val (c3, p3) = evaluateCalendarState(3)
        assertEquals(false, c3)
        assertEquals(true, p3)

        // 7 of 8: partial progress, not completed
        val (c7, p7) = evaluateCalendarState(7)
        assertEquals(false, c7)
        assertEquals(true, p7)

        // 8 of 8: full completed state, not partial
        val (c8, p8) = evaluateCalendarState(8)
        assertEquals(true, c8)
        assertEquals(false, p8)

        // 9 of 8 (exceeded): full completed state, not partial
        val (c9, p9) = evaluateCalendarState(9)
        assertEquals(true, c9)
        assertEquals(false, p9)
    }

    @Test
    fun `calendar completion state semantics for single count habit`() {
        val targetCount = 1

        fun evaluateCalendarState(totalSum: Int): Pair<Boolean, Boolean> {
            val isCompleted = totalSum >= targetCount
            val isPartial = totalSum in 1 until targetCount
            return Pair(isCompleted, isPartial)
        }

        // 0: neither completed nor partial
        val (c0, p0) = evaluateCalendarState(0)
        assertEquals(false, c0)
        assertEquals(false, p0)

        // 1: completed, not partial
        val (c1, p1) = evaluateCalendarState(1)
        assertEquals(true, c1)
        assertEquals(false, p1)

        // 2: completed, not partial
        val (c2, p2) = evaluateCalendarState(2)
        assertEquals(true, c2)
        assertEquals(false, p2)
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

    // ── Phase 2A: Flexible Weekly Targets Unit Tests ──

    @Test
    fun `weekly quota 3x per week completed across Mon Wed Fri`() {
        // Week 1: 2026-10-05 (Mon), 2026-10-07 (Wed), 2026-10-09 (Fri)
        // Today is Sunday 2026-10-11
        val today = LocalDate.of(2026, 10, 11)
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-05", count = 1, trackerId = "gym"),
            HabitEntry(id = 2, dateString = "2026-10-07", count = 1, trackerId = "gym"),
            HabitEntry(id = 3, dateString = "2026-10-09", count = 1, trackerId = "gym")
        )
        val tracker = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.bestStreak)
    }

    @Test
    fun `weekly quota 3x per week completed Tue Thu Sat`() {
        // Week 1: 2026-10-06 (Tue), 2026-10-08 (Thu), 2026-10-10 (Sat)
        val today = LocalDate.of(2026, 10, 11) // Sunday
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-06", count = 1, trackerId = "gym"),
            HabitEntry(id = 2, dateString = "2026-10-08", count = 1, trackerId = "gym"),
            HabitEntry(id = 3, dateString = "2026-10-10", count = 1, trackerId = "gym")
        )
        val tracker = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.bestStreak)
    }

    @Test
    fun `weekly quota 3x per week missed with only 2 completed days`() {
        // Week 1: Mon 2026-10-05, Wed 2026-10-07 only (2 days out of 3)
        // Today is Monday of Week 2 (2026-10-12)
        val today = LocalDate.of(2026, 10, 12)
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-05", count = 1, trackerId = "gym"),
            HabitEntry(id = 2, dateString = "2026-10-07", count = 1, trackerId = "gym")
        )
        val tracker = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        // Week 1 missed quota, so currentStreak is 0
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
    }

    @Test
    fun `current week incomplete does not prematurely break previous streak`() {
        // Week 1: 2026-10-05 (Mon), 2026-10-07 (Wed), 2026-10-09 (Fri) -> Completed week 1
        // Week 2: 2026-10-12 (Mon), 2026-10-14 (Wed), 2026-10-16 (Fri) -> Completed week 2
        // Week 3: Today is Wednesday 2026-10-21, user only logged Monday 2026-10-19 (1/3 days)
        val today = LocalDate.of(2026, 10, 21) // Wednesday of week 3
        val entries = listOf(
            // Week 1
            HabitEntry(id = 1, dateString = "2026-10-05", count = 1, trackerId = "gym"),
            HabitEntry(id = 2, dateString = "2026-10-07", count = 1, trackerId = "gym"),
            HabitEntry(id = 3, dateString = "2026-10-09", count = 1, trackerId = "gym"),
            // Week 2
            HabitEntry(id = 4, dateString = "2026-10-12", count = 1, trackerId = "gym"),
            HabitEntry(id = 5, dateString = "2026-10-14", count = 1, trackerId = "gym"),
            HabitEntry(id = 6, dateString = "2026-10-16", count = 1, trackerId = "gym"),
            // Week 3 (in progress)
            HabitEntry(id = 7, dateString = "2026-10-19", count = 1, trackerId = "gym")
        )
        val tracker = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        // Week 1 and Week 2 were completed -> Streak must be 2 weeks (not broken by in-progress week 3)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.bestStreak)

        // Once Week 3 completes 3rd day on Friday Oct 23:
        val withWeek3Done = entries + listOf(
            HabitEntry(id = 8, dateString = "2026-10-21", count = 1, trackerId = "gym"),
            HabitEntry(id = 9, dateString = "2026-10-23", count = 1, trackerId = "gym")
        )
        val statsWeek3Done = invokeCalculateStats(withWeek3Done, null, "", tracker, today)
        assertEquals(3, statsWeek3Done.currentStreak)
        assertEquals(3, statsWeek3Done.bestStreak)
    }

    @Test
    fun `sunday boundary behavior for weekly quota`() {
        // Week ends on Sunday.
        // User logs on Friday, Saturday, and Sunday 2026-10-11
        val sunday = LocalDate.of(2026, 10, 11)
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-09", count = 1, trackerId = "gym"), // Fri
            HabitEntry(id = 2, dateString = "2026-10-10", count = 1, trackerId = "gym"), // Sat
            HabitEntry(id = 3, dateString = "2026-10-11", count = 1, trackerId = "gym")  // Sun
        )
        val tracker = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1
        )

        // On Sunday with 3 logs -> Current week is completed (streak = 1)
        val statsSunday = invokeCalculateStats(entries, null, "", tracker, sunday)
        assertEquals(1, statsSunday.currentStreak)

        // On Next Monday 2026-10-12 morning -> Previous week is preserved (streak = 1)
        val monday = LocalDate.of(2026, 10, 12)
        val statsMonday = invokeCalculateStats(entries, null, "", tracker, monday)
        assertEquals(1, statsMonday.currentStreak)
    }

    @Test
    fun `5x per week weekly quota`() {
        val today = LocalDate.of(2026, 10, 11) // Sunday
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-05", count = 1, trackerId = "study"), // Mon
            HabitEntry(id = 2, dateString = "2026-10-06", count = 1, trackerId = "study"), // Tue
            HabitEntry(id = 3, dateString = "2026-10-07", count = 1, trackerId = "study"), // Wed
            HabitEntry(id = 4, dateString = "2026-10-08", count = 1, trackerId = "study"), // Thu
            HabitEntry(id = 5, dateString = "2026-10-09", count = 1, trackerId = "study")  // Fri
        )
        val tracker = TrackerConfig(
            id = "study",
            title = "Study",
            icon = "📖",
            frequencyType = "weekly_quota",
            weeklyTarget = 5,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `1x per week weekly quota`() {
        val today = LocalDate.of(2026, 10, 11) // Sunday
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-07", count = 1, trackerId = "laundry") // Wed
        )
        val tracker = TrackerConfig(
            id = "laundry",
            title = "Laundry",
            icon = "🧺",
            frequencyType = "weekly_quota",
            weeklyTarget = 1,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `7x per week weekly quota`() {
        val today = LocalDate.of(2026, 10, 11) // Sunday
        val entries = (0..6).map { d ->
            val date = LocalDate.of(2026, 10, 5).plusDays(d.toLong()).toString()
            HabitEntry(id = d + 1, dateString = date, count = 1, trackerId = "meditation")
        }
        val tracker = TrackerConfig(
            id = "meditation",
            title = "Meditation",
            icon = "🧘",
            frequencyType = "weekly_quota",
            weeklyTarget = 7,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `multiple logs on one day count as ONE weekly day`() {
        val today = LocalDate.of(2026, 10, 11) // Sunday
        // User logs 10 times on Monday, but only Monday!
        val entries = (1..10).map { i ->
            HabitEntry(id = i, dateString = "2026-10-05", count = 1, trackerId = "gym")
        }
        val tracker = TrackerConfig(
            id = "gym",
            title = "Gym",
            icon = "🏋️",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1
        )

        val stats = invokeCalculateStats(entries, null, "", tracker, today)
        // 10 entries on Monday count as only 1 distinct completed day -> Quota of 3 not met
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun `multi-count habit must satisfy its daily target before counting toward weekly quota`() {
        val today = LocalDate.of(2026, 10, 11) // Sunday
        val tracker = TrackerConfig(
            id = "water",
            title = "Water",
            icon = "💧",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 8
        )

        val entries = listOf(
            // Monday: 8 cups (Complete)
            HabitEntry(id = 1, dateString = "2026-10-05", count = 8, trackerId = "water"),
            // Tuesday: only 4 cups (Incomplete daily target)
            HabitEntry(id = 2, dateString = "2026-10-06", count = 4, trackerId = "water"),
            // Wednesday: 8 cups (Complete)
            HabitEntry(id = 3, dateString = "2026-10-07", count = 8, trackerId = "water")
        )

        // Only 2 days met the daily target of 8 -> Quota of 3 is not met
        val statsIncomplete = invokeCalculateStats(entries, null, "", tracker, today)
        assertEquals(0, statsIncomplete.currentStreak)

        // Friday logs 8 cups -> now 3 days met the daily target
        val completeEntries = entries + HabitEntry(id = 4, dateString = "2026-10-09", count = 8, trackerId = "water")
        val statsComplete = invokeCalculateStats(completeEntries, null, "", tracker, today)
        assertEquals(1, statsComplete.currentStreak)
    }

    @Test
    fun `existing weekly fixed-day tracker still behaves exactly as before`() {
        // Target days: MON, WED, FRI
        val tracker = TrackerConfig(
            id = "gym_fixed",
            title = "Gym Fixed",
            icon = "🏋️",
            frequencyType = "weekly",
            targetDays = "MON,WED,FRI",
            targetCount = 1
        )

        // Mon 2026-10-05 and Wed 2026-10-07 completed
        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-05", count = 1, trackerId = "gym_fixed"),
            HabitEntry(id = 2, dateString = "2026-10-07", count = 1, trackerId = "gym_fixed")
        )

        // On Thursday 2026-10-08 (a rest day): streak should be 2
        val thursday = LocalDate.of(2026, 10, 8)
        val statsThursday = invokeCalculateStats(entries, null, "", tracker, thursday)
        assertEquals(2, statsThursday.currentStreak)

        // On Friday 2026-10-09 completed: streak becomes 3
        val fridayEntries = entries + HabitEntry(id = 3, dateString = "2026-10-09", count = 1, trackerId = "gym_fixed")
        val friday = LocalDate.of(2026, 10, 9)
        val statsFriday = invokeCalculateStats(fridayEntries, null, "", tracker, friday)
        assertEquals(3, statsFriday.currentStreak)
    }

    @Test
    fun `existing custom_days behavior remains unchanged`() {
        val tracker = TrackerConfig(
            id = "study_custom",
            title = "Study Custom",
            icon = "📚",
            frequencyType = "custom_days",
            targetDays = "TUE,THU,SAT",
            targetCount = 1
        )

        val entries = listOf(
            HabitEntry(id = 1, dateString = "2026-10-06", count = 1, trackerId = "study_custom"), // Tue
            HabitEntry(id = 2, dateString = "2026-10-08", count = 1, trackerId = "study_custom")  // Thu
        )

        // Friday 2026-10-09 is a rest day, streak from Thu should be 2
        val friday = LocalDate.of(2026, 10, 9)
        val stats = invokeCalculateStats(entries, null, "", tracker, friday)
        assertEquals(2, stats.currentStreak)
    }
}
