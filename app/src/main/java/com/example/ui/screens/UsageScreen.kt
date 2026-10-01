package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitEntry
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun UsageScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val allTrackers by viewModel.trackers.collectAsState()
    val activeTracker by viewModel.activeTracker.collectAsState()
    val selectedTrackerId by viewModel.selectedTrackerId.collectAsState()
    
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)
    val basePrimaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val primaryColor = remember(activeTracker.accentColor, basePrimaryColor) {
        if (activeTracker.accentColor != null) {
            try {
                Color(android.graphics.Color.parseColor(activeTracker.accentColor))
            } catch (e: Exception) {
                basePrimaryColor
            }
        } else {
            basePrimaryColor
        }
    }

    val today = remember { LocalDate.now() }
    val clipboardManager = LocalClipboardManager.current
    
    // Filtering states
    var searchQuery by remember { mutableStateOf("") }
    var selectedRangeTab by remember { mutableStateOf("All Time") } // "All Time", "This Month", "Last 30 Days", "This Year"
    var selectedTrackerFilter by remember { mutableStateOf("current") } // "current", "all", or specific tracker ID
    var filterNotesOnly by remember { mutableStateOf(false) }
    var selectedMoodFilter by remember { mutableStateOf<String?>(null) }
    
    var showJournalModal by remember { mutableStateOf(false) }
    var journalTargetDate by remember { mutableStateOf<LocalDate?>(null) }
    var entryToEdit by remember { mutableStateOf<HabitEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<HabitEntry?>(null) }
    var showAddPastLogDialog by remember { mutableStateOf(false) }

    // Filter entries based on all criteria
    val filteredEntries = remember(entries, searchQuery, selectedRangeTab, filterNotesOnly, selectedMoodFilter, today) {
        entries.filter { entry ->
            val parsedDate = try { LocalDate.parse(entry.dateString) } catch (e: Exception) { null }
            
            // 1. Time range filter
            val matchesRange = when (selectedRangeTab) {
                "This Month" -> parsedDate != null && parsedDate.year == today.year && parsedDate.monthValue == today.monthValue
                "Last 30 Days" -> parsedDate != null && !parsedDate.isBefore(today.minusDays(30)) && !parsedDate.isAfter(today)
                "This Year" -> parsedDate != null && parsedDate.year == today.year
                else -> true
            }

            // 2. Search query filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val notes = entry.notes ?: ""
                val dateText = entry.dateString
                notes.contains(searchQuery, ignoreCase = true) || dateText.contains(searchQuery, ignoreCase = true)
            }

            // 3. Notes only filter
            val matchesNotesOnly = if (!filterNotesOnly) true else !entry.notes.isNullOrBlank()

            // 4. Mood filter
            val matchesMood = if (selectedMoodFilter == null) true else {
                val notes = entry.notes ?: ""
                notes.contains("[MOOD:${selectedMoodFilter}]")
            }

            matchesRange && matchesSearch && matchesNotesOnly && matchesMood
        }.sortedByDescending { it.dateString }
    }

    // Aggregate statistics for the filtered range
    val totalLoggedCount = remember(filteredEntries) { filteredEntries.sumOf { it.count } }
    val totalActiveDays = remember(filteredEntries) { filteredEntries.map { it.dateString }.distinct().size }
    val totalNotesCount = remember(filteredEntries) { filteredEntries.count { !it.notes.isNullOrBlank() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Title Header & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "📜",
                        fontSize = 24.sp
                    )
                    Column {
                        Text(
                            text = "History & Logs",
                            fontFamily = appFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = textColor
                        )
                        Text(
                            text = "${filteredEntries.size} logs · ${activeTracker.title}",
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showAddPastLogDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(cardBgColor)
                            .border(0.5.dp, borderColor, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Past Log", tint = primaryColor, modifier = Modifier.size(18.dp))
                    }

                    Button(
                        onClick = { showJournalModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✍️ Journal",
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White
                        )
                    }
                }
            }

            // Habits Filter Carousel
            val trackerScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(trackerScroll)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTrackers.forEach { tracker ->
                    val isSelected = tracker.id == selectedTrackerId
                    val tAccent = tracker.accentColor?.let {
                        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { primaryColor }
                    } ?: primaryColor

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) tAccent.copy(alpha = 0.2f) else cardBgColor)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) tAccent else borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectTracker(tracker.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = tracker.icon, fontSize = 13.sp)
                            Text(
                                text = tracker.title,
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) tAccent else textColor
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs, notes, reflections...", fontFamily = appFont, fontSize = 12.sp, color = secondaryTextColor) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = secondaryTextColor, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBgColor,
                    unfocusedContainerColor = cardBgColor,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = primaryColor
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFont, fontSize = 13.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Date Range & Tag Filter Chips
            val rangeScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rangeScroll)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All Time", "This Month", "Last 30 Days", "This Year").forEach { tab ->
                    val isSelected = selectedRangeTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                            .border(if (isSelected) 1.dp else 0.5.dp, if (isSelected) primaryColor else borderColor, RoundedCornerShape(10.dp))
                            .clickable { selectedRangeTab = tab }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tab,
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primaryColor else secondaryTextColor
                        )
                    }
                }

                // Notes Only Filter Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (filterNotesOnly) primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                        .border(if (filterNotesOnly) 1.dp else 0.5.dp, if (filterNotesOnly) primaryColor else borderColor, RoundedCornerShape(10.dp))
                        .clickable { filterNotesOnly = !filterNotesOnly }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📝 Notes Only",
                        fontFamily = appFont,
                        fontSize = 11.sp,
                        fontWeight = if (filterNotesOnly) FontWeight.Bold else FontWeight.Normal,
                        color = if (filterNotesOnly) primaryColor else secondaryTextColor
                    )
                }

                // Mood Filter Chips
                listOf("😊", "🔥", "⚡", "🧘", "🌧️").forEach { mood ->
                    val isMoodActive = selectedMoodFilter == mood
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMoodActive) primaryColor.copy(alpha = 0.25f) else Color.Transparent)
                            .border(if (isMoodActive) 1.dp else 0.5.dp, if (isMoodActive) primaryColor else borderColor, RoundedCornerShape(10.dp))
                            .clickable {
                                selectedMoodFilter = if (isMoodActive) null else mood
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = mood, fontSize = 11.sp)
                    }
                }
            }

            // Mini Summary Metric Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(0.5.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active Days", fontFamily = appFont, fontSize = 10.sp, color = secondaryTextColor)
                        Text("$totalActiveDays", fontFamily = appFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(borderColor.copy(alpha = 0.5f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Check-ins", fontFamily = appFont, fontSize = 10.sp, color = secondaryTextColor)
                        Text("$totalLoggedCount", fontFamily = appFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(borderColor.copy(alpha = 0.5f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Journals & Notes", fontFamily = appFont, fontSize = 10.sp, color = secondaryTextColor)
                        Text("$totalNotesCount", fontFamily = appFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timeline List
            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("📜", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedMoodFilter != null || filterNotesOnly) "No matching logs found" else "No habit logs yet",
                            fontFamily = appFont,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try adjusting your search terms or filters." else "Log your first check-in from the Home tab or tap '✍️ Journal' to add reflections!",
                            fontFamily = appFont,
                            color = secondaryTextColor,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    items(filteredEntries, key = { "${it.dateString}_${it.trackerId}_${it.id}" }) { entry ->
                        val tracker = allTrackers.find { it.id == entry.trackerId } ?: activeTracker
                        EnhancedTimelineCard(
                            appFont = appFont,
                            entry = entry,
                            habitTitle = tracker.title,
                            habitIcon = tracker.icon,
                            primaryColor = primaryColor,
                            cardBgColor = cardBgColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            onEdit = { entryToEdit = entry },
                            onDelete = { entryToDelete = entry },
                            onOpenJournal = {
                                try {
                                    journalTargetDate = LocalDate.parse(entry.dateString)
                                    showJournalModal = true
                                } catch (e: Exception) {}
                            }
                        )
                    }
                }
            }
        }

        // Daily Journal Modal
        if (showJournalModal) {
            DailyJournalModal(
                viewModel = viewModel,
                initialDate = journalTargetDate ?: today,
                onDismiss = {
                    showJournalModal = false
                    journalTargetDate = null
                }
            )
        }

        // Edit Entry Dialog
        entryToEdit?.let { entry ->
            var editCount by remember(entry) { mutableIntStateOf(entry.count) }
            var editNotes by remember(entry) { mutableStateOf(entry.notes ?: "") }

            AlertDialog(
                onDismissRequest = { entryToEdit = null },
                containerColor = cardBgColor,
                title = {
                    Text("Edit Log (${entry.dateString})", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Check-in Count:", fontFamily = appFont, fontSize = 13.sp, color = textColor)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { if (editCount > 1) editCount-- },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                                ) {
                                    Text("-", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                                }
                                Text("$editCount", fontFamily = appFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                IconButton(
                                    onClick = { editCount++ },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                                ) {
                                    Text("+", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Reflection / Notes", fontFamily = appFont, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFont, fontSize = 12.sp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.insertEntry(
                                HabitEntry(
                                    dateString = entry.dateString,
                                    count = editCount,
                                    timestamp = entry.timestamp,
                                    notes = editNotes.ifBlank { null },
                                    trackerId = entry.trackerId
                                )
                            )
                            entryToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White)
                    ) {
                        Text("Save Changes", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToEdit = null }) {
                        Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        entryToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                containerColor = cardBgColor,
                title = { Text("Delete Log?", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor) },
                text = {
                    Text(
                        "Are you sure you want to delete the log for ${entry.dateString} (${entry.count} counts)?",
                        fontFamily = appFont,
                        fontSize = 13.sp,
                        color = textColor
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteEntry(entry)
                            entryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = Color.White)
                    ) {
                        Text("Delete", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                    }
                }
            )
        }

        // Add Past Log Dialog
        if (showAddPastLogDialog) {
            var selectedDateText by remember { mutableStateOf(today.toString()) }
            var pastCount by remember { mutableIntStateOf(1) }
            var pastNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddPastLogDialog = false },
                containerColor = cardBgColor,
                title = { Text("Log Past Check-in", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Backfill a missed habit check-in with notes:", fontFamily = appFont, fontSize = 12.sp, color = secondaryTextColor)
                        OutlinedTextField(
                            value = selectedDateText,
                            onValueChange = { selectedDateText = it },
                            label = { Text("Date (YYYY-MM-DD)", fontFamily = appFont, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Count:", fontFamily = appFont, fontSize = 13.sp, color = textColor)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { if (pastCount > 1) pastCount-- },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                                ) {
                                    Text("-", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                                }
                                Text("$pastCount", fontFamily = appFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                IconButton(
                                    onClick = { pastCount++ },
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(borderColor.copy(alpha = 0.3f))
                                ) {
                                    Text("+", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = pastNotes,
                            onValueChange = { pastNotes = it },
                            label = { Text("Notes (Optional)", fontFamily = appFont, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedDateText.isNotBlank()) {
                                viewModel.insertEntry(
                                    HabitEntry(
                                        dateString = selectedDateText.trim(),
                                        count = pastCount,
                                        notes = pastNotes.ifBlank { null },
                                        trackerId = selectedTrackerId
                                    )
                                )
                                showAddPastLogDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White)
                    ) {
                        Text("Add Log", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPastLogDialog = false }) {
                        Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun EnhancedTimelineCard(
    appFont: FontFamily,
    entry: HabitEntry,
    habitTitle: String,
    habitIcon: String,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenJournal: () -> Unit
) {
    val formattedDate = remember(entry.dateString) {
        try {
            val date = LocalDate.parse(entry.dateString)
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
            val month = date.month.getDisplayName(TextStyle.SHORT, Locale.US)
            "$dayOfWeek, $month ${date.dayOfMonth}, ${date.year}"
        } catch (e: Exception) {
            entry.dateString
        }
    }

    val moodTag = remember(entry.notes) {
        val notes = entry.notes ?: ""
        val match = Regex("\\[MOOD:([^\\]]+)\\]").find(notes)
        match?.groupValues?.get(1)
    }

    val cleanNotes = remember(entry.notes) {
        val notes = entry.notes ?: ""
        notes.replace(Regex("\\[MOOD:[^\\]]+\\]"), "").trim()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(0.5.dp, borderColor),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Date & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f))
                            .border(0.5.dp, primaryColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = habitIcon, fontSize = 14.sp)
                    }
                    Column {
                        Text(
                            text = formattedDate,
                            fontFamily = appFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (moodTag != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(primaryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = moodTag, fontSize = 11.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(primaryColor.copy(alpha = 0.2f))
                            .border(0.5.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${entry.count}",
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = secondaryTextColor, modifier = Modifier.size(14.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Journal / Notes Content
            if (cleanNotes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.06f))
                        .border(0.5.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { onOpenJournal() }
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "“", fontSize = 14.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        Text(
                            text = cleanNotes,
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            color = textColor,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
