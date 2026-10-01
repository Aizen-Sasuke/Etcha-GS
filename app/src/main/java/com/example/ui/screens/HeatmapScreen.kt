package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.data.HabitEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun HeatmapScreen(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val basePrimaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)
    val emptyCellColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.2f) else Color(0xFF1C1C1C)
    val listRowBgColor = if (ThemeStyles.isLightTheme(selectedTheme)) borderColor.copy(alpha = 0.15f) else Color(0xFF1B1B1B)
    
    val activeTracker by viewModel.activeTracker.collectAsState()
    val primaryColor = activeTracker.accentColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { basePrimaryColor }
    } ?: basePrimaryColor

    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val completionsByDate by viewModel.completionsByDate.collectAsState()

    val today = remember { LocalDate.now() }
    
    // Dynamic startDate (Sunday of the week for the first logged entry)
    val startDate = remember(entries) {
        val sortedDates = entries.mapNotNull {
            try { LocalDate.parse(it.dateString) } catch (e: Exception) { null }
        }.sorted()
        // If empty, fall back to 12 months ago to present a clean canvas
        val first = sortedDates.firstOrNull() ?: LocalDate.now().minusMonths(11).withDayOfMonth(1)
        val offset = first.dayOfWeek.value % 7 // 0 = Sunday
        first.minusDays(offset.toLong())
    }

    // Dynamic endDate (Saturday of the week for the last logged entry, or today's Saturday)
    val endDate = remember(entries) {
        val sortedDates = entries.mapNotNull {
            try { LocalDate.parse(it.dateString) } catch (e: Exception) { null }
        }.sorted()
        val last = sortedDates.lastOrNull() ?: LocalDate.now()
        val offset = 6 - (last.dayOfWeek.value % 7) // Saturday is index 6
        last.plusDays(offset.toLong())
    }

    val scrollState = rememberScrollState()

    var showCellDetails by remember { mutableStateOf(false) }
    var selectedCellDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedCellEntries by remember { mutableStateOf<List<HabitEntry>>(emptyList()) }

    // Generate days grid data
    val daysBetween = remember(startDate, endDate) { 
        ChronoUnit.DAYS.between(startDate, endDate).toInt() 
    }
    
    // Grid starts exactly on Sunday, so startDayOfWeek has no offset
    val startDayOfWeek = 0

    if (showCellDetails && selectedCellDate != null) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showCellDetails = false },
            containerColor = cardBgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val formattedDate = selectedCellDate!!.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                val totalCount = selectedCellEntries.sumOf { it.count }
                
                Text(fontFamily = appFont, 
                    text = formattedDate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                Text(fontFamily = appFont, 
                    text = "Total signals: $totalCount",
                    fontSize = 15.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (selectedCellEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fontFamily = appFont, text = "Log Times:", color = secondaryTextColor, fontSize = 13.sp)
                    
                    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                    selectedCellEntries.sortedBy { it.timestamp }.forEach { entry ->
                        val timeStr = java.time.Instant.ofEpochMilli(entry.timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(timeFormatter)
                            
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(listRowBgColor)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(fontFamily = appFont, text = timeStr, color = textColor, fontSize = 14.sp)
                            Text(fontFamily = appFont, text = "+${entry.count}", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

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
                .padding(bottom = 80.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Activity Heatmap",
                fontFamily = appFont,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = textColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(fontFamily = appFont, 
                text = "Your custom habit contribution graph from the first logged date.",
                fontSize = 13.sp,
                color = secondaryTextColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val horizontalScrollState = rememberScrollState()
            LaunchedEffect(daysBetween) {
                horizontalScrollState.scrollTo(horizontalScrollState.maxValue)
            }

            // Heatmap Grid wrapped in a horizontally scrollable row to allow full width
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 20.dp, spotColor = primaryColor, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBgColor)
                    .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
                    .horizontalScroll(horizontalScrollState)
                    .padding(16.dp)
            ) {
                    Column {
                        // Month Labels
                        Row(modifier = Modifier.padding(start = 36.dp, bottom = 8.dp)) {
                            // Find the first day of each month to place labels
                            var currentMonthStr = ""
                            for (i in 0..daysBetween step 7) {
                                val date = startDate.plusDays(i.toLong())
                                val monthStr = date.month.getDisplayName(TextStyle.SHORT, Locale.US)
                                if (monthStr != currentMonthStr && date.dayOfMonth <= 14) {
                                    Text(fontFamily = appFont, 
                                        text = monthStr,
                                        color = secondaryTextColor,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(44.dp) // approx width for 3 weeks
                                    )
                                    currentMonthStr = monthStr
                                } else {
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                            }
                        }

                        Row {
                            // Days of Week Labels (Left sidebar)
                            Column(
                                modifier = Modifier.padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("", "Mon", "", "Wed", "", "Fri", "").forEach { day ->
                                    Text(fontFamily = appFont, 
                                        text = day,
                                        color = secondaryTextColor,
                                        fontSize = 11.sp,
                                        modifier = Modifier.height(16.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            // The Grid (7 rows x N columns)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val weeks = (daysBetween + startDayOfWeek) / 7 + 1
                                for (w in 0 until weeks) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (d in 0..6) {
                                            val dayOffset = w * 7 + d - startDayOfWeek
                                            if (dayOffset < 0 || dayOffset > daysBetween) {
                                                // Empty slot at start/end
                                                Box(modifier = Modifier.size(16.dp).background(Color.Transparent))
                                            } else {
                                                val cellDate = startDate.plusDays(dayOffset.toLong())
                                                val dateStr = cellDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                val entriesForDay = completionsByDate[dateStr] ?: emptyList()
                                                val count = entriesForDay.sumOf { it.count }
                                                
                                                val intensity = when {
                                                    count == 0 -> 0f
                                                    count == 1 -> 0.25f
                                                    count == 2 -> 0.45f
                                                    count == 3 -> 0.65f
                                                    else -> 0.85f
                                                }
                                                
                                                val cellColor = if (count > 0) {
                                                    primaryColor.copy(alpha = intensity)
                                                } else {
                                                    emptyCellColor
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(cellColor)
                                                        .then(
                                                            if (count > 0) Modifier.clickable { 
                                                                selectedCellDate = cellDate
                                                                selectedCellEntries = entriesForDay
                                                                showCellDetails = true 
                                                            } else Modifier
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Legend
                        Row(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fontFamily = appFont, text = "Less", color = secondaryTextColor, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                            listOf(0f, 0.25f, 0.45f, 0.65f, 0.85f).forEach { intensity ->
                                val color = if (intensity == 0f) emptyCellColor else primaryColor.copy(alpha = intensity)
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                )
                            }
                            Text(fontFamily = appFont, text = "More", color = secondaryTextColor, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
            }
        }
    }
}
