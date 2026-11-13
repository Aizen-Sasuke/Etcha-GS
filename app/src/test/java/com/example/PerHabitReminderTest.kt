package com.example

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.PreferenceEntry
import com.example.data.Tracker
import com.example.ui.HabitViewModel
import com.example.worker.ReminderManager
import com.example.worker.ReminderReceiver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerHabitReminderTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("habit_notification_actions", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @After
    fun teardown() {
        db.close()
        // Reset singleton AppDatabase instance if initialized by ViewModel tests
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

    private fun scheduledTrackerIds(): List<String> {
        val shadow = Shadows.shadowOf(alarmManager)
        return shadow.scheduledAlarms.mapNotNull { alarm ->
            val intent = Shadows.shadowOf(alarm.operation).savedIntent
            if (intent.action == ReminderManager.ACTION_HABIT_REMINDER) {
                intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
            } else {
                null
            }
        }
    }

    private fun scheduledAlarmsForTracker(trackerId: String, snoozeOnly: Boolean? = null): List<Intent> {
        val shadow = Shadows.shadowOf(alarmManager)
        return shadow.scheduledAlarms.mapNotNull { alarm ->
            val intent = Shadows.shadowOf(alarm.operation).savedIntent
            if (intent.action == ReminderManager.ACTION_HABIT_REMINDER &&
                intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID) == trackerId
            ) {
                val isSnooze = intent.getBooleanExtra(ReminderManager.EXTRA_IS_SNOOZE, false)
                if (snoozeOnly == null || snoozeOnly == isSnooze) intent else null
            } else {
                null
            }
        }
    }

    private fun hasGlobalAlarm(): Boolean {
        val shadow = Shadows.shadowOf(alarmManager)
        return shadow.scheduledAlarms.any { alarm ->
            Shadows.shadowOf(alarm.operation).savedIntent.action == ReminderManager.ACTION_GLOBAL_REMINDER
        }
    }

    @Test
    fun testReminderConfigurationDefaultsAndPersistence() = runBlocking {
        val defaultTracker = Tracker(id = "meditate", title = "Meditate", icon = "🧘")
        assertFalse(defaultTracker.reminderEnabled)
        assertEquals("21:00", defaultTracker.reminderTime)

        db.trackerDao().insertTracker(defaultTracker)
        val loadedDefault = db.trackerDao().getAllTrackersList().find { it.id == "meditate" }
        assertNotNull(loadedDefault)
        assertFalse(loadedDefault!!.reminderEnabled)
        assertEquals("21:00", loadedDefault.reminderTime)

        db.trackerDao().updateTracker(loadedDefault.copy(reminderEnabled = true, reminderTime = "08:15"))
        val updated = db.trackerDao().getAllTrackersList().find { it.id == "meditate" }
        assertTrue(updated!!.reminderEnabled)
        assertEquals("08:15", updated.reminderTime)
    }

    @Test
    fun testSchedulingAndIsolatedCancellationPerHabit() {
        val now = LocalDateTime.of(2026, 11, 9, 10, 0) // Monday
        val h1 = Tracker(id = "habit_a", title = "Habit A", icon = "🅰️", reminderEnabled = true, reminderTime = "18:00")
        val h2 = Tracker(id = "habit_b", title = "Habit B", icon = "🅱️", reminderEnabled = true, reminderTime = "19:30")

        ReminderManager.scheduleReminder(context, "21:00", now)
        ReminderManager.scheduleTrackerReminder(context, h1, now)
        ReminderManager.scheduleTrackerReminder(context, h2, now)

        assertTrue(hasGlobalAlarm())
        assertTrue("habit_a" in scheduledTrackerIds())
        assertTrue("habit_b" in scheduledTrackerIds())

        // Rescheduling habit_a should not create duplicate alarms or cancel habit_b/global
        ReminderManager.scheduleTrackerReminder(context, h1.copy(reminderTime = "20:00"), now)
        assertEquals(1, scheduledAlarmsForTracker("habit_a", snoozeOnly = false).size)
        assertEquals(1, scheduledAlarmsForTracker("habit_b", snoozeOnly = false).size)
        assertTrue(hasGlobalAlarm())

        // Canceling habit_a must leave habit_b and global intact
        ReminderManager.cancelTrackerReminder(context, "habit_a")
        assertFalse("habit_a" in scheduledTrackerIds())
        assertTrue("habit_b" in scheduledTrackerIds())
        assertTrue(hasGlobalAlarm())
    }

    @Test
    fun testFrequencyAwareNextTriggerCalculation() {
        // 2026-11-13 is Friday
        val fridayNight = LocalDateTime.of(2026, 11, 13, 22, 0)

        val daily = Tracker(
            id = "d", title = "Daily", icon = "📅",
            frequencyType = "daily", reminderEnabled = true, reminderTime = "21:00"
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 14, 21, 0),
            ReminderManager.calculateNextTriggerDateTime(daily, fridayNight)
        )

        val weekdays = Tracker(
            id = "w", title = "Weekdays", icon = "💼",
            frequencyType = "weekdays", reminderEnabled = true, reminderTime = "21:00"
        )
        // Friday 21:00 already passed -> skips Sat & Sun, schedules Monday 2026-11-16 21:00
        assertEquals(
            LocalDateTime.of(2026, 11, 16, 21, 0),
            ReminderManager.calculateNextTriggerDateTime(weekdays, fridayNight)
        )

        val customDays = Tracker(
            id = "c", title = "MWF", icon = "🏋️",
            frequencyType = "custom_days", targetDays = "MON,WED,FRI",
            reminderEnabled = true, reminderTime = "07:30"
        )
        val tuesdayMorning = LocalDateTime.of(2026, 11, 10, 6, 0)
        assertEquals(
            LocalDateTime.of(2026, 11, 11, 7, 30), // Wednesday
            ReminderManager.calculateNextTriggerDateTime(customDays, tuesdayMorning)
        )
    }

    @Test
    fun testWeeklyQuotaBehaviorSuppressesNaggingOnceQuotaMet() = runBlocking {
        // Week of Monday 2026-11-09 .. Sunday 2026-11-15
        val quotaHabit = Tracker(
            id = "gym_3x",
            title = "Gym 3x",
            icon = "🏋️",
            type = "good",
            frequencyType = "weekly_quota",
            weeklyTarget = 3,
            targetCount = 1,
            reminderEnabled = true,
            reminderTime = "18:00"
        )
        db.trackerDao().insertTracker(quotaHabit)

        // Completed Mon & Wed (2 of 3)
        db.habitDao().insertEntry(HabitEntry(dateString = "2026-11-09", count = 1, trackerId = "gym_3x"))
        db.habitDao().insertEntry(HabitEntry(dateString = "2026-11-11", count = 1, trackerId = "gym_3x"))

        // Thursday (2/3 completed): reminder SHOULD still show to encourage progress
        val thursdayEvening = LocalDateTime.of(2026, 11, 12, 18, 0)
        assertTrue(ReminderManager.shouldShowNotificationNow(quotaHabit, db, thursdayEvening))

        // Complete Friday (3 of 3 quota achieved)
        db.habitDao().insertEntry(HabitEntry(dateString = "2026-11-13", count = 1, trackerId = "gym_3x"))

        // Saturday rest day (3/3 already completed): must NOT nag on rest day
        val saturdayEvening = LocalDateTime.of(2026, 11, 14, 18, 0)
        assertFalse(ReminderManager.shouldShowNotificationNow(quotaHabit, db, saturdayEvening))
    }

    @Test
    fun testArchivedHabitsExcludedAndRescheduledOnUnarchive() = runBlocking {
        val now = LocalDateTime.of(2026, 11, 9, 9, 0)
        val activeHabit = Tracker(
            id = "active_h", title = "Read", icon = "📚",
            isArchived = false, reminderEnabled = true, reminderTime = "20:00"
        )
        val archivedHabit = Tracker(
            id = "archived_h", title = "Old Habit", icon = "📦",
            isArchived = true, reminderEnabled = true, reminderTime = "12:00"
        )
        db.trackerDao().insertTracker(activeHabit)
        db.trackerDao().insertTracker(archivedHabit)

        // Pre-schedule archived habit alarm to prove rescheduleAll cancels stale alarms
        ReminderManager.scheduleTrackerReminder(context, archivedHabit.copy(isArchived = false), now)
        assertTrue("archived_h" in scheduledTrackerIds())

        ReminderManager.rescheduleAllFromDatabase(context, db, now)

        assertTrue("active_h" in scheduledTrackerIds())
        assertFalse("archived_h" in scheduledTrackerIds())
        assertFalse(ReminderManager.shouldShowNotificationNow(archivedHabit, db, now))

        // Unarchive -> rescheduleAll restores alarm
        db.trackerDao().setArchived("archived_h", false)
        ReminderManager.rescheduleAllFromDatabase(context, db, now)
        assertTrue("archived_h" in scheduledTrackerIds())
    }

    @Test
    fun testViewModelArchiveAndUnarchiveUpdatesScheduledAlarms() = runBlocking {
        val singletonDb = AppDatabase.getDatabase(context)
        singletonDb.trackerDao().insertTracker(
            Tracker(id = "reading", title = "Daily Reading", icon = "📚", reminderEnabled = false)
        )
        val app = context.applicationContext as Application
        val vm = HabitViewModel(app)

        vm.setTrackerReminder("reading", enabled = true, time = "20:30")
        var attempts = 0
        while (attempts < 40 && "reading" !in scheduledTrackerIds()) {
            Thread.sleep(50)
            attempts++
        }
        assertTrue("reading" in scheduledTrackerIds())

        vm.archiveTracker("reading")
        attempts = 0
        while (attempts < 30 && "reading" in scheduledTrackerIds()) {
            Thread.sleep(50)
            attempts++
        }
        assertFalse("reading" in scheduledTrackerIds())

        vm.unarchiveTracker("reading")
        attempts = 0
        while (attempts < 30 && "reading" !in scheduledTrackerIds()) {
            Thread.sleep(50)
            attempts++
        }
        assertTrue("reading" in scheduledTrackerIds())
    }

    @Test
    fun testNotificationActionValidationPreventsDuplicateTapsAndRespectsMultiCountAndUndo() = runBlocking {
        val now = LocalDateTime.of(2026, 11, 9, 14, 0)
        val todayStr = "2026-11-09"

        val water = Tracker(
            id = "water_test", title = "Water", icon = "💧",
            type = "good", targetCount = 3, reminderEnabled = true, reminderTime = "14:00"
        )
        val archived = Tracker(
            id = "archived_test", title = "Archived", icon = "📦",
            type = "good", isArchived = true, reminderEnabled = true
        )
        db.trackerDao().insertTracker(water)
        db.trackerDao().insertTracker(archived)

        // Archived and missing trackers must reject check-in
        assertFalse(ReminderReceiver.handleCheckInAction(context, db, "archived_test", "tok_arch", false, now).executed)
        assertFalse(ReminderReceiver.handleCheckInAction(context, db, "nonexistent", "tok_miss", false, now).executed)

        // Multi-count increment 1 -> 2 -> 3
        val outcome1 = ReminderReceiver.handleCheckInAction(context, db, "water_test", "tok_1", false, now)
        assertTrue(outcome1.executed)
        assertEquals(1, outcome1.newCount)

        // Reusing same token (double-tap) MUST NOT double increment
        val duplicateTap = ReminderReceiver.handleCheckInAction(context, db, "water_test", "tok_1", false, now)
        assertFalse(duplicateTap.executed)
        assertEquals(1, db.habitDao().getEntriesForTrackerAndDate("water_test", todayStr).sumOf { it.count })

        // Distinct tokens advance 2/3 and 3/3
        val outcome2 = ReminderReceiver.handleCheckInAction(context, db, "water_test", "tok_2", false, now)
        assertTrue(outcome2.executed)
        assertEquals(2, outcome2.newCount)

        val outcome3 = ReminderReceiver.handleCheckInAction(context, db, "water_test", "tok_3", false, now)
        assertTrue(outcome3.executed)
        assertEquals(3, outcome3.newCount)

        // Once daily target (3/3) reached, another Check In token for good habit is prevented
        val overTarget = ReminderReceiver.handleCheckInAction(context, db, "water_test", "tok_4", false, now)
        assertFalse(overTarget.executed)
        assertTrue(overTarget.alreadyCompleted)
        assertEquals(3, db.habitDao().getEntriesForTrackerAndDate("water_test", todayStr).sumOf { it.count })

        // Undo outcome3 reverts 3 -> 2 cleanly
        val undone = ReminderReceiver.handleUndoAction(
            context = context,
            db = db,
            trackerId = "water_test",
            dateString = outcome3.dateString!!,
            entryId = outcome3.entryId!!,
            previousCount = outcome3.previousCount,
            previousNotes = outcome3.previousNotes,
            actionToken = "undo_tok_3"
        )
        assertTrue(undone)
        assertEquals(2, db.habitDao().getEntriesForTrackerAndDate("water_test", todayStr).sumOf { it.count })

        // Undo token reuse is idempotent / ignored
        val duplicateUndo = ReminderReceiver.handleUndoAction(
            context = context,
            db = db,
            trackerId = "water_test",
            dateString = outcome3.dateString!!,
            entryId = outcome3.entryId!!,
            previousCount = 0,
            previousNotes = null,
            actionToken = "undo_tok_3"
        )
        assertFalse(duplicateUndo)
        assertEquals(2, db.habitDao().getEntriesForTrackerAndDate("water_test", todayStr).sumOf { it.count })
    }

    @Test
    fun testBadHabitReminderSemanticsAndSlipUndo() = runBlocking {
        val now = LocalDateTime.of(2026, 11, 9, 20, 0)
        val smoking = Tracker(
            id = "no_smoke", title = "No Smoking", icon = "🚭",
            type = "bad", reminderEnabled = true, reminderTime = "20:00"
        )
        db.trackerDao().insertTracker(smoking)

        // Mindful check-in reminder allowed when clean today
        assertTrue(ReminderManager.shouldShowNotificationNow(smoking, db, now))

        // Positive check-in action MUST be rejected on bad habit so clean streak isn't corrupted
        val invalidPositive = ReminderReceiver.handleCheckInAction(context, db, "no_smoke", "tok_pos", isSlipAction = false, now = now)
        assertFalse(invalidPositive.executed)
        assertTrue(db.habitDao().getEntriesForTrackerAndDate("no_smoke", "2026-11-09").isEmpty())

        // Log Slip succeeds once and suppresses subsequent nagging today
        val slip = ReminderReceiver.handleCheckInAction(context, db, "no_smoke", "tok_slip", isSlipAction = true, now = now)
        assertTrue(slip.executed)
        assertEquals(1, slip.newCount)
        assertFalse(ReminderManager.shouldShowNotificationNow(smoking, db, now))

        // Second slip tap from notification prevented
        val duplicateSlip = ReminderReceiver.handleCheckInAction(context, db, "no_smoke", "tok_slip_2", isSlipAction = true, now = now)
        assertFalse(duplicateSlip.executed)
        assertTrue(duplicateSlip.alreadyCompleted)

        // Undo removes the brand-new slip entry cleanly
        val reverted = ReminderReceiver.handleUndoAction(
            context = context,
            db = db,
            trackerId = "no_smoke",
            dateString = slip.dateString!!,
            entryId = slip.entryId!!,
            previousCount = slip.previousCount,
            previousNotes = slip.previousNotes,
            actionToken = "undo_slip"
        )
        assertTrue(reverted)
        assertTrue(db.habitDao().getEntriesForTrackerAndDate("no_smoke", "2026-11-09").isEmpty())
    }

    @Test
    fun testSnoozeSchedulesSeparateAlarmWithoutCancelingDailySchedule() {
        val now = LocalDateTime.of(2026, 11, 9, 18, 0)
        val tracker = Tracker(
            id = "journal", title = "Journal", icon = "📓",
            reminderEnabled = true, reminderTime = "18:00"
        )

        ReminderManager.scheduleTrackerReminder(context, tracker, now)
        val snoozeMillis = ReminderManager.scheduleTrackerSnooze(context, tracker, delayMinutes = 60L, now = now)
        assertNotNull(snoozeMillis)

        assertEquals(1, scheduledAlarmsForTracker("journal", snoozeOnly = false).size)
        assertEquals(1, scheduledAlarmsForTracker("journal", snoozeOnly = true).size)
    }

    @Test
    fun testGlobalAndRebootReconstructionCompatibility() = runBlocking {
        val singletonDb = AppDatabase.getDatabase(context)
        singletonDb.habitDao().insertPreference(PreferenceEntry("reminders_enabled", "true"))
        singletonDb.habitDao().insertPreference(PreferenceEntry("reminder_time", "21:30"))
        singletonDb.trackerDao().insertTracker(
            Tracker(id = "boot_habit", title = "Stretch", icon = "🤸", reminderEnabled = true, reminderTime = "07:00")
        )

        val receiver = ReminderReceiver()
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        var attempts = 0
        while (attempts < 40 && ("boot_habit" !in scheduledTrackerIds() || !hasGlobalAlarm())) {
            Thread.sleep(50)
            attempts++
        }

        assertTrue("boot_habit" in scheduledTrackerIds())
        assertTrue(hasGlobalAlarm())

        // Verify global notification exclusion filter preserves unreminded active habits and skips custom-reminded ones
        singletonDb.trackerDao().insertTracker(
            Tracker(id = "global_only_habit", title = "Walk", icon = "🚶", reminderEnabled = false, isArchived = false)
        )
        val unremindedActive = singletonDb.trackerDao().getAllTrackersList()
            .filter { !it.isArchived && !it.reminderEnabled }
        assertTrue(unremindedActive.any { it.id == "global_only_habit" })
        assertFalse(unremindedActive.any { it.id == "boot_habit" })
    }

    @Test
    fun testViewModelDeleteTrackerCancelsScheduledAlarm() = runBlocking {
        val singletonDb = AppDatabase.getDatabase(context)
        singletonDb.trackerDao().insertTracker(
            Tracker(id = "gardening", title = "Gardening", icon = "🌿", reminderEnabled = false)
        )
        val app = context.applicationContext as Application
        val vm = HabitViewModel(app)

        vm.setTrackerReminder("gardening", enabled = true, time = "16:30")
        var attempts = 0
        while (attempts < 40 && "gardening" !in scheduledTrackerIds()) {
            Thread.sleep(50)
            attempts++
        }
        assertTrue("gardening" in scheduledTrackerIds())

        vm.deleteTracker("gardening")
        attempts = 0
        while (attempts < 30 && "gardening" in scheduledTrackerIds()) {
            Thread.sleep(50)
            attempts++
        }
        assertFalse("gardening" in scheduledTrackerIds())
    }
}
