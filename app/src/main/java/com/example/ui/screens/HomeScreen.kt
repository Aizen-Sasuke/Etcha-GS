package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.FocusSessionState
import com.example.ui.HabitStats
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TodayDashboardState
import com.example.ui.TodayHabitItem
import com.example.ui.TrackerConfig
import com.example.ui.VacationPauseHelper
import com.example.ui.VacationPauseState
import com.example.ui.components.ConfettiCelebration
import com.example.ui.components.MotionSpecs
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
    var trackerForSettings by mutableStateOf<TrackerConfig?>(null)
    var settingsInitialTab by mutableIntStateOf(0)
    var detailItemForTodaySheet by mutableStateOf<TodayHabitItem?>(null)
    var showFreezeDialog by mutableStateOf(false)
}

@Immutable
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
    val modalBgColor: Color = cardBgColor,
    val onPrimaryColor: Color = ThemeStyles.contrastOn(primaryColor),
    val accentTextColor: Color = ThemeStyles.accentOnSurface(primaryColor, modalBgColor, fallbackInk = textColor),
    val neutralWellColor: Color = ThemeStyles.getNeutralWellColor(selectedTheme),
    val dangerColor: Color = ThemeStyles.semanticDangerColor(ThemeStyles.isLightColor(modalBgColor.copy(alpha = 1f))),
    val onDangerColor: Color = ThemeStyles.contrastOn(dangerColor),
    val positiveColor: Color = ThemeStyles.semanticPositiveColor(ThemeStyles.isLightColor(modalBgColor.copy(alpha = 1f)))
)

@Composable
fun HomeScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    isTwoPane: Boolean = false,
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

    val palette = remember(selectedTheme, customHex, customBgHex) {
        ThemeStyles.resolvePalette(selectedTheme, customHex, customBgHex)
    }

    val themeConfig = remember(
        selectedTheme,
        palette,
        selectedFontName,
        calendarCellShapeName,
        checkmarkStyleName
    ) {
        HomeThemeConfig(
            selectedTheme = selectedTheme,
            bgColor = palette.background,
            primaryColor = palette.primary,
            cardBgColor = palette.cardSurface,
            borderColor = palette.border,
            textColor = palette.textPrimary,
            secondaryTextColor = palette.textSecondary,
            appFont = ThemeStyles.getSelectedFontFamily(selectedFontName),
            cellShapeKey = calendarCellShapeName,
            cellShape = ThemeStyles.getCalendarCellShape(calendarCellShapeName),
            checkmarkStyleName = checkmarkStyleName,
            modalBgColor = palette.modalSurface,
            onPrimaryColor = palette.onPrimary,
            accentTextColor = palette.accentOnSurface,
            neutralWellColor = palette.neutralWell,
            dangerColor = palette.danger,
            onDangerColor = palette.onDanger,
            positiveColor = palette.positive
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
    val vacationState by viewModel.vacationState.collectAsState()
    val focusSession by viewModel.focusSession.collectAsState()

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
            isTwoPane = isTwoPane,
            vacationState = vacationState,
            themeConfig = themeConfig,
            onDismissDemoBanner = { viewModel.dismissDemoBanner() },
            onClearDemoTracker = { viewModel.clearDemoTracker() },
            onFreezeClick = { dialogState.showFreezeDialog = true },
            onFilterChange = { selectedRoutineFilter = it },
            onSelectTracker = { viewModel.selectTracker(it) },
            onLongClickTracker = {
                dialogState.settingsInitialTab = 0
                dialogState.trackerForSettings = it
            },
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
            onStartFocus = { viewModel.openFocusSheet(activeTracker) },
            onMonthPickerClick = { dialogState.showMonthPicker = true },
            onCellClick = { dateStr -> triggerQuickCheckInForTracker(activeTracker.id, dateStr, null) },
            onCellLongClick = { date -> dialogState.selectedDateForDetails = date }
        )

        // Floating Quick Add Button
        val isLight = !palette.isDark
        FloatingActionButton(
            onClick = { dialogState.showQuickAddDialog = true },
            containerColor = themeConfig.primaryColor,
            contentColor = themeConfig.onPrimaryColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(56.dp)
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
            val snackContainer = if (isLight) Color(0xFF1C1F26) else themeConfig.modalBgColor
            val snackContent = if (isLight) Color.White else themeConfig.textColor
            val snackAction = ThemeStyles.accentOnSurface(
                accent = themeConfig.primaryColor,
                surface = snackContainer,
                fallbackInk = snackContent
            )
            Snackbar(
                snackbarData = snackbarData,
                containerColor = snackContainer,
                contentColor = snackContent,
                actionColor = snackAction,
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
            vacationState = vacationState,
            focusSession = focusSession,
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
    isTwoPane: Boolean,
    vacationState: VacationPauseState,
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
    onStartFocus: () -> Unit,
    onMonthPickerClick: () -> Unit,
    onCellClick: (String) -> Unit,
    onCellLongClick: (LocalDate) -> Unit
) {
    val todayDateString = remember(today) { today.toString() }
    val todayEntries = completionsByDate[todayDateString] ?: emptyList()
    val todayCount = todayEntries.sumOf { it.count }
    val isPausedToday = remember(today, activeTracker.pausedUntilDate, vacationState) {
        VacationPauseHelper.isPausedOnEffectiveDate(today, activeTracker.pausedUntilDate, vacationState)
    }
    val pauseBadge = remember(today, activeTracker.pausedUntilDate, vacationState) {
        VacationPauseHelper.formatTrackerPauseBadge(activeTracker.pausedUntilDate, vacationState, today)
    }

    if (isTwoPane) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))

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
                cardBgColor = themeConfig.modalBgColor,
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

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("home_two_pane_layout"),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item(key = "habit_bar") {
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
                    }

                    item(key = "today_dashboard") {
                        UnifiedTodayDashboardCard(
                            dashboard = todayDashboard,
                            selectedTrackerId = selectedTrackerId,
                            selectedRoutineFilter = selectedRoutineFilter,
                            themeConfig = themeConfig,
                            onQuickIncrement = onDashboardQuickIncrement,
                            onOpenHabitDetail = onDashboardOpenDetail,
                            onAddTrackerClick = onAddTrackerClick
                        )
                    }

                    item(key = "today_status") {
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
                            onOpenJournal = onOpenJournal,
                            isPausedToday = isPausedToday,
                            pauseBadge = pauseBadge,
                            onStartFocus = onStartFocus
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item(key = "calendar_section") {
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
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "header") {
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
                    cardBgColor = themeConfig.modalBgColor,
                    onFreezeClick = onFreezeClick
                )
            }

            if (showDemoBanner) {
                item(key = "demo_banner") {
                    DemoTrackerBannerCard(
                        themeConfig = themeConfig,
                        onDismiss = onDismissDemoBanner,
                        onClearDemo = onClearDemoTracker
                    )
                }
            }

            item(key = "habit_bar") {
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
            }

            item(key = "today_dashboard") {
                UnifiedTodayDashboardCard(
                    dashboard = todayDashboard,
                    selectedTrackerId = selectedTrackerId,
                    selectedRoutineFilter = selectedRoutineFilter,
                    themeConfig = themeConfig,
                    onQuickIncrement = onDashboardQuickIncrement,
                    onOpenHabitDetail = onDashboardOpenDetail,
                    onAddTrackerClick = onAddTrackerClick
                )
            }

            item(key = "today_status") {
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
                    onOpenJournal = onOpenJournal,
                    isPausedToday = isPausedToday,
                    pauseBadge = pauseBadge,
                    onStartFocus = onStartFocus
                )
            }

            item(key = "calendar_section") {
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
    vacationState: VacationPauseState,
    focusSession: FocusSessionState,
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

    UnifiedHabitSettingsSheet(
        tracker = dialogState.trackerForSettings,
        initialTab = dialogState.settingsInitialTab,
        today = today,
        vacationState = vacationState,
        canDelete = trackers.size + archivedTrackers.size > 1,
        onDismiss = { dialogState.trackerForSettings = null },
        onPauseUntil = { id, dateStr -> viewModel.setTrackerPausedUntil(id, dateStr) },
        onArchive = { config -> dialogState.trackerToArchive = config },
        onDelete = { config -> dialogState.trackerToDelete = config },
        viewModel = viewModel,
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.modalBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme
    )

    FocusTimerSheet(
        session = focusSession,
        appFont = themeConfig.appFont,
        primaryColor = themeConfig.primaryColor,
        textColor = themeConfig.textColor,
        secondaryTextColor = themeConfig.secondaryTextColor,
        cardBgColor = themeConfig.modalBgColor,
        borderColor = themeConfig.borderColor,
        selectedTheme = themeConfig.selectedTheme,
        onDismiss = { viewModel.dismissFocusSheet() },
        onSelectMode = { viewModel.setFocusMode(it) },
        onSelectDuration = { viewModel.setFocusDurationMinutes(it) },
        onStartOrResume = { viewModel.startOrResumeFocus() },
        onPause = { viewModel.pauseFocus() },
        onReset = { viewModel.resetFocus() },
        onCompleteAndLog = { viewModel.completeFocusAndLog() }
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
            dialogState.settingsInitialTab = 0
            dialogState.trackerForSettings = item.tracker
        },
        onViewHistory = onNavigateToHistory,
        onViewReports = onNavigateToReports,
        onStartFocus = { cfg -> viewModel.openFocusSheet(cfg) }
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
                            color = themeConfig.accentTextColor
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
                            contentColor = themeConfig.onPrimaryColor
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
                            color = themeConfig.accentTextColor,
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
