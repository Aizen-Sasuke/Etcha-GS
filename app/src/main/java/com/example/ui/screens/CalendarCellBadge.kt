package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ThemeStyles

/**
 * Returns true for shapes whose diagonal corners curve inward enough that corner-aligned badges clip.
 */
fun isCurvedCalendarShape(shapeKey: String): Boolean {
    return shapeKey == "circle" || shapeKey == "pill" || shapeKey == "squircle" || shapeKey == "diamond"
}

/**
 * Full-surface soft diagonal sheen for completed calendar cells & preview swatches.
 * Replaces corner-only radial spotlights so the entire shape illuminates uniformly.
 */
fun Modifier.completedCellCornerGlow(primaryColor: Color): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.24f),
            0.48f to primaryColor.copy(alpha = 0.14f),
            1f to primaryColor.copy(alpha = 0.04f),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
}

/**
 * Shared shape-aware completion/partial/mood indicator used in CalendarDayCell and Preferences preview.
 * Prevents corner clipping on circle/pill/squircle/diamond shapes by using centered axis accents
 * and safe 4dp insets on angular shapes.
 */
@Composable
fun BoxScope.CalendarCompletionIndicator(
    dayNumber: Int,
    totalSum: Int,
    targetCount: Int,
    isCompleted: Boolean,
    isPartial: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    hasNotes: Boolean,
    moodTag: String?,
    cellShapeKey: String,
    cellShape: Shape,
    checkmarkStyleName: String,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    accentTextColor: Color = ThemeStyles.accentOnSurface(
        primaryColor,
        if (ThemeStyles.isLightColor(textColor)) Color(0xFF16181F) else Color(0xFFF5F3EC),
        fallbackInk = textColor
    )
) {
    val isCurved = isCurvedCalendarShape(cellShapeKey)
    val isDiamond = cellShapeKey == "diamond"
    val safeCornerInset = 4.dp
    val topCenterPad = if (isDiamond) 5.dp else 3.dp
    val bottomCenterPad = if (isDiamond) 5.dp else 4.dp

    // 1. Optional mood / journal marker
    if (moodTag != null) {
        Text(
            text = moodTag,
            fontSize = if (isDiamond) 7.5.sp else 8.sp,
            modifier = if (isCurved) {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topCenterPad)
            } else {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = safeCornerInset, top = safeCornerInset)
            }
        )
    } else if (hasNotes) {
        Box(
            modifier = (if (isCurved) {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topCenterPad + 1.dp)
            } else {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = safeCornerInset + 1.dp, top = safeCornerInset + 1.dp)
            })
                .size(3.5.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.78f))
        )
    }

    // 2. Filled style concentric inner highlight (works symmetrically on every shape)
    if (isCompleted && checkmarkStyleName == "filled") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .border(1.25.dp, primaryColor.copy(alpha = 0.55f), cellShape)
        )
    }

    val hasBottomAccent = (isCompleted || isPartial) && checkmarkStyleName != "filled"
    val indicatorAlignment = if (isCurved || checkmarkStyleName == "bar") {
        Alignment.BottomCenter
    } else {
        Alignment.TopEnd
    }
    val indicatorPadding = if (isCurved || checkmarkStyleName == "bar") {
        Modifier.padding(bottom = bottomCenterPad)
    } else {
        Modifier.padding(top = safeCornerInset, end = safeCornerInset)
    }

    // 3. Style-specific completion or partial indicator
    if (isCompleted) {
        val dotSize = when {
            totalSum >= 4 -> 9.dp
            totalSum == 3 -> 8.dp
            totalSum == 2 -> 7.dp
            else -> 6.5.dp
        }
        val dotAlpha = when {
            totalSum >= 4 -> 1.0f
            totalSum == 3 -> 0.90f
            totalSum == 2 -> 0.78f
            else -> 0.68f
        }
        val iconColor = accentTextColor

        when (checkmarkStyleName) {
            "dot" -> Box(
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = dotAlpha))
            )
            "tick" -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = iconColor.copy(alpha = dotAlpha),
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
                    .size(dotSize + 3.dp)
            )
            "cross" -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Logged",
                tint = iconColor.copy(alpha = dotAlpha),
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
                    .size(dotSize + 3.dp)
            )
            "number" -> Text(
                text = totalSum.toString(),
                color = iconColor,
                fontSize = 8.5.sp,
                lineHeight = 9.sp,
                fontFamily = appFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
            )
            "ring" -> Box(
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
                    .size(dotSize)
                    .border(1.5.dp, iconColor.copy(alpha = dotAlpha), CircleShape)
            )
            "bar" -> Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomCenterPad)
                    .height(2.5.dp)
                    .fillMaxWidth(if (isDiamond) 0.28f else if (isCurved) 0.36f else 0.46f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(iconColor.copy(alpha = dotAlpha))
            )
        }
    } else if (isPartial) {
        val iconColor = accentTextColor
        when (checkmarkStyleName) {
            "dot" -> Box(
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
                    .size(6.5.dp)
                    .border(1.2.dp, iconColor.copy(alpha = 0.55f), CircleShape)
            )
            "tick" -> Text(
                text = "$totalSum",
                color = iconColor.copy(alpha = 0.80f),
                fontSize = 8.5.sp,
                lineHeight = 9.sp,
                fontFamily = appFont,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
            )
            "cross" -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Partial",
                tint = iconColor.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
                    .size(9.dp)
            )
            "number" -> Text(
                text = "$totalSum",
                color = secondaryTextColor,
                fontSize = 8.5.sp,
                lineHeight = 9.sp,
                fontFamily = appFont,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
            )
            "ring" -> Box(
                modifier = Modifier
                    .align(indicatorAlignment)
                    .then(indicatorPadding)
                    .size(6.5.dp)
                    .border(0.9.dp, iconColor.copy(alpha = 0.45f), CircleShape)
            )
            "bar" -> {
                val safeTarget = targetCount.coerceAtLeast(1)
                val progressFraction = (totalSum.toFloat() / safeTarget.toFloat()).coerceIn(0.15f, 1f)
                val baseWidth = if (isDiamond) 0.28f else if (isCurved) 0.36f else 0.46f
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomCenterPad)
                        .height(2.dp)
                        .fillMaxWidth((baseWidth * progressFraction).coerceAtLeast(0.12f))
                        .clip(RoundedCornerShape(1.dp))
                        .background(iconColor.copy(alpha = 0.45f))
                )
            }
        }
    }

    // 4. Date numeral — centered on curved shapes (nudged up smoothly when bottom accent is active),
    // and centered/bottom-balanced on angular shapes so corner badges never collide.
    val numeralModifier = if (isCurved) {
        val topAdjust = when {
            moodTag != null && hasBottomAccent -> 0.dp
            hasBottomAccent -> (-4).dp
            moodTag != null -> 3.dp
            else -> 0.dp
        }
        Modifier
            .align(Alignment.Center)
            .offset(y = topAdjust)
    } else if (checkmarkStyleName == "bar" && (isCompleted || isPartial)) {
        Modifier
            .align(Alignment.Center)
            .offset(y = (-2).dp)
    } else {
        Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 5.dp)
    }

    Text(
        text = dayNumber.toString(),
        modifier = numeralModifier,
        color = when {
            isToday -> if (isCompleted) textColor else accentTextColor
            isCurrentMonth -> textColor.copy(alpha = if (isCompleted) 0.96f else 0.85f)
            else -> secondaryTextColor.copy(alpha = 0.32f)
        },
        fontSize = if (isCurved && hasBottomAccent) 12.5.sp else 13.5.sp,
        fontFamily = appFont,
        fontWeight = if (isToday || isCompleted) FontWeight.Bold else FontWeight.Medium
    )
}

/**
 * Week-view right status badge aligned with active cell shape and checkmark style.
 */
@Composable
fun CalendarWeekBadge(
    isDone: Boolean,
    isPartial: Boolean,
    isFuture: Boolean,
    totalCount: Int,
    targetCount: Int,
    cellShape: Shape,
    checkmarkStyleName: String,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    appFont: FontFamily,
    accentTextColor: Color = ThemeStyles.accentOnSurface(
        primaryColor,
        if (ThemeStyles.isLightColor(textColor)) Color(0xFF16181F) else Color(0xFFF5F3EC),
        fallbackInk = textColor
    )
) {
    val accentTint = accentTextColor
    when {
        isDone -> {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(cellShape)
                    .background(primaryColor.copy(alpha = if (checkmarkStyleName == "filled") 0.28f else 0.18f))
                    .then(
                        if (checkmarkStyleName == "filled") {
                            Modifier.padding(2.dp).border(1.2.dp, accentTint.copy(alpha = 0.6f), cellShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (checkmarkStyleName) {
                    "cross" -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Done",
                        tint = accentTint,
                        modifier = Modifier.size(16.dp)
                    )
                    "dot" -> Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentTint)
                    )
                    "ring" -> Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(2.dp, accentTint, CircleShape)
                    )
                    "number" -> Text(
                        text = "$totalCount",
                        fontFamily = appFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = accentTint
                    )
                    "bar" -> Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentTint)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = accentTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        isPartial -> {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(cellShape)
                    .background(primaryColor.copy(alpha = 0.10f))
                    .border(0.8.dp, primaryColor.copy(alpha = 0.45f), cellShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$totalCount/$targetCount",
                    fontFamily = appFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = accentTint
                )
            }
        }
        !isFuture -> {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(cellShape)
                    .border(1.dp, borderColor, cellShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = secondaryTextColor, fontSize = 15.sp)
            }
        }
    }
}
