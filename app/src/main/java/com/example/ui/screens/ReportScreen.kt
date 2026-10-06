package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.HabitStats
import com.example.ui.ThemeStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalTextApi::class)
@Composable
fun ReportScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState(initial = HabitStats())
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val activeTracker by viewModel.activeTracker.collectAsState()
    
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val basePrimaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)

    val primaryColor = activeTracker.accentColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { basePrimaryColor }
    } ?: basePrimaryColor

    val scrollState = rememberScrollState()

    // 1. Calculate stats dynamically to support all history ranges exactly matching screenshot
    val loggedDaysCount = remember(entries) {
        entries.groupBy { it.dateString }.size
    }

    val totalCountVal = remember(entries) {
        entries.sumOf { it.count }
    }

    val totalSpanDays = remember(entries) {
        if (entries.isEmpty()) 1 else {
            val dates = entries.mapNotNull {
                try { LocalDate.parse(it.dateString) } catch (e: Exception) { null }
            }
            if (dates.isEmpty()) 1 else {
                val minDate = dates.minOrNull()!!
                val maxDate = dates.maxOrNull()!!
                (ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1).coerceAtLeast(1)
            }
        }
    }

    val countSubtitle = remember(entries) {
        if (entries.isEmpty()) "No range logged" else {
            val dates = entries.mapNotNull {
                try { LocalDate.parse(it.dateString) } catch (e: Exception) { null }
            }.sorted()
            if (dates.isEmpty()) "No range logged" else {
                val minDate = dates.first()
                val maxDate = dates.last()
                val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
                "${minDate.format(formatter)} – ${maxDate.format(formatter)}"
            }
        }
    }

    val avgPerMonth = remember(entries, totalSpanDays) {
        if (entries.isEmpty() || totalSpanDays <= 0) 0.0 else {
            val months = (totalSpanDays / 30.4).coerceAtLeast(1.0)
            val total = entries.sumOf { it.count }
            total / months
        }
    }

    val avgSubtitle = remember(entries, totalSpanDays) {
        if (entries.isEmpty() || totalSpanDays <= 0) "No logs recorded" else {
            val totalCountVal = entries.sumOf { it.count }
            val ratio = if (totalCountVal == 0) 0.0 else totalSpanDays.toDouble() / totalCountVal.toDouble()
            "roughly every ${String.format(Locale.US, "%.1f", ratio)} days"
        }
    }

    val streakLabel = remember(activeTracker) {
        if (activeTracker.type == "bad") "Longest clean streak" else "Longest consecutive streak"
    }

    val streakSubtitle = remember(stats) {
        stats.bestStreakTimeline ?: "best record"
    }

    var trendFilter by remember { mutableStateOf("all") }
    var dowFilter by remember { mutableStateOf("all") }

    // 2. Generate monthly trend data spanning from the first logged date till last logged date
    val monthlyData = remember(entries, trendFilter) {
        val filtered = if (trendFilter == "year") {
            val currentYear = LocalDate.now().year
            entries.filter {
                try { LocalDate.parse(it.dateString).year == currentYear } catch (e: Exception) { false }
            }
        } else {
            entries
        }
        getTrendAggregates(filtered)
    }

    // 3. Generate Day of Week distribution
    val dayOfWeekPoints = remember(entries, dowFilter) {
        val filtered = if (dowFilter == "year") {
            val currentYear = LocalDate.now().year
            entries.filter {
                try { LocalDate.parse(it.dateString).year == currentYear } catch (e: Exception) { false }
            }
        } else {
            entries
        }

        val dayOfWeekCounts = mutableMapOf<Int, Int>() // 1 = Mon, 7 = Sun
        for (d in 1..7) dayOfWeekCounts[d] = 0
        
        filtered.forEach { entry ->
            try {
                val date = LocalDate.parse(entry.dateString)
                val dow = date.dayOfWeek.value // 1 = Mon, 7 = Sun
                dayOfWeekCounts[dow] = (dayOfWeekCounts[dow] ?: 0) + entry.count
            } catch (e: Exception) {}
        }
        
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        days.mapIndexed { index, name ->
            DayOfWeekDataPoint(name, dayOfWeekCounts[index + 1] ?: 0)
        }
    }

    // 4. Generate dynamic annual trends
    val yearAggregates = remember(entries) {
        entries.groupBy {
            try { LocalDate.parse(it.dateString).year } catch (e: Exception) { LocalDate.now().year }
        }.mapValues { (_, entryList) -> entryList.sumOf { it.count } }
    }

    val annualInsights = remember(entries, yearAggregates) {
        if (entries.isEmpty()) {
            "2026 is tracking nicely — start logging to unlock premium patterns and insights."
        } else {
            val sortedYears = yearAggregates.keys.sorted()
            val currentYear = LocalDate.now().year
            val currentYearCount = yearAggregates[currentYear] ?: 0
            
            val maxYearEntry = yearAggregates.maxByOrNull { it.value }
            var message = ""
            if (maxYearEntry != null) {
                message += "${maxYearEntry.key} was your highest year — ${maxYearEntry.value} total"
                val previousYear = maxYearEntry.key - 1
                val previousYearCount = yearAggregates[previousYear]
                if (previousYearCount != null) {
                    message += ", up from $previousYearCount in $previousYear."
                } else {
                    message += "."
                }
            }
            
            val dayOfYear = LocalDate.now().dayOfYear
            val trackingAnnualized = if (dayOfYear > 0) (currentYearCount.toDouble() / dayOfYear * 365).toInt() else 0
            if (currentYearCount > 0) {
                if (message.isNotEmpty()) message += "\n"
                message += "$currentYear is tracking at ~$trackingAnnualized annualized — showing consistent progress."
            }
            message
        }
    }

    var selectedReportTab by remember { mutableIntStateOf(0) }

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

    // Month consistency calculation
    val today = remember { LocalDate.now() }
    val daysInCurrentMonth = remember(today) { today.lengthOfMonth() }
    val currentMonthLoggedDays = remember(entries, today) {
        entries.filter {
            try {
                val d = LocalDate.parse(it.dateString)
                d.year == today.year && d.monthValue == today.monthValue
            } catch (e: Exception) { false }
        }.groupBy { it.dateString }.size
    }
    val monthProgressFraction = remember(currentMonthLoggedDays, daysInCurrentMonth) {
        (currentMonthLoggedDays.toFloat() / daysInCurrentMonth.toFloat()).coerceIn(0f, 1f)
    }
    val monthPercent = remember(monthProgressFraction) { (monthProgressFraction * 100).toInt() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Segmented Top Tab Selector (always visible at top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.25f) else Color(0xFF141414))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Analytics & Badges
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedReportTab == 0) primaryColor else Color.Transparent)
                        .clickable { selectedReportTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊 Analytics & Streaks",
                        fontFamily = appFont,
                        color = if (selectedReportTab == 0) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (selectedReportTab == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Tab 1: Heatmap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedReportTab == 1) primaryColor else Color.Transparent)
                        .clickable { selectedReportTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🟩 Year Heatmap",
                        fontFamily = appFont,
                        color = if (selectedReportTab == 1) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (selectedReportTab == 1) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            if (selectedReportTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 90.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // 1. STATS ROW (4 items, horizontal scrollable with minimum width to fit beautifully on any viewport size)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CompactStatsCard(
                            appFont = appFont,
                            title = "Total logged days",
                            value = "$loggedDaysCount",
                            subtitle = "out of ~$totalSpanDays days",
                            cardBgColor = cardBgColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            modifier = Modifier.width(165.dp)
                        )

                        CompactStatsCard(
                            appFont = appFont,
                            title = "Total count",
                            value = "$totalCountVal",
                            subtitle = countSubtitle,
                            cardBgColor = cardBgColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            modifier = Modifier.width(165.dp)
                        )

                        CompactStatsCard(
                            appFont = appFont,
                            title = "Avg per month",
                            value = String.format(Locale.US, "%.1f", avgPerMonth),
                            subtitle = avgSubtitle,
                            cardBgColor = cardBgColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            modifier = Modifier.width(165.dp)
                        )

                        val streakUnit = if (isWeeklyQuota) {
                            if (stats.bestStreak == 1) "week" else "weeks"
                        } else "days"

                        CompactStatsCard(
                            appFont = appFont,
                            title = streakLabel,
                            value = "${stats.bestStreak} $streakUnit",
                            subtitle = streakSubtitle,
                            cardBgColor = cardBgColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            modifier = Modifier.width(165.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. THIS MONTH'S CONSISTENCY CARD
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(0.5.dp, borderColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                        text = "$currentMonthLoggedDays of $daysInCurrentMonth days logged",
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                }
                                Text(
                                    text = "$monthPercent%",
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = primaryColor
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { monthProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = primaryColor,
                                trackColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.4f) else Color(0xFF262626)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. STREAK MILESTONES & ACHIEVEMENTS
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
                            color = primaryColor
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(milestonesList) { milestone ->
                            val isUnlocked = effectiveStreak >= milestone.targetDays
                            val progress = (effectiveStreak.toFloat() / milestone.targetDays.toFloat()).coerceIn(0f, 1f)

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUnlocked) primaryColor.copy(alpha = 0.12f) else cardBgColor
                                ),
                                border = BorderStroke(
                                    width = if (isUnlocked) 1.5.dp else 0.5.dp,
                                    color = if (isUnlocked) primaryColor.copy(alpha = 0.6f) else borderColor
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.width(135.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isUnlocked) primaryColor.copy(alpha = 0.25f) else (if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.3f) else Color(0xFF222222))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = milestone.icon,
                                            fontSize = 20.sp
                                        )
                                    }

                                    Text(
                                        text = milestone.title,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
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
                                                color = if (primaryColor == Color.White) Color.Black else Color.White
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = primaryColor,
                                                trackColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.4f) else Color(0xFF333333)
                                            )
                                            Text(
                                                text = if (isWeeklyQuota) "$effectiveStreak / ${milestone.targetDays} wks" else "$effectiveStreak / ${milestone.targetDays} d",
                                                fontFamily = appFont,
                                                fontSize = 10.sp,
                                                color = secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. MONTHLY TREND CARD
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
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = borderColor
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LineChartWithTranslucentFill(
                        appFont = appFont,
                        dataPoints = monthlyData,
                        lineColor = primaryColor,
                        textColor = secondaryTextColor,
                        gridColor = borderColor,
                        cardBgColor = cardBgColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. BY DAY OF WEEK CARD
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
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = borderColor
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BarChartByDayOfWeek(
                        appFont = appFont,
                        dataPoints = dayOfWeekPoints,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        gridColor = borderColor,
                        cardBgColor = cardBgColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. PATTERNS & INSIGHTS CARD
            Text(
                text = "PATTERNS & INSIGHTS",
                fontFamily = appFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = secondaryTextColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
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
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = annualInsights,
                        fontFamily = appFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        HeatmapScreen(
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    }
}
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
    textColor: Color = Color.White,
    secondaryTextColor: Color = Color.Gray
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(0.5.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                fontFamily = appFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = value,
                fontFamily = appFont,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontFamily = appFont,
                color = secondaryTextColor.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun LineChartWithTranslucentFill(
    appFont: FontFamily,
    dataPoints: List<MonthDataPoint>,
    lineColor: Color = Color.White,
    textColor: Color = Color.Gray,
    gridColor: Color = Color(0xFF222222),
    cardBgColor: Color = Color(0xFF0C0C0C),
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val animatedPoints = remember(dataPoints) { Animatable(0f) }
    
    LaunchedEffect(dataPoints) {
        animatedPoints.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val style = TextStyle(
        color = textColor,
        fontSize = 10.sp,
        fontFamily = appFont
    )

    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        
        val leftMargin = 55f
        val bottomMargin = 50f
        
        val chartWidth = w - leftMargin
        val chartHeight = h - bottomMargin

        val maxCountVal = (dataPoints.maxOfOrNull { it.count } ?: 10).coerceAtLeast(1)
        val yMaxVal = (((maxCountVal + 4) / 5) * 5).coerceAtLeast(5)
        
        // Draw standard dotted reference grid (4 slots)
        val gridLinesCount = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        for (i in 0..gridLinesCount) {
            val yFactor = i.toFloat() / gridLinesCount.toFloat()
            val gridY = chartHeight * yFactor
            
            drawLine(
                color = gridColor,
                start = Offset(leftMargin, gridY),
                end = Offset(w, gridY),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )
            
            val yValText = (yMaxVal - (yMaxVal * yFactor).toInt()).toString()
            drawText(
                textMeasurer = textMeasurer,
                text = yValText,
                style = style,
                topLeft = Offset(10f, gridY - 14f)
            )
        }

        val stepsX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
        val coordinates = dataPoints.mapIndexed { index, dp ->
            val scaleVal = dp.count.toFloat() / yMaxVal.toFloat()
            val pointY = chartHeight - (chartHeight * scaleVal)
            val pointX = leftMargin + (index * stepsX)
            Offset(pointX, pointY)
        }

        // Draw axis month names
        coordinates.forEachIndexed { idx, point ->
            val shouldDraw = dataPoints.size <= 8 || idx % ((dataPoints.size / 6).coerceAtLeast(1)) == 0
            if (shouldDraw) {
                val mText = dataPoints[idx].monthLabel
                val textLayoutResult = textMeasurer.measure(mText, style)
                val textWidth = textLayoutResult.size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = mText,
                    style = style,
                    topLeft = Offset(point.x - (textWidth / 2f), chartHeight + 10f)
                )
            }
        }

        val linePath = Path()
        val fillPath = Path()

        val animatedCoors = coordinates.map {
            val progressFactor = animatedPoints.value
            Offset(it.x, chartHeight - (chartHeight - it.y) * progressFactor)
        }

        animatedCoors.forEachIndexed { index, point ->
            if (index == 0) {
                linePath.moveTo(point.x, point.y)
                fillPath.moveTo(point.x, chartHeight)
                fillPath.lineTo(point.x, point.y)
            } else {
                linePath.lineTo(point.x, point.y)
                fillPath.lineTo(point.x, point.y)
            }
            
            if (index == animatedCoors.size - 1) {
                fillPath.lineTo(point.x, chartHeight)
                fillPath.close()
            }
        }

        // Draw Translucent Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.25f),
                    Color.Transparent
                )
            )
        )

        // Draw connection line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw micro-contrast circles on nodes
        animatedCoors.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 7f,
                center = point
            )
            drawCircle(
                color = cardBgColor,
                radius = 3.5f,
                center = point
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun BarChartByDayOfWeek(
    appFont: FontFamily,
    dataPoints: List<DayOfWeekDataPoint>,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    gridColor: Color,
    cardBgColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val animatedProgress = remember(dataPoints) { Animatable(0f) }
    
    LaunchedEffect(dataPoints) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val style = TextStyle(
        color = secondaryTextColor,
        fontSize = 10.sp,
        fontFamily = appFont
    )

    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        
        val leftMargin = 55f
        val bottomMargin = 50f
        
        val chartWidth = w - leftMargin
        val chartHeight = h - bottomMargin

        val maxCountVal = (dataPoints.maxOfOrNull { it.count } ?: 80).coerceAtLeast(1)
        val yMaxVal = ((((maxCountVal + 19) / 20) * 20)).coerceAtLeast(20)
        
        // Draw standard dotted horizontal grid lines (4 slots)
        val gridLinesCount = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        for (i in 0..gridLinesCount) {
            val yFactor = i.toFloat() / gridLinesCount.toFloat()
            val gridY = chartHeight * yFactor
            
            drawLine(
                color = gridColor,
                start = Offset(leftMargin, gridY),
                end = Offset(w, gridY),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )
            
            val yValText = (yMaxVal - (yMaxVal * yFactor).toInt()).toString()
            drawText(
                textMeasurer = textMeasurer,
                text = yValText,
                style = style,
                topLeft = Offset(10f, gridY - 14f)
            )
        }

        // Draw Bars
        val barCount = dataPoints.size
        val barSpacingFactor = 0.35f
        val stepX = chartWidth / barCount
        val barWidth = stepX * (1f - barSpacingFactor)
        
        // Thursday is Index 3
        val maxIndex = dataPoints.indices.maxByOrNull { dataPoints[it].count } ?: -1

        dataPoints.forEachIndexed { index, dp ->
            val barHeightVal = (dp.count.toFloat() / yMaxVal.toFloat() * chartHeight * animatedProgress.value).coerceAtLeast(0f)
            val barLeft = leftMargin + (index * stepX) + (stepX * barSpacingFactor / 2f)
            val barTop = chartHeight - barHeightVal

            val isHighlighted = if (maxIndex >= 0 && dataPoints[maxIndex].count > 0) index == maxIndex else index == 3
            val barColor = if (isHighlighted) {
                primaryColor
            } else {
                primaryColor.copy(alpha = 0.35f)
            }

            // Solid/translucent bars
            drawRoundRect(
                color = barColor,
                topLeft = Offset(barLeft, barTop),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeightVal),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            // Draw Days of Week label text
            val textLayoutResult = textMeasurer.measure(dp.dayName, style)
            val textWidth = textLayoutResult.size.width
            drawText(
                textMeasurer = textMeasurer,
                text = dp.dayName,
                style = style,
                topLeft = Offset(barLeft + (barWidth - textWidth) / 2f, chartHeight + 10f)
            )
        }
    }
}

// Generate trend aggregates dynamically spanning all logged months from first date to present
fun getTrendAggregates(entries: List<HabitEntry>): List<MonthDataPoint> {
    if (entries.isEmpty()) {
        return getSixMonthsAggregates(entries)
    }
    
    val sortedDates = entries.mapNotNull {
        try { LocalDate.parse(it.dateString) } catch (e: Exception) { null }
    }.sorted()
    
    if (sortedDates.isEmpty()) {
        return getSixMonthsAggregates(entries)
    }

    val first = sortedDates.first()
    val last = LocalDate.now()
    
    val list = mutableListOf<MonthDataPoint>()
    var temp = first.withDayOfMonth(1)
    val limit = last.withDayOfMonth(1)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    while (!temp.isAfter(limit)) {
        val y = temp.year
        val m = temp.monthValue
        
        val countCombined = entries.filter {
            try {
                val date = LocalDate.parse(it.dateString, formatter)
                date.year == y && date.monthValue == m
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.count }
        
        val monthName = temp.month.getDisplayName(JavaTextStyle.SHORT, Locale.US)
        val label = if (temp.monthValue == 1 || temp == first.withDayOfMonth(1)) {
            "$monthName ${temp.year % 100}"
        } else {
            monthName
        }
        
        list.add(MonthDataPoint(label, countCombined))
        temp = temp.plusMonths(1)
    }
    
    if (list.size < 6) {
        return getSixMonthsAggregates(entries)
    }
    return list
}

fun getSixMonthsAggregates(entries: List<HabitEntry>): List<MonthDataPoint> {
    val today = LocalDate.now()
    val list = mutableListOf<MonthDataPoint>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    for (i in 5 downTo 0) {
        val targetMonth = today.minusMonths(i.toLong())
        val monthValue = targetMonth.monthValue
        val yearValue = targetMonth.year
        
        val countCombined = entries.filter {
            try {
                val date = LocalDate.parse(it.dateString, formatter)
                date.year == yearValue && date.monthValue == monthValue
            } catch (e: Exception) {
                false
            }
        }.sumOf { it.count }

        val label = targetMonth.month.getDisplayName(JavaTextStyle.SHORT, Locale.US)
        list.add(MonthDataPoint(monthLabel = label, count = countCombined))
    }

    return list
}

data class DayOfWeekDataPoint(
    val dayName: String,
    val count: Int
)

data class MonthDataPoint(
    val monthLabel: String,
    val count: Int
)

@Composable
fun FilterSelector(
    selected: String,
    onSelect: (String) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    borderColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(secondaryTextColor.copy(alpha = 0.05f))
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        listOf("all" to "All time", "year" to "This year").forEach { (id, label) ->
            val isSelected = selected == id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) primaryColor.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = appFont,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) primaryColor else secondaryTextColor
                )
            }
        }
    }
}
