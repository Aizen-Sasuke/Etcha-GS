package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.VacationPauseHelper
import com.example.ui.VacationPauseState
import com.example.ui.components.recessedWell
import java.time.LocalDate

@Composable
internal fun TrackerBasicsFields(
    title: String,
    onTitleChange: (String) -> Unit,
    icon: String,
    onIconChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    targetCount: Int,
    onTargetCountChange: (Int) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    onPrimaryColor: Color,
    accentTextColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    wellColor: Color,
    isLight: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(fontFamily = appFont, text = "Tracker Name", color = secondaryTextColor) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentTextColor,
                unfocusedBorderColor = borderColor,
                focusedLabelColor = accentTextColor,
                unfocusedLabelColor = secondaryTextColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = accentTextColor
            ),
            placeholder = {
                Text(
                    fontFamily = appFont,
                    text = "e.g. Drink Water",
                    color = secondaryTextColor.copy(alpha = 0.55f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tracker_title_input")
        )

        OutlinedTextField(
            value = icon,
            onValueChange = onIconChange,
            label = { Text(fontFamily = appFont, text = "Icon / Emoji", color = secondaryTextColor) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentTextColor,
                unfocusedBorderColor = borderColor,
                focusedLabelColor = accentTextColor,
                unfocusedLabelColor = secondaryTextColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = accentTextColor
            ),
            placeholder = {
                Text(
                    fontFamily = appFont,
                    text = "e.g. 🎯",
                    color = secondaryTextColor.copy(alpha = 0.55f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tracker_icon_input")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                fontFamily = appFont,
                text = "Tracker Type",
                color = secondaryTextColor,
                fontSize = 12.sp
            )
            val trackerTypes = listOf(
                Triple("good", "Good", "Streak breaks on missed days"),
                Triple("misc", "Misc", "Counting only, no streaks"),
                Triple("bad", "Bad", "Avoidance, logging breaks streak")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .recessedWell(RoundedCornerShape(12.dp), isLight = isLight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(wellColor)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                trackerTypes.forEach { (typeId, label, _) ->
                    val isSelected = type == typeId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) primaryColor else Color.Transparent)
                            .clickable { onTypeChange(typeId) }
                            .padding(vertical = 8.dp)
                            .testTag("tracker_type_$typeId"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            fontFamily = appFont,
                            text = label,
                            color = if (isSelected) onPrimaryColor else secondaryTextColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            val selectedTypeDesc = trackerTypes.firstOrNull { it.first == type }?.third ?: ""
            if (selectedTypeDesc.isNotEmpty()) {
                Text(
                    fontFamily = appFont,
                    text = selectedTypeDesc,
                    color = secondaryTextColor,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    fontFamily = appFont,
                    text = "Daily Target Count",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    fontFamily = appFont,
                    text = "e.g. 1 per day, or 8 cups of water",
                    color = secondaryTextColor,
                    fontSize = 11.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (targetCount > 1) onTargetCountChange(targetCount - 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(wellColor)
                        .border(0.5.dp, borderColor, CircleShape)
                ) {
                    Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Text(
                    text = "$targetCount",
                    fontFamily = appFont,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                IconButton(
                    onClick = { if (targetCount < 50) onTargetCountChange(targetCount + 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(wellColor)
                        .border(0.5.dp, borderColor, CircleShape)
                ) {
                    Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }
    }
}

@Composable
internal fun TrackerScheduleFields(
    timeOfDay: String,
    onTimeOfDayChange: (String) -> Unit,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    weeklyTarget: Int,
    onWeeklyTargetChange: (Int) -> Unit,
    customDays: Set<String>,
    onCustomDaysChange: (Set<String>) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    onPrimaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    wellColor: Color,
    isLight: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            fontFamily = appFont,
            text = "Routine (Time of Day)",
            color = secondaryTextColor,
            fontSize = 12.sp
        )
        val routineTypes = listOf(
            "anytime" to "Anytime",
            "morning" to "🌅 Morning",
            "afternoon" to "☀️ Afternoon",
            "evening" to "🌙 Evening"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .recessedWell(RoundedCornerShape(12.dp), isLight = isLight)
                .clip(RoundedCornerShape(12.dp))
                .background(wellColor)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            routineTypes.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowItems.forEach { (rKey, rLabel) ->
                        val isSelected = timeOfDay == rKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) primaryColor else Color.Transparent)
                                .clickable { onTimeOfDayChange(rKey) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                fontFamily = appFont,
                                text = rLabel,
                                color = if (isSelected) onPrimaryColor else secondaryTextColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Text(
            fontFamily = appFont,
            text = "Target Frequency",
            color = secondaryTextColor,
            fontSize = 12.sp
        )
        val freqOptions = listOf(
            "daily" to "Daily",
            "weekdays" to "Weekdays (M-F)",
            "custom_days" to "Custom Days",
            "weekly_quota" to "Weekly Target"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .recessedWell(RoundedCornerShape(12.dp), isLight = isLight)
                .clip(RoundedCornerShape(12.dp))
                .background(wellColor)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            freqOptions.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowItems.forEach { (fKey, fLabel) ->
                        val isSelected = frequency == fKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) primaryColor else Color.Transparent)
                                .clickable { onFrequencyChange(fKey) }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("tracker_freq_$fKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                fontFamily = appFont,
                                text = fLabel,
                                color = if (isSelected) onPrimaryColor else secondaryTextColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (frequency == "weekly_quota") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        fontFamily = appFont,
                        text = "Weekly Target Days",
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        fontFamily = appFont,
                        text = "Complete on $weeklyTarget distinct days per week",
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (weeklyTarget > 1) onWeeklyTargetChange(weeklyTarget - 1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(wellColor)
                            .border(0.5.dp, borderColor, CircleShape)
                    ) {
                        Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Text(
                        text = "$weeklyTarget d/wk",
                        fontFamily = appFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    IconButton(
                        onClick = { if (weeklyTarget < 7) onWeeklyTargetChange(weeklyTarget + 1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(wellColor)
                            .border(0.5.dp, borderColor, CircleShape)
                    ) {
                        Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }
        } else if (frequency == "custom_days") {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    fontFamily = appFont,
                    text = "Select Active Days",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val dayKeys = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayKeys.forEach { dKey ->
                        val isDaySelected = dKey in customDays
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDaySelected) primaryColor else wellColor)
                                .border(0.5.dp, if (isDaySelected) primaryColor else borderColor, CircleShape)
                                .clickable {
                                    val updated = if (isDaySelected) {
                                        if (customDays.size > 1) customDays - dKey else customDays
                                    } else {
                                        customDays + dKey
                                    }
                                    onCustomDaysChange(updated)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dKey.take(1),
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDaySelected) onPrimaryColor else secondaryTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrackerFocusDurationSection(
    focusMinutes: Int,
    onFocusMinutesChange: (Int) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    accentTextColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    wellColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            fontFamily = appFont,
            text = "Default Focus Session",
            color = secondaryTextColor,
            fontSize = 12.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 25, 45, 60).forEach { mins ->
                val isSel = focusMinutes == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) primaryColor.copy(alpha = 0.18f) else wellColor)
                        .border(
                            if (isSel) 1.dp else 0.5.dp,
                            if (isSel) accentTextColor else borderColor,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onFocusMinutesChange(mins) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${mins}m",
                        fontFamily = appFont,
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) accentTextColor else secondaryTextColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedHabitSettingsSheet(
    tracker: TrackerConfig?,
    initialTab: Int = 0,
    today: LocalDate,
    vacationState: VacationPauseState,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onPauseUntil: (trackerId: String, pausedUntilDate: String?) -> Unit,
    onArchive: (TrackerConfig) -> Unit,
    onDelete: (TrackerConfig) -> Unit,
    viewModel: HabitViewModel,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    selectedTheme: String
) {
    if (tracker == null) return

    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val accentTextColor = ThemeStyles.accentOnSurface(primaryColor, cardBgColor, fallbackInk = textColor)
    val isLight = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val wellColor = ThemeStyles.getNeutralWellColor(selectedTheme)
    val dangerColor = ThemeStyles.semanticDangerColor(isLight)

    var selectedTab by remember(tracker.id, initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var editTitle by remember(tracker.id) { mutableStateOf(tracker.title) }
    var editIcon by remember(tracker.id) { mutableStateOf(tracker.icon) }
    var editType by remember(tracker.id) { mutableStateOf(tracker.type) }
    var editTimeOfDay by remember(tracker.id) { mutableStateOf(tracker.timeOfDay) }
    var editFrequency by remember(tracker.id) {
        mutableStateOf(HabitViewModel.normalizeFrequencyType(tracker.frequencyType))
    }
    var editTargetCount by remember(tracker.id) { mutableIntStateOf(tracker.targetCount.coerceAtLeast(1)) }
    var editWeeklyTarget by remember(tracker.id) {
        mutableIntStateOf(if (tracker.weeklyTarget in 1..7) tracker.weeklyTarget else 3)
    }
    var editCustomDays by remember(tracker.id) {
        val canonical = HabitViewModel.normalizeTargetDays(tracker.targetDays)
        mutableStateOf(canonical.split(",").toSet())
    }
    var editReminderEnabled by remember(tracker.id) { mutableStateOf(tracker.reminderEnabled) }
    var editReminderTime by remember(tracker.id) { mutableStateOf(tracker.reminderTime) }
    var editFocusMinutes by remember(tracker.id) { mutableIntStateOf(tracker.defaultFocusMinutes.coerceIn(5, 180)) }

    val pauseBadge = remember(tracker.pausedUntilDate, vacationState, today) {
        VacationPauseHelper.formatTrackerPauseBadge(tracker.pausedUntilDate, vacationState, today)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 24.dp)
                .testTag("unified_habit_settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Current Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(primaryColor.copy(alpha = 0.16f))
                        .border(0.75.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = editIcon.ifBlank { tracker.icon }, fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = editTitle.ifBlank { tracker.title },
                        fontFamily = appFont,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val remLabel = if (editReminderEnabled) "🔔 $editReminderTime" else "🔕 No alert"
                    val freqLabel = when (editFrequency) {
                        "weekdays" -> "Mon–Fri"
                        "custom_days" -> "${editCustomDays.size}d/wk"
                        "weekly_quota" -> "${editWeeklyTarget}x/wk"
                        else -> "Daily"
                    }
                    Text(
                        text = "${editType.replaceFirstChar { it.uppercase() }} · $freqLabel · $remLabel",
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Actions Strip (Pause / Archive / Delete — replaces extra modal step)
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (tracker.pausedUntilDate != null) primaryColor.copy(alpha = 0.16f) else wellColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        0.75.dp,
                        if (tracker.pausedUntilDate != null) accentTextColor else borderColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = 1 }
                        .testTag("habit_settings_quick_pause")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Text(
                            text = if (tracker.pausedUntilDate != null) "🌴 Paused" else "🌴 Pause",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tracker.pausedUntilDate != null) accentTextColor else textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = wellColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.75.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onDismiss()
                            onArchive(tracker)
                        }
                        .testTag("habit_settings_quick_archive")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Text(
                            text = "📦 Archive",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = if (canDelete) dangerColor.copy(alpha = 0.12f) else wellColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.75.dp, if (canDelete) dangerColor.copy(alpha = 0.4f) else borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = canDelete) {
                            onDismiss()
                            onDelete(tracker)
                        }
                        .testTag("habit_settings_quick_delete")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Habit",
                            tint = if (canDelete) dangerColor else secondaryTextColor.copy(alpha = 0.45f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canDelete) dangerColor else secondaryTextColor.copy(alpha = 0.45f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Segmented 3-Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .recessedWell(RoundedCornerShape(12.dp), isLight = isLight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(wellColor)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Basics", "Schedule", "Alerts & Focus").forEachIndexed { idx, label ->
                    val isSel = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSel) primaryColor else Color.Transparent)
                            .clickable { selectedTab = idx }
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .testTag("habit_settings_tab_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) onPrimaryColor else secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Tab Body
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> TrackerBasicsFields(
                        title = editTitle,
                        onTitleChange = { editTitle = it },
                        icon = editIcon,
                        onIconChange = { editIcon = it },
                        type = editType,
                        onTypeChange = { editType = it },
                        targetCount = editTargetCount,
                        onTargetCountChange = { editTargetCount = it },
                        appFont = appFont,
                        primaryColor = primaryColor,
                        onPrimaryColor = onPrimaryColor,
                        accentTextColor = accentTextColor,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        borderColor = borderColor,
                        wellColor = wellColor,
                        isLight = isLight
                    )

                    1 -> {
                        TrackerScheduleFields(
                            timeOfDay = editTimeOfDay,
                            onTimeOfDayChange = { editTimeOfDay = it },
                            frequency = editFrequency,
                            onFrequencyChange = { editFrequency = it },
                            weeklyTarget = editWeeklyTarget,
                            onWeeklyTargetChange = { editWeeklyTarget = it },
                            customDays = editCustomDays,
                            onCustomDaysChange = { editCustomDays = it },
                            appFont = appFont,
                            primaryColor = primaryColor,
                            onPrimaryColor = onPrimaryColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            borderColor = borderColor,
                            wellColor = wellColor,
                            isLight = isLight
                        )

                        // Inline Scheduled Pause & Rest Card
                        Surface(
                            color = wellColor,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.75.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🌴 Scheduled Pause & Rest",
                                    fontFamily = appFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = pauseBadge ?: "Pause temporarily without breaking streaks or Habit Strength.",
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    color = if (pauseBadge != null) accentTextColor else secondaryTextColor
                                )

                                val pauseOptions = listOf(
                                    "Resume" to null,
                                    "Today Only" to today.toString(),
                                    "3 Days" to today.plusDays(2).toString(),
                                    "7 Days" to today.plusDays(6).toString()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    pauseOptions.forEachIndexed { idx, (label, untilDate) ->
                                        val isCurrent = tracker.pausedUntilDate == untilDate
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isCurrent) primaryColor.copy(alpha = 0.18f) else cardBgColor)
                                                .border(
                                                    width = if (isCurrent) 1.dp else 0.5.dp,
                                                    color = if (isCurrent) accentTextColor else borderColor,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .testTag("habit_settings_pause_$idx")
                                                .clickable { onPauseUntil(tracker.id, untilDate) }
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontFamily = appFont,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) accentTextColor else textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        TrackerFocusDurationSection(
                            focusMinutes = editFocusMinutes,
                            onFocusMinutesChange = { editFocusMinutes = it },
                            appFont = appFont,
                            primaryColor = primaryColor,
                            accentTextColor = accentTextColor,
                            secondaryTextColor = secondaryTextColor,
                            borderColor = borderColor,
                            wellColor = wellColor
                        )

                        PerHabitReminderCard(
                            enabled = editReminderEnabled,
                            time = editReminderTime,
                            trackerType = editType,
                            frequencyType = editFrequency,
                            customDays = editCustomDays,
                            weeklyTarget = editWeeklyTarget,
                            onEnabledChange = { editReminderEnabled = it },
                            onTimeChange = { editReminderTime = it },
                            appFont = appFont,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            borderColor = borderColor,
                            cardBgColor = cardBgColor,
                            selectedTheme = selectedTheme
                        )
                    }
                }
            }

            // Sticky Action Bar
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                ) {
                    Text(
                        fontFamily = appFont,
                        text = "Cancel",
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = {
                        if (editTitle.isNotBlank() && editIcon.isNotBlank()) {
                            val canonicalDays = HabitViewModel.normalizeTargetDays(editCustomDays.joinToString(","))
                            val targetDaysString = when (editFrequency) {
                                "weekdays" -> "MON,TUE,WED,THU,FRI"
                                "custom_days" -> canonicalDays
                                else -> "MON,TUE,WED,THU,FRI,SAT,SUN"
                            }
                            val finalWeeklyTarget = if (editFrequency == "weekly_quota") editWeeklyTarget.coerceIn(1, 7) else 0
                            viewModel.updateTracker(
                                id = tracker.id,
                                title = editTitle,
                                icon = editIcon,
                                accentColor = tracker.accentColor,
                                type = editType,
                                frequencyType = editFrequency,
                                targetDays = targetDaysString,
                                targetCount = editTargetCount.coerceAtLeast(1),
                                timeOfDay = editTimeOfDay,
                                weeklyTarget = finalWeeklyTarget,
                                reminderEnabled = editReminderEnabled,
                                reminderTime = editReminderTime,
                                defaultFocusMinutes = editFocusMinutes
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = onPrimaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("edit_tracker_save_button")
                ) {
                    Text(
                        fontFamily = appFont,
                        text = "Save Changes",
                        color = onPrimaryColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
