package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HabitEntry
import com.example.ui.HabitStatsCalculator
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.components.StaggeredReveal
import com.example.ui.components.springPressScale
import com.example.ui.components.surface3DCard
import com.example.worker.ReminderManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

const val HEATMAP_PERIOD_ROLLING_12M = "rolling_12m"

data class HeatmapSummaryStats(
    val activeDays: Int,
    val bestStreak: Int,
    val totalSignals: Int,
    val bestDayLabel: String
)

data class HeatmapGridModel(
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val gridStart: LocalDate,
    val gridEnd: LocalDate,
    val weeks: Int,
    val monthHeaders: List<Pair<Int, String>>,
    val summary: HeatmapSummaryStats
)

fun buildAvailableHeatmapPeriods(
    entries: List<HabitEntry>,
    today: LocalDate
): List<Pair<String, String>> {
    val years = entries.mapNotNull { entry ->
        try {
            LocalDate.parse(entry.dateString).year
        } catch (_: Exception) {
            null
        }
    }.toMutableSet()
    years.add(today.year)

    val result = mutableListOf(HEATMAP_PERIOD_ROLLING_12M to "Rolling 12M")
    years.sortedDescending().forEach { year ->
        result.add(year.toString() to year.toString())
    }
    return result
}

fun buildHeatmapGridModel(
    entries: List<HabitEntry>,
    tracker: TrackerConfig,
    freezeActive: String?,
    freezeHist: String,
    periodKey: String,
    today: LocalDate
): HeatmapGridModel {
    val selectedYear = periodKey.toIntOrNull()
    val rangeStart: LocalDate
    val rangeEnd: LocalDate

    if (selectedYear != null) {
        rangeStart = LocalDate.of(selectedYear, 1, 1)
        rangeEnd = LocalDate.of(selectedYear, 12, 31)
    } else {
        rangeEnd = today
        rangeStart = today.minusDays(364)
    }

    val startOffset = rangeStart.dayOfWeek.value % 7 // 0 = Sunday
    val gridStart = rangeStart.minusDays(startOffset.toLong())
    val endOffset = 6 - (rangeEnd.dayOfWeek.value % 7) // Saturday = 6
    val gridEnd = rangeEnd.plusDays(endOffset.toLong())

    val totalDaysInGrid = ChronoUnit.DAYS.between(gridStart, gridEnd).toInt().coerceAtLeast(0)
    val weeks = (totalDaysInGrid / 7) + 1

    // Align month labels to the exact week column containing the 1st of that month
    val monthHeaders = mutableListOf<Pair<Int, String>>()
    var lastLabeledWeek = -10
    for (w in 0 until weeks) {
        val weekSunday = gridStart.plusDays((w * 7).toLong())
        var labelForWeek: String? = null
        for (d in 0..6) {
            val day = weekSunday.plusDays(d.toLong())
            if (!day.isBefore(rangeStart) && !day.isAfter(rangeEnd)) {
                if (day.dayOfMonth == 1 || (w == 0 && d == startOffset)) {
                    labelForWeek = day.month.getDisplayName(TextStyle.SHORT, Locale.US)
                    if (day.dayOfMonth == 1) break
                }
            }
        }
        if (labelForWeek != null && (w - lastLabeledWeek >= 3 || w == 0)) {
            monthHeaders.add(w to labelForWeek)
            lastLabeledWeek = w
        }
    }

    // Period-scoped summary metrics
    val filteredEntries = entries.filter { entry ->
        val d = try {
            LocalDate.parse(entry.dateString)
        } catch (_: Exception) {
            null
        }
        d != null && !d.isBefore(rangeStart) && !d.isAfter(rangeEnd) && entry.count > 0
    }
    val groupedByDate = filteredEntries.groupBy { it.dateString }
    val activeDays = groupedByDate.size
    val totalSignals = filteredEntries.sumOf { it.count }

    val bestDayEntry = groupedByDate.maxByOrNull { (_, list) -> list.sumOf { it.count } }
    val bestDayLabel = if (bestDayEntry != null) {
        val bestDate = try {
            LocalDate.parse(bestDayEntry.key)
        } catch (_: Exception) {
            null
        }
        val bestSum = bestDayEntry.value.sumOf { it.count }
        if (bestDate != null) {
            "${bestDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))} · $bestSum"
        } else {
            "$bestSum"
        }
    } else {
        "—"
    }

    val effectiveRefToday = if (today.isAfter(rangeEnd)) rangeEnd else today
    val periodStats = HabitStatsCalculator.calculateStats(
        list = filteredEntries,
        freezeActive = freezeActive,
        freezeHist = freezeHist,
        tracker = tracker,
        today = effectiveRefToday
    )

    return HeatmapGridModel(
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        gridStart = gridStart,
        gridEnd = gridEnd,
        weeks = weeks,
        monthHeaders = monthHeaders,
        summary = HeatmapSummaryStats(
            activeDays = activeDays,
            bestStreak = periodStats.bestStreak,
            totalSignals = totalSignals,
            bestDayLabel = bestDayLabel
        )
    )
}

@Composable
fun HeatmapScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val customHex by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
    val customBgHex by viewModel.customBackgroundColor.collectAsStateWithLifecycle()
    val isGlass = ThemeStyles.isGlassTheme(selectedTheme)
    val isLight = ThemeStyles.isLightTheme(selectedTheme, customBgHex)
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val modalBgColor = ThemeStyles.getOpaqueCardColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme, customBgHex)
    val cardStroke = ThemeStyles.getGlassBorderStroke(selectedTheme, customBgHex)
    val basePrimaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val textColor = ThemeStyles.getTextColor(selectedTheme, customBgHex)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme, customBgHex)
    val onPrimaryColor = if (ThemeStyles.isLightColor(basePrimaryColor)) Color.Black else Color.White
    val emptyCellColor = if (isLight) {
        borderColor.copy(alpha = 0.22f)
    } else {
        borderColor.copy(alpha = 0.28f)
    }
    val listRowBgColor = if (isLight) {
        borderColor.copy(alpha = 0.14f)
    } else {
        borderColor.copy(alpha = 0.22f)
    }

    val activeTracker by viewModel.reportTracker.collectAsStateWithLifecycle()
    val primaryColor = activeTracker.accentColor?.let {
        try {
            Color(it.toColorInt())
        } catch (_: Exception) {
            basePrimaryColor
        }
    } ?: basePrimaryColor

    val entries by viewModel.reportEntries.collectAsStateWithLifecycle(initialValue = emptyList())
    val freezeActive by viewModel.streakFreezeDate.collectAsStateWithLifecycle()
    val freezeHistory by viewModel.streakFreezeHistory.collectAsStateWithLifecycle()
    val rolloverHour by viewModel.dayRolloverHour.collectAsStateWithLifecycle()
    val today = remember(rolloverHour) { ReminderManager.getEffectiveToday(rolloverHour) }

    val availablePeriods = remember(entries, today) {
        buildAvailableHeatmapPeriods(entries, today)
    }
    var selectedPeriodKey by remember { mutableStateOf(HEATMAP_PERIOD_ROLLING_12M) }
    LaunchedEffect(availablePeriods) {
        if (availablePeriods.none { it.first == selectedPeriodKey }) {
            selectedPeriodKey = HEATMAP_PERIOD_ROLLING_12M
        }
    }

    val entriesByDate = remember(entries) {
        entries.groupBy { it.dateString }
    }

    val gridModel = remember(entries, activeTracker, freezeActive, freezeHistory, selectedPeriodKey, today) {
        buildHeatmapGridModel(
            entries = entries,
            tracker = activeTracker,
            freezeActive = freezeActive,
            freezeHist = freezeHistory,
            periodKey = selectedPeriodKey,
            today = today
        )
    }

    val scrollState = rememberScrollState()
    var showCellDetails by remember { mutableStateOf(false) }
    var selectedCellDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedCellEntries by remember { mutableStateOf<List<HabitEntry>>(emptyList()) }

    if (showCellDetails && selectedCellDate != null) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showCellDetails = false },
            containerColor = modalBgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val formattedDate = selectedCellDate!!.format(
                    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US)
                )
                val totalCount = selectedCellEntries.sumOf { it.count }

                Text(
                    fontFamily = appFont,
                    text = formattedDate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Text(
                    fontFamily = appFont,
                    text = if (totalCount > 0) {
                        "Total signals logged: $totalCount"
                    } else {
                        "No check-ins recorded on this day"
                    },
                    fontSize = 14.sp,
                    color = if (totalCount > 0) primaryColor else secondaryTextColor,
                    fontWeight = FontWeight.SemiBold
                )

                if (selectedCellEntries.isNotEmpty()) {
                    Text(
                        fontFamily = appFont,
                        text = "Log Details:",
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                    selectedCellEntries.sortedBy { it.timestamp }.forEach { entry ->
                        val timeStr = Instant.ofEpochMilli(entry.timestamp)
                            .atZone(ZoneId.systemDefault())
                            .format(timeFormatter)

                        val moodMatch = entry.notes?.let { Regex("\\[MOOD:([^\\]]+)\\]").find(it)?.groupValues?.get(1) }
                        val cleanNote = entry.notes?.replace(Regex("\\[MOOD:[^\\]]+\\]"), "")?.trim()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(listRowBgColor)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (moodMatch != null) {
                                        Text(text = moodMatch, fontSize = 13.sp)
                                    }
                                    Text(
                                        fontFamily = appFont,
                                        text = timeStr,
                                        color = textColor,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    fontFamily = appFont,
                                    text = "+${entry.count}",
                                    color = primaryColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!cleanNote.isNullOrBlank()) {
                                Text(
                                    text = cleanNote,
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isGlass) Color.Transparent else bgColor)
            .padding(horizontal = DesignTokens.ScreenHorizontalPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Title & Habit Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Activity Heatmap",
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = textColor
                    )
                    Text(
                        fontFamily = appFont,
                        text = "${activeTracker.icon} ${activeTracker.title} · Tap any day cell to inspect logs",
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Period / Year Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availablePeriods.forEach { (key, label) ->
                    val isSelected = selectedPeriodKey == key
                    Box(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .springPressScale(pressedScale = 0.92f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) basePrimaryColor else cardBgColor)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) basePrimaryColor else borderColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPeriodKey = key }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("heatmap_period_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) onPrimaryColor else secondaryTextColor
                        )
                    }
                }
            }

            // Symmetrical 2x2 Summary KPI Strip
            StaggeredReveal(index = 0) {
            val streakUnit = if (activeTracker.frequencyType == "weekly_quota") "wks" else "days"
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeatmapStatCard(
                        title = "Active Days",
                        value = "${gridModel.summary.activeDays}",
                        appFont = appFont,
                        cardBgColor = cardBgColor,
                        borderStroke = cardStroke,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = primaryColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 76.dp)
                            .testTag("heatmap_stat_active_days")
                    )
                    HeatmapStatCard(
                        title = "Best Streak",
                        value = "${gridModel.summary.bestStreak} $streakUnit",
                        appFont = appFont,
                        cardBgColor = cardBgColor,
                        borderStroke = cardStroke,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = textColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 76.dp)
                            .testTag("heatmap_stat_best_streak")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeatmapStatCard(
                        title = "Total Signals",
                        value = "${gridModel.summary.totalSignals}",
                        appFont = appFont,
                        cardBgColor = cardBgColor,
                        borderStroke = cardStroke,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = textColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 76.dp)
                            .testTag("heatmap_stat_total_signals")
                    )
                    HeatmapStatCard(
                        title = "Best Day",
                        value = gridModel.summary.bestDayLabel,
                        appFont = appFont,
                        cardBgColor = cardBgColor,
                        borderStroke = cardStroke,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = primaryColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 76.dp)
                            .testTag("heatmap_stat_best_day")
                    )
                }
            }
            }

            // Adaptive Responsive Heatmap Card with aligned month headers
            StaggeredReveal(index = 1) {
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
                        elevationDp = 8.dp
                    )
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.CardPadding)
                ) {
                    val weekdayColWidth = 34.dp
                    val colGap = 4.dp
                    val rowGap = 4.dp
                    val availableGridWidth = (maxWidth - weekdayColWidth - 8.dp).coerceAtLeast(180.dp)
                    val fitCellSize = ((availableGridWidth - colGap * (gridModel.weeks - 1).coerceAtLeast(1)) / gridModel.weeks.coerceAtLeast(1))
                    val cellSize = maxOf(14.dp, minOf(22.dp, fitCellSize))
                    val rowCellHeight = maxOf(cellSize, 16.dp)
                    val colPitch = cellSize + colGap
                    val totalGridWidth = cellSize * gridModel.weeks + colGap * (gridModel.weeks - 1).coerceAtLeast(0)

                    val horizontalScrollState = rememberScrollState()
                    LaunchedEffect(selectedPeriodKey, gridModel.weeks) {
                        horizontalScrollState.scrollTo(
                            if (selectedPeriodKey == HEATMAP_PERIOD_ROLLING_12M) {
                                horizontalScrollState.maxValue
                            } else {
                                0
                            }
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Sticky Left Weekday Labels
                            Column(
                                modifier = Modifier
                                    .width(weekdayColWidth)
                                    .padding(top = 24.dp, end = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(rowGap)
                            ) {
                                listOf("", "Mon", "", "Wed", "", "Fri", "").forEach { day ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(rowCellHeight),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            fontFamily = appFont,
                                            text = day,
                                            color = secondaryTextColor,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }

                            // Scrollable Month Header + Grid Columns
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                // Mathematically aligned month labels anchored to week columns
                                Box(
                                    modifier = Modifier
                                        .width(totalGridWidth)
                                        .height(22.dp)
                                        .padding(bottom = 4.dp)
                                ) {
                                    gridModel.monthHeaders.forEach { (weekIdx, monthName) ->
                                        Text(
                                            fontFamily = appFont,
                                            text = monthName,
                                            color = secondaryTextColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            modifier = Modifier.offset(x = colPitch * weekIdx)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(colGap)) {
                                    for (w in 0 until gridModel.weeks) {
                                        Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
                                            for (d in 0..6) {
                                                val cellDate = gridModel.gridStart.plusDays((w * 7 + d).toLong())
                                                val inPeriod = !cellDate.isBefore(gridModel.rangeStart) &&
                                                    !cellDate.isAfter(gridModel.rangeEnd)

                                                if (!inPeriod) {
                                                    Box(modifier = Modifier.size(cellSize))
                                                } else {
                                                    val dateStr = cellDate.toString()
                                                    val entriesForDay = entriesByDate[dateStr] ?: emptyList()
                                                    val count = entriesForDay.sumOf { it.count }

                                                    val intensity = when {
                                                        count == 0 -> 0f
                                                        count == 1 -> 0.28f
                                                        count == 2 -> 0.48f
                                                        count == 3 -> 0.68f
                                                        else -> 0.88f
                                                    }

                                                    val cellColor = if (count > 0) {
                                                        primaryColor.copy(alpha = intensity)
                                                    } else {
                                                        emptyCellColor
                                                    }

                                                    val isTodayCell = cellDate == today
                                                    Box(
                                                        modifier = Modifier
                                                            .size(cellSize)
                                                            .springPressScale(pressedScale = 0.84f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(cellColor)
                                                            .then(
                                                                if (isTodayCell) {
                                                                    Modifier.border(
                                                                        1.dp,
                                                                        primaryColor,
                                                                        RoundedCornerShape(4.dp)
                                                                    )
                                                                } else {
                                                                    Modifier
                                                                }
                                                            )
                                                            .clickable {
                                                                selectedCellDate = cellDate
                                                                selectedCellEntries = entriesForDay
                                                                showCellDetails = true
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Balanced Footer Legend Row
                        Row(
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                fontFamily = appFont,
                                text = if (selectedPeriodKey == HEATMAP_PERIOD_ROLLING_12M) {
                                    "Last 52 weeks"
                                } else {
                                    "Jan – Dec $selectedPeriodKey"
                                },
                                color = secondaryTextColor,
                                fontSize = 11.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    fontFamily = appFont,
                                    text = "Less",
                                    color = secondaryTextColor,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                listOf(0f, 0.28f, 0.48f, 0.68f, 0.88f).forEach { intensity ->
                                    val color = if (intensity == 0f) emptyCellColor else primaryColor.copy(alpha = intensity)
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(color)
                                    )
                                }
                                Text(
                                    fontFamily = appFont,
                                    text = "More",
                                    color = secondaryTextColor,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp)
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
private fun HeatmapStatCard(
    title: String,
    value: String,
    appFont: FontFamily,
    cardBgColor: Color,
    borderStroke: BorderStroke,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isLightCard = !ThemeStyles.isLightColor(textColor)
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = borderStroke,
        shape = DesignTokens.CardShape,
        modifier = modifier.surface3DCard(
            shape = DesignTokens.CardShape,
            isLight = isLightCard,
            primaryColor = accentColor,
            elevationDp = 5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontFamily = appFont,
                fontSize = 11.5.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = appFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (accentColor == Color.White && ThemeStyles.isLightColor(cardBgColor)) textColor else accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
