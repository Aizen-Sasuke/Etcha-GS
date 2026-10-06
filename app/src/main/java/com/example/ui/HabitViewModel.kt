package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.HabitRepository
import com.example.data.Tracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONObject

open class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val appDb = AppDatabase.getDatabase(application)
    private val repository: HabitRepository = HabitRepository(appDb, appDb.habitDao(), appDb.trackerDao())
    
    // Expose flows of raw tables
    private val rawEntries: Flow<List<HabitEntry>> = repository.allEntries
    val preferences: Flow<Map<String, String>> = repository.allPreferences.map { list ->
        list.associate { it.key to it.value }
    }

    private val trackersRaw: StateFlow<List<Tracker>> = repository.allTrackers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val current = repository.allTrackers.first()
            if (current.none { it.id == "gym" }) {
                repository.insertTracker(Tracker("gym", "Gym & Workout", "🏋️‍♂️", 1, "#FF5722", "good", "weekly", "MON,TUE,THU,FRI,SAT", 1, "morning"))
            }
            if (current.none { it.id == "gardening" }) {
                repository.insertTracker(Tracker("gardening", "Gardening", "🌿", 2, "#4CAF50", "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "afternoon"))
            }
            if (current.none { it.id == "reading" }) {
                repository.insertTracker(Tracker("reading", "Daily Reading", "📚", 3, "#2196F3", "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "evening"))
            }
            if (current.none { it.id == "water" }) {
                repository.insertTracker(Tracker("water", "Hydration", "💧", 4, "#00BCD4", "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 8, "anytime"))
            }
        }
    }

    val trackers: StateFlow<List<TrackerConfig>> = trackersRaw.map { list ->
        list.sortedBy { it.sortOrder }.map { 
            TrackerConfig(
                id = it.id, 
                title = it.title, 
                icon = it.icon, 
                accentColor = it.accentColor, 
                type = it.type,
                frequencyType = it.frequencyType,
                targetDays = it.targetDays,
                targetCount = it.targetCount,
                timeOfDay = it.timeOfDay,
                weeklyTarget = it.weeklyTarget
            ) 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTrackerId: StateFlow<String> = preferences.map { prefs ->
        prefs["selected_tracker_id"] ?: "sobriety"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "sobriety")

    val activeTracker: StateFlow<TrackerConfig> = combine(trackers, selectedTrackerId) { list, activeId ->
        list.find { it.id == activeId } ?: list.firstOrNull() ?: TrackerConfig("sobriety", "Sobriety", "🚭", null, "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "anytime", 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackerConfig("sobriety", "Sobriety", "🚭", null, "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "anytime", 0))

    val streakFreezeDate: StateFlow<String?> = preferences.map { prefs ->
        val v = prefs["streak_freeze_date"]
        if (v.isNullOrEmpty()) null else v
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val streakFreezeHistory: StateFlow<String> = preferences.map { prefs ->
        prefs["streak_freeze_history"] ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Dynamically derive title and icon from active tracker
    val habitTitle: StateFlow<String> = activeTracker.map { it.title }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Sobriety")

    val habitIcon: StateFlow<String> = activeTracker.map { it.icon }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "🚭")

    // Filtered entries flow reactively bound to active trackerId using indexed Room query
    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<List<HabitEntry>> = activeTracker.map { it.id }.distinctUntilChanged().flatMapLatest { activeId ->
        repository.getEntriesForTracker(activeId)
    }

    val selectedTheme: StateFlow<String> = preferences.map { prefs ->
        prefs["selected_theme"] ?: "matte_black"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "matte_black")

    val customPrimaryColor: StateFlow<String> = preferences.map { prefs ->
        prefs["custom_primary_color"] ?: "#FFFFFF"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#FFFFFF")

    val customBackgroundColor: StateFlow<String> = preferences.map { prefs ->
        prefs["custom_background_color"] ?: "#111111"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#111111")

    val selectedFont: StateFlow<String> = preferences.map { prefs ->
        prefs["selected_font"] ?: "cursive"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "cursive")

    val calendarCellShape: StateFlow<String> = preferences.map { prefs ->
        prefs["calendar_cell_shape"] ?: "rounded_square"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "rounded_square")

    val checkmarkStyle: StateFlow<String> = preferences.map { prefs ->
        prefs["checkmark_style"] ?: "tick"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "tick")

    val swipeHintShown: StateFlow<Boolean> = preferences.map { prefs ->
        prefs["swipe_hint_shown"] == "true"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderTime: StateFlow<String> = preferences.map { prefs ->
        prefs["reminder_time"] ?: "21:00"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "21:00")

    val remindersEnabled: StateFlow<Boolean> = preferences.map { prefs ->
        prefs["reminders_enabled"] == "true"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dayRolloverHour: StateFlow<Int> = preferences.map { prefs ->
        prefs["day_rollover_hour"]?.toIntOrNull() ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val celebrationsEnabled: StateFlow<Boolean> = preferences.map { prefs ->
        prefs["celebrations_enabled"] != "false"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasSeenOnboarding: StateFlow<Boolean> = preferences.map { prefs ->
        prefs["has_seen_onboarding"] == "true"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSupporter: StateFlow<Boolean> = preferences.map { prefs ->
        prefs["is_supporter"] == "true"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val supporterVaultFreezes: StateFlow<Int> = preferences.map { prefs ->
        prefs["supporter_vault_freezes"]?.toIntOrNull() ?: 3
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val supporterBadgeEnabled: StateFlow<Boolean> = preferences.map { prefs ->
        prefs["supporter_badge_enabled"] != "false"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val googleAccountEmail: StateFlow<String?> = preferences.map { prefs ->
        prefs["google_account_email"]?.takeIf { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleAccountName: StateFlow<String?> = preferences.map { prefs ->
        prefs["google_account_name"]?.takeIf { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastCloudBackupTime: StateFlow<String?> = preferences.map { prefs ->
        prefs["last_cloud_backup_time"]?.takeIf { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoCloudBackupEnabled: StateFlow<Boolean> = preferences.map { prefs ->
        prefs["auto_cloud_backup_enabled"] != "false"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cloudBackupSnapshots: StateFlow<List<CloudBackupSnapshot>> = preferences.map { prefs ->
        val raw = prefs["cloud_backup_history"] ?: ""
        parseCloudSnapshots(raw)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setHasSeenOnboarding(seen: Boolean) {
        viewModelScope.launch {
            repository.setPreference("has_seen_onboarding", seen.toString())
        }
    }

    fun setSupporter(supporter: Boolean) {
        viewModelScope.launch {
            repository.setPreference("is_supporter", supporter.toString())
            if (supporter) {
                repository.setPreference("supporter_badge_enabled", "true")
            }
        }
    }

    fun setSupporterBadgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPreference("supporter_badge_enabled", enabled.toString())
        }
    }

    fun connectGoogleAccount(email: String, name: String) {
        viewModelScope.launch {
            repository.setPreference("google_account_email", email.trim())
            repository.setPreference("google_account_name", name.trim())
            // Perform an initial backup
            performCloudBackup()
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            repository.setPreference("google_account_email", "")
            repository.setPreference("google_account_name", "")
        }
    }

    fun setAutoCloudBackup(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPreference("auto_cloud_backup_enabled", enabled.toString())
        }
    }

    fun useVaultFreeze(): Boolean {
        val current = supporterVaultFreezes.value
        if (current <= 0) return false
        val todayStr = getEffectiveToday().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        viewModelScope.launch {
            repository.setPreference("streak_freeze_date", todayStr)
            val currentHist = streakFreezeHistory.value
            val newHist = if (currentHist.isBlank()) todayStr else "$currentHist,$todayStr"
            repository.setPreference("streak_freeze_history", newHist)
            repository.setPreference("supporter_vault_freezes", (current - 1).toString())
        }
        return true
    }

    fun refillVaultFreezes() {
        viewModelScope.launch {
            repository.setPreference("supporter_vault_freezes", "5")
        }
    }

    fun setDayRolloverHour(hour: Int) {
        viewModelScope.launch {
            repository.setPreference("day_rollover_hour", hour.toString())
        }
    }

    fun setCelebrationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPreference("celebrations_enabled", enabled.toString())
        }
    }

    fun saveDailyJournal(dateString: String, journalText: String, moodTag: String? = null) {
        viewModelScope.launch {
            val currentId = selectedTrackerId.value
            val existing = repository.getEntriesForDate(dateString).filter { it.trackerId == currentId }
            val formattedMood = moodTag?.let { "[MOOD:$it]" }
            val combinedNote = if (formattedMood != null) {
                if (journalText.isNotBlank()) "$formattedMood $journalText" else formattedMood
            } else {
                journalText.ifBlank { null }
            }

            if (existing.isEmpty()) {
                if (!combinedNote.isNullOrBlank()) {
                    repository.insertEntry(
                        HabitEntry(
                            dateString = dateString,
                            count = 1,
                            trackerId = currentId,
                            notes = combinedNote
                        )
                    )
                }
            } else {
                val last = existing.last()
                repository.insertEntry(last.copy(notes = combinedNote))
            }
            repository.setPreference("journal_$dateString", journalText)
            if (moodTag != null) {
                repository.setPreference("mood_$dateString", moodTag)
            }
            updateWidget()
        }
    }

    open fun getEffectiveToday(): LocalDate {
        val rollover = dayRolloverHour.value
        val now = java.time.LocalDateTime.now()
        return if (now.hour < rollover) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
    }

    // Calculated Statistics as state flows (automatically listening to filtered entries)
    val completionsByDate: StateFlow<Map<String, List<HabitEntry>>> = entries.map { list ->
        list.groupBy { it.dateString }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalCount: StateFlow<Int> = entries.map { list ->
        list.sumOf { it.count }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stats: StateFlow<HabitStats> = combine(entries, activeTracker, streakFreezeDate, streakFreezeHistory) { list, tracker, freezeActive, freezeHist ->
        calculateStats(list, freezeActive, freezeHist, tracker)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitStats())

    // Tracker management actions
    fun selectTracker(id: String) {
        viewModelScope.launch {
            repository.setPreference("selected_tracker_id", id)
            updateWidget()
        }
    }

    fun addTracker(
        title: String, 
        icon: String, 
        accentColor: String? = null, 
        type: String = "good",
        frequencyType: String = "daily",
        targetDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
        targetCount: Int = 1,
        timeOfDay: String = "anytime",
        weeklyTarget: Int = 0
    ) {
        viewModelScope.launch {
            val newId = "tracker_" + System.currentTimeMillis()
            val newTracker = Tracker(
                id = newId, 
                title = title, 
                icon = icon, 
                sortOrder = trackersRaw.value.size,
                accentColor = accentColor,
                type = type,
                frequencyType = frequencyType,
                targetDays = targetDays,
                targetCount = targetCount,
                timeOfDay = timeOfDay,
                weeklyTarget = weeklyTarget
            )
            repository.insertTracker(newTracker)
            repository.setPreference("selected_tracker_id", newId)
            if (type == "bad") {
                repository.setPreference("checkmark_style", "cross")
            }
            updateWidget()
        }
    }

    fun updateTracker(
        id: String, 
        title: String, 
        icon: String, 
        accentColor: String? = null, 
        type: String = "good",
        frequencyType: String = "daily",
        targetDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
        targetCount: Int = 1,
        timeOfDay: String = "anytime",
        weeklyTarget: Int = 0
    ) {
        viewModelScope.launch {
            val existing = trackersRaw.value.find { it.id == id }
            if (existing != null) {
                repository.updateTracker(
                    existing.copy(
                        title = title, 
                        icon = icon, 
                        accentColor = accentColor, 
                        type = type,
                        frequencyType = frequencyType,
                        targetDays = targetDays,
                        targetCount = targetCount,
                        timeOfDay = timeOfDay,
                        weeklyTarget = weeklyTarget
                    )
                )
                updateWidget()
            }
        }
    }

    fun reorderTrackers(reorderedList: List<TrackerConfig>) {
        viewModelScope.launch {
            reorderedList.forEachIndexed { index, config ->
                val tracker = trackersRaw.value.find { it.id == config.id }
                if (tracker != null && tracker.sortOrder != index) {
                    repository.updateTracker(tracker.copy(sortOrder = index))
                }
            }
            updateWidget()
        }
    }

    fun deleteTracker(trackerId: String) {
        viewModelScope.launch {
            // Delete associated tracker; CASCADE will delete its entries too
            repository.deleteTrackerById(trackerId)
            
            // If deleted was selected, fallback to the remaining first
            val currentList = trackersRaw.value.filter { it.id != trackerId }
            if (currentList.isNotEmpty() && selectedTrackerId.value == trackerId) {
                repository.setPreference("selected_tracker_id", currentList.first().id)
            }
            updateWidget()
        }
    }

    // Database Actions
    fun insertEntry(entry: HabitEntry) {
        viewModelScope.launch {
            repository.insertEntry(entry)
            updateWidget()
        }
    }

    fun insertEntry(dateString: String, count: Int = 1, notes: String? = null) {
        viewModelScope.launch {
            repository.insertEntry(
                HabitEntry(
                    dateString = dateString,
                    count = count,
                    notes = notes,
                    trackerId = selectedTrackerId.value
                )
            )
            updateWidget()
        }
    }

    private val _lastQuickCheckIn = MutableStateFlow<QuickCheckInRecord?>(null)
    val lastQuickCheckIn: StateFlow<QuickCheckInRecord?> = _lastQuickCheckIn.asStateFlow()

    fun quickIncrement(
        dateString: String, 
        moodTag: String? = null,
        onLogged: ((QuickCheckInRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val currentId = selectedTrackerId.value
            val existing = repository.getEntriesForTrackerAndDate(currentId, dateString)
            val formattedMood = moodTag?.let { "[MOOD:$it]" }
            val token = System.nanoTime()
            val record: QuickCheckInRecord

            if (existing.isEmpty()) {
                val newEntry = HabitEntry(
                    dateString = dateString, 
                    count = 1, 
                    trackerId = currentId,
                    notes = formattedMood
                )
                repository.insertEntry(newEntry)
                val inserted = repository.getEntriesForTrackerAndDate(currentId, dateString).lastOrNull()
                record = QuickCheckInRecord(
                    token = token,
                    trackerId = currentId,
                    dateString = dateString,
                    previousCount = 0,
                    newCount = 1,
                    previousNotes = null,
                    entryId = inserted?.id
                )
            } else {
                // If entries exist, increment count of last entry and attach/update mood
                val last = existing.last()
                val updatedNotes = if (formattedMood != null) {
                    val cleanExisting = last.notes?.replace(Regex("\\[MOOD:[^\\]]+\\]"), "")?.trim() ?: ""
                    if (cleanExisting.isNotEmpty()) "$formattedMood $cleanExisting" else formattedMood
                } else {
                    last.notes
                }
                val newCount = last.count + 1
                repository.insertEntry(last.copy(count = newCount, notes = updatedNotes))
                record = QuickCheckInRecord(
                    token = token,
                    trackerId = currentId,
                    dateString = dateString,
                    previousCount = last.count,
                    newCount = newCount,
                    previousNotes = last.notes,
                    entryId = last.id
                )
            }
            _lastQuickCheckIn.value = record
            updateWidget()
            onLogged?.invoke(record)
        }
    }

    fun undoQuickCheckIn(token: Long? = null, onComplete: ((Boolean) -> Unit)? = null) {
        val record = _lastQuickCheckIn.value ?: run {
            onComplete?.invoke(false)
            return
        }
        if (token != null && record.token != token) {
            onComplete?.invoke(false)
            return
        }
        _lastQuickCheckIn.value = null
        viewModelScope.launch {
            if (record.previousCount == 0) {
                // Brand new entry created by this check-in: delete it
                if (record.entryId != null && record.entryId > 0) {
                    repository.deleteEntryById(record.entryId)
                } else {
                    repository.deleteEntriesForTrackerAndDate(record.trackerId, record.dateString)
                }
            } else {
                // Incremented existing entry: revert count and notes
                val existing = repository.getEntriesForTrackerAndDate(record.trackerId, record.dateString)
                val target = if (record.entryId != null) {
                    existing.find { it.id == record.entryId } ?: existing.lastOrNull()
                } else {
                    existing.lastOrNull()
                }
                if (target != null) {
                    repository.insertEntry(
                        target.copy(
                            count = record.previousCount,
                            notes = record.previousNotes
                        )
                    )
                }
            }
            updateWidget()
            onComplete?.invoke(true)
        }
    }

    fun clearUndoRecord() {
        _lastQuickCheckIn.value = null
    }

    fun updateWidget() {
        try {
            val intent = android.content.Intent("com.example.ACTION_UPDATE_HABIT_WIDGET").apply {
                setPackage(getApplication<Application>().packageName)
            }
            getApplication<Application>().sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignore widget broadcast error
        }
    }

    fun deleteEntry(entry: HabitEntry) {
        clearUndoRecord()
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun deleteEntryById(id: Int) {
        clearUndoRecord()
        viewModelScope.launch {
            repository.deleteEntryById(id)
        }
    }

    fun clearEntriesForDate(dateString: String) {
        clearUndoRecord()
        viewModelScope.launch {
            val currentId = selectedTrackerId.value
            repository.deleteEntriesForTrackerAndDate(currentId, dateString)
            updateWidget()
        }
    }

    fun setSelectedTheme(theme: String) {
        viewModelScope.launch {
            repository.setPreference("selected_theme", theme)
        }
    }

    fun setCustomPrimaryColorHex(hex: String) {
        viewModelScope.launch {
            repository.setPreference("custom_primary_color", hex)
        }
    }

    fun setCustomBackgroundColorHex(hex: String) {
        viewModelScope.launch {
            repository.setPreference("custom_background_color", hex)
        }
    }

    fun setSelectedFont(font: String) {
        viewModelScope.launch {
            repository.setPreference("selected_font", font)
        }
    }

    fun setCalendarCellShape(shape: String) {
        viewModelScope.launch {
            repository.setPreference("calendar_cell_shape", shape)
        }
    }

    fun setCheckmarkStyle(style: String) {
        viewModelScope.launch {
            repository.setPreference("checkmark_style", style)
        }
    }
    
    fun setSwipeHintShown() {
        viewModelScope.launch {
            repository.setPreference("swipe_hint_shown", "true")
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            repository.setPreference("reminder_time", time)
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPreference("reminders_enabled", enabled.toString())
        }
    }

    fun applyStreakFreeze() {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        viewModelScope.launch {
            repository.setPreference("streak_freeze_date", today)
        }
    }

    // Stats calculations
    init {
        viewModelScope.launch {
            if (repository.getAllTrackersList().isEmpty()) {
                repository.insertTracker(Tracker("sobriety", "Sobriety", "💪", 0, null, "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "anytime", 0))
            }
        }
        viewModelScope.launch {
            combine(entries, streakFreezeDate, streakFreezeHistory, ::Triple).collect { (list, freezeActive, freezeHist) ->
                if (freezeActive != null) {
                    val today = LocalDate.now()
                    val freezeDate = try { LocalDate.parse(freezeActive) } catch(e:Exception) { null }
                    if (freezeDate != null && freezeDate.isBefore(today)) {
                        val hasEntry = list.any { it.dateString == freezeActive && it.count > 0 }
                        if (!hasEntry) {
                            val newHist = if (freezeHist.isEmpty()) freezeActive else "$freezeHist,$freezeActive"
                            repository.setPreference("streak_freeze_history", newHist)
                            repository.setPreference("streak_freeze_date", "")
                        } else {
                            repository.setPreference("streak_freeze_date", "")
                        }
                    }
                }
            }
        }
    }

    private fun getScheduledDaysOfWeek(tracker: TrackerConfig): Set<java.time.DayOfWeek> {
        return when (tracker.frequencyType) {
            "weekdays" -> setOf(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY
            )
            "custom_days", "weekly" -> {
                val parts = tracker.targetDays.split(",").map { it.trim().uppercase() }
                val set = mutableSetOf<java.time.DayOfWeek>()
                for (p in parts) {
                    when (p) {
                        "MON" -> set.add(java.time.DayOfWeek.MONDAY)
                        "TUE" -> set.add(java.time.DayOfWeek.TUESDAY)
                        "WED" -> set.add(java.time.DayOfWeek.WEDNESDAY)
                        "THU" -> set.add(java.time.DayOfWeek.THURSDAY)
                        "FRI" -> set.add(java.time.DayOfWeek.FRIDAY)
                        "SAT" -> set.add(java.time.DayOfWeek.SATURDAY)
                        "SUN" -> set.add(java.time.DayOfWeek.SUNDAY)
                    }
                }
                if (set.isEmpty()) java.time.DayOfWeek.values().toSet() else set
            }
            else -> java.time.DayOfWeek.values().toSet()
        }
    }

    private fun calculateHabitStrength(
        list: List<HabitEntry>,
        today: LocalDate,
        tracker: TrackerConfig
    ): Pair<Int, String> {
        if (list.isEmpty()) return Pair(0, "Starting 🌱")

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val target = tracker.targetCount.coerceAtLeast(1)
        val completionDates = list.groupBy { it.dateString }
            .filter { (_, entries) -> entries.sumOf { it.count } >= target }
            .keys
            .mapNotNull {
                try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
            }.toSet()

        val sortedDates = list.mapNotNull {
            try { LocalDate.parse(it.dateString, formatter) } catch (e: Exception) { null }
        }.sorted()

        if (sortedDates.isEmpty()) return Pair(0, "Starting 🌱")

        if (tracker.frequencyType == "weekly_quota") {
            val weeklyTarget = tracker.weeklyTarget.coerceIn(1, 7)
            val firstDate = sortedDates.first()
            val startMonday = firstDate.with(java.time.DayOfWeek.MONDAY)
            val currentMonday = today.with(java.time.DayOfWeek.MONDAY)

            var strength = 0.0
            val alpha = 0.15
            val beta = 0.12

            var weekMon = startMonday
            while (!weekMon.isAfter(currentMonday)) {
                val weekSun = weekMon.plusDays(6)
                var distinctCompletedDays = 0
                for (d in 0..6) {
                    val day = weekMon.plusDays(d.toLong())
                    if (day in completionDates) {
                        distinctCompletedDays++
                    }
                }

                val isSuccessfulWeek = distinctCompletedDays >= weeklyTarget
                val isCurrentWeek = weekMon == currentMonday

                if (isSuccessfulWeek) {
                    strength += alpha * (1.0 - strength)
                } else {
                    if (!isCurrentWeek) {
                        strength *= (1.0 - beta)
                    }
                }

                weekMon = weekMon.plusWeeks(1)
            }

            val score = (strength * 100.0).toInt().coerceIn(0, 100)
            val status = when {
                score >= 85 -> "Ironclad 💎"
                score >= 65 -> "Established 🏆"
                score >= 45 -> "Building 🚀"
                score >= 20 -> "Developing ⚡"
                else -> "Starting 🌱"
            }
            return Pair(score, status)
        }

        val startDate = sortedDates.first().coerceAtLeast(today.minusDays(180))
        val allowedDaysSet = getScheduledDaysOfWeek(tracker)

        var strength = 0.0
        val alpha = 0.05
        val beta = 0.035

        var cur = startDate
        while (!cur.isAfter(today)) {
            val isTargetDay = cur.dayOfWeek in allowedDaysSet
            if (tracker.type == "bad") {
                val hadSlip = cur in completionDates
                if (!hadSlip) {
                    strength += alpha * (1.0 - strength)
                } else {
                    strength *= (1.0 - beta * 2.5)
                }
            } else {
                if (isTargetDay) {
                    val done = cur in completionDates
                    if (done) {
                        strength += alpha * (1.0 - strength)
                    } else if (cur.isBefore(today)) {
                        strength *= (1.0 - beta)
                    }
                }
            }
            cur = cur.plusDays(1)
        }

        val score = (strength * 100.0).toInt().coerceIn(0, 100)
        val status = when {
            score >= 85 -> "Ironclad 💎"
            score >= 65 -> "Established 🏆"
            score >= 45 -> "Building 🚀"
            score >= 20 -> "Developing ⚡"
            else -> "Starting 🌱"
        }
        return Pair(score, status)
    }

    private fun calculateStats(list: List<HabitEntry>, freezeActive: String?, freezeHist: String, tracker: TrackerConfig): HabitStats {
        if (list.isEmpty()) return HabitStats()

        val today = getEffectiveToday()
        val currentYear = today.year
        val currentMonth = today.monthValue

        val totalSum = list.sumOf { it.count }
        val (strengthScore, strengthStatus) = calculateHabitStrength(list, today, tracker)

        // Filter and calculate yearly and monthly counts
        var thisYearSum = 0
        var thisMonthSum = 0
        var lastMonthSum = 0

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (entry in list) {
            try {
                val date = LocalDate.parse(entry.dateString, formatter)
                if (date.year == currentYear) {
                    thisYearSum += entry.count
                }
                if (date.year == currentYear && date.monthValue == currentMonth) {
                    thisMonthSum += entry.count
                }
                val lastMonthDate = today.minusMonths(1)
                if (date.year == lastMonthDate.year && date.monthValue == lastMonthDate.monthValue) {
                    lastMonthSum += entry.count
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        // Percentage calculations
        val percentageChange: Double
        val percentageIsDecrease: Boolean
        val percentageText: String

        if (lastMonthSum == 0) {
            percentageChange = if (thisMonthSum > 0) 100.0 else 0.0
            percentageIsDecrease = false
            percentageText = if (thisMonthSum > 0) "↑100%" else "0%"
        } else {
            val diff = thisMonthSum - lastMonthSum
            percentageChange = (diff.toDouble() / lastMonthSum.toDouble()) * 100.0
            percentageIsDecrease = diff < 0
            val absVal = abs(percentageChange)
            val sign = if (diff < 0) "↓" else "↑"
            percentageText = String.format("%s%.1f%%", sign, absVal)
        }

        if (tracker.type == "misc") {
            return HabitStats(
                totalCount = totalSum,
                thisYearCount = thisYearSum,
                thisMonthCount = thisMonthSum,
                lastMonthCount = lastMonthSum,
                percentageChange = percentageChange,
                percentageText = percentageText,
                percentageIsDecrease = percentageIsDecrease,
                currentStreak = 0,
                bestStreak = 0,
                bestStreakTimeline = null,
                habitStrengthScore = strengthScore,
                habitStrengthStatus = strengthStatus
            )
        }
        else if (tracker.type == "bad") {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val allDates = list.mapNotNull {
                try { LocalDate.parse(it.dateString, formatter) } catch(e: Exception) { null }
            }.sorted()
            
            if (allDates.isEmpty()) {
                return HabitStats(
                    totalCount = totalSum,
                    thisYearCount = thisYearSum,
                    thisMonthCount = thisMonthSum,
                    lastMonthCount = lastMonthSum,
                    percentageChange = percentageChange,
                    percentageText = percentageText,
                    percentageIsDecrease = percentageIsDecrease,
                    currentStreak = 0,
                    bestStreak = 0,
                    bestStreakTimeline = null,
                    habitStrengthScore = strengthScore,
                    habitStrengthStatus = strengthStatus
                )
            }
            
            val firstDate = allDates.first()
            val upperBoundary = if (allDates.last().isAfter(today)) allDates.last() else today

            val activeDates = list.filter { it.count > 0 }.mapNotNull {
                try { LocalDate.parse(it.dateString, formatter) } catch(e: Exception) { null }
            }.sorted()
            
            var currentStreak = 0
            var bestStreak = 0
            var bestStart: LocalDate? = null
            var bestEnd: LocalDate? = null
            
            if (activeDates.isNotEmpty()) {
                val lastSlip = activeDates.last()
                val slipDiff = ChronoUnit.DAYS.between(lastSlip, upperBoundary).toInt()
                currentStreak = maxOf(0, slipDiff)
            } else {
                currentStreak = ChronoUnit.DAYS.between(firstDate, upperBoundary).toInt() + 1
            }

            // Find longest clean streak candidate intervals
            val cleanIntervals = mutableListOf<Pair<LocalDate, LocalDate>>()
            if (activeDates.isEmpty()) {
                if (!firstDate.isAfter(upperBoundary)) {
                    cleanIntervals.add(Pair(firstDate, upperBoundary))
                }
            } else {
                if (firstDate.isBefore(activeDates.first())) {
                    cleanIntervals.add(Pair(firstDate, activeDates.first().minusDays(1)))
                }
                for (i in 1 until activeDates.size) {
                    val s1 = activeDates[i - 1]
                    val s2 = activeDates[i]
                    if (s1.plusDays(1).isBefore(s2)) {
                        cleanIntervals.add(Pair(s1.plusDays(1), s2.minusDays(1)))
                    }
                }
                if (activeDates.last().isBefore(upperBoundary)) {
                    cleanIntervals.add(Pair(activeDates.last().plusDays(1), upperBoundary))
                }
            }

            var maxLen = 0
            cleanIntervals.forEach { (start, end) ->
                val len = ChronoUnit.DAYS.between(start, end).toInt() + 1
                if (len > maxLen) {
                    maxLen = len
                    bestStart = start
                    bestEnd = end
                }
            }
            bestStreak = maxLen
            
            val bestStreakTimeline = formatStreakTimeline(bestStart, bestEnd)

            return HabitStats(
                totalCount = totalSum,
                thisYearCount = thisYearSum,
                thisMonthCount = thisMonthSum,
                lastMonthCount = lastMonthSum,
                percentageChange = percentageChange,
                percentageText = percentageText,
                percentageIsDecrease = percentageIsDecrease,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                bestStreakTimeline = bestStreakTimeline,
                habitStrengthScore = strengthScore,
                habitStrengthStatus = strengthStatus
            )
        }

        if (tracker.frequencyType == "weekly_quota") {
            val streakRes = calculateWeeklyQuotaStreaks(list, today, tracker)
            return HabitStats(
                totalCount = totalSum,
                thisYearCount = thisYearSum,
                thisMonthCount = thisMonthSum,
                lastMonthCount = lastMonthSum,
                percentageChange = percentageChange,
                percentageText = percentageText,
                percentageIsDecrease = percentageIsDecrease,
                currentStreak = streakRes.currentStreak,
                bestStreak = streakRes.bestStreak,
                bestStreakTimeline = formatStreakTimeline(streakRes.bestStreakStart, streakRes.bestStreakEnd),
                habitStrengthScore = strengthScore,
                habitStrengthStatus = strengthStatus
            )
        }

        // Streaks calculation based on dates with entries meeting target count
        val frozenDates = freezeHist.split(",").filter { it.isNotBlank() }.toSet()
        val streakRes = calculateStreaks(list, frozenDates, freezeActive, today, tracker)

        return HabitStats(
            totalCount = totalSum,
            thisYearCount = thisYearSum,
            thisMonthCount = thisMonthSum,
            lastMonthCount = lastMonthSum,
            percentageChange = percentageChange,
            percentageText = percentageText,
            percentageIsDecrease = percentageIsDecrease,
            currentStreak = streakRes.currentStreak,
            bestStreak = streakRes.bestStreak,
            bestStreakTimeline = formatStreakTimeline(streakRes.bestStreakStart, streakRes.bestStreakEnd),
            habitStrengthScore = strengthScore,
            habitStrengthStatus = strengthStatus
        )
    }

    private fun calculateWeeklyQuotaStreaks(
        list: List<HabitEntry>,
        today: LocalDate,
        tracker: TrackerConfig
    ): StreakResult {
        val targetCount = tracker.targetCount.coerceAtLeast(1)
        val weeklyTarget = tracker.weeklyTarget.coerceIn(1, 7)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val completedDates = list
            .groupBy { it.dateString }
            .filter { (_, entries) -> entries.sumOf { it.count } >= targetCount }
            .keys
            .mapNotNull {
                try { LocalDate.parse(it, formatter) } catch (e: Exception) { null }
            }
            .toSet()

        if (completedDates.isEmpty()) return StreakResult(0, 0, null, null)

        val firstCompletedDate = completedDates.minOrNull()!!
        val startMonday = firstCompletedDate.with(java.time.DayOfWeek.MONDAY)
        val currentMonday = today.with(java.time.DayOfWeek.MONDAY)

        val completedWeeks = mutableSetOf<LocalDate>()
        var weekCursor = startMonday
        while (!weekCursor.isAfter(currentMonday)) {
            var distinctCompletedDays = 0
            for (d in 0..6) {
                val day = weekCursor.plusDays(d.toLong())
                if (day in completedDates) {
                    distinctCompletedDays++
                }
            }
            if (distinctCompletedDays >= weeklyTarget) {
                completedWeeks.add(weekCursor)
            }
            weekCursor = weekCursor.plusWeeks(1)
        }

        // 1. Current Streak calculation
        var currentStreak = 0
        val isCurrentWeekCompleted = currentMonday in completedWeeks

        var checkWeek = if (isCurrentWeekCompleted) {
            currentStreak++
            currentMonday.minusWeeks(1)
        } else {
            // Current week in progress; walk backward from previous week without breaking
            currentMonday.minusWeeks(1)
        }

        while (!checkWeek.isBefore(startMonday)) {
            if (checkWeek in completedWeeks) {
                currentStreak++
                checkWeek = checkWeek.minusWeeks(1)
            } else {
                break
            }
        }

        // 2. Best Streak calculation
        var bestStreak = 0
        var bestStart: LocalDate? = null
        var bestEnd: LocalDate? = null

        var tempStreak = 0
        var tempStart: LocalDate? = null
        var tempEnd: LocalDate? = null

        var iterWeek = startMonday
        while (!iterWeek.isAfter(currentMonday)) {
            if (iterWeek in completedWeeks) {
                if (tempStreak == 0) {
                    tempStart = iterWeek
                }
                tempStreak++
                tempEnd = iterWeek.plusDays(6)
                if (tempStreak > bestStreak) {
                    bestStreak = tempStreak
                    bestStart = tempStart
                    bestEnd = tempEnd
                }
            } else {
                tempStreak = 0
                tempStart = null
                tempEnd = null
            }
            iterWeek = iterWeek.plusWeeks(1)
        }

        if (bestStreak < currentStreak) {
            bestStreak = currentStreak
        }

        return StreakResult(currentStreak, bestStreak, bestStart, bestEnd)
    }

    private fun calculateStreaks(
        list: List<HabitEntry>,
        frozenDates: Set<String>, 
        activeFreeze: String?,
        today: LocalDate,
        tracker: TrackerConfig
    ): StreakResult {
        val target = tracker.targetCount.coerceAtLeast(1)
        val completedDates = list
            .groupBy { it.dateString }
            .filter { (_, entries) -> entries.sumOf { it.count } >= target }
            .keys
            .toSet()

        val dates = completedDates + frozenDates + listOfNotNull(activeFreeze)
        val scheduledDays = getScheduledDaysOfWeek(tracker)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        if (dates.isEmpty()) return StreakResult(0, 0, null, null)

        val parsedDates = dates.mapNotNull {
            try { LocalDate.parse(it, formatter) } catch(e: Exception) { null }
        }.sorted()

        if (parsedDates.isEmpty()) return StreakResult(0, 0, null, null)

        val dateSet = parsedDates.toSet()
        val firstDate = parsedDates.first()

        // 1. Current Streak calculation:
        var currentStreak = 0
        var checkDate = today

        val isTodayScheduled = today.dayOfWeek in scheduledDays
        val isTodayCompleted = today in dateSet

        if (isTodayScheduled) {
            if (isTodayCompleted) {
                currentStreak++
                checkDate = today.minusDays(1)
            } else {
                checkDate = today.minusDays(1)
            }
        } else {
            checkDate = today.minusDays(1)
        }

        while (!checkDate.isBefore(firstDate.minusDays(7))) {
            if (checkDate.dayOfWeek in scheduledDays) {
                if (checkDate in dateSet) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                } else {
                    break
                }
            } else {
                // Rest day (non-scheduled): keep walking backward without breaking streak
                checkDate = checkDate.minusDays(1)
            }
        }

        // 2. Best Streak calculation:
        var bestStreak = 0
        var bestStart: LocalDate? = null
        var bestEnd: LocalDate? = null

        var tempStreak = 0
        var tempStart: LocalDate? = null
        var tempEnd: LocalDate? = null

        var cur = firstDate
        while (!cur.isAfter(today)) {
            if (cur.dayOfWeek in scheduledDays) {
                if (cur in dateSet) {
                    if (tempStreak == 0) {
                        tempStart = cur
                    }
                    tempStreak++
                    tempEnd = cur
                    if (tempStreak > bestStreak) {
                        bestStreak = tempStreak
                        bestStart = tempStart
                        bestEnd = tempEnd
                    }
                } else {
                    tempStreak = 0
                    tempStart = null
                    tempEnd = null
                }
            }
            cur = cur.plusDays(1)
        }

        if (bestStreak < currentStreak) {
            bestStreak = currentStreak
        }

        return StreakResult(currentStreak, bestStreak, bestStart, bestEnd)
    }

    private fun formatStreakTimeline(start: LocalDate?, end: LocalDate?): String? {
        if (start == null || end == null) return null
        val startDay = start.dayOfMonth
        val startMonth = start.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).lowercase()
        val startYear = start.year

        val endDay = end.dayOfMonth
        val endMonth = end.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).lowercase()
        val endYear = end.year

        return if (startYear == endYear) {
            if (startMonth == endMonth) {
                if (startDay == endDay) {
                    "$startDay $startMonth $startYear"
                } else {
                    "$startDay–$endDay $startMonth $startYear"
                }
            } else {
                "$startDay $startMonth – $endDay $endMonth $startYear"
            }
        } else {
            "$startDay $startMonth $startYear – $endDay $endMonth $endYear"
        }
    }

    // Export & Backup
    fun getEntriesForExport(): Flow<List<HabitEntry>> {
        return entries
    }

    fun exportBackupJson(currentEntries: List<HabitEntry>): String {
        return try {
            val mainObj = JSONObject()
            
            // Habits logs array
            val logsArray = JSONArray()
            for (entry in currentEntries) {
                val item = JSONObject()
                item.put("date", entry.dateString)
                item.put("count", entry.count)
                item.put("timestamp", entry.timestamp)
                item.put("notes", entry.notes ?: "")
                logsArray.put(item)
            }
            mainObj.put("entries", logsArray)
            mainObj.put("exported_at", System.currentTimeMillis())
            mainObj.put("app", "HabitTrackerBackup")

            mainObj.toString(4)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val mainObj = JSONObject(jsonString)
            if (mainObj.optString("app") != "HabitTrackerBackup") return false

            val logsArray = mainObj.optJSONArray("entries") ?: return false
            val trackerId = selectedTrackerId.value
            val entriesToImport = mutableListOf<HabitEntry>()
            for (i in 0 until logsArray.length()) {
                val item = logsArray.getJSONObject(i)
                val date = item.getString("date")
                val count = item.getInt("count")
                val timestamp = item.getLong("timestamp")
                val notes = item.optString("notes", "")
                entriesToImport.add(
                    HabitEntry(
                        dateString = date,
                        count = count,
                        timestamp = timestamp,
                        notes = if (notes.isEmpty()) null else notes,
                        trackerId = trackerId
                    )
                )
            }

            viewModelScope.launch {
                repository.importEntriesDeduplicated(entriesToImport)
                updateWidget()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── GOOGLE ACCOUNT CLOUD BACKUP & SYNC ──────────────────────────
    fun performCloudBackup(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val allT = trackersRaw.value
                val allE = rawEntries.first()
                val currentP = preferences.first()
                
                val root = JSONObject()
                root.put("app", "HabitTrackerCloudBackup")
                root.put("version", 2)
                root.put("timestamp", System.currentTimeMillis())
                root.put("email", googleAccountEmail.value ?: "device@account")
                root.put("deviceName", android.os.Build.MODEL ?: "Android Device")

                val trackersArr = JSONArray()
                for (t in allT) {
                    val tobj = JSONObject()
                    tobj.put("id", t.id)
                    tobj.put("title", t.title)
                    tobj.put("icon", t.icon)
                    tobj.put("accentColor", t.accentColor ?: "")
                    tobj.put("type", t.type)
                    tobj.put("frequencyType", t.frequencyType)
                    tobj.put("targetDays", t.targetDays)
                    tobj.put("targetCount", t.targetCount)
                    tobj.put("timeOfDay", t.timeOfDay)
                    tobj.put("sortOrder", t.sortOrder)
                    tobj.put("weeklyTarget", t.weeklyTarget)
                    trackersArr.put(tobj)
                }
                root.put("trackers", trackersArr)

                val entriesArr = JSONArray()
                for (e in allE) {
                    val eobj = JSONObject()
                    eobj.put("date", e.dateString)
                    eobj.put("count", e.count)
                    eobj.put("timestamp", e.timestamp)
                    eobj.put("notes", e.notes ?: "")
                    eobj.put("trackerId", e.trackerId)
                    entriesArr.put(eobj)
                }
                root.put("entries", entriesArr)

                val prefsObj = JSONObject()
                for ((k, v) in currentP) {
                    // Do not store circular backup history
                    if (k != "cloud_backup_history") {
                        prefsObj.put(k, v)
                    }
                }
                root.put("preferences", prefsObj)

                val dataJson = root.toString()
                val nowTime = System.currentTimeMillis()
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a")
                val displayDate = java.time.LocalDateTime.now().format(formatter)
                val snapshotId = "snap_${nowTime}"

                val newSnapshot = CloudBackupSnapshot(
                    id = snapshotId,
                    timestamp = nowTime,
                    displayDate = displayDate,
                    deviceName = android.os.Build.MODEL ?: "Android Device",
                    totalTrackers = allT.size,
                    totalEntries = allE.size,
                    dataJson = dataJson
                )

                // Update snapshot history (keep latest 5 snapshots)
                val currentList = cloudBackupSnapshots.value.toMutableList()
                currentList.add(0, newSnapshot)
                val trimmed = currentList.take(5)
                
                val historyArr = JSONArray()
                for (s in trimmed) {
                    val sObj = JSONObject()
                    sObj.put("id", s.id)
                    sObj.put("timestamp", s.timestamp)
                    sObj.put("displayDate", s.displayDate)
                    sObj.put("deviceName", s.deviceName)
                    sObj.put("totalTrackers", s.totalTrackers)
                    sObj.put("totalEntries", s.totalEntries)
                    sObj.put("dataJson", s.dataJson)
                    historyArr.put(sObj)
                }

                repository.setPreference("cloud_backup_history", historyArr.toString())
                repository.setPreference("last_cloud_backup_time", displayDate)
                onComplete(true, "Device snapshot created successfully ($displayDate)")
            } catch (e: Exception) {
                onComplete(false, "Backup failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun restoreFromCloudSnapshot(snapshot: CloudBackupSnapshot, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val root = JSONObject(snapshot.dataJson)
                if (root.optString("app") != "HabitTrackerCloudBackup") {
                    onComplete(false)
                    return@launch
                }

                // 1. Restore Trackers
                val trackersList = mutableListOf<Tracker>()
                val trackersArr = root.optJSONArray("trackers")
                if (trackersArr != null) {
                    for (i in 0 until trackersArr.length()) {
                        val obj = trackersArr.getJSONObject(i)
                        trackersList.add(
                            Tracker(
                                id = obj.getString("id"),
                                title = obj.getString("title"),
                                icon = obj.getString("icon"),
                                accentColor = obj.optString("accentColor").takeIf { it.isNotBlank() },
                                type = obj.optString("type", "good"),
                                frequencyType = obj.optString("frequencyType", "daily"),
                                targetDays = obj.optString("targetDays", "MON,TUE,WED,THU,FRI,SAT,SUN"),
                                targetCount = obj.optInt("targetCount", 1),
                                timeOfDay = obj.optString("timeOfDay", "anytime"),
                                sortOrder = obj.optInt("sortOrder", i),
                                weeklyTarget = obj.optInt("weeklyTarget", 0)
                            )
                        )
                    }
                }

                // 2. Restore Entries
                val entriesList = mutableListOf<HabitEntry>()
                val entriesArr = root.optJSONArray("entries")
                if (entriesArr != null) {
                    for (i in 0 until entriesArr.length()) {
                        val obj = entriesArr.getJSONObject(i)
                        val notes = obj.optString("notes", "")
                        entriesList.add(
                            HabitEntry(
                                dateString = obj.getString("date"),
                                count = obj.getInt("count"),
                                timestamp = obj.getLong("timestamp"),
                                notes = if (notes.isEmpty()) null else notes,
                                trackerId = obj.optString("trackerId", "sobriety")
                            )
                        )
                    }
                }

                // 3. Restore Preferences
                val prefsMap = mutableMapOf<String, String>()
                val prefsObj = root.optJSONObject("preferences")
                if (prefsObj != null) {
                    val keys = prefsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k != "cloud_backup_history") {
                            prefsMap[k] = prefsObj.getString(k)
                        }
                    }
                }

                repository.restoreDatabaseTransaction(trackersList, entriesList, prefsMap)
                updateWidget()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deleteCloudSnapshot(snapshotId: String) {
        viewModelScope.launch {
            val currentList = cloudBackupSnapshots.value.filter { it.id != snapshotId }
            val historyArr = JSONArray()
            for (s in currentList) {
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("timestamp", s.timestamp)
                sObj.put("displayDate", s.displayDate)
                sObj.put("deviceName", s.deviceName)
                sObj.put("totalTrackers", s.totalTrackers)
                sObj.put("totalEntries", s.totalEntries)
                sObj.put("dataJson", s.dataJson)
                historyArr.put(sObj)
            }
            repository.setPreference("cloud_backup_history", historyArr.toString())
        }
    }

    private fun parseCloudSnapshots(rawJson: String): List<CloudBackupSnapshot> {
        if (rawJson.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<CloudBackupSnapshot>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CloudBackupSnapshot(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        displayDate = obj.getString("displayDate"),
                        deviceName = obj.optString("deviceName", "Android Device"),
                        totalTrackers = obj.optInt("totalTrackers", 0),
                        totalEntries = obj.optInt("totalEntries", 0),
                        dataJson = obj.optString("dataJson", "{}")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class CloudBackupSnapshot(
    val id: String,
    val timestamp: Long,
    val displayDate: String,
    val deviceName: String,
    val totalTrackers: Int,
    val totalEntries: Int,
    val dataJson: String
)

data class HabitStats(
    val totalCount: Int = 0,
    val thisYearCount: Int = 0,
    val thisMonthCount: Int = 0,
    val lastMonthCount: Int = 0,
    val percentageChange: Double = 0.0,
    val percentageText: String = "0%",
    val percentageIsDecrease: Boolean = false,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val bestStreakTimeline: String? = null,
    val habitStrengthScore: Int = 0,
    val habitStrengthStatus: String = "Starting 🌱"
)

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val bestStreakStart: LocalDate?,
    val bestStreakEnd: LocalDate?
)

data class TrackerConfig(
    val id: String,
    val title: String,
    val icon: String,
    val accentColor: String? = null,
    val type: String = "good",
    val frequencyType: String = "daily",
    val targetDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val targetCount: Int = 1,
    val timeOfDay: String = "anytime",
    val weeklyTarget: Int = 0
)

data class QuickCheckInRecord(
    val token: Long = System.nanoTime(),
    val trackerId: String,
    val dateString: String,
    val previousCount: Int,
    val newCount: Int,
    val previousNotes: String?,
    val entryId: Int?
)
