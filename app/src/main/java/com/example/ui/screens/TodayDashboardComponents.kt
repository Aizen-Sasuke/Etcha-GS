package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ThemeStyles
import com.example.ui.TodayDashboardState
import com.example.ui.TodayHabitItem
import com.example.ui.TodayHabitStatus
import com.example.ui.TrackerConfig
import com.example.ui.components.AnimatedNumberText
import com.example.ui.components.MotionSpecs
import com.example.ui.components.recessedWell
import com.example.ui.components.surface3DCard
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UnifiedTodayDashboardCard(
    dashboard: TodayDashboardState,
    selectedTrackerId: String,
    selectedRoutineFilter: String,
    themeConfig: HomeThemeConfig,
    onQuickIncrement: (String) -> Unit,
    onOpenHabitDetail: (TodayHabitItem) -> Unit,
    onAddTrackerClick: () -> Unit
) {
    val primaryColor = themeConfig.primaryColor
    val textColor = themeConfig.textColor
    val secondaryTextColor = themeConfig.secondaryTextColor
    val cardBgColor = themeConfig.cardBgColor
    val borderColor = themeConfig.borderColor
    val appFont = themeConfig.appFont
    val isLight = ThemeStyles.isLightTheme(themeConfig.selectedTheme)
    val onPrimaryTextColor = themeConfig.onPrimaryColor
    val primaryAccentTextColor = themeConfig.accentTextColor

    var isCardExpanded by remember { mutableStateOf(true) }
    var showAllAttention by remember { mutableStateOf(false) }
    var showCompletedGroup by remember { mutableStateOf(false) }
    var showRestGroup by remember { mutableStateOf(false) }

    val filterMatches: (TodayHabitItem) -> Boolean = { item ->
        selectedRoutineFilter == "all" || item.tracker.timeOfDay == selectedRoutineFilter
    }

    val filteredAttention = remember(dashboard.attentionItems, selectedRoutineFilter) {
        dashboard.attentionItems.filter(filterMatches)
    }
    val filteredAvoidance = remember(dashboard.avoidanceItems, selectedRoutineFilter) {
        dashboard.avoidanceItems.filter(filterMatches)
    }
    val filteredCompleted = remember(dashboard.completedItems, selectedRoutineFilter) {
        dashboard.completedItems.filter(filterMatches)
    }
    val filteredRest = remember(dashboard.restAndProtectedItems, selectedRoutineFilter) {
        dashboard.restAndProtectedItems.filter(filterMatches)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = dashboard.overallProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "today_overall_progress"
    )
    val progressPercent = (dashboard.overallProgress * 100).toInt()
    val dateHeader = remember(dashboard.effectiveToday) {
        dashboard.effectiveToday.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
    }

    val isGlass = ThemeStyles.isGlassTheme(themeConfig.selectedTheme)
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = ThemeStyles.getGlassBorderStroke(themeConfig.selectedTheme),
        shape = DesignTokens.CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .surface3DCard(
                shape = DesignTokens.CardShape,
                isLight = isLight,
                primaryColor = primaryColor,
                isGlass = isGlass,
                elevationDp = 6.dp
            )
            .testTag("today_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header + Overall Progress Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCardExpanded = !isCardExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Today Overview",
                            fontFamily = appFont,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "· $dateHeader",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        if (dashboard.isVacationActive) {
                            DashboardStatChip(
                                label = "🌴 Vacation",
                                bg = primaryColor.copy(alpha = 0.16f),
                                fg = primaryAccentTextColor,
                                appFont = appFont
                            )
                        }
                        if (dashboard.isTodayFrozen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(primaryColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🧊 Frozen",
                                    fontFamily = appFont,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryAccentTextColor
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            dashboard.totalActiveCount == 0 -> "No active habits yet"
                            dashboard.dueTotalCount == 0 -> "No habits due right now · All clear"
                            dashboard.dueCompletedCount >= dashboard.dueTotalCount ->
                                "All ${dashboard.dueTotalCount} due habits complete!"
                            else ->
                                "${dashboard.dueCompletedCount} of ${dashboard.dueTotalCount} due complete · ${dashboard.dueTotalCount - dashboard.dueCompletedCount} remaining"
                        },
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        color = secondaryTextColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("today_overall_percentage")
                    ) {
                        AnimatedNumberText(
                            value = if (dashboard.totalActiveCount == 0) 0 else progressPercent,
                            suffix = "%",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccentTextColor
                        )
                    }
                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isCardExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "today_expand_arrow"
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCardExpanded) "Collapse Today Overview" else "Expand Today Overview",
                        tint = secondaryTextColor,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            if (dashboard.totalActiveCount > 0) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .recessedWell(RoundedCornerShape(3.dp), isLight = isLight)
                        .clip(RoundedCornerShape(3.dp)),
                    color = primaryColor,
                    trackColor = borderColor.copy(alpha = 0.28f)
                )

                // Compact Status & Next Reminder Row (wraps cleanly on narrow/scaled screens)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredAttention.isNotEmpty()) {
                            DashboardStatChip(
                                label = "${filteredAttention.size} Left",
                                bg = primaryColor.copy(alpha = 0.14f),
                                fg = primaryAccentTextColor,
                                appFont = appFont
                            )
                        }
                        if (filteredCompleted.isNotEmpty()) {
                            DashboardStatChip(
                                label = "✓ ${filteredCompleted.size} Done",
                                bg = borderColor.copy(alpha = 0.22f),
                                fg = textColor,
                                appFont = appFont
                            )
                        }
                        if (filteredRest.isNotEmpty()) {
                            DashboardStatChip(
                                label = "${filteredRest.size} Rest/Safe",
                                bg = borderColor.copy(alpha = 0.18f),
                                fg = secondaryTextColor,
                                appFont = appFont
                            )
                        }
                    }

                    if (dashboard.nextReminderText != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Next scheduled reminder",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = dashboard.nextReminderText,
                                fontFamily = appFont,
                                fontSize = 10.5.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isCardExpanded,
                enter = expandVertically(animationSpec = MotionSpecs.CardRevealSizeSpring) + fadeIn(tween(200)),
                exit = shrinkVertically(animationSpec = MotionSpecs.CardRevealSizeSpring) + fadeOut(tween(140))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dashboard.totalActiveCount == 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(borderColor.copy(alpha = 0.14f))
                                .clickable { onAddTrackerClick() }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Create your first habit to build your Today Dashboard.",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                color = secondaryTextColor,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "+ Add",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccentTextColor
                            )
                        }
                    } else {
                        // 1. Needs Attention / In-Progress Items
                        if (filteredAttention.isNotEmpty()) {
                            val visibleLimit = 5
                            val shownAttention = if (showAllAttention) {
                                filteredAttention
                            } else {
                                filteredAttention.take(visibleLimit)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                shownAttention.forEach { item ->
                                    TodayAttentionHabitRow(
                                        item = item,
                                        isSelected = item.tracker.id == selectedTrackerId,
                                        themeConfig = themeConfig,
                                        isLight = isLight,
                                        primaryAccentTextColor = primaryAccentTextColor,
                                        onPrimaryTextColor = onPrimaryTextColor,
                                        onQuickIncrement = { onQuickIncrement(item.tracker.id) },
                                        onOpenDetails = { onOpenHabitDetail(item) }
                                    )
                                }
                                if (filteredAttention.size > visibleLimit) {
                                    TextButton(
                                        onClick = { showAllAttention = !showAllAttention },
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .heightIn(min = 36.dp)
                                    ) {
                                        Text(
                                            text = if (showAllAttention) {
                                                "Show fewer"
                                            } else {
                                                "Show all ${filteredAttention.size} needing attention"
                                            },
                                            fontFamily = appFont,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryAccentTextColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryColor.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (selectedRoutineFilter == "all") {
                                        "✨ Nothing pending right now — great momentum today!"
                                    } else {
                                        "✨ No pending habits in this routine right now."
                                    },
                                    fontFamily = appFont,
                                    fontSize = 11.5.sp,
                                    color = primaryAccentTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 2. Avoidance / Bad Habits Section (Calm Clean-Day Status or Slip Feedback)
                        if (filteredAvoidance.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredAvoidance.forEach { item ->
                                    TodayAvoidanceHabitRow(
                                        item = item,
                                        isSelected = item.tracker.id == selectedTrackerId,
                                        themeConfig = themeConfig,
                                        isLight = isLight,
                                        onOpenDetails = { onOpenHabitDetail(item) }
                                    )
                                }
                            }
                        }

                        // 3. Collapsible Completed Today Drawer
                        if (filteredCompleted.isNotEmpty()) {
                            CollapsibleDashboardGroup(
                                title = "✓ Completed today (${filteredCompleted.size})",
                                expanded = showCompletedGroup,
                                onToggle = { showCompletedGroup = !showCompletedGroup },
                                items = filteredCompleted,
                                themeConfig = themeConfig,
                                primaryAccentTextColor = primaryAccentTextColor,
                                testTagName = "dashboard_completed_toggle",
                                onItemClick = onOpenHabitDetail
                            )
                        }

                        // 4. Collapsible Rest Days, Paused, Weekly Goal Met & Frozen Drawer
                        if (filteredRest.isNotEmpty()) {
                            CollapsibleDashboardGroup(
                                title = "🛋️ Rest, Paused & Frozen (${filteredRest.size})",
                                expanded = showRestGroup,
                                onToggle = { showRestGroup = !showRestGroup },
                                items = filteredRest,
                                themeConfig = themeConfig,
                                primaryAccentTextColor = primaryAccentTextColor,
                                testTagName = "dashboard_rest_toggle",
                                onItemClick = onOpenHabitDetail
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatChip(
    label: String,
    bg: Color,
    fg: Color,
    appFont: androidx.compose.ui.text.font.FontFamily
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontFamily = appFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
private fun TodayAttentionHabitRow(
    item: TodayHabitItem,
    isSelected: Boolean,
    themeConfig: HomeThemeConfig,
    isLight: Boolean,
    primaryAccentTextColor: Color,
    onPrimaryTextColor: Color,
    onQuickIncrement: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val rowBg = if (isSelected) {
        themeConfig.primaryColor.copy(alpha = 0.08f)
    } else if (isLight) {
        themeConfig.borderColor.copy(alpha = 0.14f)
    } else {
        themeConfig.neutralWellColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .surface3DCard(
                shape = RoundedCornerShape(12.dp),
                isLight = isLight,
                primaryColor = themeConfig.primaryColor,
                isGlass = ThemeStyles.isGlassTheme(themeConfig.selectedTheme),
                elevationDp = if (isSelected) 4.dp else 2.dp
            )
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) themeConfig.primaryColor.copy(alpha = 0.5f) else themeConfig.borderColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onOpenDetails() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("dashboard_habit_row_${item.tracker.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(themeConfig.primaryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.tracker.icon, fontSize = 16.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.tracker.title,
                        fontFamily = themeConfig.appFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeConfig.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.reminderSummary != null) {
                        Text(
                            text = item.reminderSummary,
                            fontFamily = themeConfig.appFont,
                            fontSize = 10.sp,
                            color = themeConfig.secondaryTextColor,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = item.statusLabel,
                    fontFamily = themeConfig.appFont,
                    fontSize = 11.sp,
                    color = if (item.status == TodayHabitStatus.IN_PROGRESS) primaryAccentTextColor else themeConfig.secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.targetCount > 1 || item.isWeeklyQuota) {
                    val barProgress = if (item.targetCount > 1) {
                        item.progressFraction
                    } else {
                        (item.weeklyCompletedDays.toFloat() / item.weeklyTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    }
                    val animatedBarProgress by animateFloatAsState(
                        targetValue = barProgress,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "row_progress_${item.tracker.id}"
                    )
                    LinearProgressIndicator(
                        progress = { animatedBarProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .recessedWell(RoundedCornerShape(2.dp), isLight = isLight)
                            .clip(RoundedCornerShape(2.dp)),
                        color = themeConfig.primaryColor,
                        trackColor = themeConfig.borderColor.copy(alpha = 0.25f)
                    )
                }
            }
        }

        val actionLabel = if (item.targetCount > 1 || item.tracker.type == "misc") "+1" else "Check In"
        Button(
            onClick = onQuickIncrement,
            colors = ButtonDefaults.buttonColors(
                containerColor = themeConfig.primaryColor,
                contentColor = onPrimaryTextColor
            ),
            shape = DesignTokens.ControlShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier
                .widthIn(min = 84.dp)
                .heightIn(min = DesignTokens.ActionButtonHeight)
                .defaultMinSize(minWidth = 84.dp, minHeight = DesignTokens.ActionButtonHeight)
                .testTag("dashboard_action_${item.tracker.id}")
        ) {
            Text(
                text = actionLabel,
                fontFamily = themeConfig.appFont,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TodayAvoidanceHabitRow(
    item: TodayHabitItem,
    isSelected: Boolean,
    themeConfig: HomeThemeConfig,
    isLight: Boolean,
    onOpenDetails: () -> Unit
) {
    val isSlip = item.status == TodayHabitStatus.BAD_SLIP
    val dangerColor = themeConfig.dangerColor
    val rowBg = if (isSlip) {
        dangerColor.copy(alpha = 0.10f)
    } else if (isSelected) {
        themeConfig.primaryColor.copy(alpha = 0.08f)
    } else if (isLight) {
        themeConfig.borderColor.copy(alpha = 0.14f)
    } else {
        themeConfig.neutralWellColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .surface3DCard(
                shape = RoundedCornerShape(12.dp),
                isLight = isLight,
                primaryColor = themeConfig.primaryColor,
                isGlass = ThemeStyles.isGlassTheme(themeConfig.selectedTheme),
                elevationDp = if (isSelected) 4.dp else 2.dp
            )
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .border(
                width = 0.5.dp,
                color = if (isSlip) dangerColor.copy(alpha = 0.45f) else themeConfig.borderColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onOpenDetails() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("dashboard_habit_row_${item.tracker.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = item.tracker.icon, fontSize = 18.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.tracker.title,
                    fontFamily = themeConfig.appFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themeConfig.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.statusLabel,
                    fontFamily = themeConfig.appFont,
                    fontSize = 11.sp,
                    color = if (isSlip) dangerColor else themeConfig.secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSlip) dangerColor.copy(alpha = 0.14f)
                    else themeConfig.primaryColor.copy(alpha = 0.14f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isSlip) "Slip logged" else "🛡️ Clean",
                fontFamily = themeConfig.appFont,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSlip) dangerColor else themeConfig.accentTextColor
            )
        }
    }
}

@Composable
private fun CollapsibleDashboardGroup(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<TodayHabitItem>,
    themeConfig: HomeThemeConfig,
    primaryAccentTextColor: Color,
    testTagName: String,
    onItemClick: (TodayHabitItem) -> Unit
) {
    val isLightTray = ThemeStyles.isLightTheme(themeConfig.selectedTheme)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .recessedWell(RoundedCornerShape(10.dp), isLight = isLightTray)
            .clip(RoundedCornerShape(10.dp))
            .background(themeConfig.borderColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        val groupArrowRot by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = MotionSpecs.BouncyPopSpring,
            label = "group_arrow_$testTagName"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp)
                .clickable { onToggle() }
                .padding(vertical = 4.dp)
                .testTag(testTagName),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = themeConfig.appFont,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeConfig.secondaryTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = title,
                tint = themeConfig.secondaryTextColor,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(groupArrowRot)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = MotionSpecs.CardRevealSizeSpring) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = MotionSpecs.CardRevealSizeSpring) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier.padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onItemClick(item) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("dashboard_habit_row_${item.tracker.id}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = item.tracker.icon, fontSize = 14.sp)
                            Column {
                                Text(
                                    text = item.tracker.title,
                                    fontFamily = themeConfig.appFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = themeConfig.textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.statusLabel,
                                    fontFamily = themeConfig.appFont,
                                    fontSize = 10.5.sp,
                                    color = themeConfig.secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (item.status == TodayHabitStatus.COMPLETED) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = primaryAccentTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayHabitDetailSheet(
    item: TodayHabitItem?,
    themeConfig: HomeThemeConfig,
    onDismiss: () -> Unit,
    onFocusOnCalendar: (String) -> Unit,
    onQuickIncrement: (String) -> Unit,
    onOpenJournal: (String) -> Unit,
    onOpenEdit: (TodayHabitItem) -> Unit,
    onViewHistory: (String) -> Unit,
    onViewReports: (String) -> Unit,
    onStartFocus: (TrackerConfig) -> Unit = {}
) {
    if (item == null) return
    val tracker = item.tracker
    val primaryAccent = themeConfig.accentTextColor
    val onPrimary = themeConfig.onPrimaryColor
    val dangerColor = themeConfig.dangerColor
    val isBad = tracker.type == "bad"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = themeConfig.modalBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(themeConfig.primaryColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = tracker.icon, fontSize = 24.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tracker.title,
                        fontFamily = themeConfig.appFont,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeConfig.textColor
                    )
                    Text(
                        text = item.statusLabel,
                        fontFamily = themeConfig.appFont,
                        fontSize = 12.sp,
                        color = if (item.status == TodayHabitStatus.BAD_SLIP) dangerColor else themeConfig.secondaryTextColor
                    )
                }
            }

            if (item.targetCount > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Today's Progress: ${item.todayCount} / ${item.targetCount}",
                        fontFamily = themeConfig.appFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeConfig.textColor
                    )
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = themeConfig.primaryColor,
                        trackColor = themeConfig.borderColor.copy(alpha = 0.28f)
                    )
                }
            }

            if (item.isWeeklyQuota) {
                val quotaFraction = (item.weeklyCompletedDays.toFloat() / item.weeklyTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "This Week: ${item.weeklyCompletedDays} / ${item.weeklyTarget} days",
                        fontFamily = themeConfig.appFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeConfig.textColor
                    )
                    LinearProgressIndicator(
                        progress = { quotaFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = themeConfig.primaryColor,
                        trackColor = themeConfig.borderColor.copy(alpha = 0.28f)
                    )
                }
            }

            // Schedule & Reminder Info Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeConfig.borderColor.copy(alpha = 0.14f))
                    .padding(12.dp)
            ) {
                val freqDesc = when (tracker.frequencyType) {
                    "weekdays" -> "Weekdays (Mon–Fri)"
                    "custom_days", "weekly" -> "Custom Days (${tracker.targetDays})"
                    "weekly_quota", "3x_week" -> "Weekly Quota (${item.weeklyTarget} days/wk)"
                    else -> "Daily"
                }
                val remDesc = if (tracker.reminderEnabled) {
                    "Reminder On · ${tracker.reminderTime}"
                } else {
                    "Individual reminder Off"
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Schedule: $freqDesc",
                        fontFamily = themeConfig.appFont,
                        fontSize = 12.sp,
                        color = themeConfig.textColor
                    )
                    Text(
                        text = remDesc,
                        fontFamily = themeConfig.appFont,
                        fontSize = 11.5.sp,
                        color = themeConfig.secondaryTextColor
                    )
                }
            }

            // Primary Action Row
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onQuickIncrement(tracker.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBad) dangerColor.copy(alpha = 0.14f) else themeConfig.primaryColor,
                        contentColor = if (isBad) dangerColor else onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("detail_quick_action_button")
                ) {
                    Text(
                        text = when {
                            isBad -> "Log Slip"
                            item.targetCount > 1 || tracker.type == "misc" || item.status == TodayHabitStatus.COMPLETED -> "+1 Log"
                            else -> "Check In"
                        },
                        fontFamily = themeConfig.appFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        onOpenJournal(tracker.id)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, themeConfig.borderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "✍️ Journal",
                        fontFamily = themeConfig.appFont,
                        color = themeConfig.textColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isBad) {
                OutlinedButton(
                    onClick = {
                        onStartFocus(tracker)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, themeConfig.primaryColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("detail_focus_button")
                ) {
                    Text(
                        text = "⏱️ Start Focus Session (${tracker.defaultFocusMinutes}m)",
                        fontFamily = themeConfig.appFont,
                        color = primaryAccent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Cross-Tab Navigation: History & Reports
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onViewHistory(tracker.id)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, themeConfig.borderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("detail_view_history_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                        Text("History", fontFamily = themeConfig.appFont, color = themeConfig.textColor, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                OutlinedButton(
                    onClick = {
                        onViewReports(tracker.id)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, themeConfig.borderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("detail_view_reports_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                        Text("Reports", fontFamily = themeConfig.appFont, color = themeConfig.textColor, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        onFocusOnCalendar(tracker.id)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Show on Calendar",
                        fontFamily = themeConfig.appFont,
                        color = primaryAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenEdit(item)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = themeConfig.secondaryTextColor, modifier = Modifier.size(15.dp))
                        Text("Edit Habit", fontFamily = themeConfig.appFont, color = themeConfig.secondaryTextColor)
                    }
                }
            }
        }
    }
}
