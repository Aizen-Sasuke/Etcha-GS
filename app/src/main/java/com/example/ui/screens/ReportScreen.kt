package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.HabitStats
import com.example.ui.HabitViewModel
import com.example.ui.ReportAnalyticsSnapshot
import com.example.ui.ThemeStyles
import com.example.ui.components.AnimatedNumberText
import com.example.ui.components.MotionSpecs
import com.example.ui.components.recessedWell
import com.example.ui.components.surface3DCard
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    isTwoPane: Boolean = false
) {
    val stats by viewModel.reportStats.collectAsStateWithLifecycle(initialValue = HabitStats())
    val analytics by viewModel.reportAnalytics.collectAsStateWithLifecycle(initialValue = ReportAnalyticsSnapshot())
    val correlationInsights by viewModel.correlationInsights.collectAsStateWithLifecycle()
    val activeTracker by viewModel.reportTracker.collectAsStateWithLifecycle()
    val allTrackers by viewModel.allTrackers.collectAsStateWithLifecycle()
    val vacationState by viewModel.vacationState.collectAsStateWithLifecycle()
    val dayRolloverHour by viewModel.dayRolloverHour.collectAsStateWithLifecycle()
    val today = remember(dayRolloverHour) { viewModel.getEffectiveToday() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var trackerToAdjustReminder by remember { mutableStateOf<com.example.ui.TrackerConfig?>(null) }
    var trackerToArchive by remember { mutableStateOf<com.example.ui.TrackerConfig?>(null) }
    var trackerToDelete by remember { mutableStateOf<com.example.ui.TrackerConfig?>(null) }

    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val customHex by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
    val customBgHex by viewModel.customBackgroundColor.collectAsStateWithLifecycle()
    val palette = remember(selectedTheme, customHex, customBgHex) {
        ThemeStyles.resolvePalette(selectedTheme, customHex, customBgHex)
    }
    val isGlass = palette.isGlass
    val isLight = !palette.isDark
    val bgColor = palette.background
    val cardBgColor = palette.cardSurface
    val modalBgColor = palette.modalSurface
    val neutralWellColor = palette.neutralWell
    val borderColor = palette.border
    val cardStroke = ThemeStyles.getGlassBorderStroke(selectedTheme, customBgHex)
    val primaryColor = palette.primary
    val accentTextColor = palette.accentOnSurface
    val textColor = palette.textPrimary
    val secondaryTextColor = palette.textSecondary
    val onPrimaryColor = palette.onPrimary

    val streakLabel = remember(activeTracker) {
        if (activeTracker.type == "bad") "Longest clean streak" else "Longest consecutive streak"
    }
    val streakSubtitle = remember(stats) {
        stats.bestStreakTimeline ?: "best record"
    }

    var trendFilter by remember { mutableStateOf("all") }
    var dowFilter by remember { mutableStateOf("all") }
    var selectedReportTab by remember { mutableIntStateOf(0) }

    val monthlyData = remember(analytics, trendFilter) {
        if (trendFilter == "year") analytics.monthlyTrendYear else analytics.monthlyTrendAll
    }
    val dayOfWeekPoints = remember(analytics, dowFilter) {
        if (dowFilter == "year") analytics.dayOfWeekYear else analytics.dayOfWeekAll
    }

    val isWeeklyQuota = activeTracker.frequencyType == "weekly_quota"

    val milestonesList = remember(isWeeklyQuota) {
        if (isWeeklyQuota) {
            listOf(
                HabitMilestone("1_week", "🌱", "First Spark", 1, "Complete 1 full week target"),
                HabitMilestone("2_week", "⚡", "Momentum", 2, "2 consecutive weeks"),
                HabitMilestone("4_week", "🔥", "Month Master", 4, "4 consecutive weeks"),
                HabitMilestone("8_week", "🚀", "Two Months", 8, "8 consecutive weeks"),
                HabitMilestone("12_week", "🧠", "Quarter Loop", 12, "12 consecutive weeks"),
                HabitMilestone("26_week", "🏆", "Half Year", 26, "26 weeks dedication"),
                HabitMilestone("52_week", "👑", "Centurion", 52, "52 weeks legendary")
            )
        } else {
            listOf(
                HabitMilestone("1_day", "🌱", "First Spark", 1, "Log your first day"),
                HabitMilestone("3_day", "⚡", "Momentum", 3, "3 days streak"),
                HabitMilestone("7_day", "🔥", "Week Streak", 7, "7 consecutive days"),
                HabitMilestone("14_day", "🚀", "Two Weeks", 14, "14 days unbroken"),
                HabitMilestone("21_day", "🧠", "Habit Builder", 21, "21 days habit loop"),
                HabitMilestone("30_day", "🏆", "Month Master", 30, "30 days dedication"),
                HabitMilestone("50_day", "💎", "Unbreakable", 50, "50 days resilience"),
                HabitMilestone("100_day", "👑", "Centurion", 100, "100 days legendary")
            )
        }
    }

    val effectiveStreak = maxOf(stats.currentStreak, stats.bestStreak)
    val unlockedCount = remember(effectiveStreak, milestonesList) {
        milestonesList.count { effectiveStreak >= it.targetDays }
    }

    val monthProgressFraction = remember(analytics.currentMonthLoggedDays, analytics.daysInCurrentMonth) {
        (analytics.currentMonthLoggedDays.toFloat() / analytics.daysInCurrentMonth.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    }
    val monthPercent = remember(monthProgressFraction) { (monthProgressFraction * 100).toInt() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isGlass) Color.Transparent else bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Habit Selection Row (Supports active and archived habits)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(allTrackers, key = { it.id }) { tracker ->
                    val isSelected = tracker.id == activeTracker.id
                    val isArchived = tracker.isArchived
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) primaryColor.copy(alpha = 0.2f) else cardBgColor)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) primaryColor else borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectReportTracker(tracker.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = if (isArchived) "📦 ${tracker.icon}" else tracker.icon, fontSize = 13.sp)
                            Text(
                                text = if (isArchived) "${tracker.title} (Archived)" else tracker.title,
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentTextColor else (if (isArchived) secondaryTextColor else textColor)
                            )
                        }
                    }
                }
            }

            if (activeTracker.isArchived) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(borderColor.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📦", fontSize = 13.sp)
                        Text(
                            text = "Archived Habit — Past logs, streaks, and milestone achievements are safely preserved.",
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Segmented Top Tab Selector — adaptive uniform height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.ScreenHorizontalPadding, vertical = 6.dp)
                    .heightIn(min = DesignTokens.SegmentedTabHeight)
                    .height(IntrinsicSize.Max)
                    .recessedWell(DesignTokens.ControlShape, isLight = isLight)
                    .clip(DesignTokens.ControlShape)
                    .background(if (isLight) borderColor.copy(alpha = 0.25f) else cardBgColor)
                    .border(cardStroke, DesignTokens.ControlShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedReportTab == 0) primaryColor else Color.Transparent)
                        .clickable { selectedReportTab = 0 }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊 Analytics & Streaks",
                        fontFamily = appFont,
                        color = if (selectedReportTab == 0) onPrimaryColor else secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (selectedReportTab == 0) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedReportTab == 1) primaryColor else Color.Transparent)
                        .clickable { selectedReportTab = 1 }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🟩 Year Heatmap",
                        fontFamily = appFont,
                        color = if (selectedReportTab == 1) onPrimaryColor else secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (selectedReportTab == 1) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedContent(
                targetState = selectedReportTab,
                transitionSpec = {
                    fadeIn(tween(100)) togetherWith fadeOut(tween(70))
                },
                label = "report_tab_transition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                if (tab == 0) {
                    val maxContentWidth = if (isTwoPane) 1040.dp else 640.dp
                    val streakUnit = if (isWeeklyQuota) {
                        if (stats.bestStreak == 1) "week" else "weeks"
                    } else "days"

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. STATS GRID (Symmetrical 2x2 grid)
                        item(key = "stats_grid") {
                            Column(
                                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CompactStatsCard(
                                        appFont = appFont,
                                        title = "Total logged days",
                                        value = "${analytics.loggedDaysCount}",
                                        subtitle = "out of ~${analytics.totalSpanDays} days",
                                        cardBgColor = cardBgColor,
                                        borderColor = borderColor,
                                        borderStroke = cardStroke,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 104.dp)
                                    )

                                    CompactStatsCard(
                                        appFont = appFont,
                                        title = "Total count",
                                        value = "${analytics.totalCount}",
                                        subtitle = analytics.countSubtitle,
                                        cardBgColor = cardBgColor,
                                        borderColor = borderColor,
                                        borderStroke = cardStroke,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 104.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CompactStatsCard(
                                        appFont = appFont,
                                        title = "Avg per month",
                                        value = String.format(Locale.US, "%.1f", analytics.avgPerMonth),
                                        subtitle = analytics.avgSubtitle,
                                        cardBgColor = cardBgColor,
                                        borderColor = borderColor,
                                        borderStroke = cardStroke,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 104.dp)
                                    )

                                    CompactStatsCard(
                                        appFont = appFont,
                                        title = streakLabel,
                                        value = "${stats.bestStreak} $streakUnit",
                                        subtitle = streakSubtitle,
                                        cardBgColor = cardBgColor,
                                        borderColor = borderColor,
                                        borderStroke = cardStroke,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 104.dp)
                                    )
                                }
                            }
                        }

                        // 2. THIS MONTH'S CONSISTENCY CARD
                        item(key = "month_consistency") {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = cardStroke,
                                shape = DesignTokens.CardShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = maxContentWidth)
                                    .surface3DCard(
                                        shape = DesignTokens.CardShape,
                                        isLight = isLight,
                                        primaryColor = primaryColor,
                                        isGlass = isGlass,
                                        elevationDp = 6.dp
                                    )
                            ) {
                                Column(modifier = Modifier.padding(DesignTokens.CardPadding)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "THIS MONTH'S CONSISTENCY",
                                                fontFamily = appFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = secondaryTextColor
                                            )
                                            Text(
                                                text = "${analytics.currentMonthLoggedDays} of ${analytics.daysInCurrentMonth} days logged",
                                                fontFamily = appFont,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )
                                        }
                                        AnimatedNumberText(
                                            value = monthPercent,
                                            suffix = "%",
                                            fontFamily = appFont,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 22.sp,
                                            color = accentTextColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val animatedMonthProgress by animateFloatAsState(
                                        targetValue = monthProgressFraction,
                                        animationSpec = MotionSpecs.SnappyFloatSpring,
                                        label = "month_consistency_bar"
                                    )
                                    LinearProgressIndicator(
                                        progress = { animatedMonthProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .recessedWell(RoundedCornerShape(3.dp), isLight = isLight)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = primaryColor,
                                        trackColor = if (isLight) borderColor.copy(alpha = 0.4f) else borderColor.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }

                        // 3. 12-WEEK SCHEDULE ADHERENCE
                        item(key = "weekly_consistency") {
                            Box(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                                WeeklyConsistencyChartCard(
                                    appFont = appFont,
                                    points = analytics.weeklyConsistency,
                                    primaryColor = primaryColor,
                                    textColor = textColor,
                                    secondaryTextColor = secondaryTextColor,
                                    cardBgColor = cardBgColor,
                                    borderColor = borderColor
                                )
                            }
                        }

                        // 4. STREAK MILESTONES & ACHIEVEMENTS
                        item(key = "milestones_section") {
                            Column(
                                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STREAK MILESTONES",
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = secondaryTextColor
                                    )
                                    Text(
                                        text = "$unlockedCount / ${milestonesList.size} unlocked",
                                        fontFamily = appFont,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentTextColor
                                    )
                                }

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(milestonesList, key = { it.id }) { milestone ->
                                        val isUnlocked = effectiveStreak >= milestone.targetDays
                                        val progress = (effectiveStreak.toFloat() / milestone.targetDays.toFloat()).coerceIn(0f, 1f)
                                        val badgeBg = if (isUnlocked) primaryColor.copy(alpha = 0.22f) else neutralWellColor

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isUnlocked) primaryColor.copy(alpha = 0.12f) else cardBgColor
                                            ),
                                            border = BorderStroke(
                                                width = if (isUnlocked) 1.5.dp else 0.5.dp,
                                                color = if (isUnlocked) primaryColor.copy(alpha = 0.6f) else borderColor
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .width(136.dp)
                                                .heightIn(min = 136.dp)
                                                .surface3DCard(
                                                    shape = RoundedCornerShape(14.dp),
                                                    isLight = isLight,
                                                    primaryColor = primaryColor,
                                                    isGlass = isGlass,
                                                    elevationDp = if (isUnlocked) 5.dp else 3.dp
                                                )
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                MilestoneProgressRingBadge(
                                                    icon = milestone.icon,
                                                    progress = progress,
                                                    isUnlocked = isUnlocked,
                                                    primaryColor = primaryColor,
                                                    trackColor = borderColor.copy(alpha = 0.35f),
                                                    badgeBgColor = badgeBg
                                                )

                                                Text(
                                                    text = milestone.title,
                                                    fontFamily = appFont,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = textColor,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                if (isUnlocked) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(primaryColor)
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "UNLOCKED",
                                                            fontFamily = appFont,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = onPrimaryColor,
                                                            maxLines = 1
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = if (isWeeklyQuota) "$effectiveStreak / ${milestone.targetDays} wks" else "$effectiveStreak / ${milestone.targetDays} d",
                                                        fontFamily = appFont,
                                                        fontSize = 10.sp,
                                                        color = secondaryTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. MONTHLY TREND CARD
                        item(key = "monthly_trend") {
                            Column(
                                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MONTHLY TREND",
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = secondaryTextColor
                                    )

                                    FilterSelector(
                                        selected = trendFilter,
                                        onSelect = { trendFilter = it },
                                        appFont = appFont,
                                        primaryColor = primaryColor,
                                        accentTextColor = accentTextColor,
                                        secondaryTextColor = secondaryTextColor,
                                        borderColor = borderColor,
                                        isLight = isLight
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = cardStroke,
                                    shape = DesignTokens.CardShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .surface3DCard(
                                            shape = DesignTokens.CardShape,
                                            isLight = isLight,
                                            primaryColor = primaryColor,
                                            isGlass = isGlass,
                                            elevationDp = 6.dp
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        InteractiveMonthlyTrendChart(
                                            appFont = appFont,
                                            dataPoints = monthlyData,
                                            lineColor = primaryColor,
                                            textColor = textColor,
                                            secondaryTextColor = secondaryTextColor,
                                            gridColor = borderColor,
                                            cardBgColor = cardBgColor
                                        )
                                    }
                                }
                            }
                        }

                        // 6. BY DAY OF WEEK CARD
                        item(key = "dow_chart") {
                            Column(
                                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BY DAY OF WEEK",
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = secondaryTextColor
                                    )

                                    FilterSelector(
                                        selected = dowFilter,
                                        onSelect = { dowFilter = it },
                                        appFont = appFont,
                                        primaryColor = primaryColor,
                                        accentTextColor = accentTextColor,
                                        secondaryTextColor = secondaryTextColor,
                                        borderColor = borderColor,
                                        isLight = isLight
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = cardStroke,
                                    shape = DesignTokens.CardShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .surface3DCard(
                                            shape = DesignTokens.CardShape,
                                            isLight = isLight,
                                            primaryColor = primaryColor,
                                            isGlass = isGlass,
                                            elevationDp = 6.dp
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        InteractiveDayOfWeekBarChart(
                                            appFont = appFont,
                                            dataPoints = dayOfWeekPoints,
                                            primaryColor = primaryColor,
                                            secondaryTextColor = secondaryTextColor,
                                            gridColor = borderColor,
                                            cardBgColor = modalBgColor
                                        )
                                    }
                                }
                            }
                        }

                        // 7. MOOD & CHECK-IN RHYTHM
                        if (analytics.moodBreakdown.isNotEmpty() || analytics.timeOfDayBreakdown.isNotEmpty()) {
                            item(key = "mood_rhythm") {
                                Box(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                                    MoodAndRhythmBreakdownCard(
                                        appFont = appFont,
                                        moods = analytics.moodBreakdown,
                                        timeOfDay = analytics.timeOfDayBreakdown,
                                        primaryColor = primaryColor,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        cardBgColor = cardBgColor,
                                        borderColor = borderColor
                                    )
                                }
                            }
                        }

                        // 8. PATTERNS & INSIGHTS CARD
                        item(key = "patterns_insights") {
                            Column(
                                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "PATTERNS & INSIGHTS",
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = secondaryTextColor,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = cardStroke,
                                    shape = DesignTokens.CardShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .surface3DCard(
                                            shape = DesignTokens.CardShape,
                                            isLight = isLight,
                                            primaryColor = primaryColor,
                                            isGlass = isGlass,
                                            elevationDp = 5.dp
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Highlight Icon",
                                            tint = accentTextColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = analytics.annualInsights,
                                            fontFamily = appFont,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // 9. BEHAVIORAL CORRELATIONS & DROP-OFFS
                        item(key = "behavioral_insights") {
                            Box(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                                BehavioralInsightsSection(
                                    insights = correlationInsights,
                                    appFont = appFont,
                                    primaryColor = primaryColor,
                                    cardBgColor = cardBgColor,
                                    borderColor = borderColor,
                                    cardStroke = cardStroke,
                                    textColor = textColor,
                                    secondaryTextColor = secondaryTextColor,
                                    isLight = isLight,
                                    isGlass = isGlass,
                                    onAdjustReminder = { trackerId ->
                                        val target = allTrackers.find { it.id == trackerId }
                                        if (target != null) {
                                            trackerToAdjustReminder = target
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Open Settings or edit the habit to fine-tune reminder timing.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    HeatmapScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        UnifiedHabitSettingsSheet(
            tracker = trackerToAdjustReminder,
            initialTab = 2,
            today = today,
            vacationState = vacationState,
            canDelete = allTrackers.size > 1,
            onDismiss = { trackerToAdjustReminder = null },
            onPauseUntil = { id, dateStr -> viewModel.setTrackerPausedUntil(id, dateStr) },
            onArchive = { trackerToArchive = it },
            onDelete = { trackerToDelete = it },
            viewModel = viewModel,
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = modalBgColor,
            borderColor = borderColor,
            selectedTheme = selectedTheme
        )

        ArchiveTrackerDialog(
            tracker = trackerToArchive,
            onDismiss = { trackerToArchive = null },
            onConfirm = { id -> viewModel.archiveTracker(id) },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = modalBgColor,
            borderColor = borderColor
        )

        DeleteTrackerDialog(
            tracker = trackerToDelete,
            onDismiss = { trackerToDelete = null },
            onConfirm = { id -> viewModel.deleteTracker(id) },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = modalBgColor,
            borderColor = borderColor
        )
    }
}

data class HabitMilestone(
    val id: String,
    val icon: String,
    val title: String,
    val targetDays: Int,
    val description: String
)

@Composable
fun CompactStatsCard(
    appFont: FontFamily,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    cardBgColor: Color = Color(0xFF161616),
    borderColor: Color = Color(0xFF262626),
    borderStroke: BorderStroke = BorderStroke(0.5.dp, borderColor),
    textColor: Color = Color.White,
    secondaryTextColor: Color = Color.Gray
) {
    val isLightCard = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = borderStroke,
        shape = DesignTokens.CardShape,
        modifier = modifier.surface3DCard(
            shape = DesignTokens.CardShape,
            isLight = isLightCard,
            elevationDp = 5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.CardPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontFamily = appFont,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                fontFamily = appFont,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontFamily = appFont,
                color = secondaryTextColor.copy(alpha = 0.85f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FilterSelector(
    selected: String,
    onSelect: (String) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    isLight: Boolean = false,
    accentTextColor: Color = ThemeStyles.accentOnSurface(
        primaryColor,
        if (isLight) Color(0xFFF5F3EC) else Color(0xFF16181F),
        fallbackInk = if (isLight) Color(0xFF141414) else Color.White
    )
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .recessedWell(RoundedCornerShape(8.dp), isLight = isLight)
            .clip(RoundedCornerShape(8.dp))
            .background(secondaryTextColor.copy(alpha = 0.05f))
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        listOf("all" to "All time", "year" to "This year").forEach { (id, label) ->
            val isSelected = selected == id
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) primaryColor.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = appFont,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentTextColor else secondaryTextColor
                )
            }
        }
    }
}

