package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DayOfWeekStat
import com.example.ui.MonthlyTrendPoint
import com.example.ui.MoodStat
import com.example.ui.TimeOfDayStat
import com.example.ui.WeeklyConsistencyPoint
import com.example.ui.ThemeStyles
import com.example.ui.components.MotionSpecs
import com.example.ui.components.recessedWell
import com.example.ui.components.surface3DCard
import kotlin.math.roundToInt

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveMonthlyTrendChart(
    appFont: FontFamily,
    dataPoints: List<MonthlyTrendPoint>,
    lineColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    gridColor: Color,
    cardBgColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val animatedProgress = remember(dataPoints) { Animatable(0f) }
    var selectedIndex by remember(dataPoints) { mutableStateOf<Int?>(null) }

    LaunchedEffect(dataPoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
        )
    }

    val avgCount = remember(dataPoints) {
        if (dataPoints.isEmpty()) 0.0 else dataPoints.map { it.count }.average()
    }
    val inspectedPoint = selectedIndex?.let { dataPoints.getOrNull(it) }
    val headerAccent = ThemeStyles.accentOnSurface(lineColor, cardBgColor.copy(alpha = 1f), fallbackInk = textColor)

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip / Summary Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerText = if (inspectedPoint != null) {
                "${inspectedPoint.fullMonthLabel} · ${inspectedPoint.count} logs"
            } else {
                "Tap or drag along curve to inspect · Avg ${String.format(java.util.Locale.US, "%.1f", avgCount)}/mo"
            }
            Text(
                text = headerText,
                fontFamily = appFont,
                fontSize = 11.5.sp,
                fontWeight = if (inspectedPoint != null) FontWeight.Bold else FontWeight.Normal,
                color = if (inspectedPoint != null) headerAccent else secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (inspectedPoint != null) {
                Text(
                    text = "Reset",
                    fontFamily = appFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryTextColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { selectedIndex = null }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        val axisStyle = TextStyle(
            color = secondaryTextColor,
            fontSize = 10.sp,
            fontFamily = appFont
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .testTag("monthly_trend_chart")
                .pointerInput(dataPoints) {
                    if (dataPoints.isEmpty()) return@pointerInput
                    val leftMargin = 54f
                    val chartWidth = (size.width - leftMargin).coerceAtLeast(1f)
                    detectTapGestures { offset ->
                        val relX = ((offset.x - leftMargin) / chartWidth).coerceIn(0f, 1f)
                        val idx = (relX * (dataPoints.size - 1)).roundToInt().coerceIn(0, dataPoints.size - 1)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
                .pointerInput(dataPoints) {
                    if (dataPoints.isEmpty()) return@pointerInput
                    val leftMargin = 54f
                    val chartWidth = (size.width - leftMargin).coerceAtLeast(1f)
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val relX = ((change.position.x - leftMargin) / chartWidth).coerceIn(0f, 1f)
                        val idx = (relX * (dataPoints.size - 1)).roundToInt().coerceIn(0, dataPoints.size - 1)
                        selectedIndex = idx
                    }
                }
        ) {
            if (dataPoints.isEmpty()) return@Canvas

            val w = size.width
            val h = size.height
            val leftMargin = 54f
            val rightMargin = 16f
            val topMargin = 12f
            val bottomMargin = 42f
            val chartWidth = (w - leftMargin - rightMargin).coerceAtLeast(1f)
            val chartHeight = (h - topMargin - bottomMargin).coerceAtLeast(1f)

            val maxCountVal = (dataPoints.maxOfOrNull { it.count } ?: 5).coerceAtLeast(1)
            val yMaxVal = (((maxCountVal + 4) / 5) * 5).coerceAtLeast(5)

            val gridLinesCount = 4
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            for (i in 0..gridLinesCount) {
                val yFactor = i.toFloat() / gridLinesCount.toFloat()
                val gridY = topMargin + chartHeight * yFactor
                drawLine(
                    color = gridColor.copy(alpha = 0.55f),
                    start = Offset(leftMargin, gridY),
                    end = Offset(w - rightMargin, gridY),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
                val yValText = (yMaxVal - (yMaxVal * yFactor).roundToInt()).toString()
                drawText(
                    textMeasurer = textMeasurer,
                    text = yValText,
                    style = axisStyle,
                    topLeft = Offset(6f, gridY - 12f)
                )
            }

            // Average Reference Line
            if (avgCount > 0.0) {
                val avgY = topMargin + chartHeight - (chartHeight * (avgCount.toFloat() / yMaxVal.toFloat())).coerceIn(0f, chartHeight)
                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = Offset(leftMargin, avgY),
                    end = Offset(w - rightMargin, avgY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                )
            }

            val stepsX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
            val baselineY = topMargin + chartHeight
            val coordinates = dataPoints.mapIndexed { index, dp ->
                val scaleVal = dp.count.toFloat() / yMaxVal.toFloat()
                val targetY = baselineY - (chartHeight * scaleVal)
                val pointY = baselineY - (baselineY - targetY) * animatedProgress.value
                val pointX = leftMargin + (index * stepsX)
                Offset(pointX, pointY)
            }

            // X-axis month labels
            coordinates.forEachIndexed { idx, point ->
                val shouldDraw = dataPoints.size <= 7 || idx % ((dataPoints.size / 6).coerceAtLeast(1)) == 0
                if (shouldDraw) {
                    val mText = dataPoints[idx].monthLabel
                    val layout = textMeasurer.measure(mText, axisStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = mText,
                        style = axisStyle,
                        topLeft = Offset((point.x - layout.size.width / 2f).coerceIn(leftMargin, w - layout.size.width), baselineY + 10f)
                    )
                }
            }

            // Smooth cubic Bezier path
            val linePath = Path()
            val fillPath = Path()
            coordinates.forEachIndexed { idx, p ->
                if (idx == 0) {
                    linePath.moveTo(p.x, p.y)
                    fillPath.moveTo(p.x, baselineY)
                    fillPath.lineTo(p.x, p.y)
                } else {
                    val prev = coordinates[idx - 1]
                    val midX = (prev.x + p.x) / 2f
                    linePath.cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
                    fillPath.cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
                }
                if (idx == coordinates.lastIndex) {
                    fillPath.lineTo(p.x, baselineY)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                    startY = topMargin,
                    endY = baselineY
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Inspected vertical crosshair
            selectedIndex?.let { selIdx ->
                coordinates.getOrNull(selIdx)?.let { selPt ->
                    drawLine(
                        color = lineColor.copy(alpha = 0.5f),
                        start = Offset(selPt.x, topMargin),
                        end = Offset(selPt.x, baselineY),
                        strokeWidth = 2f
                    )
                    drawCircle(color = lineColor.copy(alpha = 0.25f), radius = 16f, center = selPt)
                }
            }

            coordinates.forEachIndexed { idx, point ->
                val isSelected = selectedIndex == idx
                drawCircle(color = lineColor, radius = if (isSelected) 9f else 6f, center = point)
                drawCircle(color = cardBgColor, radius = if (isSelected) 4.5f else 3f, center = point)
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveDayOfWeekBarChart(
    appFont: FontFamily,
    dataPoints: List<DayOfWeekStat>,
    primaryColor: Color,
    secondaryTextColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
    cardBgColor: Color = Color.Unspecified
) {
    val textMeasurer = rememberTextMeasurer()
    val animatedProgress = remember(dataPoints) { Animatable(0f) }
    var selectedBarIndex by remember(dataPoints) { mutableStateOf<Int?>(null) }
    val inferredSurface = if (cardBgColor != Color.Unspecified) {
        cardBgColor.copy(alpha = 1f)
    } else if (ThemeStyles.isLightColor(secondaryTextColor)) {
        Color(0xFF16181F)
    } else {
        Color(0xFFF5F3EC)
    }
    val summaryAccent = ThemeStyles.accentOnSurface(primaryColor, inferredSurface)

    LaunchedEffect(dataPoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
        )
    }

    val peakIndex = remember(dataPoints) {
        dataPoints.indices.maxByOrNull { dataPoints[it].count } ?: -1
    }
    val inspectedIndex = selectedBarIndex ?: peakIndex.takeIf { it >= 0 && (dataPoints.getOrNull(it)?.count ?: 0) > 0 }
    val inspectedStat = inspectedIndex?.let { dataPoints.getOrNull(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val summary = if (inspectedStat != null) {
                if (selectedBarIndex != null) {
                    "${inspectedStat.dayName}: ${inspectedStat.count} logs"
                } else {
                    "Peak Day: ${inspectedStat.dayName} (${inspectedStat.count} logs)"
                }
            } else {
                "Tap any day bar to inspect"
            }
            Text(
                text = summary,
                fontFamily = appFont,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (inspectedStat != null) summaryAccent else secondaryTextColor
            )
        }

        val axisStyle = TextStyle(
            color = secondaryTextColor,
            fontSize = 10.sp,
            fontFamily = appFont
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .testTag("day_of_week_bar_chart")
                .pointerInput(dataPoints) {
                    if (dataPoints.isEmpty()) return@pointerInput
                    val leftMargin = 54f
                    val rightMargin = 12f
                    val chartWidth = (size.width - leftMargin - rightMargin).coerceAtLeast(1f)
                    detectTapGestures { offset ->
                        val stepX = chartWidth / dataPoints.size
                        val idx = ((offset.x - leftMargin) / stepX).toInt().coerceIn(0, dataPoints.lastIndex)
                        selectedBarIndex = if (selectedBarIndex == idx) null else idx
                    }
                }
        ) {
            if (dataPoints.isEmpty()) return@Canvas

            val w = size.width
            val h = size.height
            val leftMargin = 54f
            val rightMargin = 12f
            val topMargin = 10f
            val bottomMargin = 42f
            val chartWidth = (w - leftMargin - rightMargin).coerceAtLeast(1f)
            val chartHeight = (h - topMargin - bottomMargin).coerceAtLeast(1f)

            val maxCountVal = (dataPoints.maxOfOrNull { it.count } ?: 10).coerceAtLeast(1)
            val yMaxVal = (((maxCountVal + 4) / 5) * 5).coerceAtLeast(5)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            for (i in 0..4) {
                val yFactor = i.toFloat() / 4f
                val gridY = topMargin + chartHeight * yFactor
                drawLine(
                    color = gridColor.copy(alpha = 0.55f),
                    start = Offset(leftMargin, gridY),
                    end = Offset(w - rightMargin, gridY),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
                val yValText = (yMaxVal - (yMaxVal * yFactor).roundToInt()).toString()
                drawText(
                    textMeasurer = textMeasurer,
                    text = yValText,
                    style = axisStyle,
                    topLeft = Offset(6f, gridY - 12f)
                )
            }

            val stepX = chartWidth / dataPoints.size
            val barSpacingFactor = 0.36f
            val barWidth = stepX * (1f - barSpacingFactor)
            val baselineY = topMargin + chartHeight

            dataPoints.forEachIndexed { index, dp ->
                val barHeight = (dp.count.toFloat() / yMaxVal.toFloat() * chartHeight * animatedProgress.value).coerceAtLeast(0f)
                val barLeft = leftMargin + (index * stepX) + (stepX * barSpacingFactor / 2f)
                val barTop = baselineY - barHeight

                val isHighlighted = index == inspectedIndex
                val barColor = if (isHighlighted) primaryColor else primaryColor.copy(alpha = 0.32f)

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                val layout = textMeasurer.measure(dp.dayName, axisStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = dp.dayName,
                    style = axisStyle,
                    topLeft = Offset(barLeft + (barWidth - layout.size.width) / 2f, baselineY + 10f)
                )
            }
        }
    }
}

@Composable
fun WeeklyConsistencyChartCard(
    appFont: FontFamily,
    points: List<WeeklyConsistencyPoint>,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    if (points.isEmpty()) return
    var selectedWeekIdx by remember(points) { mutableStateOf<Int?>(null) }
    val avgPct = remember(points) {
        points.map { it.completionPercent }.average().roundToInt()
    }
    val inspected = selectedWeekIdx?.let { points.getOrNull(it) }

    val isLight = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val accentText = ThemeStyles.accentOnSurface(primaryColor, cardBgColor.copy(alpha = 1f), fallbackInk = textColor)
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(0.5.dp, borderColor),
        shape = DesignTokens.CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .surface3DCard(
                shape = DesignTokens.CardShape,
                isLight = isLight,
                primaryColor = primaryColor,
                elevationDp = 6.dp
            )
            .testTag("weekly_consistency_card")
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "12-WEEK SCHEDULE ADHERENCE",
                        fontFamily = appFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryTextColor
                    )
                    Text(
                        text = if (inspected != null) {
                            "Week of ${inspected.weekLabel}: ${inspected.completedDays}/${inspected.scheduledDays} scheduled (${inspected.completionPercent}%)"
                        } else {
                            "Schedule-aware consistency (tap a bar)"
                        },
                        fontFamily = appFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (inspected != null) accentText else textColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$avgPct% avg",
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentText
                    )
                }
            }

            val barReveal = remember(points) { Animatable(0f) }
            LaunchedEffect(points) {
                barReveal.snapTo(0f)
                barReveal.animateTo(1f, animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEachIndexed { idx, pt ->
                    val isSel = selectedWeekIdx == idx
                    val targetFrac = ((pt.completionPercent / 100f).coerceIn(0.06f, 1f)) * barReveal.value
                    val animatedHeightFrac by animateFloatAsState(
                        targetValue = targetFrac,
                        animationSpec = MotionSpecs.SnappyFloatSpring,
                        label = "week_bar_$idx"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .testTag("weekly_bar_$idx")
                            .clickable { selectedWeekIdx = if (isSel) null else idx },
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedHeightFrac)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(
                                    when {
                                        isSel -> primaryColor
                                        pt.completionPercent >= 80 -> primaryColor.copy(alpha = 0.78f)
                                        pt.completionPercent >= 40 -> primaryColor.copy(alpha = 0.45f)
                                        else -> borderColor.copy(alpha = 0.45f)
                                    }
                                )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = points.first().weekLabel,
                    fontFamily = appFont,
                    fontSize = 10.sp,
                    color = secondaryTextColor
                )
                Text(
                    text = "This week",
                    fontFamily = appFont,
                    fontSize = 10.sp,
                    color = secondaryTextColor
                )
            }
        }
    }
}

@Composable
fun MoodAndRhythmBreakdownCard(
    appFont: FontFamily,
    moods: List<MoodStat>,
    timeOfDay: List<TimeOfDayStat>,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    if (moods.isEmpty() && timeOfDay.isEmpty()) return

    val isLight = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val accentText = ThemeStyles.accentOnSurface(primaryColor, cardBgColor.copy(alpha = 1f), fallbackInk = textColor)
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(0.5.dp, borderColor),
        shape = DesignTokens.CardShape,
        modifier = Modifier
            .fillMaxWidth()
            .surface3DCard(
                shape = DesignTokens.CardShape,
                isLight = isLight,
                primaryColor = primaryColor,
                elevationDp = 6.dp
            )
            .testTag("mood_rhythm_card")
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (timeOfDay.isNotEmpty()) {
                val peakWindow = timeOfDay.maxByOrNull { it.count }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHECK-IN RHYTHM",
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryTextColor
                        )
                        if (peakWindow != null && peakWindow.count > 0) {
                            Text(
                                text = "Peak: ${peakWindow.icon} ${peakWindow.label}",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeOfDay.forEach { slot ->
                            val isPeak = peakWindow?.label == slot.label && slot.count > 0
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPeak) primaryColor.copy(alpha = 0.14f) else borderColor.copy(alpha = 0.14f))
                                    .border(
                                        width = if (isPeak) 1.dp else 0.5.dp,
                                        color = if (isPeak) primaryColor.copy(alpha = 0.5f) else borderColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = slot.icon, fontSize = 15.sp)
                                Text(
                                    text = "${(slot.fraction * 100).roundToInt()}%",
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPeak) accentText else textColor
                                )
                                Text(
                                    text = slot.label,
                                    fontFamily = appFont,
                                    fontSize = 10.sp,
                                    color = secondaryTextColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (moods.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MOOD TAGS ON CHECK-INS",
                        fontFamily = appFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryTextColor
                    )
                    moods.forEach { mood ->
                        val animatedFrac by animateFloatAsState(
                            targetValue = mood.fraction.coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 360),
                            label = "mood_${mood.emoji}"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = mood.emoji, fontSize = 15.sp)
                            LinearProgressIndicator(
                                progress = { animatedFrac },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .recessedWell(RoundedCornerShape(4.dp), isLight = isLight)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = primaryColor,
                                trackColor = borderColor.copy(alpha = 0.25f)
                            )
                            Text(
                                text = "${mood.count} (${(mood.fraction * 100).roundToInt()}%)",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MilestoneProgressRingBadge(
    icon: String,
    progress: Float,
    isUnlocked: Boolean,
    primaryColor: Color,
    trackColor: Color,
    badgeBgColor: Color
) {
    val animatedArc by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 520),
        label = "milestone_ring"
    )
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 4f
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx)
            )
            drawArc(
                color = if (isUnlocked) primaryColor else primaryColor.copy(alpha = 0.7f),
                startAngle = -90f,
                sweepAngle = 360f * animatedArc,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(badgeBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
    }
}
