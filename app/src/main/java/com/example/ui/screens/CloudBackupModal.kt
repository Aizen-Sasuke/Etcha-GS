package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.CloudBackupSnapshot
import com.example.ui.components.LiquidAmbientBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupModal(
    viewModel: HabitViewModel,
    onClose: () -> Unit,
    onOpenDonate: () -> Unit = {}
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val lastBackup by viewModel.lastCloudBackupTime.collectAsState()
    val snapshots by viewModel.cloudBackupSnapshots.collectAsState()

    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val modalBgColor = ThemeStyles.getOpaqueCardColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme, customBgHex)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = ThemeStyles.getTextColor(selectedTheme, customBgHex)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme, customBgHex)
    val isDark = ThemeStyles.isThemeDark(selectedTheme, customBgHex)

    val clipboardManager = LocalClipboardManager.current
    var isBackingUp by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var snapshotToRestore by remember { mutableStateOf<CloudBackupSnapshot?>(null) }
    val jsonBackupController = rememberJsonBackupController(
        viewModel = viewModel,
        onStatus = { statusMessage = it }
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scope = rememberCoroutineScope()
    val closeAndDismiss = {
        visible = false
        scope.launch {
            delay(200)
            onClose()
        }
    }

    Dialog(
        onDismissRequest = { closeAndDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(tween(280)),
            exit = slideOutVertically { it } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            LiquidAmbientBackdrop(
                selectedTheme = selectedTheme,
                customPrimaryHex = customHex,
                customBackgroundHex = customBgHex,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar — symmetric 48dp left/right touch targets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { closeAndDismiss() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                        }
                        Text(
                            text = "Local Snapshots & Backup",
                            fontFamily = appFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = {
                                isBackingUp = true
                                viewModel.performCloudBackup { _, msg ->
                                    isBackingUp = false
                                    statusMessage = msg
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Create Snapshot", tint = primaryColor)
                        }
                    }

                    // Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = DesignTokens.ScreenHorizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SectionSpacing),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                    ) {
                        // 1. Honest Storage Information Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                shape = DesignTokens.CardShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(DesignTokens.CardPadding)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("💾", fontSize = 20.sp)
                                        }

                                        Column {
                                            Text(
                                                text = "Local Storage & Snapshots",
                                                fontFamily = appFont,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                            Text(
                                                text = "All data is stored private and offline on this device",
                                                fontFamily = appFont,
                                                fontSize = 12.sp,
                                                color = secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Status notification banner if any
                        statusMessage?.let { msg ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(DesignTokens.ControlShape)
                                        .background(primaryColor.copy(alpha = 0.15f))
                                        .border(1.dp, primaryColor.copy(alpha = 0.4f), DesignTokens.ControlShape)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(msg, fontFamily = appFont, fontSize = 12.sp, color = textColor, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(32.dp)) {
                                            Text("✕", fontSize = 12.sp, color = secondaryTextColor)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Snapshot Actions Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                shape = DesignTokens.CardShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(DesignTokens.CardPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Last Snapshot", fontFamily = appFont, fontSize = 12.sp, color = secondaryTextColor)
                                            Text(
                                                text = lastBackup ?: "No snapshot created yet",
                                                fontFamily = appFont,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)

                                    Button(
                                        onClick = {
                                            isBackingUp = true
                                            viewModel.performCloudBackup { _, msg ->
                                                isBackingUp = false
                                                statusMessage = msg
                                            }
                                        },
                                        enabled = !isBackingUp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White),
                                        shape = DesignTokens.ControlShape
                                    ) {
                                        if (isBackingUp) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Saving snapshot...", fontFamily = appFont, fontSize = 13.sp)
                                        } else {
                                            Text("💾 Create Device Restore Point", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Saved Snapshots
                        item {
                            Text(
                                text = "SAVED RESTORE POINTS",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        if (snapshots.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.5f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                    shape = DesignTokens.CardShape,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "No saved restore points found yet.\nTap 'Create Device Restore Point' to create a local snapshot.",
                                            fontFamily = appFont,
                                            fontSize = 12.sp,
                                            color = secondaryTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(snapshots) { snap ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                    shape = DesignTokens.CardShape,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(DesignTokens.CardPadding)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(snap.displayDate, fontFamily = appFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                Text("Device: ${snap.deviceName}", fontFamily = appFont, fontSize = 11.sp, color = secondaryTextColor)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { snapshotToRestore = snap },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Text("📥", fontSize = 18.sp)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteCloudSnapshot(snap.id) },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Snapshot", tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(DesignTokens.SmallBadgeRadius)).background(primaryColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                Text("${snap.totalTrackers} Habits", fontFamily = appFont, fontSize = 10.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                                            }
                                            Box(modifier = Modifier.clip(RoundedCornerShape(DesignTokens.SmallBadgeRadius)).background(secondaryTextColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                Text("${snap.totalEntries} Total Logs", fontFamily = appFont, fontSize = 10.sp, color = textColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Local File Backup Options (Full Backup JSON + File Picker)
                        item {
                            Text(
                                text = "FULL PORTABLE JSON BACKUP",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { jsonBackupController.openExport() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = DesignTokens.ControlShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Text("📄 Export JSON", fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = textColor)
                                }

                                OutlinedButton(
                                    onClick = { jsonBackupController.openImportPaste() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = DesignTokens.ControlShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Text("📂 Import JSON", fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = textColor)
                                }
                            }
                        }
                    }
                }

                // Restore Confirmation Dialog
                snapshotToRestore?.let { snap ->
                    AlertDialog(
                        onDismissRequest = { snapshotToRestore = null },
                        containerColor = modalBgColor,
                        shape = DesignTokens.CardShape,
                        title = {
                            Text("Restore from Local Snapshot?", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                        },
                        text = {
                            Text(
                                "This will restore your habits (${snap.totalTrackers} habits, ${snap.totalEntries} entries) to the state saved on ${snap.displayDate}.\n\nExisting local data will be restored.",
                                fontFamily = appFont,
                                fontSize = 13.sp,
                                color = textColor
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val s = snapshotToRestore!!
                                    snapshotToRestore = null
                                    viewModel.restoreFromCloudSnapshot(s) { ok ->
                                        statusMessage = if (ok) "Successfully restored from ${s.displayDate}" else "Restore failed"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (isDark) Color.Black else Color.White),
                                shape = DesignTokens.ControlShape
                            ) {
                                Text("Confirm Restore", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { snapshotToRestore = null }) {
                                Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                            }
                        }
                    )
                }

                JsonBackupDialogsHost(
                    controller = jsonBackupController,
                    selectedTheme = selectedTheme,
                    primaryColor = primaryColor,
                    cardBgColor = modalBgColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    appFont = appFont,
                    onStatus = { statusMessage = it }
                )
            }
        }
    }
}
