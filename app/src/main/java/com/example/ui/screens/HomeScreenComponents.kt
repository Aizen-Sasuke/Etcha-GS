@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.draw.scale
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
import com.example.ui.components.AnimatedNumberText
import com.example.ui.components.MotionSpecs
import com.example.ui.components.recessedWell
import com.example.ui.components.springPressScale
import com.example.ui.components.surface3DCard
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

@OptIn(ExperimentalLayoutApi::class)
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
    showSupporterBadge: Boolean = false,
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
            val isLight = !ThemeStyles.isLightColor(textColor)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .springPressScale(0.92f)
                    .surface3DCard(
                        shape = RoundedCornerShape(14.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = false,
                        elevationDp = 4.dp
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryColor.copy(alpha = 0.12f))
                    .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = habitIcon,
                    transitionSpec = {
                        (scaleIn(MotionSpecs.BouncyPopSpring, initialScale = 0.72f) + fadeIn(tween(140))) togetherWith
                            (scaleOut(tween(110), targetScale = 0.72f) + fadeOut(tween(110)))
                    },
                    label = "header_icon_pop"
                ) { iconChar ->
                    Text(
                        text = iconChar,
                        fontSize = 22.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = habitTitle,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showSupporterBadge) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryColor.copy(alpha = 0.16f))
                                .border(0.5.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("supporter_emblem_badge")
                        ) {
                            Text(
                                text = "⭐",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
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

        // Streak Badge Pill — adaptive heightIn(min = 44.dp) to match habit icon cleanly across font scales
        val isFrozen = freezeDate != null
        val streakText = if (activeTracker.type == "bad") {
            "🛡️ ${stats.currentStreak}d clean"
        } else if (activeTracker.type == "misc") {
            "📊 ${stats.thisMonthCount} logs"
        } else {
            if (isFrozen) "🧊 ${stats.currentStreak}d" else "🔥 ${stats.currentStreak}d"
        }

        var previousStreak by remember { mutableIntStateOf(stats.currentStreak) }
        var streakBurst by remember { mutableStateOf(false) }
        LaunchedEffect(stats.currentStreak) {
            if (stats.currentStreak > previousStreak) {
                streakBurst = true
                kotlinx.coroutines.delay(220)
                streakBurst = false
            }
            previousStreak = stats.currentStreak
        }
        val streakPillScale by animateFloatAsState(
            targetValue = if (streakBurst) 1.09f else 1f,
            animationSpec = MotionSpecs.BouncyPopSpring,
            label = "streak_pill_scale"
        )

        val isLightBadge = !ThemeStyles.isLightColor(textColor)
        Box(
            modifier = Modifier
                .scale(streakPillScale)
                .springPressScale(0.93f)
                .heightIn(min = 44.dp)
                .surface3DCard(
                    shape = RoundedCornerShape(14.dp),
                    isLight = isLightBadge,
                    primaryColor = primaryColor,
                    isGlass = false,
                    elevationDp = 4.dp
                )
                .clip(RoundedCornerShape(14.dp))
                .background(primaryColor.copy(alpha = if (isFrozen) 0.28f else 0.14f))
                .border(1.dp, primaryColor.copy(alpha = if (isFrozen) 0.65f else 0.35f), RoundedCornerShape(14.dp))
                .clickable { onFreezeClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("home_freeze_badge"),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = streakText,
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(MotionSpecs.BouncyPopSpring, initialScale = 0.88f)) togetherWith
                        (fadeOut(tween(110)) + scaleOut(targetScale = 0.88f))
                },
                label = "streak_badge_text"
            ) { label ->
                Text(
                    text = label,
                    color = if (primaryColor == Color.White) textColor else primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFont,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DemoTrackerBannerCard(
    themeConfig: HomeThemeConfig,
    onDismiss: () -> Unit,
    onClearDemo: () -> Unit
) {
    val accentText = if (themeConfig.primaryColor == Color.White) themeConfig.textColor else themeConfig.primaryColor
    val onAccent = if (ThemeStyles.isLightColor(themeConfig.primaryColor)) Color.Black else Color.White

    val isLight = !ThemeStyles.isLightColor(themeConfig.textColor)
    val isGlass = ThemeStyles.isGlassTheme(themeConfig.selectedTheme)
    Card(
        colors = CardDefaults.cardColors(containerColor = themeConfig.cardBgColor),
        border = ThemeStyles.getGlassBorderStroke(themeConfig.selectedTheme),
        shape = DesignTokens.CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .surface3DCard(
                shape = DesignTokens.CardShape,
                isLight = isLight,
                primaryColor = themeConfig.primaryColor,
                isGlass = isGlass,
                elevationDp = 6.dp
            )
            .testTag("demo_tracker_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeConfig.primaryColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌱", fontSize = 18.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Sample Demo Tracker Loaded",
                        fontFamily = themeConfig.appFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = themeConfig.textColor
                    )
                    Text(
                        text = "\"Morning Walk\" is preloaded with 45 days of organic logs, natural rest gaps, moods & notes so you can explore charts and the heatmap right away.",
                        fontFamily = themeConfig.appFont,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = themeConfig.secondaryTextColor
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearDemo,
                    shape = DesignTokens.ControlShape,
                    border = BorderStroke(0.8.dp, themeConfig.borderColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 44.dp)
                        .springPressScale(0.95f)
                        .testTag("demo_clear_button")
                ) {
                    Text(
                        text = "Clear Demo Data",
                        fontFamily = themeConfig.appFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeConfig.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeConfig.primaryColor,
                        contentColor = onAccent
                    ),
                    shape = DesignTokens.ControlShape,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 44.dp)
                        .springPressScale(0.95f)
                        .testTag("demo_dismiss_button")
                ) {
                    Text(
                        text = "Got It",
                        fontFamily = themeConfig.appFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (themeConfig.primaryColor == Color.White) Color.Black else onAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Pure helper to safely reorder trackers by their stable string IDs, ignoring clicks/drops on
 * non-habit header/trailing items in the LazyRow.
 */
fun reorderTrackersByKey(
    current: List<TrackerConfig>,
    fromKey: Any?,
    toKey: Any?
): List<TrackerConfig> {
    val fromId = fromKey as? String ?: return current
    val toId = toKey as? String ?: return current
    val fromIdx = current.indexOfFirst { it.id == fromId }
    val toIdx = current.indexOfFirst { it.id == toId }
    if (fromIdx == -1 || toIdx == -1 || fromIdx == toIdx) return current
    return current.toMutableList().apply {
        add(toIdx, removeAt(fromIdx))
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
        currentTrackers = reorderTrackersByKey(currentTrackers, from.key, to.key)
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

    // All items in this bar have uniform visual styling and >= 40dp accessible height
    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Routine Quick Filter Pill
        item(key = "routine_filter_pill") {
            val isLightBar = !ThemeStyles.isLightColor(textColor)
            val routineIcons = mapOf(
                "all" to "✨ All",
                "morning" to "🌅 Morning",
                "afternoon" to "☀️ Afternoon",
                "evening" to "🌙 Evening"
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .springPressScale(0.94f)
                    .surface3DCard(
                        shape = RoundedCornerShape(14.dp),
                        isLight = isLightBar,
                        primaryColor = primaryColor,
                        isGlass = false,
                        elevationDp = if (selectedRoutineFilter != "all") 4.dp else 2.dp
                    )
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = routineIcons[selectedRoutineFilter] ?: "✨ All",
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(MotionSpecs.BouncyPopSpring, initialScale = 0.90f)) togetherWith
                            fadeOut(tween(100))
                    },
                    label = "routine_filter_anim"
                ) { txt ->
                    Text(
                        text = txt,
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedRoutineFilter != "all") (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor,
                        maxLines = 1
                    )
                }
            }
        }

        // Habit Chips
        items(displayTrackers, key = { it.id }) { tracker ->
            ReorderableItem(reorderableState, key = tracker.id) {
                val isSelected = tracker.id == selectedTrackerId
                val chipScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.03f else 1f,
                    animationSpec = MotionSpecs.BouncyPopSpring,
                    label = "tracker_chip_scale_${tracker.id}"
                )
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) primaryColor.copy(alpha = 0.18f) else cardBgColor,
                    animationSpec = tween(180),
                    label = "tracker_chip_bg_${tracker.id}"
                )
                val isLightChip = !ThemeStyles.isLightColor(textColor)
                Box(
                    modifier = Modifier
                        .scale(chipScale)
                        .springPressScale(0.95f)
                        .heightIn(min = 40.dp)
                        .longPressDraggableHandle()
                        .surface3DCard(
                            shape = RoundedCornerShape(14.dp),
                            isLight = isLightChip,
                            primaryColor = primaryColor,
                            isGlass = false,
                            elevationDp = if (isSelected) 5.dp else 2.dp
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(chipBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) primaryColor.copy(alpha = 0.65f) else borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .combinedClickable(
                            onClick = { onSelectTracker(tracker.id) },
                            onLongClick = { onLongClickTracker(tracker) }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                            fontFamily = appFont,
                            maxLines = 1
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

        // Add Tracker Button — explicit key + uniform >=40.dp size
        item(key = "add_tracker_btn") {
            Box(
                modifier = Modifier
                    .springPressScale(0.91f)
                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                    .size(40.dp)
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

        // Archived Habits shortcut badge — explicit key + adaptive height
        if (archivedCount > 0) {
            item(key = "archived_badge") {
                Box(
                    modifier = Modifier
                        .springPressScale(0.94f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(borderColor.copy(alpha = 0.2f))
                        .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { onOpenArchivedClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                            color = secondaryTextColor,
                            maxLines = 1
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

    val animatedCardContainer by animateColorAsState(
        targetValue = if (isCardHighlight) primaryColor.copy(alpha = 0.12f) else cardBgColor,
        animationSpec = tween(durationMillis = 240),
        label = "today_card_container"
    )
    val animatedCardBorder by animateColorAsState(
        targetValue = if (isCardHighlight) primaryColor.copy(alpha = 0.55f) else borderColor,
        animationSpec = tween(durationMillis = 240),
        label = "today_card_border"
    )
    val cardStroke = if (ThemeStyles.isGlassTheme(selectedTheme) && !isCardHighlight) {
        ThemeStyles.getGlassBorderStroke(selectedTheme)
    } else {
        BorderStroke(width = if (isCardHighlight) 1.dp else 0.5.dp, color = animatedCardBorder)
    }

    val isLight = !ThemeStyles.isLightColor(textColor)
    val isGlass = ThemeStyles.isGlassTheme(selectedTheme)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = animatedCardContainer
        ),
        border = cardStroke,
        shape = DesignTokens.CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .surface3DCard(
                shape = DesignTokens.CardShape,
                isLight = isLight,
                primaryColor = primaryColor,
                isGlass = isGlass,
                elevationDp = 6.dp
            )
    ) {
        Column(modifier = Modifier.padding(DesignTokens.CardPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        AnimatedContent(
                            targetState = if (isCardHighlight) (if (isBadHabit) "✕" else "✓") else activeTracker.icon,
                            transitionSpec = {
                                (scaleIn(MotionSpecs.BouncyPopSpring, initialScale = 0.6f) + fadeIn(tween(150))) togetherWith
                                    (scaleOut(tween(100), targetScale = 0.6f) + fadeOut(tween(100)))
                            },
                            label = "today_status_badge"
                        ) { iconText ->
                            Text(
                                text = iconText,
                                fontSize = if (isCardHighlight) 20.sp else 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCardHighlight) (if (isBadHabit) Color(0xFFEF5350) else primaryColor) else textColor
                            )
                        }
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

                // Quick Action Check-in Button — min height 40.dp, spring press & rebound
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
                    shape = DesignTokens.ControlShape,
                    modifier = Modifier
                        .springPressScale(0.93f)
                        .widthIn(min = 88.dp)
                        .heightIn(min = DesignTokens.ActionButtonHeight),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    AnimatedContent(
                        targetState = buttonText,
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(MotionSpecs.BouncyPopSpring, initialScale = 0.9f)) togetherWith
                                fadeOut(tween(100))
                        },
                        label = "checkin_btn_label"
                    ) { txt ->
                        Text(
                            text = txt,
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Multi-count target progress (e.g. 5/8 glasses of water)
            if (activeTracker.targetCount > 1) {
                val targetProgress = (todayCount.toFloat() / activeTracker.targetCount.toFloat()).coerceIn(0f, 1f)
                val animatedTargetProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "today_target_progress"
                )
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
                        progress = { animatedTargetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .recessedWell(RoundedCornerShape(3.dp), isLight = isLight)
                            .clip(RoundedCornerShape(3.dp)),
                        color = primaryColor,
                        trackColor = borderColor.copy(alpha = 0.3f)
                    )
                }
            }

            // Habit Strength Score, Journal & Mood Selector — FlowRow prevents horizontal squeeze/clipping
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Habit Strength Pill — dynamic height with generous padding
                    Box(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .recessedWell(RoundedCornerShape(10.dp), isLight = isLight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLight) borderColor.copy(alpha = 0.25f) else Color(0xFF1B1D24))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Strength: ${stats.habitStrengthScore}% · ${stats.habitStrengthStatus}",
                            fontFamily = appFont,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Journal Quick Button — dynamic height with crisp padding
                    Box(
                        modifier = Modifier
                            .springPressScale(0.94f)
                            .heightIn(min = 32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                            .border(0.5.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable { onOpenJournal() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                color = if (primaryColor == Color.White) textColor else primaryColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Mood selection chips — symmetric 32.dp x 32.dp targets with spring pop
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val moods = listOf("😊", "⚡", "🧘", "🌧️", "🎯")
                    moods.forEach { mood ->
                        val isMoodSelected = selectedMoodTag == mood
                        val moodScale by animateFloatAsState(
                            targetValue = if (isMoodSelected) 1.12f else 1f,
                            animationSpec = MotionSpecs.BouncyPopSpring,
                            label = "mood_scale_$mood"
                        )
                        val moodBg by animateColorAsState(
                            targetValue = if (isMoodSelected) primaryColor.copy(alpha = 0.25f) else Color.Transparent,
                            animationSpec = tween(160),
                            label = "mood_bg_$mood"
                        )
                        Box(
                            modifier = Modifier
                                .scale(moodScale)
                                .springPressScale(0.88f)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(moodBg)
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
    cellShapeKey: String = "rounded_square",
    cellShape: Shape,
    checkmarkStyleName: String,
    selectedTheme: String,
    onMonthPickerClick: () -> Unit,
    onCellClick: (String) -> Unit,
    onCellLongClick: (LocalDate) -> Unit
) {
    var calendarViewMode by remember { mutableStateOf("month") } // "month" or "week"
    val coroutineScope = rememberCoroutineScope()

    // Month navigation and counters row — responsive FlowRow so long month names & controls never clip
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Left: Month Header & Chevron buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .springPressScale(0.95f)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMonthPickerClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val monthName = currentYearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.US)
                    AnimatedContent(
                        targetState = "$monthName ${currentYearMonth.year}",
                        transitionSpec = {
                            (fadeIn(tween(170)) + slideInVertically { it / 4 }) togetherWith
                                (fadeOut(tween(120)) + slideOutVertically { -it / 4 })
                        },
                        label = "month_header_title"
                    ) { titleText ->
                        Text(
                            text = titleText,
                            fontSize = 18.sp,
                            color = textColor,
                            fontFamily = appFont,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                modifier = Modifier
                    .size(40.dp)
                    .springPressScale(0.88f)
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
                modifier = Modifier
                    .size(40.dp)
                    .springPressScale(0.88f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Right: View toggle [ Month | Week ] & Completions badge — adaptive heightIn(min = 36.dp)
        val isLightCal = !ThemeStyles.isLightColor(textColor)
        val isGlassCal = ThemeStyles.isGlassTheme(selectedTheme)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .height(IntrinsicSize.Max)
                    .recessedWell(RoundedCornerShape(10.dp), isLight = isLightCal)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBgColor)
                    .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monthPillBg by animateColorAsState(
                    targetValue = if (calendarViewMode == "month") primaryColor.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "cal_mode_month_bg"
                )
                val weekPillBg by animateColorAsState(
                    targetValue = if (calendarViewMode == "week") primaryColor.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "cal_mode_week_bg"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .springPressScale(0.94f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(monthPillBg)
                        .clickable { calendarViewMode = "month" }
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Month",
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        fontWeight = if (calendarViewMode == "month") FontWeight.Bold else FontWeight.Normal,
                        color = if (calendarViewMode == "month") (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .springPressScale(0.94f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(weekPillBg)
                        .clickable { calendarViewMode = "week" }
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Week",
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        fontWeight = if (calendarViewMode == "week") FontWeight.Bold else FontWeight.Normal,
                        color = if (calendarViewMode == "week") (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor,
                        maxLines = 1
                    )
                }
            }

            // Month logs badge
            Box(
                modifier = Modifier
                    .springPressScale(0.94f)
                    .heightIn(min = 36.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(10.dp),
                        isLight = isLightCal,
                        primaryColor = primaryColor,
                        isGlass = isGlassCal,
                        elevationDp = 3.dp
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBgColor)
                    .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onMonthPickerClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedNumberText(
                        value = selectedMonthEntriesCount,
                        color = if (primaryColor == Color.White) textColor else primaryColor,
                        fontSize = 13.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "logs",
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        fontFamily = appFont,
                        maxLines = 1
                    )
                }
            }
        }
    }

    AnimatedContent(
        targetState = calendarViewMode,
        transitionSpec = {
            (fadeIn(tween(200)) + scaleIn(MotionSpecs.SnappyFloatSpring, initialScale = 0.96f)) togetherWith
                (fadeOut(tween(150)) + scaleOut(targetScale = 0.96f))
        },
        label = "calendar_view_mode_transition"
    ) { mode ->
        if (mode == "week") {
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
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rows.forEach { rowCells ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                        cellShapeKey = cellShapeKey,
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

            val cardContainer by animateColorAsState(
                targetValue = if (isDone) primaryColor.copy(alpha = 0.12f) else cardBgColor,
                animationSpec = tween(220),
                label = "week_card_bg_${dateStr}"
            )
            val weekCardStroke = if (ThemeStyles.isGlassTheme(selectedTheme) && !isToday && !isDone) {
                ThemeStyles.getGlassBorderStroke(selectedTheme)
            } else {
                BorderStroke(
                    width = if (isToday) 1.5.dp else if (isDone) 1.dp else 0.5.dp,
                    color = if (isToday) primaryColor else if (isDone) primaryColor.copy(alpha = 0.5f) else borderColor
                )
            }

            val isLightWeek = !ThemeStyles.isLightColor(textColor)
            val isGlassWeek = ThemeStyles.isGlassTheme(selectedTheme)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardContainer),
                border = weekCardStroke,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .springPressScale(0.97f)
                    .surface3DCard(
                        shape = RoundedCornerShape(12.dp),
                        isLight = isLightWeek,
                        primaryColor = primaryColor,
                        isGlass = isGlassWeek,
                        elevationDp = if (isDone || isToday) 5.dp else 3.dp
                    )
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
                                    color = if (isFuture) secondaryTextColor.copy(alpha = 0.6f) else textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
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

                    // Right action / checkmark badge — 34.dp matching left date badge & active style
                    CalendarWeekBadge(
                        isDone = isDone,
                        isPartial = isPartial,
                        isFuture = isFuture,
                        totalCount = totalCount,
                        targetCount = targetCount,
                        cellShape = cellShape,
                        checkmarkStyleName = checkmarkStyleName,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        borderColor = borderColor,
                        appFont = appFont
                    )
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
    cellShapeKey: String = "rounded_square",
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
    val cellBorderColor = remember(isToday, isCompleted, isPartial, primaryColor, borderColor) {
        when {
            isToday -> primaryColor.copy(alpha = 0.90f)
            isCompleted -> primaryColor.copy(alpha = 0.65f)
            isPartial -> primaryColor.copy(alpha = 0.35f)
            else -> borderColor
        }
    }

    val boxBgColor = remember(isCompleted, isPartial, checkmarkStyleName, totalSum, targetCount, primaryColor, cardBgColor) {
        when {
            isCompleted -> {
                val fillAlpha = when (checkmarkStyleName) {
                    "filled" -> (0.20f + (totalSum - targetCount) * 0.04f).coerceIn(0.20f, 0.34f)
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
    }

    var prevSum by remember(cell.dateString) { mutableIntStateOf(totalSum) }
    var bounceTrigger by remember(cell.dateString) { mutableStateOf(false) }
    LaunchedEffect(totalSum) {
        if (totalSum != prevSum) {
            bounceTrigger = true
            kotlinx.coroutines.delay(140)
            bounceTrigger = false
            prevSum = totalSum
        }
    }
    val cellScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 1.09f else 1f,
        animationSpec = MotionSpecs.BouncyPopSpring,
        label = "cell_bounce_${cell.dateString}"
    )

    val isLightCell = !ThemeStyles.isLightColor(textColor)
    val isGlassCell = ThemeStyles.isGlassTheme(selectedTheme)
    // Outer Day Box — lightweight 2D soft sheen on completed cells & subtle recessed well on empty cells
    Box(
        modifier = modifier
            .scale(cellScale)
            .aspectRatio(1f)
            .then(
                when {
                    isCompleted -> Modifier.surface3DCard(
                        shape = cellShape,
                        isLight = isLightCell,
                        primaryColor = primaryColor,
                        isGlass = isGlassCell,
                        elevationDp = 3.dp
                    )
                    !isPartial -> Modifier.recessedWell(cellShape, isLight = isLightCell)
                    else -> Modifier
                }
            )
            .clip(cellShape)
            .background(boxBgColor)
            .then(
                if (isCompleted) Modifier.completedCellCornerGlow(primaryColor) else Modifier
            )
            .border(width = borderWidth, color = cellBorderColor, shape = cellShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        CalendarCompletionIndicator(
            dayNumber = cell.date.dayOfMonth,
            totalSum = totalSum,
            targetCount = targetCount,
            isCompleted = isCompleted,
            isPartial = isPartial,
            isToday = isToday,
            isCurrentMonth = cell.isCurrentMonth,
            hasNotes = hasNotes,
            moodTag = moodTag,
            cellShapeKey = cellShapeKey,
            cellShape = cellShape,
            checkmarkStyleName = checkmarkStyleName,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            appFont = appFont
        )
    }
}
