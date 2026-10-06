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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.worker.ReminderManager
import androidx.core.app.ShareCompat
import android.content.Intent
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

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
    
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)

    val isLight = ThemeStyles.isLightTheme(selectedTheme)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)
    val neutralBg = if (isLight) Color(0xFFE2E2E2) else Color(0xFF222222)
    val neutralStroke = if (isLight) Color(0xFFCCCCCC) else Color(0xFF333333)
    val innerDarkText = if (isLight && selectedTheme == "revolutionary_cream") Color.White else if (isLight) Color.White else Color.Black

    val reminderTime by viewModel.reminderTime.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val dayRolloverHour by viewModel.dayRolloverHour.collectAsState()
    val celebrationsEnabled by viewModel.celebrationsEnabled.collectAsState()
    val isSupporter by viewModel.isSupporter.collectAsState()
    
    var showTimeDialog by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showCloudBackupModal by remember { mutableStateOf(false) }
    val googleEmail by viewModel.googleAccountEmail.collectAsState()
    val lastBackupTime by viewModel.lastCloudBackupTime.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
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

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    val currentThemeName = com.example.ui.ThemeData.ALL_THEMES.find { it.id == selectedTheme }?.name ?: "Custom"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemePicker = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(primaryColor))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Theme", fontFamily = appFont, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
                        Text(currentThemeName, fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)

                    val currentFontName = listOf(
                        "cursive"        to "Cursive",
                        "serif"          to "Serif",
                        "monospace"      to "Monospace",
                        "sans_serif"     to "Sans Serif",
                        "default"        to "Default",
                        "jetbrains_mono" to "JetBrains Mono",
                        "space_grotesk"  to "Space Grotesk",
                        "comfortaa"      to "Comfortaa",
                        "nunito"         to "Nunito"
                    ).find { it.first == selectedFont }?.second ?: "Default"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFontPicker = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font", fontFamily = appFont, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
                        Text(currentFontName, fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                    }
                    if (selectedTheme == "custom") {
                        HorizontalDivider(color = neutralStroke, thickness = 0.5.dp)
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Text("Accent Color", fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            CustomColorPicker(
                                appFont = appFont,
                                initialHex = customHex,
                                onColorChanged = { viewModel.setCustomPrimaryColorHex(it) },
                                fontFamily = appFont
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Background Color", fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            CustomColorPicker(
                                appFont = appFont,
                                initialHex = customBgHex,
                                onColorChanged = { viewModel.setCustomBackgroundColorHex(it) },
                                fontFamily = appFont
                            )
                        }
                    }
                }
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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
                            "pill"           to "Pill"
                        )
                        shapeOptions.forEach { (sKey, sLabel) ->
                            val isSelected = calendarCellShapeName == sKey
                            val shapeObj = ThemeStyles.getCalendarCellShape(sKey)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.setCalendarCellShape(sKey) }) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(shapeObj)
                                        .background(if (isSelected) primaryColor.copy(alpha = 0.12f) else cardBgColor)
                                        .border(if (isSelected) 2.dp else 1.5.dp, primaryColor, shapeObj)
                                )
                                Text(sLabel, color = if(isSelected) textColor else secondaryTextColor, fontSize = 10.sp, fontFamily = appFont, modifier = Modifier.padding(top=6.dp))
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.setCheckmarkStyle(cKey) }) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(cellShapeObj)
                                        .background(cardBgColor)
                                        .then(
                                            if (cKey == "filled") {
                                                val fillAlpha = (0.13f + (2 - 1) * 0.04f).coerceAtMost(0.28f)
                                                Modifier.background(primaryColor.copy(alpha = fillAlpha))
                                            } else Modifier
                                        )
                                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) primaryColor else neutralStroke, cellShapeObj)
                                ) {
                                    val dotSize = 8.dp
                                    val dotAlpha = 0.75f
                                    when (cKey) {
                                        "tick" -> Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = primaryColor.copy(alpha = dotAlpha),
                                            modifier = Modifier
                                                .size(dotSize + 4.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-3).dp, y = 3.dp)
                                        )
                                        "cross" -> Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = primaryColor.copy(alpha = dotAlpha),
                                            modifier = Modifier
                                                .size(dotSize + 4.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-3).dp, y = 3.dp)
                                        )
                                        "dot" -> Box(
                                            modifier = Modifier
                                                .size(dotSize)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-4).dp, y = 4.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = dotAlpha))
                                        )
                                        "number" -> Text(
                                            text = "2",
                                            color = primaryColor,
                                            fontSize = 9.sp,
                                            fontFamily = appFont,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-3).dp, y = 3.dp)
                                        )
                                        "ring" -> Box(
                                            modifier = Modifier
                                                .size(dotSize)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-4).dp, y = 4.dp)
                                                .border(1.5.dp, primaryColor.copy(alpha = dotAlpha), CircleShape)
                                        )
                                        "bar" -> Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp)
                                                .height(2.5.dp)
                                                .fillMaxWidth(0.45f)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(primaryColor.copy(alpha = dotAlpha))
                                        )
                                    }
                                }
                                Text(cLabel, color = if(isSelected) textColor else secondaryTextColor, fontSize = 10.sp, fontFamily = appFont, modifier = Modifier.padding(top=6.dp))
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(neutralBg),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(0 to "12 AM", 1 to "1 AM", 2 to "2 AM", 3 to "3 AM", 4 to "4 AM").forEach { (h, label) ->
                                val isSelected = dayRolloverHour == h
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) primaryColor else Color.Transparent)
                                        .clickable { viewModel.setDayRolloverHour(h) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = appFont,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor
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

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Reminder Notifications",
                                color = textColor,
                                fontSize = 15.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Schedule notifications to avoid breaking streaks",
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(neutralBg)
                                .clickable { showTimeDialog = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Scheduled Reminder Time:", color = textColor, fontSize = 13.sp, fontFamily = appFont)
                            Text(
                                text = "$reminderTime ⏰",
                                color = textColor,
                                fontSize = 15.sp,
                                fontFamily = appFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Local Snapshots & Backup Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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

                    Button(
                        onClick = { showCloudBackupModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = innerDarkText),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💾 Manage Snapshots & JSON Backups",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Support the Developer / Tip Jar Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, if (isSupporter) primaryColor.copy(alpha = 0.5f) else borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = if (isSupporter) "💖" else "☕", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = if (isSupporter) "Generous Supporter ✨" else "Support the App",
                                    color = textColor,
                                    fontSize = 15.sp,
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSupporter) "Thank you for supporting offline development!" else "Buy a coffee & keep this app 100% free & ad-free",
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
                            contentColor = if (isSupporter) primaryColor else (if (primaryColor == Color.White) Color.Black else Color.White)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSupporter) "💖 Supporter Active · View Tip Jar" else "☕ Buy Me a Coffee (Tip Jar)",
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
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
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
            }
        }

        if (showTimeDialog) {
            var hourIn by remember { mutableStateOf(reminderTime.substringBefore(":").toIntOrNull() ?: 20) }
            var minIn by remember { mutableStateOf(reminderTime.substringAfter(":").toIntOrNull() ?: 0) }

            Dialog(onDismissRequest = { showTimeDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
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
                                        text = String.format("%02d", hourIn),
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
                                        text = String.format("%02d", minIn),
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = { showTimeDialog = false }) {
                                Text(text = "Cancel", color = secondaryTextColor, fontFamily = appFont)
                            }
                            Button(
                                onClick = {
                                    val formatted = String.format("%02d:%02d", hourIn, minIn)
                                    viewModel.setReminderTime(formatted)
                                    if (remindersEnabled) {
                                        ReminderManager.scheduleReminder(context, formatted)
                                    }
                                            Toast.makeText(context, "Reminder time updated to $formatted!", Toast.LENGTH_SHORT).show()
                                    showTimeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = innerDarkText),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Save", fontWeight = FontWeight.Bold, fontFamily = appFont)
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
    }
}

@Composable
fun CustomColorPicker(
    appFont: androidx.compose.ui.text.font.FontFamily,
    initialHex: String?,
    onColorChanged: (String) -> Unit,
    fontFamily: FontFamily
) {
    var hue by remember { mutableStateOf(0f) }
    var value by remember { mutableStateOf(1f) }
    var hexInput by remember { mutableStateOf("") }
    
    LaunchedEffect(initialHex) {
        if (initialHex != null) {
            try {
                val color = android.graphics.Color.parseColor(initialHex)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(color, hsv)
                hue = hsv[0]
                value = hsv[2]
                hexInput = initialHex
            } catch (e: Exception) {}
        } else {
            hue = 260f
            hexInput = "#A855F7"
            onColorChanged(hexInput)
        }
    }
    
    val currentColor = Color.hsv(hue, 1f, value)
    
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        var canvasWidth by remember { mutableStateOf(1f) }
        Canvas(modifier = Modifier.fillMaxWidth().height(30.dp).pointerInput(Unit) {
            detectTapGestures { offset ->
                val x = offset.x.coerceIn(0f, canvasWidth)
                hue = (x / canvasWidth) * 360f
                val hex = String.format("#%06X", (0xFFFFFF and Color.hsv(hue, 1f, value).toArgb()))
                hexInput = hex
                onColorChanged(hex)
            }
        }.pointerInput(Unit) {
            detectDragGestures { change, _ -> 
                val x = change.position.x.coerceIn(0f, canvasWidth)
                hue = (x / canvasWidth) * 360f
                val hex = String.format("#%06X", (0xFFFFFF and Color.hsv(hue, 1f, value).toArgb()))
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
            drawLine(Color.White, androidx.compose.ui.geometry.Offset(selectorX, 0f), androidx.compose.ui.geometry.Offset(selectorX, size.height), strokeWidth = 6f)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("Brightness", color = Color.Gray, fontSize = 12.sp, fontFamily = appFont)
        Slider(
            value = value,
            onValueChange = { 
                value = it 
                val hex = String.format("#%06X", (0xFFFFFF and Color.hsv(hue, 1f, value).toArgb()))
                hexInput = hex
                onColorChanged(hex)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = currentColor)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(currentColor).border(2.dp, Color.White, CircleShape))
            OutlinedTextField(
                value = hexInput,
                onValueChange = {
                    hexInput = it
                    if (it.length == 7 && it.startsWith("#")) {
                        try {
                            val c = android.graphics.Color.parseColor(it.uppercase())
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(c, hsv)
                            hue = hsv[0]
                            value = hsv[2]
                            onColorChanged(it.uppercase())
                        } catch (e: Exception) {}
                    }
                },
                textStyle = TextStyle(color = Color.White, fontFamily = appFont),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentColor,
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

