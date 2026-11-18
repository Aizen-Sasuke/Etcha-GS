package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.VacationPauseHelper
import com.example.ui.components.recessedWell
import com.example.ui.components.surface3DCard
import com.example.worker.AutoBackupScheduler
import com.example.worker.HabitQuickSettingsTileService
import com.example.worker.ReminderManager
import java.io.File
import java.util.Locale

@Composable
fun PreferencesScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    isTwoPane: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
    val ambientMotionEnabled by viewModel.ambientMotionEnabled.collectAsState()

    val palette = remember(selectedTheme, customHex, customBgHex) {
        ThemeStyles.resolvePalette(selectedTheme, customHex, customBgHex)
    }
    val isGlass = palette.isGlass
    val bgColor = palette.background
    val cardBgColor = palette.cardSurface
    val modalBgColor = palette.modalSurface
    val borderColor = palette.border
    val cardStroke = ThemeStyles.getGlassBorderStroke(selectedTheme, customBgHex)
    val primaryColor = palette.primary
    val onPrimaryColor = palette.onPrimary
    val accentTextColor = palette.accentOnSurface
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)

    val isLight = !palette.isDark
    val textColor = palette.textPrimary
    val secondaryTextColor = palette.textSecondary
    val neutralBg = palette.neutralWell
    val neutralStroke = borderColor.copy(alpha = 0.55f)

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
    var showVacationDialog by remember { mutableStateOf(false) }
    var trackerToDeletePermanently by remember { mutableStateOf<com.example.ui.TrackerConfig?>(null) }
    val jsonBackupController = rememberJsonBackupController(
        viewModel = viewModel,
        onStatus = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    )
    val archivedTrackers by viewModel.archivedTrackers.collectAsState()
    val activeTrackers by viewModel.trackers.collectAsState()
    val selectedTrackerId by viewModel.selectedTrackerId.collectAsState()
    val customReminderCount = remember(activeTrackers) { activeTrackers.count { it.reminderEnabled } }
    val lastBackupTime by viewModel.lastCloudBackupTime.collectAsState()
    val vacationState by viewModel.vacationState.collectAsState()
    val autoBackupCadence by viewModel.autoBackupCadence.collectAsState()
    val quickTileTrackerId by viewModel.quickTileTrackerId.collectAsState()
    val effectiveToday = remember(dayRolloverHour) { ReminderManager.getEffectiveToday(dayRolloverHour) }

    val shareCsvExport: (String?) -> Unit = { filterTrackerId ->
        viewModel.prepareCsvExport(filterTrackerId) { csvContent, filename ->
            try {
                val outFile = File(context.cacheDir, filename)
                outFile.writeText(csvContent)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                val shareIntent = ShareCompat.IntentBuilder(context)
                    .setType("text/csv")
                    .setStream(uri)
                    .setSubject(filename)
                    .setChooserTitle("Export Habit CSV")
                    .createChooserIntent()
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                clipboardManager.setText(AnnotatedString(csvContent))
                Toast.makeText(context, "CSV copied to clipboard ($filename).", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isGlass) Color.Transparent else bgColor)
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = if (isTwoPane) 760.dp else 640.dp)
                .align(Alignment.TopCenter)
                .testTag("preferences_lazy_column"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "pref_header") {
                Text(
                    text = "Preferences",
                    fontFamily = appFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item(key = "pref_appearance") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                            .clickable { showFontPicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font", fontFamily = appFont, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
                        Text(currentFontName, fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Ambient Background Motion",
                                fontFamily = appFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Text(
                                text = "Animate live shader backdrops on glass themes (off keeps scrolling 120Hz crisp)",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                        Switch(
                            checked = ambientMotionEnabled,
                            onCheckedChange = { viewModel.setAmbientMotionEnabled(it) },
                            modifier = Modifier.testTag("pref_ambient_motion_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = onPrimaryColor,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = secondaryTextColor,
                                uncheckedTrackColor = neutralBg
                            )
                        )
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
                                isLightSurface = isLight,
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
                                isLightSurface = isLight,
                                onColorChanged = { viewModel.setCustomBackgroundColorHex(it) }
                            )
                        }
                    }
                }
            }
            }
            
            item(key = "pref_calendar_visuals") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.setCalendarCellShape(sKey) }
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
                                        .border(if (isSelected) 2.dp else 1.5.dp, if (isSelected) primaryColor else neutralStroke, shapeObj)
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.setCheckmarkStyle(cKey) }
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
                                        appFont = appFont,
                                        accentTextColor = accentTextColor
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

            item(key = "pref_behavior") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                                color = accentTextColor,
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
                                        color = if (isSelected) onPrimaryColor else secondaryTextColor,
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
                                checkedThumbColor = onPrimaryColor,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = secondaryTextColor,
                                uncheckedTrackColor = neutralBg
                            )
                        )
                    }
                }
            }
            }

            item(key = "pref_reminders") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                                checkedThumbColor = onPrimaryColor,
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
                                    color = accentTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = appFont
                                )
                            }
                        }
                    }
                }
            }
            }

            // Local Snapshots & Backup Card
            item(key = "pref_backup_csv") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = onPrimaryColor),
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

                    HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)

                    Text(
                        text = "Spreadsheet CSV Export",
                        color = textColor,
                        fontSize = 13.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shareCsvExport(null) },
                            shape = DesignTokens.ControlShape,
                            border = BorderStroke(0.8.dp, borderColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .testTag("pref_export_csv_all")
                        ) {
                            Text(
                                "📊 CSV (All Habits)",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { shareCsvExport(selectedTrackerId.takeIf { it.isNotBlank() }) },
                            shape = DesignTokens.ControlShape,
                            border = BorderStroke(0.8.dp, borderColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .testTag("pref_export_csv_active")
                        ) {
                            Text(
                                "📋 CSV (Active Habit)",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)

                    Text(
                        text = "Automatic Periodic Backups",
                        color = textColor,
                        fontSize = 13.sp,
                        fontFamily = appFont,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AutoBackupScheduler.CADENCE_OFF to "Off",
                            AutoBackupScheduler.CADENCE_DAILY to "Daily",
                            AutoBackupScheduler.CADENCE_WEEKLY to "Weekly"
                        ).forEach { (cadenceKey, cadenceLabel) ->
                            val isSel = autoBackupCadence == cadenceKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) primaryColor.copy(alpha = 0.2f) else neutralBg)
                                    .border(
                                        width = if (isSel) 1.dp else 0.5.dp,
                                        color = if (isSel) primaryColor else neutralStroke,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.setAutoBackupCadence(cadenceKey)
                                        Toast.makeText(context, "Automatic backups: $cadenceLabel", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("pref_autobackup_$cadenceKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cadenceLabel,
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
            }

            // Vacation & Rest Mode Card
            item(key = "pref_vacation") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
                    .testTag("pref_vacation_mode_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable { showVacationDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🌴", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Vacation & Rest Mode",
                                color = textColor,
                                fontSize = 15.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Status: ${VacationPauseHelper.formatVacationSummary(vacationState, effectiveToday)} · Protects streaks & pauses alerts",
                                color = secondaryTextColor,
                                fontFamily = appFont,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Configure Vacation Mode",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            }

            // Android Quick Settings Shade Tile Card
            item(key = "pref_quick_tile") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .surface3DCard(
                        shape = RoundedCornerShape(16.dp),
                        isLight = isLight,
                        primaryColor = primaryColor,
                        isGlass = isGlass,
                        elevationDp = 6.dp
                    )
                    .testTag("pref_quick_tile_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "⚡", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Quick Settings Shade Tile",
                                color = textColor,
                                fontSize = 15.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1-tap habit check-in directly from your Android Quick Settings shade",
                                color = secondaryTextColor,
                                fontFamily = appFont,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val autoSelected = quickTileTrackerId.isNullOrBlank()
                        Box(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (autoSelected) primaryColor.copy(alpha = 0.2f) else neutralBg)
                                .border(if (autoSelected) 1.dp else 0.5.dp, if (autoSelected) primaryColor else neutralStroke, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setQuickTileTrackerId(null) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Auto (Selected)",
                                fontFamily = appFont,
                                fontSize = 11.5.sp,
                                fontWeight = if (autoSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (autoSelected) accentTextColor else secondaryTextColor
                            )
                        }

                        activeTrackers.filter { it.type != "bad" }.forEach { tr ->
                            val isSel = quickTileTrackerId == tr.id
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) primaryColor.copy(alpha = 0.2f) else neutralBg)
                                    .border(if (isSel) 1.dp else 0.5.dp, if (isSel) primaryColor else neutralStroke, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setQuickTileTrackerId(tr.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${tr.icon} ${tr.title}",
                                    fontFamily = appFont,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) accentTextColor else textColor
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val chosen = activeTrackers.find { it.id == quickTileTrackerId }
                                ?: activeTrackers.find { it.id == selectedTrackerId }
                            val label = chosen?.let { "${it.icon} ${it.title}" } ?: "Habit Check-In"
                            HabitQuickSettingsTileService.requestPinTile(context, label) { msg ->
                                (context as? Activity)?.runOnUiThread {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                } ?: Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor.copy(alpha = 0.16f),
                            contentColor = accentTextColor
                        ),
                        border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.4f)),
                        shape = DesignTokens.ControlShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pref_pin_quick_tile_button")
                    ) {
                        Text(
                            text = "⚡ Add Tile to Quick Settings",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }

            // Archived Habits Card
            item(key = "pref_archived") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                            contentColor = accentTextColor
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
            }

            // Supporter Edition & Bonus Vault Card
            item(key = "pref_supporter") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, if (isSupporter) primaryColor.copy(alpha = 0.5f) else borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                            contentColor = if (isSupporter) accentTextColor else onPrimaryColor
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
            }

            // Interactive Guide & Tutorial Card
            item(key = "pref_guide_demo") {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardStroke,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = onPrimaryColor),
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

        GlobalVacationDialog(
            visible = showVacationDialog,
            today = effectiveToday,
            currentVacation = vacationState,
            onDismiss = { showVacationDialog = false },
            onApplyVacation = { enabled, start, end ->
                viewModel.setGlobalVacation(enabled, start, end)
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
    isLightSurface: Boolean = !ThemeStyles.isLightColor(textColor),
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
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp).recessedWell(RoundedCornerShape(8.dp), isLight = isLightSurface).clip(RoundedCornerShape(8.dp)).pointerInput(Unit) {
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

