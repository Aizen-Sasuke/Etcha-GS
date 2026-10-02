package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyJournalModal(
    viewModel: HabitViewModel,
    initialDate: LocalDate? = null,
    onDismiss: () -> Unit
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val allEntries by viewModel.entries.collectAsState(initial = emptyList())
    val activeTracker by viewModel.activeTracker.collectAsState()

    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)
    val isLight = ThemeStyles.isLightTheme(selectedTheme)

    val targetDate = initialDate ?: viewModel.getEffectiveToday()
    val targetDateString = targetDate.toString()

    val existingEntryForDate = remember(allEntries, targetDateString) {
        allEntries.find { it.dateString == targetDateString && it.trackerId == activeTracker.id }
    }

    var hasUserEdited by remember(targetDateString, activeTracker.id) { mutableStateOf(false) }

    var selectedMood by remember(targetDateString, activeTracker.id) {
        val existingNotes = existingEntryForDate?.notes ?: ""
        val match = Regex("\\[MOOD:([^\\]]+)\\]").find(existingNotes)
        mutableStateOf(match?.groupValues?.get(1) ?: "😊")
    }

    var journalText by remember(targetDateString, activeTracker.id) {
        val existingNotes = existingEntryForDate?.notes ?: ""
        val cleanNotes = existingNotes.replace(Regex("\\[MOOD:[^\\]]+\\]"), "").trim()
        mutableStateOf(cleanNotes)
    }

    LaunchedEffect(existingEntryForDate) {
        if (!hasUserEdited && existingEntryForDate != null) {
            val existingNotes = existingEntryForDate.notes ?: ""
            val match = Regex("\\[MOOD:([^\\]]+)\\]").find(existingNotes)
            if (match != null) {
                selectedMood = match.groupValues[1]
            }
            journalText = existingNotes.replace(Regex("\\[MOOD:[^\\]]+\\]"), "").trim()
        }
    }

    var activeTab by remember { mutableStateOf("write") } // "write" or "timeline"

    val promptList = listOf(
        "What went well today?",
        "One small victory I'm proud of",
        "An obstacle I overcame",
        "What gave me energy today?"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .testTag("daily_journal_modal")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "✍️", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "Daily Journal & Notes",
                                    fontFamily = appFont,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = targetDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)),
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = secondaryTextColor)
                        }
                    }

                    // Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLight) Color(0xFFEEEEEE) else Color(0xFF181818))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("write" to "✍️ Today's Entry", "timeline" to "📖 Past Reflections").forEach { (tabKey, tabLabel) ->
                            val isSelected = activeTab == tabKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                                    .clickable { activeTab = tabKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabLabel,
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (primaryColor == Color.White) Color.Black else Color.White) else secondaryTextColor
                                )
                            }
                        }
                    }

                    if (activeTab == "write") {
                        // Write Entry Form
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Mood Selector
                            Text(
                                text = "Daily Mood & Energy",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryTextColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    "😊" to "Proud",
                                    "⚡" to "Energized",
                                    "🧘" to "Calm",
                                    "🌧️" to "Struggled",
                                    "🎯" to "Focused"
                                ).forEach { (emoji, label) ->
                                    val isMoodSelected = selectedMood == emoji
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { 
                                            selectedMood = emoji 
                                            hasUserEdited = true
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(if (isMoodSelected) primaryColor.copy(alpha = 0.2f) else (if (isLight) Color(0xFFEEEEEE) else Color(0xFF1B1B1B)))
                                                .border(
                                                    width = if (isMoodSelected) 2.dp else 0.5.dp,
                                                    color = if (isMoodSelected) primaryColor else borderColor,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                        Text(
                                            text = label,
                                            fontFamily = appFont,
                                            fontSize = 10.sp,
                                            color = if (isMoodSelected) primaryColor else secondaryTextColor,
                                            fontWeight = if (isMoodSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Inspiration Prompts
                            Text(
                                text = "Reflection Prompts",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryTextColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                promptList.take(2).forEach { prompt ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(primaryColor.copy(alpha = 0.08f))
                                            .border(0.5.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                hasUserEdited = true
                                                if (!journalText.contains(prompt)) {
                                                    journalText = if (journalText.isBlank()) "$prompt\n" else "$journalText\n\n$prompt\n"
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "💡 $prompt",
                                            fontFamily = appFont,
                                            fontSize = 10.sp,
                                            color = primaryColor,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            // Multi-line Text Editor
                            OutlinedTextField(
                                value = journalText,
                                onValueChange = { 
                                    journalText = it 
                                    hasUserEdited = true
                                },
                                placeholder = {
                                    Text(
                                        text = "Write your thoughts, reflections, or notes for today...",
                                        fontFamily = appFont,
                                        fontSize = 13.sp,
                                        color = secondaryTextColor.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = if (isLight) Color.White else Color(0xFF141414),
                                    unfocusedContainerColor = if (isLight) Color.White else Color(0xFF141414),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                )
                            )
                        }

                        // Save Button
                        Button(
                            onClick = {
                                viewModel.saveDailyJournal(targetDateString, journalText, selectedMood)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💾 Save Reflection",
                                fontFamily = appFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (primaryColor == Color.White) Color.Black else Color.White
                            )
                        }
                    } else {
                        // Past Reflections Timeline
                        val journalEntries = remember(allEntries) {
                            allEntries.filter { !it.notes.isNullOrBlank() }
                        }

                        if (journalEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "📖", fontSize = 36.sp)
                                    Text(
                                        text = "No journal notes logged yet.",
                                        fontFamily = appFont,
                                        fontSize = 13.sp,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(journalEntries) { entry ->
                                    val moodMatch = Regex("\\[MOOD:([^\\]]+)\\]").find(entry.notes ?: "")
                                    val mood = moodMatch?.groupValues?.get(1) ?: "📝"
                                    val cleanContent = (entry.notes ?: "").replace(Regex("\\[MOOD:[^\\]]+\\]"), "").trim()

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isLight) Color(0xFFF7F7F7) else Color(0xFF171717)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(text = mood, fontSize = 16.sp)
                                                    Text(
                                                        text = entry.dateString,
                                                        fontFamily = appFont,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor
                                                    )
                                                }
                                                Text(
                                                    text = "Habit: ${entry.trackerId}",
                                                    fontFamily = appFont,
                                                    fontSize = 10.sp,
                                                    color = secondaryTextColor
                                                )
                                            }
                                            if (cleanContent.isNotBlank()) {
                                                Text(
                                                    text = cleanContent,
                                                    fontFamily = appFont,
                                                    fontSize = 12.sp,
                                                    color = textColor,
                                                    lineHeight = 16.sp
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
        }
    }
}
