package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import androidx.core.graphics.toColorInt
import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.components.recessedWell
import com.example.worker.ReminderManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

    var newTitle by remember { mutableStateOf("") }
    var newIcon by remember { mutableStateOf("🎯") }
    var newAccentColor by remember { mutableStateOf<String?>(null) }
    var newTargetCount by remember { mutableIntStateOf(1) }
    var newWeeklyTarget by remember { mutableIntStateOf(3) }
    var newCustomDays by remember { mutableStateOf(setOf("MON", "WED", "FRI")) }
    var newType by remember { mutableStateOf("good") }
    var newTimeOfDay by remember { mutableStateOf("anytime") }
    var newFrequency by remember { mutableStateOf("daily") }
    var newReminderEnabled by remember { mutableStateOf(false) }
    var newReminderTime by remember { mutableStateOf("21:00") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor,
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
                placeholder = { Text(fontFamily = appFont, text = "e.g. 🎯", color = secondaryTextColor.copy(alpha = 0.5f)) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .recessedWell(RoundedCornerShape(12.dp), isLight = ThemeStyles.isLightTheme(selectedTheme))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
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

            // Routine / Time of Day (balanced 2x2 grid for dynamic dimensions)
            Text(fontFamily = appFont, text = "Routine (Time of Day)", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            val routineTypes = listOf(
                "anytime" to "Anytime",
                "morning" to "🌅 Morning",
                "afternoon" to "☀️ Afternoon",
                "evening" to "🌙 Evening"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .recessedWell(RoundedCornerShape(12.dp), isLight = ThemeStyles.isLightTheme(selectedTheme))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                routineTypes.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowItems.forEach { (rKey, rLabel) ->
                            val isSelected = newTimeOfDay == rKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { newTimeOfDay = rKey }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    fontFamily = appFont,
                                    text = rLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
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

            // Frequency Schedule (balanced 2x2 grid for dynamic dimensions)
            Text(fontFamily = appFont, text = "Target Frequency", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            val freqOptions = listOf(
                "daily" to "Daily",
                "weekdays" to "Weekdays (M-F)",
                "custom_days" to "Custom Days",
                "weekly_quota" to "Weekly Target"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .recessedWell(RoundedCornerShape(12.dp), isLight = ThemeStyles.isLightTheme(selectedTheme))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                freqOptions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowItems.forEach { (fKey, fLabel) ->
                            val isSelected = newFrequency == fKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { newFrequency = fKey }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    fontFamily = appFont,
                                    text = fLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
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

            // Frequency Mode Controls
            if (newFrequency == "weekly_quota") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(fontFamily = appFont, text = "Weekly Target Days", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(fontFamily = appFont, text = "Complete on $newWeeklyTarget distinct days per week", color = secondaryTextColor, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (newWeeklyTarget > 1) newWeeklyTarget-- },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                        ) {
                            Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                        Text(text = "$newWeeklyTarget d/wk", fontFamily = appFont, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                        IconButton(
                            onClick = { if (newWeeklyTarget < 7) newWeeklyTarget++ },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                        ) {
                            Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            } else if (newFrequency == "custom_days") {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(fontFamily = appFont, text = "Select Active Days", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val dayKeys = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        dayKeys.forEach { dKey ->
                            val isDaySelected = dKey in newCustomDays
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDaySelected) primaryColor else borderColor.copy(alpha = 0.25f))
                                    .clickable {
                                        newCustomDays = if (isDaySelected) {
                                            if (newCustomDays.size > 1) newCustomDays - dKey else newCustomDays
                                        } else {
                                            newCustomDays + dKey
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dKey.take(1),
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaySelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor
                                )
                            }
                        }
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
                                if (colorHex == null) (if (ThemeStyles.isLightTheme(selectedTheme)) borderColor else Color.DarkGray) else Color(colorHex.toColorInt())
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
            
            // Per-Habit Reminder Control (Disabled by default for new habits)
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
                    Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newIcon.isNotBlank()) {
                            val targetDaysString = when (newFrequency) {
                                "weekdays" -> "MON,TUE,WED,THU,FRI"
                                "custom_days" -> newCustomDays.joinToString(",")
                                else -> "MON,TUE,WED,THU,FRI,SAT,SUN"
                            }
                            val finalWeeklyTarget = if (newFrequency == "weekly_quota") newWeeklyTarget else 0
                            viewModel.addTracker(
                                title = newTitle, 
                                icon = newIcon, 
                                accentColor = newAccentColor, 
                                type = newType,
                                frequencyType = newFrequency,
                                targetDays = targetDaysString,
                                targetCount = newTargetCount,
                                timeOfDay = newTimeOfDay,
                                weeklyTarget = finalWeeklyTarget,
                                reminderEnabled = newReminderEnabled,
                                reminderTime = newReminderTime
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                ) {
                    Text(fontFamily = appFont, 
                        text = "Create",
                        color = if (primaryColor == Color.White) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTrackerSheet(
    tracker: TrackerConfig?,
    onDismiss: () -> Unit,
    onEdit: (TrackerConfig) -> Unit,
    onArchive: (TrackerConfig) -> Unit,
    onDelete: (String) -> Unit,
    canDelete: Boolean,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color
) {
    if (tracker == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = "Manage ${tracker.icon} ${tracker.title}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = appFont
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (tracker.reminderEnabled) primaryColor.copy(alpha = 0.14f)
                        else secondaryTextColor.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (tracker.reminderEnabled) {
                        "🔔 Reminder On · ${tracker.reminderTime}"
                    } else {
                        "🔕 Reminder Off"
                    },
                    fontFamily = appFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tracker.reminderEnabled) {
                        if (primaryColor == Color.White) textColor else primaryColor
                    } else {
                        secondaryTextColor
                    }
                )
            }
            
            Button(
                onClick = { onEdit(tracker) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth().testTag("manage_edit_tracker_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = if (primaryColor == Color.White) Color.Black else Color.White)
                    Text(fontFamily = appFont, text = "Edit Settings & Reminder", color = if (primaryColor == Color.White) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Primary Safe Retirement: Archive Habit
            Button(
                onClick = { onArchive(tracker) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor.copy(alpha = 0.16f),
                    contentColor = if (primaryColor == Color.White) textColor else primaryColor
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📦", fontSize = 16.sp)
                    Text(fontFamily = appFont, text = "Archive habit", color = if (primaryColor == Color.White) textColor else primaryColor, fontWeight = FontWeight.Bold)
                }
            }
            
            Button(
                onClick = {
                    if (canDelete) {
                        onDelete(tracker.id)
                    }
                },
                enabled = canDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.12f),
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
                        text = if (canDelete) "Delete Permanently" else "Delete Permanently (Must have at least one)", 
                        color = if (canDelete) Color.Red else secondaryTextColor.copy(alpha = 0.5f), 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTrackerSheet(
    tracker: TrackerConfig?,
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
    if (tracker == null) return

    var editTitle by remember(tracker) { mutableStateOf(tracker.title) }
    var editIcon by remember(tracker) { mutableStateOf(tracker.icon) }
    var editAccentColor by remember(tracker) { mutableStateOf(tracker.accentColor) }
    var editType by remember(tracker) { mutableStateOf(tracker.type) }
    var editTimeOfDay by remember(tracker) { mutableStateOf(tracker.timeOfDay) }
    var editFrequency by remember(tracker) { mutableStateOf(if (tracker.frequencyType == "3x_week") "weekly_quota" else tracker.frequencyType) }
    var editTargetCount by remember(tracker) { mutableIntStateOf(tracker.targetCount) }
    var editWeeklyTarget by remember(tracker) { 
        mutableIntStateOf(if (tracker.weeklyTarget in 1..7) tracker.weeklyTarget else 3) 
    }
    var editCustomDays by remember(tracker) {
        val parts = tracker.targetDays.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
        mutableStateOf(if (parts.isNotEmpty()) parts else setOf("MON", "WED", "FRI"))
    }
    var editReminderEnabled by remember(tracker) { mutableStateOf(tracker.reminderEnabled) }
    var editReminderTime by remember(tracker) { mutableStateOf(tracker.reminderTime) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor,
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
                placeholder = { Text(fontFamily = appFont, text = "e.g. 🎯", color = secondaryTextColor.copy(alpha = 0.5f)) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .recessedWell(RoundedCornerShape(12.dp), isLight = ThemeStyles.isLightTheme(selectedTheme))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414)),
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

            // Routine / Time of Day (balanced 2x2 grid for dynamic dimensions)
            Text(fontFamily = appFont, text = "Routine (Time of Day)", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            val routineTypes = listOf(
                "anytime" to "Anytime",
                "morning" to "🌅 Morning",
                "afternoon" to "☀️ Afternoon",
                "evening" to "🌙 Evening"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .recessedWell(RoundedCornerShape(12.dp), isLight = ThemeStyles.isLightTheme(selectedTheme))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                routineTypes.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowItems.forEach { (rKey, rLabel) ->
                            val isSelected = editTimeOfDay == rKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { editTimeOfDay = rKey }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    fontFamily = appFont,
                                    text = rLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
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

            // Frequency Schedule (balanced 2x2 grid for dynamic dimensions)
            Text(fontFamily = appFont, text = "Target Frequency", color = secondaryTextColor, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            val freqOptions = listOf(
                "daily" to "Daily",
                "weekdays" to "Weekdays (M-F)",
                "custom_days" to "Custom Days",
                "weekly_quota" to "Weekly Target"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .recessedWell(RoundedCornerShape(12.dp), isLight = ThemeStyles.isLightTheme(selectedTheme))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF141414))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                freqOptions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowItems.forEach { (fKey, fLabel) ->
                            val isSelected = editFrequency == fKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { editFrequency = fKey }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    fontFamily = appFont,
                                    text = fLabel,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
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

            // Frequency Mode Controls
            if (editFrequency == "weekly_quota") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(fontFamily = appFont, text = "Weekly Target Days", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(fontFamily = appFont, text = "Complete on $editWeeklyTarget distinct days per week", color = secondaryTextColor, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (editWeeklyTarget > 1) editWeeklyTarget-- },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                        ) {
                            Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                        Text(text = "$editWeeklyTarget d/wk", fontFamily = appFont, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                        IconButton(
                            onClick = { if (editWeeklyTarget < 7) editWeeklyTarget++ },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                        ) {
                            Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            } else if (editFrequency == "custom_days" || editFrequency == "weekly") {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(fontFamily = appFont, text = "Select Active Days", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val dayKeys = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        dayKeys.forEach { dKey ->
                            val isDaySelected = dKey in editCustomDays
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDaySelected) primaryColor else borderColor.copy(alpha = 0.25f))
                                    .clickable {
                                        editCustomDays = if (isDaySelected) {
                                            if (editCustomDays.size > 1) editCustomDays - dKey else editCustomDays
                                        } else {
                                            editCustomDays + dKey
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dKey.take(1),
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDaySelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor
                                )
                            }
                        }
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
                                if (colorHex == null) (if (ThemeStyles.isLightTheme(selectedTheme)) borderColor else Color.DarkGray) else Color(colorHex.toColorInt())
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
            
            // Per-Habit Reminder Control
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

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                ) {
                    Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = {
                        if (editTitle.isNotBlank() && editIcon.isNotBlank()) {
                            val targetDaysString = when (editFrequency) {
                                "weekdays" -> "MON,TUE,WED,THU,FRI"
                                "custom_days", "weekly" -> editCustomDays.joinToString(",")
                                else -> "MON,TUE,WED,THU,FRI,SAT,SUN"
                            }
                            val finalWeeklyTarget = if (editFrequency == "weekly_quota") editWeeklyTarget else 0
                            viewModel.updateTracker(
                                id = tracker.id,
                                title = editTitle,
                                icon = editIcon,
                                accentColor = editAccentColor,
                                type = editType,
                                frequencyType = editFrequency,
                                targetDays = targetDaysString,
                                targetCount = editTargetCount,
                                timeOfDay = editTimeOfDay,
                                weeklyTarget = finalWeeklyTarget,
                                reminderEnabled = editReminderEnabled,
                                reminderTime = editReminderTime
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("edit_tracker_save_button")
                ) {
                    Text(fontFamily = appFont, 
                        text = "Save Changes",
                        color = if (primaryColor == Color.White) Color.Black else Color.White,
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

    val isLight = ThemeStyles.isLightTheme(selectedTheme)
    val neutralBg = if (isLight) borderColor.copy(alpha = 0.16f) else Color(0xFF161616)
    val dayOrder = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val dayLabels = mapOf(
        "MON" to "Mon", "TUE" to "Tue", "WED" to "Wed",
        "THU" to "Thu", "FRI" to "Fri", "SAT" to "Sat", "SUN" to "Sun"
    )
    val scheduleSummary = when (frequencyType) {
        "weekdays" -> "Mon – Fri"
        "custom_days", "weekly" -> {
            val ordered = dayOrder.filter { it in customDays }.mapNotNull { dayLabels[it] }
            if (ordered.isEmpty()) "Selected days" else ordered.joinToString(", ")
        }
        "weekly_quota", "3x_week" -> "$weeklyTarget days/wk (pauses once goal met)"
        else -> "Every day"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) primaryColor.copy(alpha = 0.08f) else neutralBg
        ),
        border = BorderStroke(
            width = if (enabled) 1.dp else 0.5.dp,
            color = if (enabled) primaryColor.copy(alpha = 0.45f) else borderColor
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
                        tint = if (enabled) (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor,
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
                        checkedThumbColor = if (isLight) Color.White else Color.Black,
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
                    color = Color(0xFFE57373)
                )
            }

            if (enabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBgColor.copy(alpha = 0.85f))
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
                        color = if (primaryColor == Color.White) textColor else primaryColor
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
                                color = if (primaryColor == Color.White) textColor else primaryColor
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

                // Quick time presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("08:00", "13:00", "18:00", "21:00").forEach { preset ->
                        val isPreset = String.format(Locale.US, "%02d:%02d", hourIn, minIn) == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPreset) primaryColor.copy(alpha = 0.2f) else borderColor.copy(alpha = 0.2f))
                                .border(0.5.dp, if (isPreset) primaryColor else borderColor, RoundedCornerShape(8.dp))
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
                                color = if (isPreset) (if (primaryColor == Color.White) textColor else primaryColor) else secondaryTextColor
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
                            contentColor = if (isLight && primaryColor != Color.White) Color.White else if (primaryColor == Color.White) Color.Black else Color.White
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
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(
                    fontFamily = appFont,
                    text = "Archive habit",
                    color = if (primaryColor == Color.White) Color.Black else Color.White,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor,
        titleContentColor = textColor,
        textContentColor = secondaryTextColor,
        title = {
            Text(fontFamily = appFont, text = "Permanently Delete ${tracker.icon} ${tracker.title}?", fontWeight = FontWeight.Bold, color = Color.Red)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(fontFamily = appFont, text = "Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor,
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
                                            contentColor = if (primaryColor == Color.White) textColor else primaryColor
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
                
                // Yearly navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayedYearMonth = displayedYearMonth.minusYears(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Year", tint = secondaryTextColor)
                    }
                    Text(fontFamily = appFont, 
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
                        val isSelected = displayedYearMonth.monthValue == monthValue
                        Button(
                            onClick = {
                                val selectedYM = YearMonth.of(displayedYearMonth.year, monthValue)
                                onYearMonthSelected(selectedYM)
                                onDismiss()
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
            
            Text(
                text = formattedDate,
                fontFamily = appFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )

            // Large number for total count with label "times logged"
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
                                            onDeleteEntry(entry)
                                            if (dayEntries.size <= 1) {
                                                onDismiss()
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
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onOpenJournal(selectedDate)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
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
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
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
                        contentColor = if (primaryColor == Color.White) Color.Black else Color.White
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
