package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.HabitEntry
import com.example.ui.HabitStats
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.components.ConfettiCelebration
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

class HomeScreenDialogState {
    var selectedDateForDetails by mutableStateOf<LocalDate?>(null)
    var showQuickAddDialog by mutableStateOf(false)
    var showMonthPicker by mutableStateOf(false)
    var showDonateDialog by mutableStateOf(false)
    var showDailyJournalDialog by mutableStateOf(false)
    var showCloudBackupModal by mutableStateOf(false)
    var showArchivedHabitsSheet by mutableStateOf(false)
    var journalTargetDate by mutableStateOf<LocalDate?>(null)
    var showAddTrackerDialog by mutableStateOf(false)
    var trackerToArchive by mutableStateOf<TrackerConfig?>(null)
    var trackerToDelete by mutableStateOf<TrackerConfig?>(null)
    var trackerToManage by mutableStateOf<TrackerConfig?>(null)
    var trackerToEdit by mutableStateOf<TrackerConfig?>(null)
}

data class HomeThemeConfig(
    val selectedTheme: String,
    val bgColor: Color,
    val primaryColor: Color,
    val cardBgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val appFont: FontFamily,
    val cellShape: Shape,
    val checkmarkStyleName: String
)

@Composable
fun HomeScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val dayRolloverHour by viewModel.dayRolloverHour.collectAsState()
    val celebrationsEnabled by viewModel.celebrationsEnabled.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    var showConfetti by remember { mutableStateOf(false) }
    var selectedRoutineFilter by remember { mutableStateOf("all") }
    var selectedMoodTag by remember { mutableStateOf<String?>("😊") }

    val today = remember(dayRolloverHour) { viewModel.getEffectiveToday() }
    val baseMonth = remember { YearMonth.now() }
    val initialPage = remember { Int.MAX_VALUE / 2 }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )
    val coroutineScope = rememberCoroutineScope()
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }

    val habitTitle by viewModel.habitTitle.collectAsState()
    val habitIcon by viewModel.habitIcon.collectAsState()
    val activeTracker by viewModel.activeTracker.collectAsState()
    val completionsByDate by viewModel.completionsByDate.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val selectedFontName by viewModel.selectedFont.collectAsState()
    val calendarCellShapeName by viewModel.calendarCellShape.collectAsState()
    val checkmarkStyleName by viewModel.checkmarkStyle.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val freezeDate by viewModel.streakFreezeDate.collectAsState()

    val dialogState = remember { HomeScreenDialogState() }
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()

    val themeConfig = remember(selectedTheme, customBgHex, customHex, selectedFontName, calendarCellShapeName, checkmarkStyleName) {
        HomeThemeConfig(
            selectedTheme = selectedTheme,
            bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex),
            primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex),
            cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex),
            borderColor = ThemeStyles.getBorderColor(selectedTheme),
            textColor = ThemeStyles.getTextColor(selectedTheme),
            secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme),
            appFont = ThemeStyles.getSelectedFontFamily(selectedFontName),
            cellShape = ThemeStyles.getCalendarCellShape(calendarCellShapeName),
            checkmarkStyleName = checkmarkStyleName
        )
    }

    val trackers by viewModel.trackers.collectAsState()
    val archivedTrackers by viewModel.archivedTrackers.collectAsState()
    val selectedTrackerId by viewModel.selectedTrackerId.collectAsState()

    val triggerQuickCheckIn: (String, String?) -> Unit = { dateStr, mood ->
        val isBad = activeTracker.type == "bad"
        viewModel.quickIncrement(dateStr, mood) { record ->
            if (isBad) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else {
                if (celebrationsEnabled) {
                    showConfetti = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            val message = if (isBad) {
                "Slip logged for ${activeTracker.icon} ${activeTracker.title}"
            } else {
                if (activeTracker.targetCount > 1) {
                    "Logged ${activeTracker.icon} ${activeTracker.title} (${record.newCount}/${activeTracker.targetCount})"
                } else {
                    "Logged ${activeTracker.icon} ${activeTracker.title}"
                }
            }

            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoQuickCheckIn(record.token)
                }
            }
        }
    }

    val selectedMonthEntriesCount = remember(currentYearMonth, completionsByDate) {
        completionsByDate.filterKeys {
            try {
                val date = LocalDate.parse(it)
                date.year == currentYearMonth.year && date.monthValue == currentYearMonth.monthValue
            } catch (e: Exception) {
                false
            }
        }.values.flatten().sumOf { it.count }
    }

    LaunchedEffect(pagerState.currentPage) {
        currentYearMonth = baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeConfig.bgColor)
            .padding(horizontal = 16.dp)
    ) {
        ConfettiCelebration(
            isTriggered = showConfetti,
            primaryAccent = themeConfig.primaryColor,
            onFinished = { showConfetti = false }
        )

        HomeScreenContent(
            habitTitle = habitTitle,
            habitIcon = habitIcon,
            activeTracker = activeTracker,
            stats = stats,
            freezeDate = freezeDate,
            trackers = trackers,
            selectedTrackerId = selectedTrackerId,
            archivedCount = archivedTrackers.size,
            selectedRoutineFilter = selectedRoutineFilter,
            today = today,
            completionsByDate = completionsByDate,
            selectedMoodTag = selectedMoodTag,
            baseMonth = baseMonth,
            initialPage = initialPage,
            pagerState = pagerState,
            currentYearMonth = currentYearMonth,
            selectedMonthEntriesCount = selectedMonthEntriesCount,
            themeConfig = themeConfig,
            onFreezeClick = {
                if (freezeDate == null && activeTracker.type == "good") {
                    viewModel.applyStreakFreeze()
                }
            },
            onFilterChange = { selectedRoutineFilter = it },
            onSelectTracker = { viewModel.selectTracker(it) },
            onLongClickTracker = { dialogState.trackerToManage = it },
            onAddTrackerClick = { dialogState.showAddTrackerDialog = true },
            onOpenArchivedClick = { dialogState.showArchivedHabitsSheet = true },
            onReorder = { viewModel.reorderTrackers(it) },
            onMoodTagSelect = { selectedMoodTag = it },
            onQuickCheckIn = { triggerQuickCheckIn(today.toString(), selectedMoodTag) },
            onOpenJournal = {
                dialogState.journalTargetDate = today
                dialogState.showDailyJournalDialog = true
            },
            onMonthPickerClick = { dialogState.showMonthPicker = true },
            onCellClick = { dateStr -> triggerQuickCheckIn(dateStr, null) },
            onCellLongClick = { date -> dialogState.selectedDateForDetails = date }
        )

        // Floating Quick Add Button
        FloatingActionButton(
            onClick = { dialogState.showQuickAddDialog = true },
            containerColor = themeConfig.primaryColor,
            contentColor = if (themeConfig.primaryColor == Color.White) Color.Black else Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(56.dp)
                .testTag("floating_add_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Event Today",
                modifier = Modifier.size(28.dp)
            )
        }

        // Floating Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
                .testTag("undo_snackbar")
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = if (ThemeStyles.isLightTheme(themeConfig.selectedTheme)) Color(0xFF1E1E1E) else Color(0xFF2C2C2C),
                contentColor = Color.White,
                actionColor = themeConfig.primaryColor,
                shape = RoundedCornerShape(12.dp)
            )
        }

        HomeScreenDialogsHost(
            viewModel = viewModel,
            dialogState = dialogState,
            themeConfig = themeConfig,
            activeTracker = activeTracker,
            trackers = trackers,
            archivedTrackers = archivedTrackers,
            completionsByDate = completionsByDate,
            currentYearMonth = currentYearMonth,
            today = today,
            onYearMonthSelected = { selectedYM ->
                val monthsDiff = ChronoUnit.MONTHS.between(baseMonth, selectedYM).toInt()
                coroutineScope.launch {
                    pagerState.scrollToPage(initialPage + monthsDiff)
                }
                currentYearMonth = selectedYM
            },
            onQuickCheckIn = { dateStr, mood -> triggerQuickCheckIn(dateStr, mood) }
        )
    }
}

@Composable
private fun HomeScreenContent(
    habitTitle: String,
    habitIcon: String,
    activeTracker: TrackerConfig,
    stats: HabitStats,
    freezeDate: String?,
    trackers: List<TrackerConfig>,
    selectedTrackerId: String,
    archivedCount: Int,
    selectedRoutineFilter: String,
    today: LocalDate,
    completionsByDate: Map<String, List<HabitEntry>>,
    selectedMoodTag: String?,
    baseMonth: YearMonth,
    initialPage: Int,
    pagerState: PagerState,
    currentYearMonth: YearMonth,
    selectedMonthEntriesCount: Int,
    themeConfig: HomeThemeConfig,
    onFreezeClick: () -> Unit,
    onFilterChange: (String) -> Unit,
    onSelectTracker: (String) -> Unit,
    onLongClickTracker: (TrackerConfig) -> Unit,
    onAddTrackerClick: () -> Unit,
    onOpenArchivedClick: () -> Unit,
    onReorder: (List<TrackerConfig>) -> Unit,
    onMoodTagSelect: (String?) -> Unit,
    onQuickCheckIn: () -> Unit,
    onOpenJournal: () -> Unit,
    onMonthPickerClick: () -> Unit,
    onCellClick: (String) -> Unit,
    onCellLongClick: (LocalDate) -> Unit
) {
    val screenScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(screenScrollState)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Header Row
        HomeHeaderRow(
            habitIcon = habitIcon,
            habitTitle = habitTitle,
            activeTracker = activeTracker,
            stats = stats,
            freezeDate = freezeDate,
            primaryColor = themeConfig.primaryColor,
            textColor = themeConfig.textColor,
            secondaryTextColor = themeConfig.secondaryTextColor,
            appFont = themeConfig.appFont,
            onFreezeClick = onFreezeClick
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Habits Navigation & Routine Bar
        HomeHabitBar(
            trackers = trackers,
            selectedTrackerId = selectedTrackerId,
            selectedRoutineFilter = selectedRoutineFilter,
            archivedCount = archivedCount,
            primaryColor = themeConfig.primaryColor,
            cardBgColor = themeConfig.cardBgColor,
            borderColor = themeConfig.borderColor,
            textColor = themeConfig.textColor,
            secondaryTextColor = themeConfig.secondaryTextColor,
            appFont = themeConfig.appFont,
            onFilterChange = onFilterChange,
            onSelectTracker = onSelectTracker,
            onLongClickTracker = onLongClickTracker,
            onAddTrackerClick = onAddTrackerClick,
            onOpenArchivedClick = onOpenArchivedClick,
            onReorder = onReorder
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Today's Status & Action Card
        val todayDateString = remember(today) { today.toString() }
        val todayEntries = completionsByDate[todayDateString] ?: emptyList()
        val todayCount = todayEntries.sumOf { it.count }

        TodayStatusCard(
            activeTracker = activeTracker,
            today = today,
            todayCount = todayCount,
            completionsByDate = completionsByDate,
            selectedMoodTag = selectedMoodTag,
            stats = stats,
            primaryColor = themeConfig.primaryColor,
            cardBgColor = themeConfig.cardBgColor,
            borderColor = themeConfig.borderColor,
            textColor = themeConfig.textColor,
            secondaryTextColor = themeConfig.secondaryTextColor,
            appFont = themeConfig.appFont,
            selectedTheme = themeConfig.selectedTheme,
            onMoodTagSelect = onMoodTagSelect,
            onQuickCheckIn = onQuickCheckIn,
            onOpenJournal = onOpenJournal
        )

        // 4. Calendar Grid Section
        HomeCalendarSection(
            baseMonth = baseMonth,
            initialPage = initialPage,
            pagerState = pagerState,
            currentYearMonth = currentYearMonth,
            today = today,
            selectedMonthEntriesCount = selectedMonthEntriesCount,
            activeTracker = activeTracker,
            completionsByDate = completionsByDate,
            primaryColor = themeConfig.primaryColor,
            cardBgColor = themeConfig.cardBgColor,
            borderColor = themeConfig.borderColor,
            textColor = themeConfig.textColor,
            secondaryTextColor = themeConfig.secondaryTextColor,
            appFont = themeConfig.appFont,
            cellShape = themeConfig.cellShape,
            checkmarkStyleName = themeConfig.checkmarkStyleName,
            selectedTheme = themeConfig.selectedTheme,
            onMonthPickerClick = onMonthPickerClick,
            onCellClick = onCellClick,
            onCellLongClick = onCellLongClick
        )
    }
}

@Composable
private fun HomeScreenDialogsHost(
    viewModel: HabitViewModel,
    dialogState: HomeScreenDialogState,
    themeConfig: HomeThemeConfig,
    activeTracker: TrackerConfig,
    trackers: List<TrackerConfig>,
    archivedTrackers: List<TrackerConfig>,
    completionsByDate: Map<String, List<HabitEntry>>,
    currentYearMonth: YearMonth,
    today: LocalDate,
    onYearMonthSelected: (YearMonth) -> Unit,
    onQuickCheckIn: (String, String?) -> Unit
) {
    AddTrackerSheet(
        visible = dialogState.showAddTrackerDialog,
        onDismiss = { dialogState.showAddTrackerDialog = false },
        viewModel = viewModel,
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme
    )

    ManageTrackerSheet(
        tracker = dialogState.trackerToManage,
        onDismiss = { dialogState.trackerToManage = null },
        onEdit = { config ->
            dialogState.trackerToEdit = config
            dialogState.trackerToManage = null
        },
        onArchive = { config ->
            dialogState.trackerToArchive = config
            dialogState.trackerToManage = null
        },
        onDelete = { id ->
            val tr = trackers.find { it.id == id } ?: archivedTrackers.find { it.id == id }
            dialogState.trackerToDelete = tr
            dialogState.trackerToManage = null
        },
        canDelete = trackers.size + archivedTrackers.size > 1,
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor
    )

    ArchiveTrackerDialog(
        tracker = dialogState.trackerToArchive,
        onDismiss = { dialogState.trackerToArchive = null },
        onConfirm = { id ->
            viewModel.archiveTracker(id)
        },
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor
    )

    EditTrackerSheet(
        tracker = dialogState.trackerToEdit,
        onDismiss = { dialogState.trackerToEdit = null },
        viewModel = viewModel,
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme
    )

    DeleteTrackerDialog(
        tracker = dialogState.trackerToDelete,
        onDismiss = { dialogState.trackerToDelete = null },
        onConfirm = { id ->
            viewModel.deleteTracker(id)
        },
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor
    )

    ArchivedHabitsSheet(
        visible = dialogState.showArchivedHabitsSheet,
        onDismiss = { dialogState.showArchivedHabitsSheet = false },
        archivedTrackers = archivedTrackers,
        onUnarchive = { id ->
            viewModel.unarchiveTracker(id)
        },
        onDeletePermanently = { id ->
            val tr = archivedTrackers.find { it.id == id }
            dialogState.trackerToDelete = tr
        },
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor
    )

    MonthPickerDialog(
        visible = dialogState.showMonthPicker,
        currentYearMonth = currentYearMonth,
        onYearMonthSelected = onYearMonthSelected,
        onDismiss = { dialogState.showMonthPicker = false },
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme
    )

    QuickAddDialog(
        visible = dialogState.showQuickAddDialog,
        today = today,
        onDismiss = { dialogState.showQuickAddDialog = false },
        onConfirm = { note ->
            viewModel.insertEntry(
                dateString = today.toString(),
                count = 1,
                notes = note
            )
        },
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme
    )

    DayDetailsBottomSheet(
        selectedDate = dialogState.selectedDateForDetails,
        completionsByDate = completionsByDate,
        activeTracker = activeTracker,
        onDismiss = { dialogState.selectedDateForDetails = null },
        onDeleteEntry = { entry -> viewModel.deleteEntry(entry) },
        onClearDate = { dateStr -> viewModel.clearEntriesForDate(dateStr) },
        onQuickLog = { dateStr -> onQuickCheckIn(dateStr, null) },
        onOpenJournal = { date ->
            dialogState.journalTargetDate = date
            dialogState.showDailyJournalDialog = true
        },
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.cardBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme
    )

    if (dialogState.showDonateDialog) {
        DonateModal(
            viewModel = viewModel,
            onDismiss = { dialogState.showDonateDialog = false }
        )
    }

    if (dialogState.showDailyJournalDialog) {
        DailyJournalModal(
            viewModel = viewModel,
            initialDate = dialogState.journalTargetDate ?: today,
            onDismiss = {
                dialogState.showDailyJournalDialog = false
                dialogState.journalTargetDate = null
            }
        )
    }
    if (dialogState.showCloudBackupModal) {
        CloudBackupModal(
            viewModel = viewModel,
            onClose = { dialogState.showCloudBackupModal = false },
            onOpenDonate = { dialogState.showDonateDialog = true }
        )
    }
}
