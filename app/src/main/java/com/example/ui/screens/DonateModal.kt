package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import java.time.format.DateTimeFormatter

@Composable
fun DonateModal(
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val isSupporter by viewModel.isSupporter.collectAsState()
    val vaultFreezes by viewModel.supporterVaultFreezes.collectAsState()
    val badgeEnabled by viewModel.supporterBadgeEnabled.collectAsState()
    val streakFreezeDate by viewModel.streakFreezeDate.collectAsState()

    val palette = remember(selectedTheme, customHex, customBgHex) {
        ThemeStyles.resolvePalette(selectedTheme, customHex, customBgHex)
    }
    val cardBgColor = palette.modalSurface
    val neutralWell = palette.neutralWell
    val borderColor = palette.border
    val primaryColor = palette.primary
    val accentTextColor = palette.accentOnSurface
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = palette.textPrimary
    val secondaryTextColor = palette.textSecondary
    val isLight = !palette.isDark
    val onPrimaryColor = palette.onPrimary
    val vaultAccent = if (isLight) Color(0xFF9A5B00) else Color(0xFFFFD54F)
    val hapticFeedback = LocalHapticFeedback.current

    val todayStr = remember {
        viewModel.getEffectiveToday().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    val isTodayFrozen = streakFreezeDate == todayStr

    var selectedTier by remember { mutableStateOf("builder") }
    var showThankYou by remember { mutableStateOf(false) }
    var vaultFeedback by remember { mutableStateOf<String?>(null) }

    val supporterModes = listOf(
        Triple("coffee", "☕ Coffee Cheer", "Supporter Badge"),
        Triple("builder", "🚀 Daily Builder", "Bonus Themes + Vault"),
        Triple("champion", "💎 Streak Champion", "All Offline Perks")
    )

    val perks = listOf(
        "🎨" to "Bonus Theme Collection: 24K Gold, Nebula Opal, Emerald Elite, Rose Diamond & more",
        "🛡️" to "Streak Freeze Vault: 3+ bonus safety freezes to protect active streaks",
        "💾" to "Local Snapshots & Backup: One-tap device restore points & JSON backup",
        "⭐" to "Supporter Emblem: Optional badge highlight in your profile header",
        "📊" to "Deep Trend & Rhythm Insights: Weekly consistency, mood breakdown & peak hours",
        "📖" to "Rich Daily Journal: Reflection prompts & mood distribution tracking"
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
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .wrapContentHeight()
                    .testTag("donate_modal")
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Text(text = "💖", fontSize = 22.sp)
                                Text(
                                    text = "Supporter Edition & Vault",
                                    fontFamily = appFont,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = secondaryTextColor)
                            }
                        }
                    }

                    if (showThankYou || isSupporter) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(vaultAccent.copy(alpha = 0.12f))
                                    .border(1.dp, vaultAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "🎉 ⭐ 💖", fontSize = 28.sp)
                                Text(
                                    text = "Supporter Edition Active!",
                                    fontFamily = appFont,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = vaultAccent,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "All bonus themes, device snapshots, and your streak freeze vault are unlocked locally on this device.",
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(vaultAccent.copy(alpha = 0.18f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🛡️ Vault Freezes: $vaultFreezes available",
                                        fontFamily = appFont,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = vaultAccent
                                    )
                                }
                            }
                        }

                        // Interactive Vault Freeze & Emblem Controls
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(neutralWell)
                                    .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "STREAK FREEZE VAULT",
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryTextColor
                                )
                                Text(
                                    text = if (isTodayFrozen) {
                                        "Today's streak is already protected with a Freeze."
                                    } else {
                                        "Use 1 Vault Freeze to protect your good-habit streak today without breaking momentum."
                                    },
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = textColor,
                                    lineHeight = 16.sp
                                )

                                if (!vaultFeedback.isNullOrBlank()) {
                                    Text(
                                        text = vaultFeedback!!,
                                        fontFamily = appFont,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentTextColor
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.useVaultFreeze { ok, msg ->
                                                vaultFeedback = msg
                                                if (ok) {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        },
                                        enabled = !isTodayFrozen && vaultFreezes > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = primaryColor,
                                            contentColor = onPrimaryColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .heightIn(min = 48.dp)
                                            .testTag("use_vault_freeze_button")
                                    ) {
                                        Text(
                                            text = if (isTodayFrozen) "🧊 Protected" else "🛡️ Use Freeze",
                                            fontFamily = appFont,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (vaultFreezes < 5) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.refillVaultFreezes()
                                                vaultFeedback = "Vault refilled to 5 freezes."
                                            },
                                            border = BorderStroke(0.8.dp, borderColor),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .heightIn(min = 48.dp)
                                                .testTag("refill_vault_button")
                                        ) {
                                            Text(
                                                text = "🔄 Refill (5)",
                                                fontFamily = appFont,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = borderColor.copy(alpha = 0.4f), thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Supporter Emblem Badge",
                                            fontFamily = appFont,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                        Text(
                                            text = "Display a subtle ⭐ badge in your Home header",
                                            fontFamily = appFont,
                                            fontSize = 11.sp,
                                            color = secondaryTextColor
                                        )
                                    }
                                    Switch(
                                        checked = badgeEnabled,
                                        onCheckedChange = { viewModel.setSupporterBadgeEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = onPrimaryColor,
                                            checkedTrackColor = primaryColor,
                                            uncheckedThumbColor = secondaryTextColor,
                                            uncheckedTrackColor = neutralWell
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor,
                                        contentColor = onPrimaryColor
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = "Continue with Bonus Vault ✨",
                                        fontFamily = appFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.setSupporter(false)
                                        showThankYou = false
                                    },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Text(
                                        text = "Turn off Supporter Preview",
                                        fontFamily = appFont,
                                        fontSize = 12.sp,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(primaryColor.copy(alpha = 0.08f))
                                    .border(0.5.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "INCLUDED IN SUPPORTER EDITION (NO CHARGE · 100% OFFLINE):",
                                    fontFamily = appFont,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentTextColor
                                )
                                perks.forEach { (emoji, desc) ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = emoji, fontSize = 13.sp)
                                        Text(
                                            text = desc,
                                            fontFamily = appFont,
                                            fontSize = 11.5.sp,
                                            color = textColor,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                supporterModes.forEach { (tKey, tLabel, tTag) ->
                                    val isSelected = selectedTier == tKey
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else neutralWell)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) primaryColor else borderColor,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedTier = tKey }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tLabel,
                                            fontFamily = appFont,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) primaryColor else borderColor.copy(alpha = 0.5f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = tTag,
                                                fontFamily = appFont,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) onPrimaryColor else textColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    viewModel.setSupporter(true)
                                    showThankYou = true
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = onPrimaryColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = "✨ Activate Supporter Edition & Vault",
                                    fontFamily = appFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
