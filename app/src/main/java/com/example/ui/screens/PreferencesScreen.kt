package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.worker.ReminderManager
import androidx.core.app.ShareCompat
import android.content.Intent
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.components.MotionSpecs
import com.example.ui.components.StaggeredReveal
import com.example.ui.components.recessedWell
import com.example.ui.components.springPressScale
import com.example.ui.components.surface3DCard
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.util.Locale

@Composable
fun PreferencesScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setRemindersEnabled(true)
            ReminderManager.scheduleReminder(context, viewModel.reminderTime.value)
        }
    }

    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val calendarCellShapeName by viewModel.calendarCellShape.collectAsState()
    val checkmarkStyleName by viewModel.checkmarkStyle.collectAsState()
    
    val isGlass = ThemeStyles.isGlassTheme(selectedTheme)
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val modalBgColor = ThemeStyles.getOpaqueCardColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme, customBgHex)
    val cardStroke = ThemeStyles.getGlassBorderStroke(selectedTheme, customBgHex)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)

    val isLight = ThemeStyles.isLightTheme(selectedTheme, customBgHex)
    val textColor = ThemeStyles.getTextColor(selectedTheme, customBgHex)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme, customBgHex)
    val neutralBg = borderColor.copy(alpha = if (isLight) 0.25f else 0.28f)
    val neutralStroke = borderColor.copy(alpha = 0.55f)
    val innerDarkText = if (ThemeStyles.isLightColor(primaryColor)) Color.Black else Color.White

    val reminderTime by viewModel.reminderTime.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val dayRolloverHour by viewModel.dayRolloverHour.collectAsState()
    val celebrationsEnabled by viewModel.celebrationsEnabled.collectAsState()
    val isSupporter by viewModel.isSupporter.collectAsState()
    val isDemoTrackerLoaded by viewModel.isDemoTrackerLoaded.collectAsState()
    
    var showTimeDialog by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showCloudBackupModal by remember { mutableStateOf(false) }
    var showArchivedHabitsSheet by remember { mutableStateOf(false) }
    var trackerToDeletePermanently by remember { mutableStateOf<com.example.ui.TrackerConfig?>(null) }
    val jsonBackupController = rememberJsonBackupController(
        viewModel = viewModel,
        onStatus = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    )
    val archivedTrackers by viewModel.archivedTrackers.collectAsState()
    val activeTrackers by viewModel.trackers.collectAsState()
    val customReminderCount = remember(activeTrackers) { activeTrackers.count { it.reminderEnabled } }
    val lastBackupTime by viewModel.lastCloudBackupTime.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isGlass) Color.Transparent else bgColor)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Preferences",
                fontFamily = appFont,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = textColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            StaggeredReveal(index = 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column {
                    val currentThemeName = com.example.ui.ThemeData.ALL_THEMES.find { it.id == selectedTheme }?.name ?: "Custom"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springPressScale(0.98f)
                            .clickable { showThemePicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(primaryColor))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Theme Studio", fontFamily = appFont, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
                        Text(currentThemeName, fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)

                    val currentFontName = ThemeStyles.getFontDisplayName(selectedFont)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .springPressScale(0.98f)
                            .clickable { showFontPicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font", fontFamily = appFont, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
                        Text(currentFontName, fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                    }
                    if (selectedTheme == "custom") {
                        HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                            Text("Accent Color", fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            CustomColorPicker(
                                appFont = appFont,
                                initialHex = customHex,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                borderColor = borderColor,
                                onColorChanged = { viewModel.setCustomPrimaryColorHex(it) }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Background Color", fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            CustomColorPicker(
                                appFont = appFont,
                                initialHex = customBgHex,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                borderColor = borderColor,
                                onColorChanged = { viewModel.setCustomBackgroundColorHex(it) }
                            )
                        }
                    }
                }
            }
            }
            
            StaggeredReveal(index = 1) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calendar Visuals",
                        color = textColor,
                        fontSize = 15.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text("Cell Shape", color = secondaryTextColor, fontSize = 12.sp, fontFamily = appFont, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val shapeOptions = listOf(
                            "rounded_square" to "Rounded",
                            "square"         to "Square",
                            "circle"         to "Circle",
                            "squircle"       to "Squircle",
                            "pill"           to "Pill",
                            "diamond"        to "Diamond"
                        )
                        shapeOptions.forEach { (sKey, sLabel) ->
                            val isSelected = calendarCellShapeName == sKey
                            val shapeObj = ThemeStyles.getCalendarCellShape(sKey)
                            val itemScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.06f else 1f,
                                animationSpec = MotionSpecs.BouncyPopSpring,
                                label = "shape_scale_$sKey"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .scale(itemScale)
                                    .springPressScale(0.92f)
                                    .clickable { viewModel.setCalendarCellShape(sKey) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier.surface3DCard(
                                                    shape = shapeObj,
                                                    isLight = isLight,
                                                    primaryColor = primaryColor,
                                                    isGlass = isGlass,
                                                    elevationDp = 4.dp
                                                )
                                            } else {
                                                Modifier.recessedWell(shapeObj, isLight = isLight)
                                            }
                                        )
                                        .clip(shapeObj)
                                        .background(if (isSelected) primaryColor.copy(alpha = 0.14f) else cardBgColor)
                                        .border(if (isSelected) 2.dp else 1.5.dp, primaryColor, shapeObj)
                                )
                                Text(
                                    sLabel,
                                    color = if (isSelected) textColor else secondaryTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = appFont,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }

                    Text("Checkmark Style", color = secondaryTextColor, fontSize = 12.sp, fontFamily = appFont, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val checkOptions = listOf(
                            "tick"   to "Tick",
                            "cross"  to "Cross",
                            "dot"    to "Dot",
                            "number" to "Number",
                            "filled" to "Filled",
                            "ring"   to "Ring",
                            "bar"    to "Bar"
                        )
                        items(checkOptions.size) { index ->
                            val (cKey, cLabel) = checkOptions[index]
                            val isSelected = checkmarkStyleName == cKey
                            val cellShapeObj = ThemeStyles.getCalendarCellShape(calendarCellShapeName)
                            val checkScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.06f else 1f,
                                animationSpec = MotionSpecs.BouncyPopSpring,
                                label = "check_scale_$cKey"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .scale(checkScale)
                                    .springPressScale(0.92f)
                                    .clickable { viewModel.setCheckmarkStyle(cKey) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier.surface3DCard(
                                                    shape = cellShapeObj,
                                                    isLight = isLight,
                                                    primaryColor = primaryColor,
                                                    isGlass = isGlass,
                                                    elevationDp = 4.dp
                                                )
                                            } else {
                                                Modifier.recessedWell(cellShapeObj, isLight = isLight)
                                            }
                                        )
                                        .clip(cellShapeObj)
                                        .background(primaryColor.copy(alpha = if (cKey == "filled") 0.22f else 0.15f))
                                        .completedCellCornerGlow(primaryColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) primaryColor else neutralStroke,
                                            shape = cellShapeObj
                                        )
                                ) {
                                    CalendarCompletionIndicator(
                                        dayNumber = 14,
                                        totalSum = 2,
                                        targetCount = 1,
                                        isCompleted = true,
                                        isPartial = false,
                                        isToday = false,
                                        isCurrentMonth = true,
                                        hasNotes = false,
                                        moodTag = null,
                                        cellShapeKey = calendarCellShapeName,
                                        cellShape = cellShapeObj,
                                        checkmarkStyleName = cKey,
                                        primaryColor = primaryColor,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        appFont = appFont
                                    )
                                }
                                Text(
                                    cLabel,
                                    color = if (isSelected) textColor else secondaryTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = appFont,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
            }

            StaggeredReveal(index = 2) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Behavior & Night Owl Mode",
                        color = textColor,
                        fontSize = 15.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold
                    )

                    // Day Rollover Cutoff
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Day Rollover Cutoff",
                                color = textColor,
                                fontSize = 13.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (dayRolloverHour == 0) "Midnight (12 AM)" else "$dayRolloverHour:00 AM",
                                color = primaryColor,
                                fontSize = 12.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Habits logged before this hour count towards the previous day. Perfect for late-night routines!",
                            color = secondaryTextColor,
                            fontSize = 11.sp,
                            fontFamily = appFont
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                                .recessedWell(RoundedCornerShape(10.dp), isLight = isLight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(neutralBg)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(0 to "12 AM", 1 to "1 AM", 2 to "2 AM", 3 to "3 AM", 4 to "4 AM").forEach { (h, label) ->
                                val isSelected = dayRolloverHour == h
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) primaryColor else Color.Transparent)
                                        .clickable { viewModel.setDayRolloverHour(h) }
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = appFont,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)

                    // Celebrations toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Celebration Effects",
                                color = textColor,
                                fontSize = 13.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Play confetti & haptic feedback on streak milestones and logs",
                                color = secondaryTextColor,
                                fontSize = 11.sp,
                                fontFamily = appFont
                            )
                        }
                        Switch(
                            checked = celebrationsEnabled,
                            onCheckedChange = { viewModel.setCelebrationsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isLight) Color.White else Color.Black,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = secondaryTextColor,
                                uncheckedTrackColor = neutralBg
                            )
                        )
                    }
                }
            }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Daily Reminder Notifications",
                                color = textColor,
                                fontSize = 15.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (customReminderCount > 0) {
                                    "Covers active habits without custom alerts ($customReminderCount custom active)"
                                } else {
                                    "Global fallback alert when habits have no custom reminder"
                                },
                                color = secondaryTextColor,
                                fontFamily = appFont,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { isChecked -> 
                                if (isChecked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.setRemindersEnabled(true)
                                            ReminderManager.scheduleReminder(context, reminderTime)
                                        }
                                    } else {
                                        viewModel.setRemindersEnabled(true)
                                        ReminderManager.scheduleReminder(context, reminderTime)
                                    }
                                } else {
                                    viewModel.setRemindersEnabled(false)
                                    ReminderManager.cancelReminder(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isLight) Color.White else Color.Black,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = secondaryTextColor,
                                uncheckedTrackColor = neutralBg
                            )
                        )
                    }

                    if (remindersEnabled) {
                        HorizontalDivider(color = neutralStroke, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .recessedWell(RoundedCornerShape(8.dp), isLight = isLight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(neutralBg)
                                .clickable { showTimeDialog = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Global Reminder Time:", color = textColor, fontSize = 13.sp, fontFamily = appFont)
                            Text(
                                text = "$reminderTime ⏰",
                                color = textColor,
                                fontSize = 15.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if ((remindersEnabled || customReminderCount > 0) &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !ReminderManager.canScheduleExactAlarms(context)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(neutralBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Using approximate timing. Allow Exact Alarms for precise reminder delivery.",
                                color = secondaryTextColor,
                                fontSize = 11.sp,
                                fontFamily = appFont,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            TextButton(
                                onClick = { ReminderManager.openExactAlarmSettings(context) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = "Settings",
                                    color = if (primaryColor == Color.White) textColor else primaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = appFont
                                )
                            }
                        }
                    }
                }
            }

            // Local Snapshots & Backup Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💾", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = "Local Snapshots & Backup",
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (lastBackupTime != null) "Last snapshot: $lastBackupTime" else "Offline restore points & portable JSON export/import",
                                    color = secondaryTextColor,
                                    fontFamily = appFont,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { jsonBackupController.openExport() },
                            shape = DesignTokens.ControlShape,
                            border = BorderStroke(0.8.dp, borderColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                "📄 Export JSON",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { jsonBackupController.openImportPaste() },
                            shape = DesignTokens.ControlShape,
                            border = BorderStroke(0.8.dp, borderColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                "📂 Import JSON",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = { showCloudBackupModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = innerDarkText),
                        shape = DesignTokens.ControlShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "💾 Manage Snapshots & Restore Points",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Archived Habits Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📦", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = "Archived Habits",
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (archivedTrackers.isNotEmpty()) "${archivedTrackers.size} retired habit(s) · History preserved" else "No habits currently archived",
                                    color = secondaryTextColor,
                                    fontFamily = appFont,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showArchivedHabitsSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor.copy(alpha = 0.16f),
                            contentColor = if (primaryColor == Color.White) textColor else primaryColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "📦 View & Manage Archived Habits (${archivedTrackers.size})",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Supporter Edition & Bonus Vault Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, if (isSupporter) primaryColor.copy(alpha = 0.5f) else borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = if (isSupporter) "💖" else "✨", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = if (isSupporter) "Supporter Edition Active ✨" else "Supporter Edition & Bonus Vault",
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSupporter) "Bonus themes & freeze vault unlocked locally" else "Unlock bonus themes & extra streak freezes (100% offline & free)",
                                    color = secondaryTextColor,
                                    fontFamily = appFont,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showDonateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSupporter) primaryColor.copy(alpha = 0.15f) else primaryColor,
                            contentColor = if (isSupporter) primaryColor else (if (ThemeStyles.isLightColor(primaryColor)) Color.Black else Color.White)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (isSupporter) "💖 View Supporter Vault & Perks" else "✨ Explore Supporter Edition & Vault",
                            fontFamily = appFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Interactive Guide & Tutorial Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOnboardingDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "💡", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "App Tutorial & Guide",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Replay the feature walkthrough and tips",
                                    color = secondaryTextColor,
                                    fontFamily = appFont,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "View Guide",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isDemoTrackerLoaded) {
                                    viewModel.clearDemoTracker()
                                    Toast.makeText(context, "Demo tracker cleared.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.loadDemoTracker()
                                    Toast.makeText(context, "Sample Demo Tracker loaded!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "🌱", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = if (isDemoTrackerLoaded) "Clear Sample Demo Tracker" else "Load Sample Demo Tracker",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDemoTrackerLoaded) "Remove preloaded \"Morning Walk\" sample logs" else "Add \"Morning Walk\" with 45 days of organic logs",
                                    color = secondaryTextColor,
                                    fontFamily = appFont,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Toggle Demo Tracker",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (showTimeDialog) {
            var hourIn by remember { mutableStateOf(reminderTime.substringBefore(":").toIntOrNull() ?: 20) }
            var minIn by remember { mutableStateOf(reminderTime.substringAfter(":").toIntOrNull() ?: 0) }

            Dialog(onDismissRequest = { showTimeDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = modalBgColor),
                    border = BorderStroke(1.dp, borderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Set Reminder Time",
                            fontFamily = appFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Hour (0-23)", fontSize = 10.sp, color = secondaryTextColor, fontFamily = appFont)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { hourIn = (hourIn - 1 + 24) % 24 }) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Dec Hour", tint = secondaryTextColor)
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%02d", hourIn),
                                        fontSize = 28.sp,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = appFont
                                    )
                                    IconButton(onClick = { hourIn = (hourIn + 1) % 24 }) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Inc Hour", tint = secondaryTextColor)
                                    }
                                }
                            }

                            Text(
                                text = ":",
                                color = textColor,
                                fontFamily = appFont,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Minute (0-59)", fontSize = 10.sp, color = secondaryTextColor, fontFamily = appFont)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { minIn = (minIn - 5 + 60) % 60 }) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Dec Min", tint = secondaryTextColor)
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%02d", minIn),
                                        fontSize = 28.sp,
                                        color = textColor,
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { minIn = (minIn + 5) % 60 }) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Inc Min", tint = secondaryTextColor)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showTimeDialog = false },
                                modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                            ) {
                                Text(text = "Cancel", color = secondaryTextColor, fontFamily = appFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    val formatted = String.format(Locale.US, "%02d:%02d", hourIn, minIn)
                                    viewModel.setReminderTime(formatted)
                                    if (remindersEnabled) {
                                        ReminderManager.scheduleReminder(context, formatted)
                                    }
                                    Toast.makeText(context, "Reminder time updated to $formatted!", Toast.LENGTH_SHORT).show()
                                    showTimeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = innerDarkText),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                            ) {
                                Text(text = "Save", fontWeight = FontWeight.Bold, fontFamily = appFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        if (showThemePicker) {
            ThemePickerModal(
                viewModel = viewModel,
                onClose = { showThemePicker = false },
                onOpenDonate = { showDonateDialog = true }
            )
        }
        if (showFontPicker) {
            FontPickerModal(
                viewModel = viewModel,
                onClose = { showFontPicker = false }
            )
        }
        if (showDonateDialog) {
            DonateModal(
                viewModel = viewModel,
                onDismiss = { showDonateDialog = false }
            )
        }
        if (showOnboardingDialog) {
            OnboardingModal(
                viewModel = viewModel,
                onDismiss = { showOnboardingDialog = false }
            )
        }
        if (showCloudBackupModal) {
            CloudBackupModal(
                viewModel = viewModel,
                onClose = { showCloudBackupModal = false },
                onOpenDonate = { showDonateDialog = true }
            )
        }

        JsonBackupDialogsHost(
            controller = jsonBackupController,
            selectedTheme = selectedTheme,
            primaryColor = primaryColor,
            cardBgColor = modalBgColor,
            borderColor = borderColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            appFont = appFont,
            onStatus = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        )

        ArchivedHabitsSheet(
            visible = showArchivedHabitsSheet,
            onDismiss = { showArchivedHabitsSheet = false },
            archivedTrackers = archivedTrackers,
            onUnarchive = { id -> viewModel.unarchiveTracker(id) },
            onDeletePermanently = { id ->
                trackerToDeletePermanently = archivedTrackers.find { it.id == id }
            },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = modalBgColor,
            borderColor = borderColor
        )

        DeleteTrackerDialog(
            tracker = trackerToDeletePermanently,
            onDismiss = { trackerToDeletePermanently = null },
            onConfirm = { id ->
                viewModel.deleteTracker(id)
            },
            appFont = appFont,
            primaryColor = primaryColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            cardBgColor = modalBgColor,
            borderColor = borderColor
        )
    }
}

@Composable
fun CustomColorPicker(
    appFont: androidx.compose.ui.text.font.FontFamily,
    initialHex: String?,
    textColor: Color = Color.White,
    secondaryTextColor: Color = Color.Gray,
    borderColor: Color = Color.DarkGray,
    onColorChanged: (String) -> Unit
) {
    var hue by remember { mutableStateOf(260f) }
    var value by remember { mutableStateOf(1f) }
    var hexInput by remember { mutableStateOf(initialHex ?: "#A855F7") }

    LaunchedEffect(initialHex) {
        if (!initialHex.isNullOrBlank()) {
            try {
                val color = initialHex.toColorInt()
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(color, hsv)
                hue = hsv[0]
                value = hsv[2]
                hexInput = initialHex
            } catch (_: Exception) {}
        }
    }

    val currentColor = Color.hsv(hue, 1f, value)

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        var canvasWidth by remember { mutableStateOf(1f) }
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp).recessedWell(RoundedCornerShape(8.dp), isLight = ThemeStyles.isLightColor(textColor).not()).clip(RoundedCornerShape(8.dp)).pointerInput(Unit) {
            detectTapGestures { offset ->
                val x = offset.x.coerceIn(0f, canvasWidth)
                hue = (x / canvasWidth) * 360f
                val hex = String.format(Locale.US, "#%06X", (0xFFFFFF and Color.hsv(hue, 1f, value).toArgb()))
                hexInput = hex
                onColorChanged(hex)
            }
        }.pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val x = change.position.x.coerceIn(0f, canvasWidth)
                hue = (x / canvasWidth) * 360f
                val hex = String.format(Locale.US, "#%06X", (0xFFFFFF and Color.hsv(hue, 1f, value).toArgb()))
                hexInput = hex
                onColorChanged(hex)
            }
        }) {
            canvasWidth = size.width
            val step = size.width / 360f
            for (i in 0..360) {
                drawRect(
                    color = Color.hsv(i.toFloat(), 1f, 1f),
                    topLeft = androidx.compose.ui.geometry.Offset(i * step, 0f),
                    size = androidx.compose.ui.geometry.Size(step * 2, size.height)
                )
            }
            val selectorX = (hue / 360f) * size.width
            drawLine(
                color = if (ThemeStyles.isLightColor(Color.hsv(hue, 1f, 1f))) Color.Black else Color.White,
                start = androidx.compose.ui.geometry.Offset(selectorX, 0f),
                end = androidx.compose.ui.geometry.Offset(selectorX, size.height),
                strokeWidth = 6f
            )
        }

        Spacer(Modifier.height(14.dp))

        Text("Brightness", color = secondaryTextColor, fontSize = 12.sp, fontFamily = appFont)
        Slider(
            value = value,
            onValueChange = {
                value = it
                val hex = String.format(Locale.US, "#%06X", (0xFFFFFF and Color.hsv(hue, 1f, value).toArgb()))
                hexInput = hex
                onColorChanged(hex)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = currentColor,
                inactiveTrackColor = borderColor.copy(alpha = 0.4f)
            )
        )

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, borderColor, CircleShape)
            )
            OutlinedTextField(
                value = hexInput,
                onValueChange = {
                    hexInput = it
                    if (it.length == 7 && it.startsWith("#")) {
                        try {
                            val c = it.uppercase().toColorInt()
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(c, hsv)
                            hue = hsv[0]
                            value = hsv[2]
                            onColorChanged(it.uppercase())
                        } catch (_: Exception) {}
                    }
                },
                label = { Text("Hex Color", fontFamily = appFont, fontSize = 11.sp, color = secondaryTextColor) },
                textStyle = TextStyle(color = textColor, fontFamily = appFont),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentColor,
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = currentColor
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

