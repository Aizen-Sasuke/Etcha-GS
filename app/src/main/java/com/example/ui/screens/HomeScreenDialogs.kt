package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            val freqOptions = listOf(
                "daily" to "Daily",
                "weekdays" to "Weekdays (M-F)",
                "custom_days" to "Custom Days",
                "weekly_quota" to "Weekly Target"
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
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
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
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
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
                                weeklyTarget = finalWeeklyTarget
                            )
                            onDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTrackerSheet(
    tracker: TrackerConfig?,
    onDismiss: () -> Unit,
    onEdit: (TrackerConfig) -> Unit,
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
            
            Button(
                onClick = { onEdit(tracker) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = if (primaryColor == Color.White) Color.Black else Color.White)
                    Text(fontFamily = appFont, text = "Edit Settings (Type, Color, Info)", color = if (primaryColor == Color.White) Color.Black else Color.White, fontWeight = FontWeight.Bold)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                "custom_days" to "Custom Days",
                "weekly_quota" to "Weekly Target"
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
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
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
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(fontFamily = appFont, text = "Cancel", color = secondaryTextColor)
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
                                weeklyTarget = finalWeeklyTarget
                            )
                            onDismiss()
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
            Text(fontFamily = appFont, text = "Delete ${tracker.title}?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(fontFamily = appFont, text = "Are you sure you want to delete this tracker? All log entries for this habit will be permanently deleted.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(tracker.id)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(fontFamily = appFont, text = "Delete Tracker", color = if (primaryColor == Color.White) Color.Black else Color.White)
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onOpenJournal(selectedDate)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(fontFamily = appFont, text = "✍️ Journal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (dayEntries.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onClearDate(sDateString)
                            onDismiss()
                        },
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(fontFamily = appFont, text = "Clear", fontSize = 12.sp)
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(fontFamily = appFont, text = if (activeTracker.type == "bad") "Log Slip" else "+1 Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
