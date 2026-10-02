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

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isTodayDone) primaryColor.copy(alpha = 0.12f) else cardBgColor
                ),
                border = BorderStroke(
                    width = if (isTodayDone) 1.dp else 0.5.dp,
                    color = if (isTodayDone) primaryColor.copy(alpha = 0.55f) else borderColor
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isTodayDone) primaryColor.copy(alpha = 0.22f) else (if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.3f) else Color(0xFF1E1E1E))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isTodayDone) "✓" else activeTracker.icon,
                                    fontSize = if (isTodayDone) 20.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTodayDone) primaryColor else textColor
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
                                    if (todayCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(primaryColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (activeTracker.targetCount > 1) "$todayCount/${activeTracker.targetCount}" else if (todayCount > 1) "x$todayCount" else "Done",
                                                fontFamily = appFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryColor
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isTodayDone) {
                                        if (activeTracker.type == "bad") "Slip recorded today" else "Streak safe · Keep going!"
                                    } else {
                                        if (activeTracker.type == "bad") "Clean today — stay disciplined!" else if (todayCount > 0) "$todayCount of ${activeTracker.targetCount} logged · Keep going!" else "Not logged yet — tap to check in!"
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
                                viewModel.quickIncrement(todayDateString, selectedMoodTag)
                                if (celebrationsEnabled) {
                                    showConfetti = true
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTodayDone) primaryColor.copy(alpha = 0.2f) else primaryColor,
                                contentColor = if (isTodayDone) primaryColor else (if (primaryColor == Color.White) Color.Black else Color.White)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isTodayDone) "+1 Log" else if (todayCount > 0 && activeTracker.targetCount > 1) "+1 Log ($todayCount/${activeTracker.targetCount})" else "Check In",
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

                                val borderWidth = when {
                                    isToday -> 1.5.dp
                                    totalSum > 0 -> 1.dp
                                    else -> 0.5.dp
                                }
                                val cellBorderColor = when {
                                    isToday -> primaryColor.copy(alpha = 0.80f)
                                    totalSum > 0 -> primaryColor.copy(alpha = 0.55f)
                                    else -> borderColor
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.02f)
                                        .shadow(
                                            elevation = if (totalSum > 0 || isToday) 2.dp else 0.5.dp,
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
                                                if (checkmarkStyleName == "filled" && totalSum > 0) {
                                                    val fillAlpha = (0.13f + (totalSum - 1) * 0.04f).coerceAtMost(0.28f)
                                                    Modifier.background(primaryColor.copy(alpha = fillAlpha))
                                                } else Modifier
                                            )
                                            .border(width = borderWidth, color = cellBorderColor, shape = cellShape)
                                            .combinedClickable(
                                                onClick = { viewModel.quickIncrement(cell.dateString) },
                                                onLongClick = { selectedDateForDetails = cell.date }
                                            )
                                    ) {
                                        // 1. Indicator — top right, notification badge style
                                        if (totalSum > 0) {
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
                                                    contentDescription = null,
                                                    tint = primaryColor.copy(alpha = dotAlpha),
                                                    modifier = Modifier
                                                        .size(dotSize + 4.dp)
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = (-3).dp, y = 3.dp)
                                                )
                                                "cross" -> Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = null,
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

        // Add Tracker creation Sheet
        if (showAddTrackerDialog) {
            var newTitle by remember { mutableStateOf("") }
            var newIcon by remember { mutableStateOf("🎯") }
            var newAccentColor by remember { mutableStateOf<String?>(null) }
            var newTargetCount by remember { mutableIntStateOf(1) }
            
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showAddTrackerDialog = false },
                containerColor = cardBgColor,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "New Custom Tracker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = appFont
                    )
                    
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(fontFamily = appFont, text = "Tracker Name", color = secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = borderColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        placeholder = { Text(fontFamily = appFont, text = "e.g. Drink Water", color = secondaryTextColor.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = newIcon,
                        onValueChange = { newIcon = it },
                        label = { Text(fontFamily = appFont, text = "Icon / Emoji", color = secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = borderColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        placeholder = { Text(fontFamily = appFont, text = "e.g. \uD83C\uDFAF", color = secondaryTextColor.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tracker Type
                    Text(fontFamily = appFont, text = "Tracker Type", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    var newType by remember { mutableStateOf("good") }
                    val trackerTypes = listOf(
                        Triple("good", "Good", "Streak breaks on missed days"),
                        Triple("misc", "Misc", "Counting only, no streaks"),
                        Triple("bad", "Bad", "Avoidance, logging breaks streak")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        trackerTypes.forEach { (typeId, label, _) ->
                            val isSelected = newType == typeId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { newType = typeId }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fontFamily = appFont, 
                                    text = label,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Routine / Time of Day
                    Text(fontFamily = appFont, text = "Routine (Time of Day)", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    var newTimeOfDay by remember { mutableStateOf("anytime") }
                    val routineTypes = listOf(
                        "anytime" to "Anytime",
                        "morning" to "🌅 Morning",
                        "afternoon" to "☀️ Afternoon",
                        "evening" to "🌙 Evening"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        routineTypes.forEach { (rKey, rLabel) ->
                            val isSelected = newTimeOfDay == rKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { newTimeOfDay = rKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fontFamily = appFont, 
                                    text = rLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Frequency Schedule
                    Text(fontFamily = appFont, text = "Target Frequency", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    var newFrequency by remember { mutableStateOf("daily") }
                    val freqOptions = listOf(
                        "daily" to "Daily",
                        "weekdays" to "Weekdays (M-F)",
                        "3x_week" to "3x / Week"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        freqOptions.forEach { (fKey, fLabel) ->
                            val isSelected = newFrequency == fKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { newFrequency = fKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fontFamily = appFont, 
                                    text = fLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Daily Target Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(fontFamily = appFont, text = "Daily Target Count", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(fontFamily = appFont, text = "e.g. 1 per day, or 8 cups of water", color = secondaryTextColor, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { if (newTargetCount > 1) newTargetCount-- },
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                            ) {
                                Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Text(text = "$newTargetCount", fontFamily = appFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                            IconButton(
                                onClick = { if (newTargetCount < 50) newTargetCount++ },
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                            ) {
                                Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }

                    // Accent Colors
                    Text(fontFamily = appFont, text = "Accent Color (Optional)", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    val colorSwatches = listOf(null, "#F0F0F0", "#2F80ED", "#42B34B", "#FF5722", "#9C27B0", "#00E5FF")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(colorSwatches) { colorHex ->
                            val isSelected = colorHex == newAccentColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (colorHex == null) (if (ThemeStyles.isLightTheme(selectedTheme)) borderColor else Color.DarkGray) else Color(android.graphics.Color.parseColor(colorHex))
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) textColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newAccentColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorHex == null) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Default", tint = secondaryTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showAddTrackerDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
                        }
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() && newIcon.isNotBlank()) {
                                    viewModel.addTracker(
                                        title = newTitle, 
                                        icon = newIcon, 
                                        accentColor = newAccentColor, 
                                        type = newType,
                                        frequencyType = newFrequency,
                                        targetDays = "MON,TUE,WED,THU,FRI,SAT,SUN",
                                        targetCount = newTargetCount,
                                        timeOfDay = newTimeOfDay
                                    )
                                    showAddTrackerDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fontFamily = appFont, 
                                text = "Create",
                                color = if (primaryColor == Color.White) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Manage Tracker Bottom Sheet
        if (trackerToManage != null) {
            val config = trackerToManage!!
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { trackerToManage = null },
                containerColor = cardBgColor,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Manage ${config.icon} ${config.title}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = appFont
                    )
                    
                    Button(
                        onClick = {
                            trackerToEdit = config
                            trackerToManage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = if (primaryColor == Color.White) Color.Black else Color.White)
                            Text(fontFamily = appFont, text = "Edit Settings (Type, Color, Info)", color = if (primaryColor == Color.White) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    val canDelete = trackers.size > 1
                    Button(
                        onClick = {
                            if (canDelete) {
                                trackerToDelete = config.id
                                trackerToManage = null
                            }
                        },
                        enabled = canDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.15f),
                            contentColor = Color.Red
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = if (canDelete) Color.Red else secondaryTextColor.copy(alpha = 0.5f))
                            Text(
                                fontFamily = appFont, 
                                text = if (canDelete) "Delete Tracker" else "Delete Tracker (Must have at least one)", 
                                color = if (canDelete) Color.Red else secondaryTextColor.copy(alpha = 0.5f), 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { trackerToManage = null }) {
                        Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
                    }
                }
            }
        }

        // Edit Tracker Bottom Sheet
        if (trackerToEdit != null) {
            val editingConfig = trackerToEdit!!
            var editTitle by remember(editingConfig) { mutableStateOf(editingConfig.title) }
            var editIcon by remember(editingConfig) { mutableStateOf(editingConfig.icon) }
            var editAccentColor by remember(editingConfig) { mutableStateOf(editingConfig.accentColor) }
            var editType by remember(editingConfig) { mutableStateOf(editingConfig.type) }
            var editTimeOfDay by remember(editingConfig) { mutableStateOf(editingConfig.timeOfDay) }
            var editFrequency by remember(editingConfig) { mutableStateOf(editingConfig.frequencyType) }
            var editTargetCount by remember(editingConfig) { mutableIntStateOf(editingConfig.targetCount) }

            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { trackerToEdit = null },
                containerColor = cardBgColor,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Tracker Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = appFont
                    )
                    
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(fontFamily = appFont, text = "Tracker Name", color = secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = borderColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        placeholder = { Text(fontFamily = appFont, text = "e.g. Drink Water", color = secondaryTextColor.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = editIcon,
                        onValueChange = { editIcon = it },
                        label = { Text(fontFamily = appFont, text = "Icon / Emoji", color = secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = borderColor,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        placeholder = { Text(fontFamily = appFont, text = "e.g. \uD83C\uDFAF", color = secondaryTextColor.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tracker Type
                    Text(fontFamily = appFont, text = "Tracker Type", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    val trackerTypes = listOf(
                        Triple("good", "Good", "Streak breaks on missed days"),
                        Triple("misc", "Misc", "Counting only, no streaks"),
                        Triple("bad", "Bad", "Avoidance, logging breaks streak")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        trackerTypes.forEach { (typeId, label, _) ->
                            val isSelected = editType == typeId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { editType = typeId }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fontFamily = appFont, 
                                    text = label,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Routine / Time of Day
                    Text(fontFamily = appFont, text = "Routine (Time of Day)", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    val routineTypes = listOf(
                        "anytime" to "Anytime",
                        "morning" to "🌅 Morning",
                        "afternoon" to "☀️ Afternoon",
                        "evening" to "🌙 Evening"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        routineTypes.forEach { (rKey, rLabel) ->
                            val isSelected = editTimeOfDay == rKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { editTimeOfDay = rKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fontFamily = appFont, 
                                    text = rLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Frequency Schedule
                    Text(fontFamily = appFont, text = "Target Frequency", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    val freqOptions = listOf(
                        "daily" to "Daily",
                        "weekdays" to "Weekdays (M-F)",
                        "3x_week" to "3x / Week"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        freqOptions.forEach { (fKey, fLabel) ->
                            val isSelected = editFrequency == fKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { editFrequency = fKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fontFamily = appFont, 
                                    text = fLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Daily Target Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(fontFamily = appFont, text = "Daily Target Count", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(fontFamily = appFont, text = "e.g. 1 per day, or 8 cups of water", color = secondaryTextColor, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { if (editTargetCount > 1) editTargetCount-- },
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                            ) {
                                Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Text(text = "$editTargetCount", fontFamily = appFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                            IconButton(
                                onClick = { if (editTargetCount < 50) editTargetCount++ },
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                            ) {
                                Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }

                    // Accent Colors
                    Text(fontFamily = appFont, text = "Accent Color (Optional)", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    val colorSwatches = listOf(null, "#F0F0F0", "#2F80ED", "#42B34B", "#FF5722", "#9C27B0", "#00E5FF")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(colorSwatches) { colorHex ->
                            val isSelected = colorHex == editAccentColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (colorHex == null) (if (ThemeStyles.isLightTheme(selectedTheme)) borderColor else Color.DarkGray) else Color(android.graphics.Color.parseColor(colorHex))
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) textColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editAccentColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorHex == null) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Default", tint = secondaryTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { trackerToEdit = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
                        }
                        Button(
                            onClick = {
                                if (editTitle.isNotBlank() && editIcon.isNotBlank()) {
                                    viewModel.updateTracker(
                                        id = editingConfig.id,
                                        title = editTitle,
                                        icon = editIcon,
                                        accentColor = editAccentColor,
                                        type = editType,
                                        frequencyType = editFrequency,
                                        targetDays = editingConfig.targetDays,
                                        targetCount = editTargetCount,
                                        timeOfDay = editTimeOfDay
                                    )
                                    trackerToEdit = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fontFamily = appFont, 
                                text = "Save Changes",
                                color = if (primaryColor == Color.White) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Delete Tracker Confirmation Dialog
        if (trackerToDelete != null) {
            val targetTracker = trackers.find { it.id == trackerToDelete }
            if (targetTracker != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { trackerToDelete = null },
                    containerColor = cardBgColor,
                    titleContentColor = textColor,
                    textContentColor = secondaryTextColor,
                    title = {
                        Text(fontFamily = appFont, text = "Delete ${targetTracker.title}?", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(fontFamily = appFont, text = "Are you sure you want to delete this tracker? All log entries for this habit will be permanently deleted.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteTracker(trackerToDelete!!)
                                trackerToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text(fontFamily = appFont, text = "Delete Tracker", color = if (primaryColor == Color.White) Color.Black else Color.White)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { trackerToDelete = null },
                            border = BorderStroke(1.dp, borderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Text(fontFamily = appFont, text = "Cancel")
                        }
                    }
                )
            } else {
                trackerToDelete = null
            }
        }
        
        // Month selector Dialog
        if (showMonthPicker) {
            Dialog(onDismissRequest = { showMonthPicker = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = BorderStroke(0.5.dp, borderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Select Month",
                            fontFamily = appFont,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Yearly navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentYearMonth = currentYearMonth.minusYears(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Year", tint = secondaryTextColor)
                            }
                            Text(fontFamily = appFont, 
                                text = "${currentYearMonth.year}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            IconButton(onClick = { currentYearMonth = currentYearMonth.plusYears(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Year", tint = secondaryTextColor)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Scrollable grid of 12 months
                        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(12) { mIndex ->
                                val monthValue = mIndex + 1
                                val isSelected = currentYearMonth.monthValue == monthValue
                                Button(
                                    onClick = {
                                        val selectedYM = YearMonth.of(currentYearMonth.year, monthValue)
                                        val monthsDiff = ChronoUnit.MONTHS.between(baseMonth, selectedYM).toInt()
                                        coroutineScope.launch {
                                            pagerState.scrollToPage(initialPage + monthsDiff)
                                        }
                                        currentYearMonth = selectedYM
                                        showMonthPicker = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) primaryColor else (if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF262626)),
                                        contentColor = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else textColor
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(fontFamily = appFont, text = monthNames[mIndex], fontSize = 13.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showMonthPicker = false }) {
                            Text(text = "Close", color = secondaryTextColor, fontFamily = appFont)
                        }
                    }
                }
            }
        }
        
        // Log Entry dialog (Floating +)
        if (showQuickAddDialog) {
            var quickNote by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showQuickAddDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = BorderStroke(0.5.dp, borderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Log Habit for Today",
                            fontFamily = appFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(fontFamily = appFont, 
                            text = "Log that you avoided or completed your habit on standard schedule: ${today.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextField(
                            value = quickNote,
                            onValueChange = { quickNote = it },
                            placeholder = { Text(fontFamily = appFont, text = "Add a quick note...", fontSize = 13.sp, color = secondaryTextColor.copy(alpha = 0.5f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.25f) else Color(0xFF222222),
                                unfocusedContainerColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.25f) else Color(0xFF222222),
                                disabledContainerColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.25f) else Color(0xFF222222),
                                cursorColor = textColor,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = { showQuickAddDialog = false }) {
                                Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
                            }
                            Button(
                                onClick = {
                                    viewModel.insertEntry(
                                        dateString = today.toString(),
                                        count = 1,
                                        notes = if (quickNote.isEmpty()) null else quickNote
                                    )
                                    showQuickAddDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = if (primaryColor == Color.White) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(fontFamily = appFont, text = "Confirm Habit Log", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Long press details & actions list Bottom Sheet
        selectedDateForDetails?.let { sDate ->
            val formatterStr = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val sDateString = sDate.format(formatterStr)
            val dayEntries = completionsByDate[sDateString] ?: emptyList()
            val totalCount = dayEntries.sumOf { it.count }
            
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { selectedDateForDetails = null },
                containerColor = cardBgColor,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val formattedDate = sDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                    
                    Text(
                        text = formattedDate,
                        fontFamily = appFont,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    // 2. Large number for total count with label "times logged"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$totalCount",
                            fontFamily = appFont,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryColor
                        )
                        Text(
                            text = "times logged",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )
                    }

                    HorizontalDivider(color = borderColor, thickness = 0.5.dp)

                    // Log entries list style
                    if (dayEntries.isNotEmpty()) {
                        Text(
                            text = "Log History",
                            fontFamily = appFont,
                            color = secondaryTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.15f) else Color(0xFF0F0F0F)),
                            border = BorderStroke(0.5.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    dayEntries.sortedBy { it.timestamp }.forEachIndexed { idx, entry ->
                                        val timeDisplay = java.time.Instant.ofEpochMilli(entry.timestamp)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalTime()
                                            .format(timeFormatter)
                                            
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Text(
                                                    text = "Log #${idx + 1} at $timeDisplay",
                                                    fontFamily = appFont,
                                                    color = textColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (!entry.notes.isNullOrEmpty()) {
                                                    Text(
                                                        text = "Notes: ${entry.notes}",
                                                        fontFamily = appFont,
                                                        color = secondaryTextColor,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteEntry(entry)
                                                    // Close automatically if it was the last entry
                                                    if (dayEntries.size <= 1) {
                                                        selectedDateForDetails = null
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete entry",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Nothing logged on this day.",
                            fontFamily = appFont,
                            color = secondaryTextColor,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                    
                    // Actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                journalTargetDate = sDate
                                selectedDateForDetails = null
                                showDailyJournalDialog = true
                            },
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(fontFamily = appFont, text = "✍️ Journal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Clear all button - only when entries are not empty
                        if (dayEntries.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearEntriesForDate(sDateString)
                                    selectedDateForDetails = null
                                },
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(fontFamily = appFont, text = "Clear", fontSize = 12.sp)
                            }
                        }
                        
                        // Add extra +1 button
                        Button(
                            onClick = {
                                viewModel.quickIncrement(sDateString)
                                selectedDateForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = if (primaryColor == Color.White) Color.Black else Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(fontFamily = appFont, text = "+1 Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

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
