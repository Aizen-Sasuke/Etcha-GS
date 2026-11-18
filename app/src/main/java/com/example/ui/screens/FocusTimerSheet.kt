package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FocusMode
import com.example.ui.FocusPhase
import com.example.ui.FocusSessionState
import com.example.ui.ThemeStyles
import com.example.ui.components.recessedWell
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerSheet(
    session: FocusSessionState,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    selectedTheme: String,
    onDismiss: () -> Unit,
    onSelectMode: (FocusMode) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteAndLog: () -> Unit
) {
    if (!session.isSheetVisible) return

    val isLight = ThemeStyles.isLightTheme(selectedTheme)
    val onPrimary = ThemeStyles.contrastOn(primaryColor)
    val primaryAccent = ThemeStyles.accentOnSurface(primaryColor, cardBgColor.copy(alpha = 1f), fallbackInk = textColor)
    val wellColor = ThemeStyles.getNeutralWellColor(selectedTheme)
    val haptic = LocalHapticFeedback.current
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var autoCompletedTargetEpoch by remember { mutableLongStateOf(0L) }

    LaunchedEffect(session.phase, session.startedAtEpochMs, session.targetDurationMinutes, session.mode) {
        nowEpochMs = System.currentTimeMillis()
        while (session.isRunning) {
            nowEpochMs = System.currentTimeMillis()
            if (session.hasReachedPhaseTarget(nowEpochMs) && autoCompletedTargetEpoch != session.startedAtEpochMs) {
                autoCompletedTargetEpoch = session.startedAtEpochMs
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (session.isBreakPhase) {
                    onReset()
                } else {
                    onCompleteAndLog()
                }
                break
            }
            delay(250L)
        }
    }

    val displayMillis = session.remainingMillis(nowEpochMs)
    val clockText = FocusSessionState.formatClockMillis(displayMillis)
    val progress = session.progressFraction(nowEpochMs)
    val trackBgColor = borderColor.copy(alpha = 0.28f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
                .testTag("focus_timer_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = session.trackerIcon.ifBlank { "⏱️" }, fontSize = 24.sp)
                    Column {
                        Text(
                            text = session.trackerTitle.ifBlank { "Focus Session" },
                            fontFamily = appFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val phaseLabel = when (session.phase) {
                            FocusPhase.IDLE -> "${session.mode.label} · Ready"
                            FocusPhase.FOCUS_RUNNING -> "Deep Focus Active"
                            FocusPhase.FOCUS_PAUSED -> "Focus Paused"
                            FocusPhase.BREAK_RUNNING -> "Rest Break Active (${session.breakDurationMinutes}m)"
                            FocusPhase.BREAK_PAUSED -> "Break Paused"
                            FocusPhase.COMPLETED -> "Logged +1 · Focused ${session.lastCompletedLoggedMinutes}m"
                        }
                        Text(
                            text = phaseLabel,
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            color = if (session.phase == FocusPhase.COMPLETED) primaryAccent else secondaryTextColor
                        )
                    }
                }
                if (session.completedPomodoroCycles > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "🍅 ${session.completedPomodoroCycles}",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent
                        )
                    }
                }
            }

            // Mode selector pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .recessedWell(RoundedCornerShape(12.dp), isLight = isLight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(wellColor)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FocusMode.entries.forEach { mode ->
                    val isSel = session.mode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) primaryColor else Color.Transparent)
                            .clickable(enabled = !session.isRunning) { onSelectMode(mode) }
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .testTag("focus_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) onPrimary else secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Completed/Break Banner
            if (session.phase == FocusPhase.COMPLETED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.14f)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Logged +1 check-in · Focused ${session.lastCompletedLoggedMinutes}m",
                        fontFamily = appFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Circular Canvas Progress Ring + Tabular Readout
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 12.dp.toPx()
                    val diameter = minOf(size.width, size.height) - strokePx
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = trackBgColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = clockText,
                        style = TextStyle(
                            fontFamily = appFont,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum",
                            color = textColor
                        ),
                        modifier = Modifier.testTag("focus_clock_readout")
                    )
                    Text(
                        text = when {
                            session.isBreakPhase -> "Break Timer"
                            session.mode == FocusMode.STOPWATCH -> "Elapsed Time"
                            else -> "${session.targetDurationMinutes}m Goal"
                        },
                        fontFamily = appFont,
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )
                }
            }

            // Preset durations when idle or completed and not stopwatch
            if (!session.isRunning && session.mode != FocusMode.STOPWATCH && !session.isBreakPhase) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 25, 45, 60).forEach { mins ->
                        val isSel = session.targetDurationMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) primaryColor.copy(alpha = 0.2f) else borderColor.copy(alpha = 0.16f))
                                .border(
                                    width = if (isSel) 1.dp else 0.5.dp,
                                    color = if (isSel) primaryColor else borderColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectDuration(mins) }
                                .testTag("focus_preset_${mins}m"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) primaryAccent else textColor
                            )
                        }
                    }
                }
            }

            // Primary Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(borderColor.copy(alpha = 0.2f))
                        .testTag("focus_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = textColor
                    )
                }

                Button(
                    onClick = {
                        if (session.isRunning) {
                            onPause()
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStartOrResume()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("focus_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (session.isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (session.isRunning) "Pause Focus" else "Start Focus",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            session.isRunning -> "Pause"
                            session.phase == FocusPhase.FOCUS_PAUSED || session.phase == FocusPhase.BREAK_PAUSED -> "Resume"
                            else -> "Start"
                        },
                        fontFamily = appFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleteAndLog()
                    },
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("focus_complete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete and Log Habit",
                        tint = primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Log +1",
                        fontFamily = appFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
