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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
                .clickable { onFreezeClick() }
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
            val routineIcons = mapOf(
                "all" to "🌅 All",
                "morning" to "🌅 Morning",
                "afternoon" to "☀️ Afternoon",
                "evening" to "🌙 Evening"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedRoutineFilter != "all") primaryColor.copy(alpha = 0.2f) else cardBgColor)
                    .border(0.5.dp, if (selectedRoutineFilter != "all") primaryColor else borderColor, RoundedCornerShape(14.dp))
                    .clickable {
                        val next = when (selectedRoutineFilter) {
                            "all" -> "morning"
                            "morning" -> "afternoon"
                            "afternoon" -> "evening"
                            else -> "all"
                        }
                        onFilterChange(next)
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
            ReorderableItem(reorderableState, key = tracker.id) {
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
                            onClick = { onSelectTracker(tracker.id) },
                            onLongClick = { onLongClickTracker(tracker) }
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
                onClick = onAddTrackerClick,
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

        // Archived Habits shortcut badge if any habits are archived
        if (archivedCount > 0) {
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(borderColor.copy(alpha = 0.2f))
                        .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { onOpenArchivedClick() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "📦", fontSize = 11.sp)
                        Text(
                            text = "Archived ($archivedCount)",
                            fontFamily = appFont,
                            fontSize = 11.sp,
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
                            .clickable { onOpenJournal() }
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
                                .clickable { onMoodTagSelect(mood) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = mood, fontSize = 11.sp)
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
                .clickable { onMonthPickerClick() }
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

        // Month Completed times display
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
                onClick = onMonthPickerClick,
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

    // Weekday Headers
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
        modifier = modifier
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
                    onClick = onClick,
                    onLongClick = onLongClick
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
