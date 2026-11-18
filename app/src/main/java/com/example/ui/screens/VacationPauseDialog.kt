package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.ThemeStyles
import com.example.ui.TrackerConfig
import com.example.ui.VacationPauseHelper
import com.example.ui.VacationPauseState
import java.time.LocalDate

@Composable
fun GlobalVacationDialog(
    visible: Boolean,
    today: LocalDate,
    currentVacation: VacationPauseState,
    onDismiss: () -> Unit,
    onApplyVacation: (enabled: Boolean, startDate: String?, endDate: String?) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    if (!visible) return

    val primaryAccent = ThemeStyles.accentOnSurface(primaryColor, cardBgColor.copy(alpha = 1f), fallbackInk = textColor)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("global_vacation_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🌴 Vacation & Rest Mode",
                    fontFamily = appFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Protects streaks and Habit Strength across all habits and pauses daily reminders without resetting progress.",
                    fontFamily = appFont,
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )

                val presets = listOf(
                    Triple("Turn Off Vacation", false, null as LocalDate?),
                    Triple("Rest Weekend (3 days)", true, today.plusDays(2)),
                    Triple("Short Trip (7 days)", true, today.plusDays(6)),
                    Triple("Extended Break (14 days)", true, today.plusDays(13)),
                    Triple("Pause Indefinitely", true, null as LocalDate?)
                )

                presets.forEachIndexed { idx, (label, enabled, endLocal) ->
                    val isCurrent = if (!enabled) {
                        !currentVacation.globalEnabled
                    } else {
                        currentVacation.globalEnabled && currentVacation.globalEndDate == endLocal?.toString()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) primaryColor.copy(alpha = 0.16f) else borderColor.copy(alpha = 0.14f))
                            .border(
                                width = if (isCurrent) 1.dp else 0.5.dp,
                                color = if (isCurrent) primaryColor else borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!enabled) {
                                    onApplyVacation(false, null, null)
                                } else {
                                    onApplyVacation(true, today.toString(), endLocal?.toString())
                                }
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("vacation_preset_$idx"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontFamily = appFont,
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) primaryAccent else textColor
                        )
                        if (endLocal != null) {
                            Text(
                                text = "Until $endLocal",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Close", fontFamily = appFont, color = secondaryTextColor)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerPauseDialog(
    tracker: TrackerConfig?,
    today: LocalDate,
    vacationState: VacationPauseState,
    onDismiss: () -> Unit,
    onSetPausedUntil: (trackerId: String, pausedUntilDate: String?) -> Unit,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    if (tracker == null) return

    val currentBadge = VacationPauseHelper.formatTrackerPauseBadge(tracker.pausedUntilDate, vacationState, today)
    val primaryAccent = ThemeStyles.accentOnSurface(primaryColor, cardBgColor.copy(alpha = 1f), fallbackInk = textColor)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("tracker_pause_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🌴 Pause ${tracker.icon} ${tracker.title}",
                    fontFamily = appFont,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = currentBadge ?: "Schedule rest days without breaking your streak or Habit Strength.",
                    fontFamily = appFont,
                    fontSize = 12.sp,
                    color = if (currentBadge != null) primaryAccent else secondaryTextColor
                )

                val options = listOf(
                    "Resume Active Schedule" to null,
                    "Pause Today Only" to today.toString(),
                    "Pause for 3 Days" to today.plusDays(2).toString(),
                    "Pause for 7 Days" to today.plusDays(6).toString()
                )

                options.forEachIndexed { idx, (label, untilDate) ->
                    val isSelected = tracker.pausedUntilDate == untilDate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) primaryColor.copy(alpha = 0.16f) else borderColor.copy(alpha = 0.14f))
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) primaryColor else borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSetPausedUntil(tracker.id, untilDate)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("tracker_pause_option_$idx"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontFamily = appFont,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) primaryAccent else textColor
                        )
                        if (untilDate != null) {
                            Text(
                                text = untilDate,
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Close", fontFamily = appFont, color = secondaryTextColor)
                    }
                }
            }
        }
    }
}
