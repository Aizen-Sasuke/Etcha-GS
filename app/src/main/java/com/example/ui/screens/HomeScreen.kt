package com.example.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.ConfettiCelebration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@OptIn(ExperimentalFoundationApi::class)
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

    // Current dates
    val today = remember(dayRolloverHour) { viewModel.getEffectiveToday() }
    val baseMonth = remember { YearMonth.now() }
    val initialPage = remember { Int.MAX_VALUE / 2 }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )
    val coroutineScope = rememberCoroutineScope()
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    
    // State holders from ViewModel
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
    val isSupporter by viewModel.isSupporter.collectAsState()
    
    // Dialog control states
    var selectedDateForDetails by remember { mutableStateOf<LocalDate?>(null) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showDailyJournalDialog by remember { mutableStateOf(false) }
    var showCloudBackupModal by remember { mutableStateOf(false) }
    var journalTargetDate by remember { mutableStateOf<LocalDate?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val triggerQuickCheckIn: (String, String?) -> Unit = { dateStr, mood ->
        val isBad = activeTracker.type == "bad"
        viewModel.quickIncrement(dateStr, mood) { record ->
            if (isBad) {
                // Neutral feedback for relapse: NO celebratory confetti, subtle non-celebratory haptic
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else {
                // Normal positive habit: keep existing successful celebration
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
    
    // Calculate total completions in currently selected month
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

    // Dynamic daily quote generator
    val quote = remember(today) {
        val quotes = listOf(
            "The secret of your future is hidden in your daily routine.",
            "One day at a time. One decision at a time.",
            "Streaks are built day by day, don't break the chain!",
            "Small steps every day lead to massive changes.",
            "You are stronger than your excuses.",
            "Rise above the habit. Reclaim your freedom.",
            "Every day is a clean calendar. Fill it with strength.",
            "Motivation gets you going, but discipline keeps you growing.",
            "Do something today that your future self will thank you for.",
            "It does not matter how slowly you go as long as you do not stop.",
            "Success is the sum of small efforts, repeated day in and day out.",
            "Habit is either the best of servants or the worst of masters.",
            "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
            "First we make our habits, then our habits make us.",
            "Great things are not done by impulse, but by a series of small things brought together.",
            "Don't stop when you're tired. Stop when you're done.",
            "Doubt kills more dreams than failure ever will."
        )
        quotes[kotlin.math.abs(today.dayOfYear) % quotes.size]
    }

    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFontName)
    val cellShape = ThemeStyles.getCalendarCellShape(calendarCellShapeName)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)

    val trackers by viewModel.trackers.collectAsState()
    val selectedTrackerId by viewModel.selectedTrackerId.collectAsState()
    var showAddTrackerDialog by remember { mutableStateOf(false) }
    var trackerToDelete by remember { mutableStateOf<String?>(null) }
    var trackerToManage by remember { mutableStateOf<TrackerConfig?>(null) }
    var trackerToEdit by remember { mutableStateOf<TrackerConfig?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp)
    ) {
        ConfettiCelebration(
            isTriggered = showConfetti,
            primaryAccent = primaryColor,
            onFinished = { showConfetti = false }
        )

        val screenScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScrollState)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Clean & Modern Top Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                            .border(0.5.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = habitIcon,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habitTitle,
                            fontFamily = appFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            fontFamily = appFont,
                            text = when (activeTracker.timeOfDay) {
                                "morning" -> "🌅 Morning Routine"
                                "afternoon" -> "☀️ Afternoon Routine"
                                "evening" -> "🌙 Evening Routine"
                                else -> if (activeTracker.targetCount > 1) "Daily Target: ${activeTracker.targetCount}" else "Daily Tracker"
                            },
                            fontSize = 11.sp,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Streak Badge Pill
                val isFrozen = freezeDate != null
                val streakText = if (activeTracker.type == "bad") {
                    "🛡️ ${stats.currentStreak}d clean"
                } else if (activeTracker.type == "misc") {
                    "📊 ${stats.thisMonthCount} logs"
                } else {
                    if (isFrozen) "🧊 ${stats.currentStreak}d" else "🔥 ${stats.currentStreak}d"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(primaryColor.copy(alpha = if (isFrozen) 0.30f else 0.14f))
                        .border(1.dp, primaryColor.copy(alpha = if (isFrozen) 0.6f else 0.35f), RoundedCornerShape(14.dp))
                        .clickable {
                            if (!isFrozen && activeTracker.type == "good") {
                                viewModel.applyStreakFreeze()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = streakText,
                        color = if (primaryColor == Color.White) textColor else primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = appFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Unified Habits & Routine Navigation Bar
            val lazyListState = rememberLazyListState()
            var currentTrackers by remember(trackers) { mutableStateOf(trackers) }
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                currentTrackers = currentTrackers.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            }
            LaunchedEffect(reorderableState.isAnyItemDragging) {
                if (!reorderableState.isAnyItemDragging && currentTrackers != trackers) {
                    viewModel.reorderTrackers(currentTrackers)
                }
            }

            val displayTrackers = remember(currentTrackers, selectedRoutineFilter) {
                if (selectedRoutineFilter == "all") currentTrackers
                else currentTrackers.filter { it.timeOfDay == selectedRoutineFilter }
            }

            LazyRow(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Routine Quick Filter Pill
                item {
                    val routineIcons = mapOf("all" to "🌅 All", "morning" to "🌅 Morning", "afternoon" to "☀️ Afternoon", "evening" to "🌙 Evening")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedRoutineFilter != "all") primaryColor.copy(alpha = 0.2f) else cardBgColor)
                            .border(0.5.dp, if (selectedRoutineFilter != "all") primaryColor else borderColor, RoundedCornerShape(14.dp))
                            .clickable {
                                selectedRoutineFilter = when (selectedRoutineFilter) {
                                    "all" -> "morning"
                                    "morning" -> "afternoon"
                                    "afternoon" -> "evening"
                                    else -> "all"
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = routineIcons[selectedRoutineFilter] ?: "🌅 All",
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedRoutineFilter != "all") primaryColor else secondaryTextColor
                        )
                    }
                }

                items(displayTrackers, key = { it.id }) { tracker ->
                    ReorderableItem(reorderableState, key = tracker.id) { isDragging ->
                        val isSelected = tracker.id == selectedTrackerId
                        Box(
                            modifier = Modifier
                                .longPressDraggableHandle()
                                .then(if (isSelected) Modifier.shadow(elevation = 8.dp, spotColor = primaryColor, shape = RoundedCornerShape(14.dp)) else Modifier)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) primaryColor.copy(alpha = 0.18f) else cardBgColor
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) primaryColor.copy(alpha = 0.5f) else borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        viewModel.selectTracker(tracker.id)
                                    },
                                    onLongClick = {
                                        trackerToManage = tracker
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(fontFamily = appFont, text = tracker.icon, fontSize = 14.sp)
                                Text(
                                    text = tracker.title,
                                    color = if (isSelected) primaryColor else secondaryTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = appFont
                                )
                            }
                        }
                    }
                }
                
                // Add Tracker Button
                item {
                    IconButton(
                        onClick = { showAddTrackerDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(cardBgColor)
                            .border(0.5.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Tracker",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Today's Status & Quick Action Card
            val todayDateString = remember(today) { today.toString() }
            val todayEntries = completionsByDate[todayDateString] ?: emptyList()
            val todayCount = todayEntries.sumOf { it.count }
            val isTodayDone = if (activeTracker.targetCount > 1) todayCount >= activeTracker.targetCount else todayCount > 0

            val isWeeklyQuota = activeTracker.frequencyType == "weekly_quota"
            val weeklyTarget = activeTracker.weeklyTarget.coerceIn(1, 7)
            val currentWeekCompletedDays = remember(activeTracker, completionsByDate, today) {
                if (!isWeeklyQuota) 0 else {
                    val monday = today.with(java.time.DayOfWeek.MONDAY)
                    val targetDaily = activeTracker.targetCount.coerceAtLeast(1)
                    var count = 0
                    for (d in 0..6) {
                        val dStr = monday.plusDays(d.toLong()).toString()
                        val daySum = completionsByDate[dStr]?.sumOf { it.count } ?: 0
                        if (daySum >= targetDaily) {
                            count++
                        }
                    }
                    count
                }
            }
            val isWeekQuotaCompleted = isWeeklyQuota && currentWeekCompletedDays >= weeklyTarget
            val isCardHighlight = if (isWeeklyQuota) isWeekQuotaCompleted else isTodayDone

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCardHighlight) primaryColor.copy(alpha = 0.12f) else cardBgColor
                ),
                border = BorderStroke(
                    width = if (isCardHighlight) 1.dp else 0.5.dp,
                    color = if (isCardHighlight) primaryColor.copy(alpha = 0.55f) else borderColor
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBadHabit = activeTracker.type == "bad"
                        val buttonText = when {
                            isBadHabit -> if (todayCount > 0) "Log Slip (+1)" else "Log Slip"
                            isTodayDone -> "+1 Log"
                            todayCount > 0 && activeTracker.targetCount > 1 -> "+1 Log ($todayCount/${activeTracker.targetCount})"
                            else -> "Check In"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCardHighlight) {
                                        if (isBadHabit) Color.Red.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.22f)
                                    } else {
                                        if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.3f) else Color(0xFF1E1E1E)
                                    }
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCardHighlight) (if (isBadHabit) "✕" else "✓") else activeTracker.icon,
                                    fontSize = if (isCardHighlight) 20.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCardHighlight) (if (isBadHabit) Color(0xFFEF5350) else primaryColor) else textColor
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Today · ${today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))}",
                                        fontFamily = appFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    if (isWeeklyQuota) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isWeekQuotaCompleted) primaryColor.copy(alpha = 0.2f) else borderColor.copy(alpha = 0.35f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isWeekQuotaCompleted) "Week Done ($currentWeekCompletedDays/$weeklyTarget)" else "$currentWeekCompletedDays/$weeklyTarget days",
                                                fontFamily = appFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isWeekQuotaCompleted) primaryColor else textColor
                                            )
                                        }
                                    } else if (todayCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isBadHabit) Color.Red.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isBadHabit) "Slip" else if (activeTracker.targetCount > 1) "$todayCount/${activeTracker.targetCount}" else if (todayCount > 1) "x$todayCount" else "Done",
                                                fontFamily = appFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBadHabit) Color(0xFFEF5350) else primaryColor
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isWeeklyQuota) {
                                        if (isWeekQuotaCompleted) {
                                            "This Week: $currentWeekCompletedDays / $weeklyTarget days · Target reached! 🎉"
                                        } else {
                                            "This Week: $currentWeekCompletedDays / $weeklyTarget days"
                                        }
                                    } else if (isBadHabit) {
                                        if (todayCount > 0) "Slip recorded today · Tomorrow is day one" else "Clean today — stay disciplined!"
                                    } else if (isTodayDone) {
                                        "Streak safe · Keep going!"
                                    } else {
                                        if (todayCount > 0) "$todayCount of ${activeTracker.targetCount} logged · Keep going!" else "Not logged yet — tap to check in!"
                                    },
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }

                        // Quick Action Check-in Button
                        Button(
                            onClick = {
                                triggerQuickCheckIn(todayDateString, selectedMoodTag)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBadHabit) {
                                    if (todayCount > 0) Color.Red.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.85f)
                                } else if (isCardHighlight) {
                                    primaryColor.copy(alpha = 0.2f)
                                } else {
                                    primaryColor
                                },
                                contentColor = if (isBadHabit) {
                                    if (todayCount > 0) Color.Red else Color.White
                                } else if (isCardHighlight) {
                                    primaryColor
                                } else {
                                    if (primaryColor == Color.White) Color.Black else Color.White
                                }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = buttonText,
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Multi-count target progress (e.g. 5/8 glasses of water)
                    if (activeTracker.targetCount > 1) {
                        val targetProgress = (todayCount.toFloat() / activeTracker.targetCount.toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Target: $todayCount / ${activeTracker.targetCount}",
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryTextColor
                                )
                                Text(
                                    text = "${(targetProgress * 100).toInt()}%",
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                            LinearProgressIndicator(
                                progress = { targetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = primaryColor,
                                trackColor = borderColor.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Habit Strength Score & Mood Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Habit Strength Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.25f) else Color(0xFF1B1D24))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Strength: ${stats.habitStrengthScore}% · ${stats.habitStrengthStatus}",
                                    fontFamily = appFont,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryTextColor
                                )
                            }

                            // Journal Quick Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primaryColor.copy(alpha = 0.12f))
                                    .border(0.5.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        journalTargetDate = today
                                        showDailyJournalDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(text = "✍️", fontSize = 10.sp)
                                    Text(
                                        text = "Journal",
                                        fontFamily = appFont,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                            }
                        }

                        // Mood selection chips for check-in
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val moods = listOf("😊", "⚡", "🧘", "🌧️", "🎯")
                            moods.forEach { mood ->
                                val isMoodSelected = selectedMoodTag == mood
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isMoodSelected) primaryColor.copy(alpha = 0.25f) else Color.Transparent)
                                        .border(if (isMoodSelected) 1.dp else 0.dp, primaryColor, CircleShape)
                                        .clickable { selectedMoodTag = mood },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = mood, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Month navigation and counters row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Header (Click opens month switcher dialog)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable { showMonthPicker = true }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val monthName = currentYearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.US)
                    Text(
                        text = "$monthName ${currentYearMonth.year}",
                        fontSize = 22.sp,
                        color = textColor,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Change Month",
                        tint = textColor,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                
                // M♡nth Completed times display (Exactly matches the screenshot aesthetic)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Month",
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        fontFamily = appFont
                    )
                    Text(
                        text = "$selectedMonthEntriesCount",
                        fontSize = 24.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = appFont
                    )
                    Text(
                        text = "Times",
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        fontFamily = appFont
                    )
                    IconButton(
                        onClick = { showMonthPicker = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Month Analytics",
                            tint = textColor
                        )
                    }
                }
            }
            
            // Weekday Headers (e.g. "Sun M♡n Tue Wed Thu Fri Sat" inside screen style)
            val daysOfWeekHeader = listOf("Sun", "M♡n", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeekHeader.forEach { day ->
                    Text(
                        text = day,
                        color = secondaryTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontFamily = appFont,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = borderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

            // Calendar cells grid with swipeable pager
            val swipeHintShown by viewModel.swipeHintShown.collectAsState()
            
            LaunchedEffect(pagerState.currentPage) {
                currentYearMonth = baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
            }

            LaunchedEffect(Unit) {
                if (!swipeHintShown) {
                    try {
                        pagerState.animateScrollToPage(initialPage, 8f)
                        pagerState.animateScrollToPage(initialPage, -8f)
                        pagerState.animateScrollToPage(initialPage, 0f)
                        viewModel.setSwipeHintShown()
                    } catch (e: Exception) {}
                }
            }

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) { page ->
                val pageMonth = baseMonth.plusMonths((page - initialPage).toLong())
                val cells = remember(pageMonth) { generateCalendarCells(pageMonth) }
                val rows = remember(cells) { cells.chunked(7) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    rows.forEach { rowCells ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            rowCells.forEach { cell ->
                                val dayEntries = completionsByDate[cell.dateString] ?: emptyList()
                                val totalSum = dayEntries.sumOf { it.count }
                                val isToday = cell.date == today
                                val targetCount = activeTracker.targetCount.coerceAtLeast(1)
                                val isCompleted = totalSum >= targetCount
                                val isPartial = totalSum in 1 until targetCount

                                val borderWidth = when {
                                    isToday -> 1.5.dp
                                    isCompleted -> 1.dp
                                    isPartial -> 1.dp
                                    else -> 0.5.dp
                                }
                                val cellBorderColor = when {
                                    isToday -> primaryColor.copy(alpha = 0.80f)
                                    isCompleted -> primaryColor.copy(alpha = 0.65f)
                                    isPartial -> primaryColor.copy(alpha = 0.35f)
                                    else -> borderColor
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.02f)
                                        .shadow(
                                            elevation = when {
                                                isCompleted || isToday -> 2.dp
                                                isPartial -> 1.dp
                                                else -> 0.5.dp
                                            },
                                            shape = cellShape,
                                            spotColor = primaryColor.copy(alpha = if (ThemeStyles.isLightTheme(selectedTheme)) 0.12f else 0.25f),
                                            ambientColor = primaryColor.copy(alpha = if (ThemeStyles.isLightTheme(selectedTheme)) 0.04f else 0.08f)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(cellShape)
                                            .background(cardBgColor)
                                            .then(
                                                if (checkmarkStyleName == "filled") {
                                                    if (isCompleted) {
                                                        val fillAlpha = (0.13f + (totalSum - targetCount) * 0.04f).coerceIn(0.13f, 0.28f)
                                                        Modifier.background(primaryColor.copy(alpha = fillAlpha))
                                                    } else if (isPartial) {
                                                        val partialRatio = totalSum.toFloat() / targetCount.toFloat()
                                                        val fillAlpha = (0.04f + partialRatio * 0.05f)
                                                        Modifier.background(primaryColor.copy(alpha = fillAlpha))
                                                    } else Modifier
                                                } else Modifier
                                            )
                                            .border(width = borderWidth, color = cellBorderColor, shape = cellShape)
                                            .combinedClickable(
                                                onClick = { triggerQuickCheckIn(cell.dateString, null) },
                                                onLongClick = { selectedDateForDetails = cell.date }
                                            )
                                    ) {
                                        // 1. Indicator — top right, notification badge style
                                        if (isCompleted) {
                                            val dotSize = when {
                                                totalSum >= 4 -> 10.dp
                                                totalSum == 3 -> 9.dp
                                                totalSum == 2 -> 8.dp
                                                else -> 7.dp
                                            }
                                            val dotAlpha = when {
                                                totalSum >= 4 -> 1.0f
                                                totalSum == 3 -> 0.90f
                                                totalSum == 2 -> 0.75f
                                                else -> 0.60f
                                            }
                                            when (checkmarkStyleName) {
                                                "dot" -> Box(
                                                    modifier = Modifier
                                                        .size(dotSize)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-4).dp, y = 4.dp)
                                                        .clip(CircleShape)
                                                        .background(primaryColor.copy(alpha = dotAlpha))
                                                )
                                                "tick" -> Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Completed",
                                                    tint = primaryColor.copy(alpha = dotAlpha),
                                                    modifier = Modifier
                                                        .size(dotSize + 4.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-3).dp, y = 3.dp)
                                                )
                                                "cross" -> Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Logged",
                                                    tint = primaryColor.copy(alpha = dotAlpha),
                                                    modifier = Modifier
                                                        .size(dotSize + 4.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-3).dp, y = 3.dp)
                                                )
                                                "number" -> Text(
                                                    text = totalSum.toString(),
                                                    color = primaryColor,
                                                    fontSize = 9.sp,
                                                    fontFamily = appFont,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-3).dp, y = 3.dp)
                                                )
                                                "ring" -> Box(
                                                    modifier = Modifier
                                                        .size(dotSize)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-4).dp, y = 4.dp)
                                                        .border(1.5.dp, primaryColor.copy(alpha = dotAlpha), CircleShape)
                                                )
                                                "bar" -> Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 4.dp)
                                                        .height(2.5.dp)
                                                        .fillMaxWidth(0.45f)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(primaryColor.copy(alpha = dotAlpha))
                                                )
                                            }
                                        } else if (isPartial) {
                                            // Distinct partial progress state (targetCount > 1 and totalSum < targetCount)
                                            when (checkmarkStyleName) {
                                                "dot" -> Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-4).dp, y = 4.dp)
                                                        .border(1.2.dp, primaryColor.copy(alpha = 0.50f), CircleShape)
                                                )
                                                "tick" -> {
                                                    // Partial progress: shows count in soft accent rather than completed checkmark
                                                    Text(
                                                        text = "$totalSum",
                                                        color = primaryColor.copy(alpha = 0.75f),
                                                        fontSize = 8.5.sp,
                                                        fontFamily = appFont,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = (-3).dp, y = 3.dp)
                                                    )
                                                }
                                                "cross" -> Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Partial",
                                                    tint = primaryColor.copy(alpha = 0.35f),
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-3).dp, y = 3.dp)
                                                )
                                                "number" -> Text(
                                                    text = "$totalSum",
                                                    color = secondaryTextColor,
                                                    fontSize = 8.5.sp,
                                                    fontFamily = appFont,
                                                    fontWeight = FontWeight.Normal,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-3).dp, y = 3.dp)
                                                )
                                                "ring" -> Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-4).dp, y = 4.dp)
                                                        .border(0.9.dp, primaryColor.copy(alpha = 0.40f), CircleShape)
                                                )
                                                "bar" -> {
                                                    val progressFraction = (totalSum.toFloat() / targetCount.toFloat()).coerceIn(0.12f, 0.45f)
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomCenter)
                                                            .padding(bottom = 4.dp)
                                                            .height(2.dp)
                                                            .fillMaxWidth(0.45f * progressFraction)
                                                            .clip(RoundedCornerShape(1.dp))
                                                            .background(primaryColor.copy(alpha = 0.40f))
                                                    )
                                                }
                                            }
                                        }

                                        // 2. Date number — bottom center
                                        Text(
                                            text = "${cell.date.dayOfMonth}",
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 5.dp),
                                            color = when {
                                                isToday -> textColor
                                                cell.isCurrentMonth -> textColor.copy(alpha = 0.85f)
                                                else -> secondaryTextColor.copy(alpha = 0.35f)
                                            },
                                            fontSize = 14.sp,
                                            fontFamily = appFont,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Custom Styled Floating Click Button in bottom-right corner matching theme!
        FloatingActionButton(
            onClick = { showQuickAddDialog = true },
            containerColor = primaryColor,
            contentColor = if (primaryColor == Color.White) Color.Black else Color.White,
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

        // Floating Snackbar for Quick Actions and Undo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
                .testTag("undo_snackbar")
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = if (ThemeStyles.isLightTheme(selectedTheme)) Color(0xFF1E1E1E) else Color(0xFF2C2C2C),
                contentColor = Color.White,
                actionColor = primaryColor,
                shape = RoundedCornerShape(12.dp)
            )
        }

        AddTrackerSheet(
            visible = showAddTrackerDialog,
            onDismiss = { showAddTrackerDialog = false },
            viewModel = viewModel,
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            selectedTheme = selectedTheme
        )

        ManageTrackerSheet(
            tracker = trackerToManage,
            onDismiss = { trackerToManage = null },
            onEdit = { config ->
                trackerToEdit = config
                trackerToManage = null
            },
            onDelete = { id ->
                trackerToDelete = id
                trackerToManage = null
            },
            canDelete = trackers.size > 1,
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor
        )

        EditTrackerSheet(
            tracker = trackerToEdit,
            onDismiss = { trackerToEdit = null },
            viewModel = viewModel,
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            selectedTheme = selectedTheme
        )

        DeleteTrackerDialog(
            tracker = trackers.find { it.id == trackerToDelete },
            onDismiss = { trackerToDelete = null },
            onConfirm = { id ->
                viewModel.deleteTracker(id)
            },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor
        )

        MonthPickerDialog(
            visible = showMonthPicker,
            currentYearMonth = currentYearMonth,
            onYearMonthSelected = { selectedYM ->
                val monthsDiff = ChronoUnit.MONTHS.between(baseMonth, selectedYM).toInt()
                coroutineScope.launch {
                    pagerState.scrollToPage(initialPage + monthsDiff)
                }
                currentYearMonth = selectedYM
            },
            onDismiss = { showMonthPicker = false },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            selectedTheme = selectedTheme
        )

        QuickAddDialog(
            visible = showQuickAddDialog,
            today = today,
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { note ->
                viewModel.insertEntry(
                    dateString = today.toString(),
                    count = 1,
                    notes = note
                )
            },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            selectedTheme = selectedTheme
        )

        DayDetailsBottomSheet(
            selectedDate = selectedDateForDetails,
            completionsByDate = completionsByDate,
            activeTracker = activeTracker,
            onDismiss = { selectedDateForDetails = null },
            onDeleteEntry = { entry ->
                viewModel.deleteEntry(entry)
            },
            onClearDate = { dateStr ->
                viewModel.clearEntriesForDate(dateStr)
            },
            onQuickLog = { dateStr ->
                triggerQuickCheckIn(dateStr, null)
            },
            onOpenJournal = { date ->
                journalTargetDate = date
                showDailyJournalDialog = true
            },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            selectedTheme = selectedTheme
        )

        if (showDonateDialog) {
            DonateModal(
                viewModel = viewModel,
                onDismiss = { showDonateDialog = false }
            )
        }

        if (showDailyJournalDialog) {
            DailyJournalModal(
                viewModel = viewModel,
                initialDate = journalTargetDate ?: today,
                onDismiss = {
                    showDailyJournalDialog = false
                    journalTargetDate = null
                }
            )
        }
        if (showCloudBackupModal) {
            CloudBackupModal(
                viewModel = viewModel,
                onClose = { showCloudBackupModal = false },
                onOpenDonate = { showDonateDialog = true }
            )
        }
    }
}

// Helper to generate calendar cells correctly padded for weeks
fun generateCalendarCells(yearMonth: YearMonth): List<CalendarCell> {
    val firstOfMonth = yearMonth.atDay(1)
    // Sunday in java.time is DayOfWeek 7. We want Sunday as 0.
    // Sunday (7) -> 7 % 7 = 0
    // Monday (1) -> 1 % 7 = 1
    // Saturday (6) -> 6 % 7 = 6
    val firstDayOfWeek = firstOfMonth.dayOfWeek.value
    val dayOfWeekOffset = if (firstDayOfWeek == 7) 0 else firstDayOfWeek
    
    val cells = mutableListOf<CalendarCell>()
    
    // Fill leading empty days from previous month
    val prevMonth = yearMonth.minusMonths(1)
    val prevMonthLength = prevMonth.lengthOfMonth()
    val startDay = prevMonthLength - dayOfWeekOffset + 1
    
    if (dayOfWeekOffset > 0) {
        for (i in startDay..prevMonthLength) {
            val date = prevMonth.atDay(i)
            cells.add(CalendarCell(date = date, isCurrentMonth = false, dateString = date.toString()))
        }
    }
    
    // Fill current month days
    val currentMonthLength = yearMonth.lengthOfMonth()
    for (i in 1..currentMonthLength) {
        val date = yearMonth.atDay(i)
        cells.add(CalendarCell(date = date, isCurrentMonth = true, dateString = date.toString()))
    }
    
    // Pad remaining cells to complete a week row (grid multiple of 7)
    var nextMonthDays = 1
    while (cells.size % 7 != 0) {
        val date = yearMonth.plusMonths(1).atDay(nextMonthDays)
        cells.add(CalendarCell(date = date, isCurrentMonth = false, dateString = date.toString()))
        nextMonthDays++
    }
    
    return cells
}

data class CalendarCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val dateString: String // Format: "YYYY-MM-DD"
)
