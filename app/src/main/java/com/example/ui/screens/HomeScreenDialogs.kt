package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.worker.ReminderManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrackerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: HabitViewModel,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    selectedTheme: String
) {
    if (!visible) return

    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val accentTextColor = ThemeStyles.accentOnSurface(primaryColor, cardBgColor, fallbackInk = textColor)
    val isLight = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val wellColor = ThemeStyles.getNeutralWellColor(selectedTheme)

    var newTitle by remember { mutableStateOf("") }
    var newIcon by remember { mutableStateOf("🎯") }
    var newTargetCount by remember { mutableIntStateOf(1) }
    var newWeeklyTarget by remember { mutableIntStateOf(3) }
    var newCustomDays by remember { mutableStateOf(setOf("MON", "WED", "FRI")) }
    var newType by remember { mutableStateOf("good") }
    var newTimeOfDay by remember { mutableStateOf("anytime") }
    var newFrequency by remember { mutableStateOf("daily") }
    var newReminderEnabled by remember { mutableStateOf(false) }
    var newReminderTime by remember { mutableStateOf("21:00") }
    var newFocusMinutes by remember { mutableIntStateOf(25) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

            TrackerBasicsFields(
                title = newTitle,
                onTitleChange = { newTitle = it },
                icon = newIcon,
                onIconChange = { newIcon = it },
                type = newType,
                onTypeChange = { newType = it },
                targetCount = newTargetCount,
                onTargetCountChange = { newTargetCount = it },
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

            TrackerScheduleFields(
                timeOfDay = newTimeOfDay,
                onTimeOfDayChange = { newTimeOfDay = it },
                frequency = newFrequency,
                onFrequencyChange = { newFrequency = it },
                weeklyTarget = newWeeklyTarget,
                onWeeklyTargetChange = { newWeeklyTarget = it },
                customDays = newCustomDays,
                onCustomDaysChange = { newCustomDays = it },
                appFont = appFont,
                primaryColor = primaryColor,
                onPrimaryColor = onPrimaryColor,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                borderColor = borderColor,
                wellColor = wellColor,
                isLight = isLight
            )

            TrackerFocusDurationSection(
                focusMinutes = newFocusMinutes,
                onFocusMinutesChange = { newFocusMinutes = it },
                appFont = appFont,
                primaryColor = primaryColor,
                accentTextColor = accentTextColor,
                secondaryTextColor = secondaryTextColor,
                borderColor = borderColor,
                wellColor = wellColor
            )

            PerHabitReminderCard(
                enabled = newReminderEnabled,
                time = newReminderTime,
                trackerType = newType,
                frequencyType = newFrequency,
                customDays = newCustomDays,
                weeklyTarget = newWeeklyTarget,
                onEnabledChange = { newReminderEnabled = it },
                onTimeChange = { newReminderTime = it },
                appFont = appFont,
                primaryColor = primaryColor,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                borderColor = borderColor,
                cardBgColor = cardBgColor,
                selectedTheme = selectedTheme
            )

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
                        if (newTitle.isNotBlank() && newIcon.isNotBlank()) {
                            val canonicalDays = HabitViewModel.normalizeTargetDays(newCustomDays.joinToString(","))
                            val targetDaysString = when (newFrequency) {
                                "weekdays" -> "MON,TUE,WED,THU,FRI"
                                "custom_days" -> canonicalDays
                                else -> "MON,TUE,WED,THU,FRI,SAT,SUN"
                            }
                            val finalWeeklyTarget = if (newFrequency == "weekly_quota") newWeeklyTarget else 0
                            viewModel.addTracker(
                                title = newTitle,
                                icon = newIcon,
                                accentColor = null,
                                type = newType,
                                frequencyType = newFrequency,
                                targetDays = targetDaysString,
                                targetCount = newTargetCount,
                                timeOfDay = newTimeOfDay,
                                weeklyTarget = finalWeeklyTarget,
                                reminderEnabled = newReminderEnabled,
                                reminderTime = newReminderTime,
                                defaultFocusMinutes = newFocusMinutes
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = onPrimaryColor
                    ),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                ) {
                    Text(
                        fontFamily = appFont,
                        text = "Create",
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

@Composable
fun PerHabitReminderCard(
    enabled: Boolean,
    time: String,
    trackerType: String,
    frequencyType: String,
    customDays: Set<String>,
    weeklyTarget: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    cardBgColor: Color,
    selectedTheme: String
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var permissionDeniedHint by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedHint = false
            onEnabledChange(true)
        } else {
            permissionDeniedHint = true
            onEnabledChange(false)
        }
    }

    val isLight = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val wellColor = ThemeStyles.getNeutralWellColor(selectedTheme)
    val accentTextColor = ThemeStyles.accentOnSurface(primaryColor, cardBgColor, fallbackInk = textColor)
    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val dangerColor = ThemeStyles.semanticDangerColor(isLight)

    val dayOrder = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val dayLabels = mapOf(
        "MON" to "Mon", "TUE" to "Tue", "WED" to "Wed",
        "THU" to "Thu", "FRI" to "Fri", "SAT" to "Sat", "SUN" to "Sun"
    )
    val scheduleSummary = when (HabitViewModel.normalizeFrequencyType(frequencyType)) {
        "weekdays" -> "Mon – Fri"
        "custom_days" -> {
            val ordered = dayOrder.filter { it in customDays }.mapNotNull { dayLabels[it] }
            if (ordered.isEmpty()) "Selected days" else ordered.joinToString(", ")
        }
        "weekly_quota" -> "$weeklyTarget days/wk (pauses once goal met)"
        else -> "Every day"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) primaryColor.copy(alpha = 0.10f) else wellColor
        ),
        border = BorderStroke(
            width = if (enabled) 1.dp else 0.5.dp,
            color = if (enabled) accentTextColor.copy(alpha = 0.5f) else borderColor
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Habit reminder",
                        tint = if (enabled) accentTextColor else secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Habit Reminder",
                            fontFamily = appFont,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = when {
                                !enabled -> "Off · Uses global reminder if enabled"
                                trackerType == "bad" -> "Mindful check-in · $scheduleSummary at $time"
                                else -> "$scheduleSummary at $time"
                            },
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    modifier = Modifier.testTag("habit_reminder_switch"),
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    permissionDeniedHint = false
                                    onEnabledChange(true)
                                }
                            } else {
                                permissionDeniedHint = false
                                onEnabledChange(true)
                            }
                        } else {
                            permissionDeniedHint = false
                            onEnabledChange(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onPrimaryColor,
                        checkedTrackColor = primaryColor,
                        uncheckedThumbColor = secondaryTextColor,
                        uncheckedTrackColor = borderColor.copy(alpha = 0.25f)
                    )
                )
            }

            if (permissionDeniedHint) {
                Text(
                    text = "Notification permission was denied. Enable notifications for Etcha-GS in system settings to receive reminders.",
                    fontFamily = appFont,
                    fontSize = 11.sp,
                    color = dangerColor
                )
            }

            if (enabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBgColor.copy(alpha = 0.9f))
                        .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { showTimePicker = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("habit_reminder_time_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reminder Time",
                        fontFamily = appFont,
                        fontSize = 13.sp,
                        color = textColor
                    )
                    Text(
                        text = "$time ⏰",
                        fontFamily = appFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentTextColor
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !ReminderManager.canScheduleExactAlarms(context)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(borderColor.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exact alarm permission is off; Android may deliver alerts within a few minutes.",
                            fontFamily = appFont,
                            fontSize = 10.5.sp,
                            color = secondaryTextColor,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        TextButton(
                            onClick = { ReminderManager.openExactAlarmSettings(context) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("habit_exact_alarm_settings_button")
                        ) {
                            Text(
                                text = "Allow",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentTextColor
                            )
                        }
                    }
                }

                Text(
                    text = "Tip: If battery optimization is aggressive on your device, exempt Etcha-GS so reminders fire on time.",
                    fontFamily = appFont,
                    fontSize = 10.5.sp,
                    color = secondaryTextColor
                )
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialTime = time,
            onDismiss = { showTimePicker = false },
            onConfirm = { newTime ->
                onTimeChange(newTime)
                showTimePicker = false
            },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            isLight = isLight
        )
    }
}

@Composable
fun ReminderTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    isLight: Boolean
) {
    val parsed = ReminderManager.parseTime(initialTime) ?: (21 to 0)
    var hourIn by remember(initialTime) { mutableIntStateOf(parsed.first) }
    var minIn by remember(initialTime) { mutableIntStateOf(parsed.second) }
    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val accentTextColor = ThemeStyles.accentOnSurface(primaryColor, cardBgColor, fallbackInk = textColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = cardBgColor,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Set Reminder Time",
                    color = textColor,
                    fontSize = 17.sp,
                    fontFamily = appFont,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Hour (0-23)", fontSize = 10.sp, color = secondaryTextColor, fontFamily = appFont)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { hourIn = (hourIn - 1 + 24) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Decrease Hour", tint = secondaryTextColor)
                            }
                            Text(
                                text = String.format(Locale.US, "%02d", hourIn),
                                fontSize = 28.sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = appFont
                            )
                            IconButton(onClick = { hourIn = (hourIn + 1) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Increase Hour", tint = secondaryTextColor)
                            }
                        }
                    }

                    Text(
                        text = ":",
                        color = textColor,
                        fontFamily = appFont,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Minute (0-59)", fontSize = 10.sp, color = secondaryTextColor, fontFamily = appFont)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { minIn = (minIn - 5 + 60) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Decrease Minute", tint = secondaryTextColor)
                            }
                            Text(
                                text = String.format(Locale.US, "%02d", minIn),
                                fontSize = 28.sp,
                                color = textColor,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { minIn = (minIn + 5) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Increase Minute", tint = secondaryTextColor)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("08:00", "13:00", "18:00", "21:00").forEach { preset ->
                        val isPreset = String.format(Locale.US, "%02d:%02d", hourIn, minIn) == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPreset) primaryColor.copy(alpha = 0.2f) else borderColor.copy(alpha = 0.18f))
                                .border(0.5.dp, if (isPreset) accentTextColor else borderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    val p = ReminderManager.parseTime(preset)
                                    if (p != null) {
                                        hourIn = p.first
                                        minIn = p.second
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset,
                                fontFamily = appFont,
                                fontSize = 11.5.sp,
                                fontWeight = if (isPreset) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPreset) accentTextColor else secondaryTextColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                    ) {
                        Text(text = "Cancel", color = secondaryTextColor, fontFamily = appFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            onConfirm(String.format(Locale.US, "%02d:%02d", hourIn, minIn))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = onPrimaryColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                    ) {
                        Text(text = "Set Time", fontWeight = FontWeight.Bold, fontFamily = appFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveTrackerDialog(
    tracker: TrackerConfig?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    if (tracker == null) return
    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor,
        titleContentColor = textColor,
        textContentColor = secondaryTextColor,
        title = {
            Text(fontFamily = appFont, text = "Archive ${tracker.icon} ${tracker.title}?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                fontFamily = appFont,
                text = "This habit will leave your active dashboard, routine list, and daily reminders. All past check-ins, journal notes, and streak records will be safely preserved in reports. You can unarchive it at any time."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(tracker.id)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                )
            ) {
                Text(
                    fontFamily = appFont,
                    text = "Archive habit",
                    color = onPrimaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
            ) {
                Text(fontFamily = appFont, text = "Cancel")
            }
        }
    )
}

@Composable
fun DeleteTrackerDialog(
    tracker: TrackerConfig?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    if (tracker == null) return
    val isLight = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val dangerColor = ThemeStyles.semanticDangerColor(isLight)
    val onDangerColor = ThemeStyles.contrastOn(dangerColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor,
        titleContentColor = textColor,
        textContentColor = secondaryTextColor,
        title = {
            Text(fontFamily = appFont, text = "Permanently Delete ${tracker.icon} ${tracker.title}?", fontWeight = FontWeight.Bold, color = dangerColor)
        },
        text = {
            Text(
                fontFamily = appFont,
                text = "Are you sure? This will PERMANENTLY erase this habit and all its past logs, notes, and streak history. This cannot be undone.\n\nTip: To safely remove this habit from active use while keeping your history, use 'Archive habit' instead."
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(tracker.id)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = dangerColor,
                    contentColor = onDangerColor
                )
            ) {
                Text(fontFamily = appFont, text = "Delete Permanently", color = onDangerColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
            ) {
                Text(fontFamily = appFont, text = "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedHabitsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    archivedTrackers: List<TrackerConfig>,
    onUnarchive: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    if (!visible) return
    val accentTextColor = ThemeStyles.accentOnSurface(primaryColor, cardBgColor, fallbackInk = textColor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📦", fontSize = 20.sp)
                    Text(
                        text = "Archived Habits",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = appFont
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(borderColor.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${archivedTrackers.size} retired",
                        fontFamily = appFont,
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (archivedTrackers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "✨", fontSize = 32.sp)
                        Text(
                            text = "No archived habits",
                            fontFamily = appFont,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Archived habits will be safely kept here with all historical records intact.",
                            fontFamily = appFont,
                            color = secondaryTextColor,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    archivedTrackers.forEach { tracker ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = borderColor.copy(alpha = 0.15f)),
                            border = BorderStroke(0.5.dp, borderColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(borderColor.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = tracker.icon, fontSize = 20.sp)
                                    }
                                    Column {
                                        Text(
                                            text = tracker.title,
                                            fontFamily = appFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = textColor
                                        )
                                        val freqDesc = when (tracker.frequencyType) {
                                            "weekly_quota" -> "${tracker.weeklyTarget}x per week quota"
                                            "weekdays" -> "Weekdays"
                                            "custom_days" -> tracker.targetDays
                                            else -> "Daily"
                                        }
                                        Text(
                                            text = "$freqDesc · History preserved",
                                            fontFamily = appFont,
                                            fontSize = 11.sp,
                                            color = secondaryTextColor
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { onUnarchive(tracker.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = primaryColor.copy(alpha = 0.18f),
                                            contentColor = accentTextColor
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Unarchive", modifier = Modifier.size(16.dp))
                                            Text(
                                                fontFamily = appFont,
                                                text = "Unarchive",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeletePermanently(tracker.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete permanently",
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onDismiss) {
                Text(fontFamily = appFont, text = "Close", color = secondaryTextColor)
            }
        }
    }
}

@Composable
fun MonthPickerDialog(
    visible: Boolean,
    currentYearMonth: YearMonth,
    onYearMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    selectedTheme: String
) {
    if (!visible) return

    var displayedYearMonth by remember(currentYearMonth) { mutableStateOf(currentYearMonth) }
    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val wellColor = ThemeStyles.getNeutralWellColor(selectedTheme)

    Dialog(onDismissRequest = onDismiss) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayedYearMonth = displayedYearMonth.minusYears(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Year", tint = secondaryTextColor)
                    }
                    Text(
                        fontFamily = appFont,
                        text = "${displayedYearMonth.year}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    IconButton(onClick = { displayedYearMonth = displayedYearMonth.plusYears(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Year", tint = secondaryTextColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        val isSelected = displayedYearMonth.monthValue == monthValue
                        Button(
                            onClick = {
                                val selectedYM = YearMonth.of(displayedYearMonth.year, monthValue)
                                onYearMonthSelected(selectedYM)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) primaryColor else wellColor,
                                contentColor = if (isSelected) onPrimaryColor else textColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(fontFamily = appFont, text = monthNames[mIndex], fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = "Close", color = secondaryTextColor, fontFamily = appFont)
                }
            }
        }
    }
}

@Composable
fun QuickAddDialog(
    visible: Boolean,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    selectedTheme: String
) {
    if (!visible) return

    var quickNote by remember { mutableStateOf("") }
    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val wellColor = ThemeStyles.getNeutralWellColor(selectedTheme)

    Dialog(onDismissRequest = onDismiss) {
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
                Text(
                    fontFamily = appFont,
                    text = "Log that you avoided or completed your habit on standard schedule: ${today.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    fontSize = 12.sp,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = quickNote,
                    onValueChange = { quickNote = it },
                    placeholder = {
                        Text(
                            fontFamily = appFont,
                            text = "Add a quick note...",
                            fontSize = 13.sp,
                            color = secondaryTextColor.copy(alpha = 0.55f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = wellColor,
                        unfocusedContainerColor = wellColor,
                        disabledContainerColor = wellColor,
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
                    TextButton(onClick = onDismiss) {
                        Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
                    }
                    Button(
                        onClick = {
                            onConfirm(if (quickNote.isEmpty()) null else quickNote)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = onPrimaryColor
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsBottomSheet(
    selectedDate: LocalDate?,
    completionsByDate: Map<String, List<HabitEntry>>,
    activeTracker: TrackerConfig,
    onDismiss: () -> Unit,
    onDeleteEntry: (HabitEntry) -> Unit,
    onClearDate: (String) -> Unit,
    onQuickLog: (String) -> Unit,
    onOpenJournal: (LocalDate) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    selectedTheme: String
) {
    if (selectedDate == null) return

    val formatterStr = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sDateString = selectedDate.format(formatterStr)
    val dayEntries = completionsByDate[sDateString] ?: emptyList()
    val totalCount = dayEntries.sumOf { it.count }

    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val accentTextColor = ThemeStyles.accentOnSurface(primaryColor, cardBgColor, fallbackInk = textColor)
    val isLight = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val wellColor = ThemeStyles.getNeutralWellColor(selectedTheme)
    val dangerColor = ThemeStyles.semanticDangerColor(isLight)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))

            Text(
                text = formattedDate,
                fontFamily = appFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$totalCount",
                    fontFamily = appFont,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = accentTextColor
                )
                Text(
                    text = "times logged",
                    fontFamily = appFont,
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }

            HorizontalDivider(color = borderColor, thickness = 0.5.dp)

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
                    colors = CardDefaults.cardColors(containerColor = wellColor),
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
                                            onDeleteEntry(entry)
                                            if (dayEntries.size <= 1) {
                                                onDismiss()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = dangerColor,
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

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onOpenJournal(selectedDate)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, accentTextColor.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentTextColor),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        fontFamily = appFont,
                        text = "✍️ Journal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (dayEntries.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onClearDate(sDateString)
                            onDismiss()
                        },
                        border = BorderStroke(1.dp, dangerColor.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = dangerColor),
                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            fontFamily = appFont,
                            text = "Clear",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = {
                        onQuickLog(sDateString)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = onPrimaryColor
                    ),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        fontFamily = appFont,
                        text = if (activeTracker.type == "bad") "Log Slip" else "+1 Log",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
