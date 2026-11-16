package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.HabitEntry
import com.example.ui.HabitStats
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TodayDashboardState
import com.example.ui.TodayHabitItem
import com.example.ui.TrackerConfig
import com.example.ui.components.ConfettiCelebration
import com.example.ui.components.MotionSpecs
import com.example.ui.components.springPressScale
import com.example.ui.components.surface3DCard
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
    var detailItemForTodaySheet by mutableStateOf<TodayHabitItem?>(null)
    var showFreezeDialog by mutableStateOf(false)
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
    val checkmarkStyleName: String,
    val cellShapeKey: String = "rounded_square",
    val modalBgColor: Color = cardBgColor
)

@Composable
fun HomeScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    onNavigateToReports: (String) -> Unit = {},
    onNavigateToHistory: (String) -> Unit = {}
) {
    val dayRolloverHour by viewModel.dayRolloverHour.collectAsState()
    val celebrationsEnabled by viewModel.celebrationsEnabled.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    var showConfetti by remember { mutableStateOf(false) }
    var selectedRoutineFilter by remember { mutableStateOf("all") }
    var selectedMoodTag by remember { mutableStateOf<String?>("😊") }

    val today = remember(dayRolloverHour) { viewModel.getEffectiveToday() }
    val baseMonth = remember { YearMonth.now() }
    val initialPage = remember { 1200 }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2400 }
    )
    val coroutineScope = rememberCoroutineScope()
    val currentYearMonth by remember(baseMonth, initialPage) {
        derivedStateOf {
            baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
        }
    }

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
    val isGlassTheme = ThemeStyles.isGlassTheme(selectedTheme)

    val animatedBg by animateColorAsState(
        targetValue = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex),
        animationSpec = tween(260),
        label = "home_bg"
    )
    val animatedPrimary by animateColorAsState(
        targetValue = ThemeStyles.getPrimaryColor(selectedTheme, customHex),
        animationSpec = tween(260),
        label = "home_primary"
    )
    val animatedCardBg by animateColorAsState(
        targetValue = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex),
        animationSpec = tween(260),
        label = "home_card_bg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = ThemeStyles.getBorderColor(selectedTheme, customBgHex),
        animationSpec = tween(260),
        label = "home_border"
    )
    val modalBgColor = ThemeStyles.getOpaqueCardColor(selectedTheme, customBgHex)

    val themeConfig = remember(
        selectedTheme,
        animatedBg,
        animatedPrimary,
        animatedCardBg,
        animatedBorder,
        modalBgColor,
        customBgHex,
        selectedFontName,
        calendarCellShapeName,
        checkmarkStyleName
    ) {
        HomeThemeConfig(
            selectedTheme = selectedTheme,
            bgColor = animatedBg,
            primaryColor = animatedPrimary,
            cardBgColor = animatedCardBg,
            borderColor = animatedBorder,
            textColor = ThemeStyles.getTextColor(selectedTheme, customBgHex),
            secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme, customBgHex),
            appFont = ThemeStyles.getSelectedFontFamily(selectedFontName),
            cellShapeKey = calendarCellShapeName,
            cellShape = ThemeStyles.getCalendarCellShape(calendarCellShapeName),
            checkmarkStyleName = checkmarkStyleName,
            modalBgColor = modalBgColor
        )
    }

    val trackers by viewModel.trackers.collectAsState()
    val archivedTrackers by viewModel.archivedTrackers.collectAsState()
    val selectedTrackerId by viewModel.selectedTrackerId.collectAsState()
    val todayDashboard by viewModel.todayDashboard.collectAsState()
    val isSupporter by viewModel.isSupporter.collectAsState()
    val supporterBadgeEnabled by viewModel.supporterBadgeEnabled.collectAsState()
    val supporterVaultFreezes by viewModel.supporterVaultFreezes.collectAsState()
    val showDemoBanner by viewModel.showDemoBanner.collectAsState()

    val triggerQuickCheckInForTracker: (String, String, String?) -> Unit = { trackerId, dateStr, mood ->
        val targetTracker = trackers.find { it.id == trackerId } ?: activeTracker
        val isBad = targetTracker.type == "bad"
        viewModel.quickIncrementForTracker(trackerId, dateStr, mood) { record ->
            if (isBad) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else {
                if (celebrationsEnabled) {
                    showConfetti = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            val message = if (isBad) {
                "Slip logged for ${targetTracker.icon} ${targetTracker.title}"
            } else {
                if (targetTracker.targetCount > 1) {
                    "Logged ${targetTracker.icon} ${targetTracker.title} (${record.newCount}/${targetTracker.targetCount})"
                } else {
                    "Logged ${targetTracker.icon} ${targetTracker.title}"
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
        val monthPrefix = currentYearMonth.toString()
        completionsByDate.filterKeys { it.startsWith(monthPrefix) }.values.flatten().sumOf { it.count }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isGlassTheme) Color.Transparent else themeConfig.bgColor)
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
            todayDashboard = todayDashboard,
            completionsByDate = completionsByDate,
            selectedMoodTag = selectedMoodTag,
            baseMonth = baseMonth,
            initialPage = initialPage,
            pagerState = pagerState,
            currentYearMonth = currentYearMonth,
            selectedMonthEntriesCount = selectedMonthEntriesCount,
            showSupporterBadge = isSupporter && supporterBadgeEnabled,
            showDemoBanner = showDemoBanner,
            themeConfig = themeConfig,
            onDismissDemoBanner = { viewModel.dismissDemoBanner() },
            onClearDemoTracker = { viewModel.clearDemoTracker() },
            onFreezeClick = { dialogState.showFreezeDialog = true },
            onFilterChange = { selectedRoutineFilter = it },
            onSelectTracker = { viewModel.selectTracker(it) },
            onLongClickTracker = { dialogState.trackerToManage = it },
            onAddTrackerClick = { dialogState.showAddTrackerDialog = true },
            onOpenArchivedClick = { dialogState.showArchivedHabitsSheet = true },
            onReorder = { viewModel.reorderTrackers(it) },
            onDashboardQuickIncrement = { targetTrackerId ->
                val dashToday = todayDashboard.effectiveToday.toString()
                triggerQuickCheckInForTracker(targetTrackerId, dashToday, null)
            },
            onDashboardOpenDetail = { item ->
                viewModel.selectTracker(item.tracker.id)
                dialogState.detailItemForTodaySheet = item
            },
            onMoodTagSelect = { selectedMoodTag = it },
            onQuickCheckIn = { triggerQuickCheckInForTracker(activeTracker.id, today.toString(), selectedMoodTag) },
            onOpenJournal = {
                dialogState.journalTargetDate = today
                dialogState.showDailyJournalDialog = true
            },
            onMonthPickerClick = { dialogState.showMonthPicker = true },
            onCellClick = { dateStr -> triggerQuickCheckInForTracker(activeTracker.id, dateStr, null) },
            onCellLongClick = { date -> dialogState.selectedDateForDetails = date }
        )

        // Floating Quick Add Button
        val isLight = ThemeStyles.isLightTheme(selectedTheme, customBgHex)
        FloatingActionButton(
            onClick = { dialogState.showQuickAddDialog = true },
            containerColor = themeConfig.primaryColor,
            contentColor = if (themeConfig.primaryColor == Color.White) Color.Black else Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(56.dp)
                .springPressScale(pressedScale = 0.89f, depressDp = 2.dp)
                .surface3DCard(
                    shape = CircleShape,
                    isLight = isLight,
                    primaryColor = themeConfig.primaryColor,
                    isGlass = isGlassTheme,
                    elevationDp = 6.dp
                )
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
                containerColor = if (isLight) Color(0xFF1E1E1E) else Color(0xFF2C2C2C),
                contentColor = Color.White,
                actionColor = themeConfig.primaryColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.surface3DCard(
                    shape = RoundedCornerShape(12.dp),
                    isLight = false,
                    primaryColor = themeConfig.primaryColor,
                    isGlass = false,
                    elevationDp = 8.dp
                )
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
            today = todayDashboard.effectiveToday,
            onYearMonthSelected = { selectedYM ->
                val monthsDiff = ChronoUnit.MONTHS.between(baseMonth, selectedYM).toInt()
                coroutineScope.launch {
                    pagerState.scrollToPage(initialPage + monthsDiff)
                }
            },
            onQuickCheckIn = { targetId, dateStr, mood ->
                triggerQuickCheckInForTracker(targetId, dateStr, mood)
            },
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToReports = onNavigateToReports
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
    todayDashboard: TodayDashboardState,
    completionsByDate: Map<String, List<HabitEntry>>,
    selectedMoodTag: String?,
    baseMonth: YearMonth,
    initialPage: Int,
    pagerState: PagerState,
    currentYearMonth: YearMonth,
    selectedMonthEntriesCount: Int,
    showSupporterBadge: Boolean,
    showDemoBanner: Boolean,
    themeConfig: HomeThemeConfig,
    onDismissDemoBanner: () -> Unit,
    onClearDemoTracker: () -> Unit,
    onFreezeClick: () -> Unit,
    onFilterChange: (String) -> Unit,
    onSelectTracker: (String) -> Unit,
    onLongClickTracker: (TrackerConfig) -> Unit,
    onAddTrackerClick: () -> Unit,
    onOpenArchivedClick: () -> Unit,
    onReorder: (List<TrackerConfig>) -> Unit,
    onDashboardQuickIncrement: (String) -> Unit,
    onDashboardOpenDetail: (TodayHabitItem) -> Unit,
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
            showSupporterBadge = showSupporterBadge,
            onFreezeClick = onFreezeClick
        )

        AnimatedVisibility(
            visible = showDemoBanner,
            enter = expandVertically(animationSpec = MotionSpecs.CardRevealSizeSpring) + fadeIn(tween(220)),
            exit = shrinkVertically(animationSpec = MotionSpecs.CardRevealSizeSpring) + fadeOut(tween(160))
        ) {
            DemoTrackerBannerCard(
                themeConfig = themeConfig,
                onDismiss = onDismissDemoBanner,
                onClearDemo = onClearDemoTracker
            )
        }

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

        // 2b. Unified Today Dashboard (All active habits progress, due/attention, quota, rest, bad-habit clean status)
        UnifiedTodayDashboardCard(
            dashboard = todayDashboard,
            selectedTrackerId = selectedTrackerId,
            selectedRoutineFilter = selectedRoutineFilter,
            themeConfig = themeConfig,
            onQuickIncrement = onDashboardQuickIncrement,
            onOpenHabitDetail = onDashboardOpenDetail,
            onAddTrackerClick = onAddTrackerClick
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Today's Status & Action Card (Focused habit)
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
            cellShapeKey = themeConfig.cellShapeKey,
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
    onQuickCheckIn: (String, String, String?) -> Unit,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToReports: (String) -> Unit
) {
    AddTrackerSheet(
        visible = dialogState.showAddTrackerDialog,
        onDismiss = { dialogState.showAddTrackerDialog = false },
        viewModel = viewModel,
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.modalBgColor,
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
        cardBgColor = themeConfig.modalBgColor
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
        cardBgColor = themeConfig.modalBgColor,
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
        cardBgColor = themeConfig.modalBgColor,
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
        cardBgColor = themeConfig.modalBgColor,
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
        cardBgColor = themeConfig.modalBgColor,
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
        cardBgColor = themeConfig.modalBgColor,
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
        cardBgColor = themeConfig.modalBgColor,
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
        onQuickLog = { dateStr -> onQuickCheckIn(activeTracker.id, dateStr, null) },
        onOpenJournal = { date ->
            dialogState.journalTargetDate = date
            dialogState.showDailyJournalDialog = true
        },
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.modalBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme
    )

    TodayHabitDetailSheet(
        item = dialogState.detailItemForTodaySheet,
        themeConfig = themeConfig,
        onDismiss = { dialogState.detailItemForTodaySheet = null },
        onFocusOnCalendar = { trackerId -> viewModel.selectTracker(trackerId) },
        onQuickIncrement = { trackerId -> onQuickCheckIn(trackerId, today.toString(), null) },
        onOpenJournal = { trackerId ->
            viewModel.selectTracker(trackerId)
            dialogState.journalTargetDate = today
            dialogState.showDailyJournalDialog = true
        },
        onOpenEdit = { item ->
            dialogState.trackerToEdit = item.tracker
        },
        onViewHistory = onNavigateToHistory,
        onViewReports = onNavigateToReports
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

    if (dialogState.showFreezeDialog) {
        val isSupporter by viewModel.isSupporter.collectAsState()
        val vaultFreezes by viewModel.supporterVaultFreezes.collectAsState()
        val activeFreezeDate by viewModel.streakFreezeDate.collectAsState()
        val isAlreadyFrozenToday = activeFreezeDate == today.toString()

        AlertDialog(
            onDismissRequest = { dialogState.showFreezeDialog = false },
            containerColor = themeConfig.modalBgColor,
            shape = DesignTokens.CardShape,
            title = {
                Text(
                    text = if (isAlreadyFrozenToday) "🧊 Streak Freeze Active" else "🛡️ Streak Freeze & Vault",
                    fontFamily = themeConfig.appFont,
                    fontWeight = FontWeight.Bold,
                    color = themeConfig.textColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when {
                            isAlreadyFrozenToday -> "Today (${today}) is protected by a Streak Freeze. Your active habit streaks won't break if you miss a check-in today."
                            activeTracker.type != "good" -> "Streak Freezes protect Good habits. Switch to a Good habit or open the Supporter Vault to manage your Freeze reserve."
                            else -> "Taking a planned break? Activate a Streak Freeze to keep your streak intact today."
                        },
                        fontFamily = themeConfig.appFont,
                        color = themeConfig.secondaryTextColor
                    )
                    if (isSupporter) {
                        Text(
                            text = "Vault Reserve: $vaultFreezes freezes available",
                            fontFamily = themeConfig.appFont,
                            fontWeight = FontWeight.SemiBold,
                            color = themeConfig.primaryColor
                        )
                    }
                }
            },
            confirmButton = {
                if (!isAlreadyFrozenToday && activeTracker.type == "good") {
                    Button(
                        onClick = {
                            if (isSupporter && vaultFreezes > 0) {
                                viewModel.useVaultFreeze()
                            } else {
                                viewModel.applyStreakFreeze()
                            }
                            dialogState.showFreezeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeConfig.primaryColor,
                            contentColor = if (ThemeStyles.isLightColor(themeConfig.primaryColor)) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = if (isSupporter && vaultFreezes > 0) "Use Vault Freeze" else "Freeze Today",
                            fontFamily = themeConfig.appFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    TextButton(onClick = {
                        dialogState.showFreezeDialog = false
                        dialogState.showDonateDialog = true
                    }) {
                        Text(
                            text = "Open Vault",
                            fontFamily = themeConfig.appFont,
                            color = themeConfig.primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogState.showFreezeDialog = false }) {
                    Text("Close", fontFamily = themeConfig.appFont, color = themeConfig.secondaryTextColor)
                }
            }
        )
    }
}
