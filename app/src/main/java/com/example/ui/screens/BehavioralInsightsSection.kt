package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CorrelationInsightsSnapshot
import com.example.ui.ThemeStyles
import com.example.ui.components.surface3DCard

@Composable
fun BehavioralInsightsSection(
    insights: CorrelationInsightsSnapshot,
    appFont: FontFamily,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    cardStroke: BorderStroke,
    textColor: Color,
    secondaryTextColor: Color,
    isLight: Boolean,
    isGlass: Boolean,
    onAdjustReminder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryAccent = ThemeStyles.accentOnSurface(primaryColor, cardBgColor.copy(alpha = 1f), fallbackInk = textColor)
    val dangerColor = ThemeStyles.semanticDangerColor(isLight)
    val positiveColor = ThemeStyles.semanticPositiveColor(isLight)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = cardStroke,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .surface3DCard(
                shape = RoundedCornerShape(16.dp),
                isLight = isLight,
                primaryColor = primaryColor,
                isGlass = isGlass,
                elevationDp = 6.dp
            )
            .testTag("behavioral_insights_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🧠", fontSize = 18.sp)
                    Column {
                        Text(
                            text = "Behavioral Insights",
                            fontFamily = appFont,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "90-day correlation & weekday drop-off patterns",
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }

            if (!insights.hasEnoughData) {
                val progress = (insights.loggedDaysCount.toFloat() / insights.requiredDaysCount.coerceAtLeast(1).toFloat())
                    .coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Log ${insights.requiredDaysCount} distinct days to unlock habit pairings",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${insights.loggedDaysCount}/${insights.requiredDaysCount}d",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = primaryColor,
                        trackColor = borderColor.copy(alpha = 0.25f)
                    )
                }
            } else {
                insights.weekdayDropOff?.let { drop ->
                    Surface(
                        color = primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📉 ${drop.weakestDayName} Drop-Off",
                                    fontFamily = appFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(dangerColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "-${drop.dropPercentPoints}% vs avg",
                                        fontFamily = appFont,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dangerColor
                                    )
                                }
                            }
                            Text(
                                text = "${drop.trackerIcon} ${drop.trackerTitle} drops to ${drop.weakestDayRatePercent}% on ${drop.weakestDayName}s (weekly avg ${drop.weeklyAverageRatePercent}%).",
                                fontFamily = appFont,
                                fontSize = 12.sp,
                                color = secondaryTextColor
                            )
                            OutlinedButton(
                                onClick = { onAdjustReminder(drop.trackerId) },
                                border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("insights_adjust_reminder_button")
                            ) {
                                Text(
                                    text = "⏰ Adjust Reminder",
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryAccent
                                )
                            }
                        }
                    }
                }

                if (insights.topPairings.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Cross-Habit Pairings",
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryTextColor
                        )
                        insights.topPairings.forEach { pairing ->
                            val liftPrefix = if (pairing.liftPercentPoints > 0) "+" else ""
                            val badgeColor = if (pairing.isPositive) positiveColor else dangerColor
                            Surface(
                                color = borderColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, borderColor.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "${pairing.sourceIcon} ${pairing.sourceTitle} → ${pairing.targetIcon} ${pairing.targetTitle}",
                                            fontFamily = appFont,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pairing.withRatePercent}% on days completed vs ${pairing.withoutRatePercent}% otherwise",
                                            fontFamily = appFont,
                                            fontSize = 11.sp,
                                            color = secondaryTextColor
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeColor.copy(alpha = 0.16f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${liftPrefix}${pairing.liftPercentPoints}%",
                                            fontFamily = appFont,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (insights.weekdayDropOff == null) {
                    Text(
                        text = "Consistent routine across all weekdays — no major drop-offs detected yet.",
                        fontFamily = appFont,
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )
                }
            }
        }
    }
}
