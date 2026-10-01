package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles

@Composable
fun OnboardingModal(
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()

    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)
    val isLight = ThemeStyles.isLightTheme(selectedTheme)

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    Dialog(
        onDismissRequest = {
            viewModel.setHasSeenOnboarding(true)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .testTag("onboarding_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step Indicator Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(totalSteps) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == currentStep) 20.dp else 8.dp, 8.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == currentStep) primaryColor else borderColor)
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.setHasSeenOnboarding(true)
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "Skip",
                                fontFamily = appFont,
                                fontSize = 13.sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    // Content Slides
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "onboarding_step"
                    ) { step ->
                        when (step) {
                            0 -> OnboardingStepView(
                                icon = "✨",
                                title = "Welcome to Habit Tracker",
                                subtitle = "Your distraction-free, privacy-first companion to build lasting routines and master your discipline.",
                                bulletPoints = listOf(
                                    "💪 Positive Habits" to "Track daily streaks & build consistency",
                                    "🛡️ Sobriety & Vice Avoidance" to "Log clean days and discreet slips",
                                    "📊 Flexible Goals" to "Daily, weekdays, and multi-count targets"
                                ),
                                appFont = appFont,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                primaryColor = primaryColor,
                                cardBg = cardBgColor
                            )
                            1 -> OnboardingStepView(
                                icon = "🌱",
                                title = "Habit Strength Score",
                                subtitle = "Never feel demoralized by a single missed day. Our smart health algorithm measures long-term momentum.",
                                bulletPoints = listOf(
                                    "📈 Exponential Smoothing" to "Consistency stays high even if you miss once",
                                    "💎 Tier Progression" to "Level up from 🌱 Starting to 💎 Mastered",
                                    "🧊 Streak Freeze" to "Protect streaks on sick, rest, or travel days"
                                ),
                                appFont = appFont,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                primaryColor = primaryColor,
                                cardBg = cardBgColor
                            )
                            2 -> OnboardingStepView(
                                icon = "✍️",
                                title = "Routines & Daily Journal",
                                subtitle = "Organize habits by time of day and capture your daily reflections and emotional context.",
                                bulletPoints = listOf(
                                    "🌅 Routine Filtering" to "Filter habits: Morning, Afternoon, Evening",
                                    "🦉 Night Owl Cutoff" to "Midnight to 4 AM rollover offset",
                                    "📝 Daily Reflections" to "Jot down wins, notes, and mood tags"
                                ),
                                appFont = appFont,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                primaryColor = primaryColor,
                                cardBg = cardBgColor
                            )
                            3 -> OnboardingStepView(
                                icon = "🎨",
                                title = "Personalized & 100% Offline",
                                subtitle = "Tailor every visual aspect. Your data is stored strictly on your device in local SQLite with full export capabilities.",
                                bulletPoints = listOf(
                                    "🎭 Curated Themes" to "10 Dark, 10 Light, and Special Aesthetics",
                                    "⚡ Home Screen Widget" to "Check in with 1 tap directly from launcher",
                                    "🔒 Total Privacy" to "Zero accounts, zero trackers, offline backup"
                                ),
                                appFont = appFont,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                primaryColor = primaryColor,
                                cardBg = cardBgColor
                            )
                        }
                    }

                    // Bottom Action Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Text(
                                    text = "Back",
                                    fontFamily = appFont,
                                    fontSize = 14.sp,
                                    color = textColor
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStep < totalSteps - 1) {
                                    currentStep++
                                } else {
                                    viewModel.setHasSeenOnboarding(true)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.weight(if (currentStep > 0) 1.5f else 1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (currentStep == totalSteps - 1) "Get Started 🚀" else "Next →",
                                fontFamily = appFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (primaryColor == Color.White) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepView(
    icon: String,
    title: String,
    subtitle: String,
    bulletPoints: List<Pair<String, String>>,
    appFont: androidx.compose.ui.text.font.FontFamily,
    textColor: Color,
    secondaryTextColor: Color,
    primaryColor: Color,
    cardBg: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.15f))
                .border(1.dp, primaryColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 32.sp)
        }

        Text(
            text = title,
            fontFamily = appFont,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            fontFamily = appFont,
            fontSize = 12.sp,
            color = secondaryTextColor,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(primaryColor.copy(alpha = 0.06f))
                .border(0.5.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bulletPoints.forEach { (heading, desc) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "•", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Column {
                        Text(
                            text = heading,
                            fontFamily = appFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = desc,
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
    }
}
