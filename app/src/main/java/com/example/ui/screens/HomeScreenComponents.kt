@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitEntry
import com.example.ui.HabitStats
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class CalendarCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val dateString: String
)

fun generateCalendarCells(yearMonth: YearMonth): List<CalendarCell> {
    val firstOfMonth = yearMonth.atDay(1)
    val dayOfWeek = firstOfMonth.dayOfWeek.value
    val leadingEmptyDays = dayOfWeek % 7

    val cells = mutableListOf<CalendarCell>()

    val prevMonth = yearMonth.minusMonths(1)
    val prevMonthDays = prevMonth.lengthOfMonth()
    for (i in (prevMonthDays - leadingEmptyDays + 1)..prevMonthDays) {
        val date = prevMonth.atDay(i)
        cells.add(CalendarCell(date = date, isCurrentMonth = false, dateString = date.toString()))
    }

    for (day in 1..yearMonth.lengthOfMonth()) {
        val date = yearMonth.atDay(day)
        cells.add(CalendarCell(date = date, isCurrentMonth = true, dateString = date.toString()))
    }

    val totalCells = cells.size
    val trailingDays = if (totalCells % 7 == 0) 0 else 7 - (totalCells % 7)
    val nextMonth = yearMonth.plusMonths(1)
    for (day in 1..trailingDays) {
        val date = nextMonth.atDay(day)
        cells.add(CalendarCell(date = date, isCurrentMonth = false, dateString = date.toString()))
    }

    return cells
}

@Composable
fun HomeHeaderRow(
    habitIcon: String,
    habitTitle: String,
    activeTracker: TrackerConfig,
    stats: HabitStats,
    freezeDate: String?,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    onFreezeClick: () -> Unit
) {
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryColor.copy(alpha = 0.12f))
                    .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = habitIcon,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
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

        // Streak Badge Pill — height exactly 44.dp to match habit icon symmetrically
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
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(primaryColor.copy(alpha = if (isFrozen) 0.28f else 0.14f))
                .border(1.dp, primaryColor.copy(alpha = if (isFrozen) 0.65f else 0.35f), RoundedCornerShape(14.dp))
                .clickable { onFreezeClick() }
                .padding(horizontal = 14.dp),
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
}

@Composable
fun HomeHabitBar(
    trackers: List<TrackerConfig>,
    selectedTrackerId: String,
    selectedRoutineFilter: String,
    archivedCount: Int,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    onFilterChange: (String) -> Unit,
    onSelectTracker: (String) -> Unit,
    onLongClickTracker: (TrackerConfig) -> Unit,
    onAddTrackerClick: () -> Unit,
    onOpenArchivedClick: () -> Unit,
    onReorder: (List<TrackerConfig>) -> Unit
) {
    val lazyListState = rememberLazyListState()
    var currentTrackers by remember(trackers) { mutableStateOf(trackers) }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        currentTrackers = currentTrackers.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && currentTrackers != trackers) {
            onReorder(currentTrackers)
        }
    }

    val displayTrackers = remember(currentTrackers, selectedRoutineFilter) {
        if (selectedRoutineFilter == "all") currentTrackers
        else currentTrackers.filter { it.timeOfDay == selectedRoutineFilter }
    }

    // All items in this bar have an exact uniform height of 38.dp for symmetry
    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Routine Quick Filter Pill — exactly 38.dp height
        item {
            val routineIcons = mapOf(
                "all" to "✨ All",
                "morning" to "🌅 Morning",
                "afternoon" to "☀️ Afternoon",
                "evening" to "🌙 Evening"
            )
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedRoutineFilter != "all") primaryColor.copy(alpha = 0.2f) else cardBgColor)
                    .border(0.5.dp, if (selectedRoutineFilter != "all") primaryColor.copy(alpha = 0.7f) else borderColor, RoundedCornerShape(14.dp))
                    .clickable {
                        val next = when (selectedRoutineFilter) {
                            "all" -> "morning"
                            "morning" -> "afternoon"
                            "afternoon" -> "evening"
                            else -> "all"
                        }
                        onFilterChange(next)
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = routineIcons[selectedRoutineFilter] ?: "✨ All",
                    fontFamily = appFont,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedRoutineFilter != "all") (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor
                )
            }
        }

        // Habit Chips — exactly 38.dp height
        items(displayTrackers, key = { it.id }) { tracker ->
            ReorderableItem(reorderableState, key = tracker.id) {
                val isSelected = tracker.id == selectedTrackerId
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .longPressDraggableHandle()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) primaryColor.copy(alpha = 0.18f) else cardBgColor
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) primaryColor.copy(alpha = 0.65f) else borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .combinedClickable(
                            onClick = { onSelectTracker(tracker.id) },
                            onLongClick = { onLongClickTracker(tracker) }
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = tracker.icon, fontSize = 14.sp)
                        Text(
                            text = tracker.title,
                            color = if (isSelected) (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = appFont
                        )
                        if (tracker.reminderEnabled) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Reminder active at ${tracker.reminderTime}",
                                tint = if (isSelected) (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add Tracker Button — exactly 38.dp x 38.dp for symmetry
        item {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBgColor)
                    .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable { onAddTrackerClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Tracker",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Archived Habits shortcut badge — exactly 38.dp height
        if (archivedCount > 0) {
            item {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(borderColor.copy(alpha = 0.2f))
                        .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { onOpenArchivedClick() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "📦", fontSize = 12.sp)
                        Text(
                            text = "Archived ($archivedCount)",
                            fontFamily = appFont,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodayStatusCard(
    activeTracker: TrackerConfig,
    today: LocalDate,
    todayCount: Int,
    completionsByDate: Map<String, List<HabitEntry>>,
    selectedMoodTag: String?,
    stats: HabitStats,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    selectedTheme: String,
    onMoodTagSelect: (String) -> Unit,
    onQuickCheckIn: () -> Unit,
    onOpenJournal: () -> Unit
) {
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
            color = if (isCardHighlight) primaryColor.copy(alpha = 0.50f) else borderColor
        ),
        shape = RoundedCornerShape(18.dp),
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

                // Quick Action Check-in Button — height 40.dp with centered text
                Button(
                    onClick = onQuickCheckIn,
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
                            if (primaryColor == Color.White) textColor else primaryColor
                        } else {
                            if (primaryColor == Color.White) Color.Black else Color.White
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
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
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = primaryColor,
                        trackColor = borderColor.copy(alpha = 0.3f)
                    )
                }
            }

            // Habit Strength Score & Mood Selector Row — all elements exact 32.dp height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Habit Strength Pill — exactly 32.dp height
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.25f) else Color(0xFF1B1D24))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Strength: ${stats.habitStrengthScore}% · ${stats.habitStrengthStatus}",
                            fontFamily = appFont,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryTextColor
                        )
                    }

                    // Journal Quick Button — exactly 32.dp height
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                            .border(0.5.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable { onOpenJournal() }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "✍️", fontSize = 11.sp)
                            Text(
                                text = "Journal",
                                fontFamily = appFont,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (primaryColor == Color.White) textColor else primaryColor
                            )
                        }
                    }
                }

                // Mood selection chips — each exactly 32.dp x 32.dp
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val moods = listOf("😊", "⚡", "🧘", "🌧️", "🎯")
                    moods.forEach { mood ->
                        val isMoodSelected = selectedMoodTag == mood
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isMoodSelected) primaryColor.copy(alpha = 0.25f) else Color.Transparent)
                                .border(if (isMoodSelected) 1.dp else 0.dp, primaryColor, CircleShape)
                                .clickable { onMoodTagSelect(mood) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = mood, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeCalendarSection(
    baseMonth: YearMonth,
    initialPage: Int,
    pagerState: PagerState,
    currentYearMonth: YearMonth,
    today: LocalDate,
    selectedMonthEntriesCount: Int,
    activeTracker: TrackerConfig,
    completionsByDate: Map<String, List<HabitEntry>>,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    cellShape: Shape,
    checkmarkStyleName: String,
    selectedTheme: String,
    onMonthPickerClick: () -> Unit,
    onCellClick: (String) -> Unit,
    onCellLongClick: (LocalDate) -> Unit
) {
    var calendarViewMode by remember { mutableStateOf("month") } // "month" or "week"
    val coroutineScope = rememberCoroutineScope()

    // Month navigation and counters row — all elements exact 36.dp height
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Month Header & Chevron buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMonthPickerClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val monthName = currentYearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.US)
                    Text(
                        text = "$monthName ${currentYearMonth.year}",
                        fontSize = 19.sp,
                        color = textColor,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Change Month",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Right: View toggle [ Month | Week ] & Completions badge — exact 36.dp height
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // View mode toggle — exactly 36.dp height
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBgColor)
                    .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (calendarViewMode == "month") primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { calendarViewMode = "month" }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Month",
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        fontWeight = if (calendarViewMode == "month") FontWeight.Bold else FontWeight.Normal,
                        color = if (calendarViewMode == "month") (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (calendarViewMode == "week") primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { calendarViewMode = "week" }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Week",
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        fontWeight = if (calendarViewMode == "week") FontWeight.Bold else FontWeight.Normal,
                        color = if (calendarViewMode == "week") (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor
                    )
                }
            }

            // Month logs badge — exactly 36.dp height
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBgColor)
                    .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onMonthPickerClick() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$selectedMonthEntriesCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (primaryColor == Color.White) textColor else primaryColor,
                        fontFamily = appFont
                    )
                    Text(
                        text = "logs",
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        fontFamily = appFont
                    )
                }
            }
        }
    }

    if (calendarViewMode == "week") {
        CalendarWeekView(
            today = today,
            activeTracker = activeTracker,
            completionsByDate = completionsByDate,
            primaryColor = primaryColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            appFont = appFont,
            cellShape = cellShape,
            checkmarkStyleName = checkmarkStyleName,
            selectedTheme = selectedTheme,
            onCellClick = onCellClick,
            onCellLongClick = onCellLongClick
        )
    } else {
        // Weekday Headers
        val daysOfWeekHeader = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    fontFamily = appFont,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

        HorizontalPager(
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
                            CalendarDayCell(
                                cell = cell,
                                today = today,
                                activeTracker = activeTracker,
                                completionsByDate = completionsByDate,
                                primaryColor = primaryColor,
                                cardBgColor = cardBgColor,
                                borderColor = borderColor,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                appFont = appFont,
                                cellShape = cellShape,
                                checkmarkStyleName = checkmarkStyleName,
                                selectedTheme = selectedTheme,
                                modifier = Modifier.weight(1f),
                                onClick = { onCellClick(cell.dateString) },
                                onLongClick = { onCellLongClick(cell.date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarWeekView(
    today: LocalDate,
    activeTracker: TrackerConfig,
    completionsByDate: Map<String, List<HabitEntry>>,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    cellShape: Shape,
    checkmarkStyleName: String,
    selectedTheme: String,
    onCellClick: (String) -> Unit,
    onCellLongClick: (LocalDate) -> Unit
) {
    // Current week from Sunday to Saturday
    val startOfWeek = remember(today) {
        val dow = today.dayOfWeek.value % 7 // 0 for Sun, 1 for Mon...
        today.minusDays(dow.toLong())
    }
    val weekDays = remember(startOfWeek) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEach { date ->
            val dateStr = date.toString()
            val entries = completionsByDate[dateStr] ?: emptyList()
            val totalCount = entries.sumOf { it.count }
            val targetCount = activeTracker.targetCount.coerceAtLeast(1)
            val isDone = totalCount >= targetCount
            val isPartial = totalCount in 1 until targetCount
            val isToday = date == today
            val isFuture = date.isAfter(today)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDone) primaryColor.copy(alpha = 0.12f) else cardBgColor
                ),
                border = BorderStroke(
                    width = if (isToday) 1.5.dp else if (isDone) 1.dp else 0.5.dp,
                    color = if (isToday) primaryColor else if (isDone) primaryColor.copy(alpha = 0.5f) else borderColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCellClick(dateStr) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Day initial / number box — 34.dp
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(cellShape)
                                .background(
                                    if (isDone) primaryColor.copy(alpha = 0.22f)
                                    else if (isToday) primaryColor.copy(alpha = 0.12f)
                                    else borderColor.copy(alpha = 0.2f)
                                )
                                .border(
                                    0.5.dp,
                                    if (isDone || isToday) primaryColor.copy(alpha = 0.6f) else borderColor,
                                    cellShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${date.dayOfMonth}",
                                fontFamily = appFont,
                                fontWeight = if (isToday || isDone) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isDone || isToday) (if (primaryColor == Color.White) textColor else primaryColor) else textColor
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)),
                                    fontFamily = appFont,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isFuture) secondaryTextColor.copy(alpha = 0.6f) else textColor
                                )
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(primaryColor)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "TODAY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (primaryColor == Color.White) Color.Black else Color.White
                                        )
                                    }
                                }
                            }

                            // Subtitle: notes preview or status
                            val latestNote = entries.mapNotNull { it.notes }.lastOrNull()
                            val cleanNote = latestNote?.replace(Regex("\\[MOOD:[^\\]]+\\]"), "")?.trim()
                            val moodTag = latestNote?.let {
                                val match = Regex("\\[MOOD:([^\\]]+)\\]").find(it)
                                match?.groupValues?.get(1)
                            }

                            if (!cleanNote.isNullOrEmpty() || moodTag != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (moodTag != null) {
                                        Text(moodTag, fontSize = 11.sp)
                                    }
                                    if (!cleanNote.isNullOrEmpty()) {
                                        Text(
                                            text = cleanNote,
                                            fontFamily = appFont,
                                            fontSize = 11.sp,
                                            color = secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = if (isDone) {
                                        if (targetCount > 1) "Completed · $totalCount/$targetCount" else "Completed"
                                    } else if (isPartial) {
                                        "In Progress · $totalCount/$targetCount"
                                    } else if (isFuture) {
                                        "Upcoming"
                                    } else {
                                        "Tap to log"
                                    },
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }

                    // Right action / checkmark box — exactly 34.dp to match left box symmetrically
                    if (isDone) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(cellShape)
                                .background(primaryColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = if (primaryColor == Color.White) textColor else primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (isPartial) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(cellShape)
                                .background(primaryColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$totalCount/$targetCount",
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = primaryColor
                            )
                        }
                    } else if (!isFuture) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(cellShape)
                                .border(1.dp, borderColor, cellShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = secondaryTextColor, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    cell: CalendarCell,
    today: LocalDate,
    activeTracker: TrackerConfig,
    completionsByDate: Map<String, List<HabitEntry>>,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    cellShape: Shape,
    checkmarkStyleName: String,
    selectedTheme: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dayEntries = completionsByDate[cell.dateString] ?: emptyList()
    val totalSum = dayEntries.sumOf { it.count }
    val isToday = cell.date == today
    val targetCount = activeTracker.targetCount.coerceAtLeast(1)
    val isCompleted = totalSum >= targetCount
    val isPartial = totalSum in 1 until targetCount

    val hasNotes = remember(dayEntries) { dayEntries.any { !it.notes.isNullOrBlank() } }
    val moodTag = remember(dayEntries) {
        dayEntries.firstOrNull { it.notes?.contains("[MOOD:") == true }?.notes?.let { note ->
            note.substringAfter("[MOOD:").substringBefore("]")
        }
    }

    val borderWidth = when {
        isToday -> 1.5.dp
        isCompleted -> 1.dp
        isPartial -> 1.dp
        else -> 0.5.dp
    }
    val cellBorderColor = when {
        isToday -> primaryColor.copy(alpha = 0.90f)
        isCompleted -> primaryColor.copy(alpha = 0.65f)
        isPartial -> primaryColor.copy(alpha = 0.35f)
        else -> borderColor
    }

    val boxBgColor = when {
        isCompleted -> {
            val fillAlpha = when (checkmarkStyleName) {
                "filled" -> (0.18f + (totalSum - targetCount) * 0.04f).coerceIn(0.18f, 0.32f)
                else -> 0.15f
            }
            primaryColor.copy(alpha = fillAlpha)
        }
        isPartial -> {
            val partialRatio = (totalSum.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
            primaryColor.copy(alpha = 0.05f + partialRatio * 0.05f)
        }
        else -> cardBgColor
    }

    // Outer Day Box — center is cleanly and evenly lit, with exact 1:1 square symmetry
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cellShape)
            .background(boxBgColor)
            .then(
                if (isCompleted) {
                    Modifier.drawBehind {
                        val topRightRadius = (size.minDimension * 0.85f).coerceAtLeast(1f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width, 0f),
                                radius = topRightRadius
                            )
                        )
                        val bottomLeftRadius = (size.minDimension * 0.65f).coerceAtLeast(1f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(0f, size.height),
                                radius = bottomLeftRadius
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .border(width = borderWidth, color = cellBorderColor, shape = cellShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {

        // Optional mood indicator or journal dot in top-start corner
        if (moodTag != null) {
            Text(
                text = moodTag,
                fontSize = 8.5.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 3.dp, y = 3.dp)
            )
        } else if (hasNotes) {
            Box(
                modifier = Modifier
                    .size(3.5.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.75f))
            )
        }

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
                        .padding(bottom = 3.dp)
                        .height(2.5.dp)
                        .fillMaxWidth(0.45f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(primaryColor.copy(alpha = dotAlpha))
                )
            }
        } else if (isPartial) {
            when (checkmarkStyleName) {
                "dot" -> Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .border(1.2.dp, primaryColor.copy(alpha = 0.50f), CircleShape)
                )
                "tick" -> {
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
                            .padding(bottom = 3.dp)
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
                isToday -> if (isCompleted) textColor else (if (primaryColor == Color.White) textColor else primaryColor)
                cell.isCurrentMonth -> textColor.copy(alpha = if (isCompleted) 0.95f else 0.85f)
                else -> secondaryTextColor.copy(alpha = 0.32f)
            },
            fontSize = 13.5.sp,
            fontFamily = appFont,
            fontWeight = if (isToday || isCompleted) FontWeight.Bold else FontWeight.Medium
        )
    }
}
