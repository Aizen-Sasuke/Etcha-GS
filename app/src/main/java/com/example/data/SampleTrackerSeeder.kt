package com.example.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ui.ThemeStyles
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Seeds a realistic first-launch demo habit ("Morning Walk") with randomized organic
 * check-in patterns over the past 45 days—including natural occasional skip days,
 * varied multi-log intensity, realistic jittered timestamps, moods, and journal reflections—
 * so every chart, calendar cell, and heatmap shade reflects real human habit rhythms.
 */
object SampleTrackerSeeder {
    const val SAMPLE_TRACKER_ID = "sample_mindful"
    const val SAMPLE_TRACKER_TITLE = "Morning Walk"
    const val SAMPLE_TRACKER_ICON = "🌱"
    const val SAMPLE_ACCENT_HEX = "#2E7D32"
    const val SAMPLE_DAYS_COUNT = 45

    private val SAMPLE_REFLECTIONS = listOf(
        "Brisk 20-minute loop through the park, fresh air cleared my head.",
        "Steady pace before breakfast — noticed higher focus all morning.",
        "Listened to an audiobook along the quiet avenue and stretched afterward.",
        "Felt energized right from the first block; kept shoulders relaxed.",
        "Slow, mindful stride watching the sunrise colors settle over the rooftops.",
        "Quick mid-morning reset between tasks — zero screen time for 25 minutes.",
        "Extended the route by two blocks; legs felt light and steady.",
        "Calm breathing cadence throughout the walk, great mood boost.",
        "Rain-cooled breeze outside made this the easiest check-in of the week.",
        "Walked with a bottle of water and jotted down three priorities for today.",
        "Took the hilly detour by the canal; heart rate felt great.",
        "Kept it short and gentle today, just focused on stepping outdoors.",
        "Grabbed coffee after finishing the loop — solid way to anchor the morning.",
        "Spotted herons by the pond, stayed out an extra ten minutes.",
        "Evening cooldown stroll instead of scrolling before bed.",
        "Cloudy sky, quiet streets, left my headphones at home on purpose."
    )

    fun buildSampleTracker(): Tracker = Tracker(
        id = SAMPLE_TRACKER_ID,
        title = SAMPLE_TRACKER_TITLE,
        icon = SAMPLE_TRACKER_ICON,
        sortOrder = 0,
        accentColor = SAMPLE_ACCENT_HEX,
        type = "good",
        frequencyType = "daily",
        targetDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
        targetCount = 1,
        timeOfDay = "morning",
        weeklyTarget = 0,
        isArchived = false,
        reminderEnabled = false,
        reminderTime = "08:00"
    )

    fun buildStarterTrackers(): List<Tracker> = listOf(
        buildSampleTracker(),
        Tracker("sobriety", "Sobriety", "🚭", 1, null, "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "anytime", 0, false),
        Tracker("gym", "Gym & Workout", "🏋️‍♂️", 2, "#FF5722", "good", "weekly", "MON,TUE,THU,FRI,SAT", 1, "morning", 0, false),
        Tracker("gardening", "Gardening", "🌿", 3, "#4CAF50", "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "afternoon", 0, false),
        Tracker("reading", "Daily Reading", "📚", 4, "#2196F3", "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "evening", 0, false),
        Tracker("water", "Hydration", "💧", 5, "#00BCD4", "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 8, "anytime", 0, false)
    )

    /**
     * Generates realistic randomized demo entries spanning the last 45 days (ending yesterday)
     * with natural 1-2 day gaps, varied log counts (1..3+), realistic mood distribution,
     * and non-mechanical time-of-day timestamps while preserving a live active streak into today.
     */
    fun buildSampleEntries(
        today: LocalDate = LocalDate.now(),
        random: Random = Random.Default
    ): List<HabitEntry> {
        val entries = mutableListOf<HabitEntry>()
        val zone = ZoneId.systemDefault()
        val recentNotes = SAMPLE_REFLECTIONS.shuffled(random)
        var noteCursor = 0
        var consecutiveSkips = 0
        var multiCountDays = 0

        for (daysAgo in SAMPLE_DAYS_COUNT downTo 1) {
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.toString()

            // Guarantee boundary & recent 4-day streak (daysAgo == 45 or daysAgo <= 4)
            // while creating natural 1-2 day rest gaps (~20% rate) in the middle weeks.
            val mustInclude = daysAgo == SAMPLE_DAYS_COUNT || daysAgo <= 4 || (daysAgo == 10 && multiCountDays == 0)
            val isSkipDay = !mustInclude && consecutiveSkips < 2 && random.nextFloat() < 0.21f
            if (isSkipDay) {
                consecutiveSkips++
                continue
            }
            consecutiveSkips = 0

            val moodRoll = random.nextFloat()
            val mood = when {
                moodRoll < 0.34f -> "😊"
                moodRoll < 0.58f -> "⚡"
                moodRoll < 0.78f -> "🧘"
                moodRoll < 0.91f -> "🎯"
                else -> "🌧️"
            }

            val noteText = recentNotes[noteCursor % recentNotes.size]
            noteCursor++

            val timeRoll = random.nextFloat()
            val (hour, minute) = when {
                timeRoll < 0.62f -> random.nextInt(7, 10) to random.nextInt(0, 60)   // Morning walk
                timeRoll < 0.84f -> random.nextInt(12, 16) to random.nextInt(0, 60)  // Midday break
                else -> random.nextInt(18, 21) to random.nextInt(0, 60)              // Sunset stroll
            }

            val primaryTimestamp = date.atTime(hour, minute)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

            val countRoll = random.nextFloat()
            val primaryCount = when {
                (daysAgo == 10 && multiCountDays == 0) || countRoll < 0.11f -> 3
                countRoll < 0.34f -> 2
                else -> 1
            }
            if (primaryCount > 1) multiCountDays++

            entries.add(
                HabitEntry(
                    dateString = dateStr,
                    count = primaryCount,
                    timestamp = primaryTimestamp,
                    notes = "[MOOD:$mood] $noteText",
                    trackerId = SAMPLE_TRACKER_ID
                )
            )

            // Occasional second check-in later in the day (~14% chance)
            if (random.nextFloat() < 0.14f) {
                val eveningTimestamp = date.atTime(random.nextInt(18, 22), random.nextInt(0, 60))
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                entries.add(
                    HabitEntry(
                        dateString = dateStr,
                        count = 1,
                        timestamp = eveningTimestamp,
                        notes = "[MOOD:🧘] Bonus evening stroll to unwind.",
                        trackerId = SAMPLE_TRACKER_ID
                    )
                )
            }
        }
        return entries
    }

    fun seedFreshDatabase(db: SupportSQLiteDatabase, today: LocalDate = LocalDate.now()) {
        for (t in buildStarterTrackers()) {
            db.execSQL(
                "INSERT OR IGNORE INTO trackers (id, title, icon, sortOrder, accentColor, type, frequencyType, targetDays, targetCount, timeOfDay, weeklyTarget, isArchived, reminderEnabled, reminderTime, pausedUntilDate, defaultFocusMinutes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    t.id,
                    t.title,
                    t.icon,
                    t.sortOrder,
                    t.accentColor,
                    t.type,
                    t.frequencyType,
                    t.targetDays,
                    t.targetCount,
                    t.timeOfDay,
                    t.weeklyTarget,
                    if (t.isArchived) 1 else 0,
                    if (t.reminderEnabled) 1 else 0,
                    t.reminderTime,
                    t.pausedUntilDate,
                    t.defaultFocusMinutes
                )
            )
        }

        for (entry in buildSampleEntries(today)) {
            db.execSQL(
                "INSERT INTO habit_entries (dateString, count, timestamp, notes, trackerId) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    entry.dateString,
                    entry.count,
                    entry.timestamp,
                    entry.notes,
                    entry.trackerId
                )
            )
        }

        db.execSQL(
            "INSERT OR IGNORE INTO preference_entries (`key`, `value`) VALUES ('selected_tracker_id', ?)",
            arrayOf<Any>(SAMPLE_TRACKER_ID)
        )
        db.execSQL(
            "INSERT OR IGNORE INTO preference_entries (`key`, `value`) VALUES ('selected_theme', ?)",
            arrayOf<Any>(ThemeStyles.DEFAULT_THEME_ID)
        )
        db.execSQL(
            "INSERT OR IGNORE INTO preference_entries (`key`, `value`) VALUES ('selected_font', ?)",
            arrayOf<Any>(ThemeStyles.DEFAULT_FONT_ID)
        )
    }

    suspend fun seedViaRepository(repository: HabitRepository, today: LocalDate = LocalDate.now()) {
        for (t in buildStarterTrackers()) {
            repository.insertTracker(t)
        }
        for (entry in buildSampleEntries(today)) {
            repository.insertEntry(entry)
        }
        if (repository.getPreference("selected_tracker_id").isNullOrBlank()) {
            repository.setPreference("selected_tracker_id", SAMPLE_TRACKER_ID)
        }
        if (repository.getPreference("selected_theme").isNullOrBlank()) {
            repository.setPreference("selected_theme", ThemeStyles.DEFAULT_THEME_ID)
        }
        if (repository.getPreference("selected_font").isNullOrBlank()) {
            repository.setPreference("selected_font", ThemeStyles.DEFAULT_FONT_ID)
        }
    }
}
