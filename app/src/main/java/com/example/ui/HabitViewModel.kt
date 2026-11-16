package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.HabitRepository
import com.example.data.SampleTrackerSeeder
import com.example.data.SnapshotFileStore
import com.example.data.Tracker
import com.example.worker.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

open class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val appDb = AppDatabase.getDatabase(application)
    private val repository: HabitRepository = HabitRepository(appDb, appDb.habitDao(), appDb.trackerDao())
    private val snapshotStore = SnapshotFileStore(application)
    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    // Expose flows of raw tables
    private val rawEntries: Flow<List<HabitEntry>> = repository.allEntries

    // Replay=1 SharedFlows ensure derived flows wait for the real DB emission instead of pushing emptyMap()/emptyList() on launch
    private val rawPreferencesFlow: SharedFlow<Map<String, String>> = repository.allPreferences
        .map { list -> list.associate { it.key to it.value } }
        .flowOn(Dispatchers.IO)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    val preferences: StateFlow<Map<String, String>> = rawPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val rawTrackersFlow: SharedFlow<List<Tracker>> = repository.allTrackers
        .flowOn(Dispatchers.IO)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    val allTrackers: StateFlow<List<TrackerConfig>> = rawTrackersFlow.map { list ->
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
                weeklyTarget = it.weeklyTarget,
                isArchived = it.isArchived,
                reminderEnabled = it.reminderEnabled,
                reminderTime = it.reminderTime
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackers: StateFlow<List<TrackerConfig>> = allTrackers.map { list ->
        list.filter { !it.isArchived }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedTrackers: StateFlow<List<TrackerConfig>> = allTrackers.map { list ->
        list.filter { it.isArchived }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTrackerId: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["selected_tracker_id"] ?: SampleTrackerSeeder.SAMPLE_TRACKER_ID
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleTrackerSeeder.SAMPLE_TRACKER_ID)

    val activeTracker: StateFlow<TrackerConfig> = combine(trackers, selectedTrackerId) { list, activeId ->
        list.find { it.id == activeId } ?: list.firstOrNull() ?: TrackerConfig(SampleTrackerSeeder.SAMPLE_TRACKER_ID, SampleTrackerSeeder.SAMPLE_TRACKER_TITLE, SampleTrackerSeeder.SAMPLE_TRACKER_ICON, SampleTrackerSeeder.SAMPLE_ACCENT_HEX, "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "morning", 0, isArchived = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackerConfig(SampleTrackerSeeder.SAMPLE_TRACKER_ID, SampleTrackerSeeder.SAMPLE_TRACKER_TITLE, SampleTrackerSeeder.SAMPLE_TRACKER_ICON, SampleTrackerSeeder.SAMPLE_ACCENT_HEX, "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "morning", 0, isArchived = false))

    val streakFreezeDate: StateFlow<String?> = rawPreferencesFlow.map { prefs ->
        val v = prefs["streak_freeze_date"]
        if (v.isNullOrEmpty()) null else v
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val streakFreezeHistory: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["streak_freeze_history"] ?: ""
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Dynamically derive title and icon from active tracker
    val habitTitle: StateFlow<String> = activeTracker.map { it.title }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleTrackerSeeder.SAMPLE_TRACKER_TITLE)

    val habitIcon: StateFlow<String> = activeTracker.map { it.icon }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleTrackerSeeder.SAMPLE_TRACKER_ICON)

    // Filtered entries flow reactively bound to active trackerId using indexed Room query
    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<List<HabitEntry>> = activeTracker.map { it.id }.distinctUntilChanged().flatMapLatest { activeId ->
        repository.getEntriesForTracker(activeId)
    }

    val selectedTheme: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["selected_theme"] ?: ThemeStyles.DEFAULT_THEME_ID
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeStyles.DEFAULT_THEME_ID)

    val favoriteThemes: StateFlow<Set<String>> = rawPreferencesFlow.map { prefs ->
        prefs["favorite_themes"]
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val customPrimaryColor: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["custom_primary_color"] ?: "#C32021"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#C32021")

    val customBackgroundColor: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["custom_background_color"] ?: "#EBE4D3"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#EBE4D3")

    val selectedFont: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["selected_font"] ?: ThemeStyles.DEFAULT_FONT_ID
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeStyles.DEFAULT_FONT_ID)

    val calendarCellShape: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["calendar_cell_shape"] ?: "rounded_square"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "rounded_square")

    val checkmarkStyle: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["checkmark_style"] ?: "tick"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "tick")

    val swipeHintShown: StateFlow<Boolean> = rawPreferencesFlow.map { prefs ->
        prefs["swipe_hint_shown"] == "true"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderTime: StateFlow<String> = rawPreferencesFlow.map { prefs ->
        prefs["reminder_time"] ?: "21:00"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "21:00")

    val remindersEnabled: StateFlow<Boolean> = rawPreferencesFlow.map { prefs ->
        prefs["reminders_enabled"] == "true"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dayRolloverHour: StateFlow<Int> = rawPreferencesFlow.map { prefs ->
        prefs["day_rollover_hour"]?.toIntOrNull() ?: 0
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val celebrationsEnabled: StateFlow<Boolean> = rawPreferencesFlow.map { prefs ->
        prefs["celebrations_enabled"] != "false"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasSeenOnboarding: StateFlow<Boolean> = rawPreferencesFlow.map { prefs ->
        prefs["has_seen_onboarding"] != "false"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSupporter: StateFlow<Boolean> = rawPreferencesFlow.map { prefs ->
        prefs["is_supporter"] == "true"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val supporterVaultFreezes: StateFlow<Int> = rawPreferencesFlow.map { prefs ->
        prefs["supporter_vault_freezes"]?.toIntOrNull() ?: 3
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val supporterBadgeEnabled: StateFlow<Boolean> = rawPreferencesFlow.map { prefs ->
        prefs["supporter_badge_enabled"] != "false"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val googleAccountEmail: StateFlow<String?> = rawPreferencesFlow.map { prefs ->
        prefs["google_account_email"]?.takeIf { it.isNotBlank() }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleAccountName: StateFlow<String?> = rawPreferencesFlow.map { prefs ->
        prefs["google_account_name"]?.takeIf { it.isNotBlank() }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastCloudBackupTime: StateFlow<String?> = rawPreferencesFlow.map { prefs ->
        prefs["last_cloud_backup_time"]?.takeIf { it.isNotBlank() }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoCloudBackupEnabled: StateFlow<Boolean> = rawPreferencesFlow.map { prefs ->
        prefs["auto_cloud_backup_enabled"] != "false"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDemoTrackerLoaded: StateFlow<Boolean> = allTrackers.map { list ->
        list.any { it.id == SampleTrackerSeeder.SAMPLE_TRACKER_ID }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showDemoBanner: StateFlow<Boolean> = combine(
        isDemoTrackerLoaded,
        rawPreferencesFlow
    ) { demoLoaded, prefs ->
        demoLoaded && prefs["demo_banner_dismissed"] != "true"
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _cloudBackupSnapshots = MutableStateFlow<List<CloudBackupSnapshot>>(emptyList())
    val cloudBackupSnapshots: StateFlow<List<CloudBackupSnapshot>> = _cloudBackupSnapshots.asStateFlow()

    private val initJob = viewModelScope.launch(Dispatchers.IO) {
        if (repository.getAllTrackersList().isEmpty()) {
            SampleTrackerSeeder.seedViaRepository(repository)
        }

        // Load file-backed snapshots and migrate any legacy preference JSON blob once
        val legacySnapshotRaw = repository.getPreference("cloud_backup_history")
        val loadedSnapshots = if (!legacySnapshotRaw.isNullOrBlank()) {
            val migrated = snapshotStore.migrateLegacyPreferenceJson(legacySnapshotRaw)
            repository.setPreference("cloud_backup_history", "")
            migrated
        } else {
            snapshotStore.loadAllSnapshots()
        }
        _cloudBackupSnapshots.value = loadedSnapshots

        // Reconcile expired streak freeze only when a freeze date is actually active
        val freezeActive = repository.getPreference("streak_freeze_date")?.takeIf { it.isNotBlank() }
        if (freezeActive != null) {
            val rollover = repository.getPreference("day_rollover_hour")?.toIntOrNull() ?: 0
            val today = ReminderManager.getEffectiveToday(rollover)
            val freezeDate = try { LocalDate.parse(freezeActive, DATE_FORMATTER) } catch (e: Exception) { null }
            if (freezeDate != null && freezeDate.isBefore(today)) {
                val activeTrackerId = repository.getPreference("selected_tracker_id") ?: SampleTrackerSeeder.SAMPLE_TRACKER_ID
                val hasEntry = repository.getEntriesForTrackerAndDate(activeTrackerId, freezeActive).any { it.count > 0 }
                if (!hasEntry) {
                    val freezeHist = repository.getPreference("streak_freeze_history") ?: ""
                    val newHist = if (freezeHist.isEmpty()) freezeActive else "$freezeHist,$freezeActive"
                    repository.setPreference("streak_freeze_history", newHist)
                }
                repository.setPreference("streak_freeze_date", "")
            }
        }

        // Reconcile scheduled per-habit and global reminders on app launch
        try {
            ReminderManager.rescheduleAllFromDatabase(getApplication(), appDb)
        } catch (_: Exception) {
        }
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        viewModelScope.launch {
            repository.setPreference("has_seen_onboarding", seen.toString())
        }
    }

    fun dismissDemoBanner() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPreference("demo_banner_dismissed", "true")
        }
    }

    fun clearDemoTracker() {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            ReminderManager.cancelTrackerReminder(getApplication(), SampleTrackerSeeder.SAMPLE_TRACKER_ID)
            repository.deleteTrackerById(SampleTrackerSeeder.SAMPLE_TRACKER_ID)
            repository.setPreference("demo_banner_dismissed", "true")
            val remaining = repository.getAllTrackersList().filter { !it.isArchived }
            if (selectedTrackerId.value == SampleTrackerSeeder.SAMPLE_TRACKER_ID && remaining.isNotEmpty()) {
                repository.setPreference("selected_tracker_id", remaining.first().id)
            }
            updateWidget()
        }
    }

    fun loadDemoTracker() {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val sampleTracker = SampleTrackerSeeder.buildSampleTracker()
            val existing = repository.getAllTrackersList().find { it.id == SampleTrackerSeeder.SAMPLE_TRACKER_ID }
            if (existing == null) {
                repository.insertTracker(sampleTracker)
                for (entry in SampleTrackerSeeder.buildSampleEntries(getEffectiveToday())) {
                    repository.insertEntry(entry)
                }
            }
            repository.setPreference("demo_banner_dismissed", "false")
            repository.setPreference("selected_tracker_id", SampleTrackerSeeder.SAMPLE_TRACKER_ID)
            updateWidget()
        }
    }

    fun setSupporter(supporter: Boolean) {
        viewModelScope.launch {
            repository.setPreference("is_supporter", supporter.toString())
            if (supporter) {
                repository.setPreference("supporter_badge_enabled", "true")
                if (repository.getPreference("supporter_vault_freezes")?.toIntOrNull() == null) {
                    repository.setPreference("supporter_vault_freezes", "3")
                }
            }
            updateWidget()
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

    internal suspend fun awaitInitialized() {
        initJob.join()
    }

    internal fun clearForTest() {
        onCleared()
    }

    fun useVaultFreeze(onComplete: ((Boolean, String) -> Unit)? = null): Boolean {
        val current = supporterVaultFreezes.value
        val todayStr = getEffectiveToday().format(DATE_FORMATTER)
        if (streakFreezeDate.value == todayStr) {
            onComplete?.invoke(false, "Today is already protected by a Streak Freeze.")
            return false
        }
        if (current <= 0) {
            onComplete?.invoke(false, "Your Freeze Vault is empty. Refill it from Supporter Vault.")
            return false
        }
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val isSupporterPref = repository.getPreference("is_supporter") == "true"
            if (!isSupporterPref) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Activate Supporter Edition to unlock the Streak Freeze Vault.")
                }
                return@launch
            }
            val rollover = repository.getPreference("day_rollover_hour")?.toIntOrNull() ?: 0
            val effectiveTodayStr = ReminderManager.getEffectiveToday(rollover).format(DATE_FORMATTER)
            val latestCount = repository.getPreference("supporter_vault_freezes")?.toIntOrNull() ?: 3
            val latestFreeze = repository.getPreference("streak_freeze_date")
            if (latestFreeze == effectiveTodayStr) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Today is already protected by a Streak Freeze.")
                }
                return@launch
            }
            if (latestCount <= 0) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Your Freeze Vault is empty. Refill it from Supporter Vault.")
                }
                return@launch
            }
            val currentHist = repository.getPreference("streak_freeze_history") ?: ""
            val historySet = currentHist.split(",").filter { it.isNotBlank() }.toMutableSet()
            historySet.add(effectiveTodayStr)
            repository.setPreference("streak_freeze_date", effectiveTodayStr)
            repository.setPreference("streak_freeze_history", historySet.joinToString(","))
            val remaining = (latestCount - 1).coerceAtLeast(0)
            repository.setPreference("supporter_vault_freezes", remaining.toString())
            updateWidget()
            withContext(Dispatchers.Main) {
                onComplete?.invoke(true, "Vault Freeze activated for today ($remaining left).")
            }
        }
        return true
    }

    fun refillVaultFreezes() {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            repository.setPreference("supporter_vault_freezes", "5")
            updateWidget()
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
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalCount: StateFlow<Int> = entries.map { list ->
        list.sumOf { it.count }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stats: StateFlow<HabitStats> = combine(entries, activeTracker, streakFreezeDate, streakFreezeHistory) { list, tracker, freezeActive, freezeHist ->
        calculateStats(list, freezeActive, freezeHist, tracker)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitStats())

    private val _reportSelectedTrackerId = MutableStateFlow<String?>(null)
    val reportSelectedTrackerId: StateFlow<String?> = _reportSelectedTrackerId.asStateFlow()

    fun selectReportTracker(id: String?) {
        _reportSelectedTrackerId.value = id
    }

    val reportTracker: StateFlow<TrackerConfig> = combine(allTrackers, _reportSelectedTrackerId, activeTracker) { all, reportId, active ->
        if (reportId != null) all.find { it.id == reportId } ?: active else active
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackerConfig("sobriety", "Sobriety", "🚭", null, "good", "daily", "MON,TUE,WED,THU,FRI,SAT,SUN", 1, "anytime", 0, isArchived = false))

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportEntries: Flow<List<HabitEntry>> = reportTracker.map { it.id }.distinctUntilChanged().flatMapLatest { id ->
        repository.getEntriesForTracker(id)
    }

    val reportStats: StateFlow<HabitStats> = combine(reportEntries, reportTracker, streakFreezeDate, streakFreezeHistory) { list, tracker, freezeActive, freezeHist ->
        calculateStats(list, freezeActive, freezeHist, tracker)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitStats())

    val reportAnalytics: StateFlow<ReportAnalyticsSnapshot> = combine(reportEntries, reportTracker, dayRolloverHour) { entries, tracker, rollover ->
        HabitAnalyticsEngine.build(
            entries = entries,
            tracker = tracker,
            today = ReminderManager.getEffectiveToday(rollover)
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportAnalyticsSnapshot())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recentWeekEntriesFlow: Flow<List<HabitEntry>> = rawPreferencesFlow
        .map { prefs -> prefs["day_rollover_hour"]?.toIntOrNull() ?: 0 }
        .distinctUntilChanged()
        .flatMapLatest { rolloverHour ->
            val effectiveToday = ReminderManager.getEffectiveToday(rolloverHour)
            val minDate = effectiveToday.with(java.time.DayOfWeek.MONDAY).minusDays(1).toString()
            repository.getEntriesFromDate(minDate)
        }

    val todayDashboard: StateFlow<TodayDashboardState> = combine(
        trackers,
        recentWeekEntriesFlow,
        rawPreferencesFlow
    ) { activeTrackers, weekEntries, prefs ->
        TodayDashboardCalculator.build(
            trackers = activeTrackers,
            recentEntries = weekEntries,
            preferences = prefs
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayDashboardState())

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
        weeklyTarget: Int = 0,
        reminderEnabled: Boolean = false,
        reminderTime: String = "21:00"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val all = repository.getAllTrackersList()
            val newId = "tracker_" + System.currentTimeMillis()
            val newTracker = Tracker(
                id = newId,
                title = title,
                icon = icon,
                sortOrder = all.size,
                accentColor = accentColor,
                type = type,
                frequencyType = frequencyType,
                targetDays = targetDays,
                targetCount = targetCount,
                timeOfDay = timeOfDay,
                weeklyTarget = weeklyTarget,
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime
            )
            repository.insertTracker(newTracker)
            repository.setPreference("selected_tracker_id", newId)
            if (type == "bad") {
                repository.setPreference("checkmark_style", "cross")
            }
            ReminderManager.syncTrackerReminder(getApplication(), newTracker)
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
        weeklyTarget: Int = 0,
        reminderEnabled: Boolean? = null,
        reminderTime: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val existing = repository.getAllTrackersList().find { it.id == id }
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    icon = icon,
                    accentColor = accentColor,
                    type = type,
                    frequencyType = frequencyType,
                    targetDays = targetDays,
                    targetCount = targetCount,
                    timeOfDay = timeOfDay,
                    weeklyTarget = weeklyTarget,
                    reminderEnabled = reminderEnabled ?: existing.reminderEnabled,
                    reminderTime = reminderTime ?: existing.reminderTime
                )
                repository.updateTracker(updated)
                ReminderManager.syncTrackerReminder(getApplication(), updated)
                updateWidget()
            }
        }
    }

    fun setTrackerReminder(trackerId: String, enabled: Boolean, time: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val existing = repository.getAllTrackersList().find { it.id == trackerId } ?: return@launch
            val updated = existing.copy(
                reminderEnabled = enabled,
                reminderTime = time ?: existing.reminderTime
            )
            repository.updateTracker(updated)
            ReminderManager.syncTrackerReminder(getApplication(), updated)
            updateWidget()
        }
    }

    fun reorderTrackers(reorderedList: List<TrackerConfig>) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllTrackersList()
            reorderedList.forEachIndexed { index, config ->
                val tracker = all.find { it.id == config.id }
                if (tracker != null && tracker.sortOrder != index) {
                    repository.updateTracker(tracker.copy(sortOrder = index))
                }
            }
            updateWidget()
        }
    }

    fun archiveTracker(trackerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val all = repository.getAllTrackersList()
            val target = all.find { it.id == trackerId } ?: return@launch
            if (target.isArchived) return@launch // Idempotent

            repository.setTrackerArchived(trackerId, true)
            ReminderManager.cancelTrackerReminder(getApplication(), trackerId)

            // If the archived tracker was selected, switch selection to next available active tracker
            if (selectedTrackerId.value == trackerId) {
                val nextActive = all.firstOrNull { it.id != trackerId && !it.isArchived }
                if (nextActive != null) {
                    repository.setPreference("selected_tracker_id", nextActive.id)
                }
            }
            updateWidget()
        }
    }

    fun unarchiveTracker(trackerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val all = repository.getAllTrackersList()
            val target = all.find { it.id == trackerId } ?: return@launch
            if (!target.isArchived) return@launch // Idempotent

            repository.setTrackerArchived(trackerId, false)
            ReminderManager.syncTrackerReminder(getApplication(), target.copy(isArchived = false))
            updateWidget()
        }
    }

    fun deleteTracker(trackerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            ReminderManager.cancelTrackerReminder(getApplication(), trackerId)
            // Delete associated tracker; CASCADE will delete its entries too
            repository.deleteTrackerById(trackerId)

            // If deleted was selected, fallback to the remaining active first
            val currentList = repository.getAllTrackersList().filter { it.id != trackerId && !it.isArchived }
            if (currentList.isNotEmpty() && selectedTrackerId.value == trackerId) {
                repository.setPreference("selected_tracker_id", currentList.first().id)
            }
            updateWidget()
        }
    }

    // Database Actions
    fun insertEntry(entry: HabitEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetTracker = repository.getAllTrackersList().find { it.id == entry.trackerId }
            if (targetTracker != null && targetTracker.isArchived) {
                return@launch // Disallow check-in on archived tracker
            }
            repository.insertEntry(entry)
            updateWidget()
        }
    }

    fun insertEntry(dateString: String, count: Int = 1, notes: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentId = selectedTrackerId.value
            val targetTracker = repository.getAllTrackersList().find { it.id == currentId }
            if (targetTracker != null && targetTracker.isArchived) {
                return@launch // Disallow check-in on archived tracker
            }
            repository.insertEntry(
                HabitEntry(
                    dateString = dateString,
                    count = count,
                    notes = notes,
                    trackerId = currentId
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
        quickIncrementForTracker(
            trackerId = selectedTrackerId.value,
            dateString = dateString,
            moodTag = moodTag,
            onLogged = onLogged
        )
    }

    fun quickIncrementForTracker(
        trackerId: String,
        dateString: String,
        moodTag: String? = null,
        onLogged: ((QuickCheckInRecord) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            initJob.join()
            val targetTracker = repository.getAllTrackersList().find { it.id == trackerId }
            if (targetTracker != null && targetTracker.isArchived) {
                return@launch // Disallow check-in on archived tracker
            }
            val existing = repository.getEntriesForTrackerAndDate(trackerId, dateString)
            val formattedMood = moodTag?.let { "[MOOD:$it]" }
            val token = System.nanoTime()
            val record: QuickCheckInRecord

            if (existing.isEmpty()) {
                val newEntry = HabitEntry(
                    dateString = dateString,
                    count = 1,
                    trackerId = trackerId,
                    notes = formattedMood
                )
                repository.insertEntry(newEntry)
                val inserted = repository.getEntriesForTrackerAndDate(trackerId, dateString).lastOrNull()
                record = QuickCheckInRecord(
                    token = token,
                    trackerId = trackerId,
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
                    trackerId = trackerId,
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
            updateWidget()
        }
    }

    fun toggleFavoriteTheme(themeId: String) {
        val cleanId = themeId.trim()
        if (cleanId.isEmpty()) return
        viewModelScope.launch {
            val currentRaw = repository.getPreference("favorite_themes").orEmpty()
            val updated = currentRaw
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            if (!updated.add(cleanId)) {
                updated.remove(cleanId)
            }
            repository.setPreference("favorite_themes", updated.sorted().joinToString(","))
        }
    }

    fun setCustomPrimaryColorHex(hex: String) {
        viewModelScope.launch {
            repository.setPreference("custom_primary_color", hex)
            updateWidget()
        }
    }

    fun setCustomBackgroundColorHex(hex: String) {
        viewModelScope.launch {
            repository.setPreference("custom_background_color", hex)
            updateWidget()
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
        val today = getEffectiveToday().format(DATE_FORMATTER)
        viewModelScope.launch {
            repository.setPreference("streak_freeze_date", today)
            val currentHist = repository.getPreference("streak_freeze_history") ?: ""
            val historySet = currentHist.split(",").filter { it.isNotBlank() }.toMutableSet()
            historySet.add(today)
            repository.setPreference("streak_freeze_history", historySet.joinToString(","))
            updateWidget()
        }
    }

    // Stats calculations (delegated to HabitStatsCalculator; kept private calculateStats signature for test compatibility)
    private fun calculateStats(
        list: List<HabitEntry>,
        freezeActive: String?,
        freezeHist: String,
        tracker: TrackerConfig
    ): HabitStats = HabitStatsCalculator.calculateStats(
        list = list,
        freezeActive = freezeActive,
        freezeHist = freezeHist,
        tracker = tracker,
        today = getEffectiveToday()
    )

    // Export & Backup
    fun getEntriesForExport(): Flow<List<HabitEntry>> {
        return entries
    }

    suspend fun buildFullExportJson(): String {
        val allT = repository.getAllTrackersList()
        val allE = rawEntries.first()
        val currentP = rawPreferencesFlow.first()
        return BackupJsonCodec.encodeFullBackupJson(
            trackers = allT,
            entries = allE,
            preferences = currentP
        )
    }

    fun prepareFullExportJson(onReady: (json: String, habitCount: Int, entryCount: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val allT = repository.getAllTrackersList()
            val allE = rawEntries.first()
            val currentP = rawPreferencesFlow.first()
            val json = BackupJsonCodec.encodeFullBackupJson(
                trackers = allT,
                entries = allE,
                preferences = currentP
            )
            withContext(Dispatchers.Main) {
                onReady(json, allT.size, allE.size)
            }
        }
    }

    fun exportBackupJson(currentEntries: List<HabitEntry>): String {
        return try {
            val allT = allTrackers.value.mapIndexed { idx, tc ->
                Tracker(
                    id = tc.id,
                    title = tc.title,
                    icon = tc.icon,
                    sortOrder = idx,
                    accentColor = tc.accentColor,
                    type = tc.type,
                    frequencyType = tc.frequencyType,
                    targetDays = tc.targetDays,
                    targetCount = tc.targetCount,
                    timeOfDay = tc.timeOfDay,
                    weeklyTarget = tc.weeklyTarget,
                    isArchived = tc.isArchived,
                    reminderEnabled = tc.reminderEnabled,
                    reminderTime = tc.reminderTime
                )
            }
            val currentP = preferences.value
            BackupJsonCodec.encodeFullBackupJson(
                trackers = allT,
                entries = currentEntries,
                preferences = currentP
            )
        } catch (e: Exception) {
            "{}"
        }
    }

    fun previewBackupImport(jsonString: String): BackupParseResult {
        val fallbackId = selectedTrackerId.value.ifBlank { "sobriety" }
        return BackupJsonCodec.parseAndValidate(jsonString, fallbackTrackerId = fallbackId)
    }

    fun applyBackupImport(
        preview: BackupPreview,
        mode: BackupImportMode,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val preRestoreIds = repository.getAllTrackersList().map { it.id }
                if (mode == BackupImportMode.REPLACE) {
                    val trackersToRestore = if (preview.trackers.isNotEmpty()) {
                        preview.trackers
                    } else {
                        repository.getAllTrackersList()
                    }
                    repository.restoreDatabaseTransaction(
                        trackers = trackersToRestore,
                        entries = preview.entries,
                        preferences = preview.preferences
                    )
                    for (oldId in preRestoreIds) {
                        ReminderManager.cancelTrackerReminder(getApplication(), oldId)
                    }
                    val firstActive = trackersToRestore.firstOrNull { !it.isArchived } ?: trackersToRestore.firstOrNull()
                    if (firstActive != null) {
                        selectTracker(firstActive.id)
                    }
                } else {
                    repository.mergeDatabaseTransaction(
                        trackers = preview.trackers,
                        entries = preview.entries,
                        preferences = preview.preferences
                    )
                }
                ReminderManager.rescheduleAllFromDatabase(getApplication(), appDb)
                updateWidget()
                val verb = if (mode == BackupImportMode.REPLACE) "Restored" else "Merged"
                val msg = "$verb ${preview.habitCount} habits and ${preview.entryCount} entries successfully."
                withContext(Dispatchers.Main) {
                    onComplete(true, msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Import failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun importBackupJson(jsonString: String, onComplete: (Boolean) -> Unit = {}): Boolean {
        val parsed = previewBackupImport(jsonString)
        if (parsed !is BackupParseResult.Valid) {
            onComplete(false)
            return false
        }
        applyBackupImport(parsed.preview, BackupImportMode.MERGE) { ok, _ ->
            onComplete(ok)
        }
        return true
    }

    // ── LOCAL DEVICE SNAPSHOTS ──────────────────────────
    fun performCloudBackup(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                initJob.join()
                val allT = repository.getAllTrackersList()
                val allE = rawEntries.first()
                val currentP = rawPreferencesFlow.first()

                val dataJson = BackupJsonCodec.encodeFullBackupJson(
                    trackers = allT,
                    entries = allE,
                    preferences = currentP,
                    prettyIndentSpaces = 0
                )
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

                _cloudBackupSnapshots.value = snapshotStore.saveSnapshot(newSnapshot)
                repository.setPreference("last_cloud_backup_time", displayDate)
                withContext(Dispatchers.Main) {
                    onComplete(true, "Device snapshot created ($displayDate)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Snapshot failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun restoreFromCloudSnapshot(snapshot: CloudBackupSnapshot, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = BackupJsonCodec.parseAndValidate(snapshot.dataJson)
                if (parsed !is BackupParseResult.Valid) {
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }
                val preview = parsed.preview
                val preRestoreIds = repository.getAllTrackersList().map { it.id }
                repository.restoreDatabaseTransaction(
                    trackers = preview.trackers,
                    entries = preview.entries,
                    preferences = preview.preferences
                )
                for (oldId in preRestoreIds) {
                    ReminderManager.cancelTrackerReminder(getApplication(), oldId)
                }
                ReminderManager.rescheduleAllFromDatabase(getApplication(), appDb)
                updateWidget()
                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun deleteCloudSnapshot(snapshotId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _cloudBackupSnapshots.value = snapshotStore.deleteSnapshot(snapshotId)
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
    val weeklyTarget: Int = 0,
    val isArchived: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "21:00"
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
