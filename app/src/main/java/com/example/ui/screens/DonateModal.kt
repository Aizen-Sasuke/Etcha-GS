package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles

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

    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)
    val isLight = ThemeStyles.isLightTheme(selectedTheme)
    val hapticFeedback = LocalHapticFeedback.current

    var selectedTier by remember { mutableStateOf("boost") }
    var showThankYou by remember { mutableStateOf(false) }

    val tipTiers = listOf(
        Triple("coffee", "☕ Coffee Tip", "$2"),
        Triple("pizza", "🍕 Pizza Slice", "$5"),
        Triple("boost", "🚀 VIP Supporter", "$10"),
        Triple("patron", "💎 Lifetime Patron", "$25")
    )

    val perks = listOf(
        "👑" to "VIP Themes: 24K Gold, Nebula Opal, Emerald Elite, Rose Diamond & more",
        "🛡️" to "Streak Freeze Vault: 3+ bonus safety freezes & automatic slip forgiveness",
        "💾" to "Local Snapshots & Backup: One-tap device restore points & JSON export",
        "⭐" to "VIP Profile Badge: Exclusive gleaming supporter emblem in top header",
        "📊" to "Advanced Synergy & AI Insights: Peak hour efficiency & habit correlations",
        "📖" to "Rich Daily Journal: Deep reflection prompts & mood distribution analytics"
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
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "💖", fontSize = 22.sp)
                                Text(
                                    text = "Support & VIP Perks",
                                    fontFamily = appFont,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = secondaryTextColor)
                            }
                        }
                    }

                    if (showThankYou || isSupporter) {
                        // Thank you view
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFFD700).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🎉 ⭐ 💖", fontSize = 28.sp)
                                Text(
                                    text = "You're an Official Supporter!",
                                    fontFamily = appFont,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "All VIP themes, device snapshots & backup, bonus freeze vault, and exclusive perks are permanently unlocked on your account.",
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFD700).copy(alpha = 0.25f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Vault Freezes: $vaultFreezes available",
                                            fontFamily = appFont,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Enjoy VIP Experience ✨",
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White
                                )
                            }
                        }
                    } else {
                        // Supporter Perks List
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
                                    text = "EXCLUSIVE SUPPORTER PERKS:",
                                    fontFamily = appFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
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
                                            fontSize = 11.sp,
                                            color = textColor,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Donation Tiers
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tipTiers.forEach { (tKey, tLabel, tPrice) ->
                                    val isSelected = selectedTier == tKey
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else (if (isLight) Color(0xFFEEEEEE) else Color(0xFF1B1B1B)))
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
                                            color = textColor
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) primaryColor else borderColor.copy(alpha = 0.5f))
                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = tPrice,
                                                fontFamily = appFont,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) (if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White) else textColor
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
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💖 Unlock VIP & Support Project",
                                    fontFamily = appFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
