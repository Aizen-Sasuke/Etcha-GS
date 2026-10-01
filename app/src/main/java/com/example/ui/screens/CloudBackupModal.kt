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
    val isSupporter by viewModel.isSupporter.collectAsState()
    
    val googleEmail by viewModel.googleAccountEmail.collectAsState()
    val googleName by viewModel.googleAccountName.collectAsState()
    val lastBackup by viewModel.lastCloudBackupTime.collectAsState()
    val autoBackup by viewModel.autoCloudBackupEnabled.collectAsState()
    val snapshots by viewModel.cloudBackupSnapshots.collectAsState()
    val allTrackers by viewModel.trackers.collectAsState()
    val currentEntries by viewModel.getEntriesForExport().collectAsState(initial = emptyList())

    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)

    val clipboardManager = LocalClipboardManager.current
    var isBackingUp by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var snapshotToRestore by remember { mutableStateOf<CloudBackupSnapshot?>(null) }
    var showRawJsonDialog by remember { mutableStateOf(false) }
    var rawExportJson by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

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
            Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { closeAndDismiss() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                        }
                        Text(
                            text = "Cloud Backup & Sync",
                            fontFamily = appFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = {
                            if (googleEmail != null) {
                                isBackingUp = true
                                viewModel.performCloudBackup { success, msg ->
                                    isBackingUp = false
                                    statusMessage = msg
                                }
                            } else {
                                showConnectDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = primaryColor)
                        }
                    }

                    // Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
                    ) {
                        // 1. Google Account Connection Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (googleEmail != null) primaryColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (googleEmail != null) {
                                                    Text(
                                                        text = (googleName?.firstOrNull() ?: googleEmail?.firstOrNull() ?: 'G').uppercase(),
                                                        fontFamily = appFont,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp,
                                                        color = primaryColor
                                                    )
                                                } else {
                                                    Text("☁️", fontSize = 20.sp)
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = if (googleEmail != null) (googleName ?: "Google Account") else "Google Account Cloud",
                                                    fontFamily = appFont,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                                Text(
                                                    text = googleEmail ?: "Not linked yet",
                                                    fontFamily = appFont,
                                                    fontSize = 12.sp,
                                                    color = if (googleEmail != null) primaryColor else secondaryTextColor
                                                )
                                            }
                                        }

                                        if (googleEmail != null) {
                                            TextButton(onClick = { viewModel.disconnectGoogleAccount() }) {
                                                Text("Unlink", fontFamily = appFont, color = Color(0xFFE57373), fontSize = 12.sp)
                                            }
                                        } else {
                                            Button(
                                                onClick = { showConnectDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text("Connect", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (isSupporter) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFD700).copy(alpha = 0.12f))
                                                .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("⭐", fontSize = 12.sp)
                                                Text("VIP Supporter: Real-Time Priority Cloud Mirroring Active", fontFamily = appFont, fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.SemiBold)
                                            }
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
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(primaryColor.copy(alpha = 0.15f))
                                        .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(msg, fontFamily = appFont, fontSize = 12.sp, color = textColor, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(24.dp)) {
                                            Text("✕", fontSize = 12.sp, color = secondaryTextColor)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Cloud Backup Actions Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Last Cloud Sync", fontFamily = appFont, fontSize = 12.sp, color = secondaryTextColor)
                                            Text(
                                                text = lastBackup ?: "Never backed up",
                                                fontFamily = appFont,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Auto-Sync", fontFamily = appFont, fontSize = 12.sp, color = secondaryTextColor)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Switch(
                                                checked = autoBackup,
                                                onCheckedChange = { viewModel.setAutoCloudBackup(it) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)

                                    Button(
                                        onClick = {
                                            if (googleEmail == null) {
                                                showConnectDialog = true
                                            } else {
                                                isBackingUp = true
                                                viewModel.performCloudBackup { success, msg ->
                                                    isBackingUp = false
                                                    statusMessage = msg
                                                }
                                            }
                                        },
                                        enabled = !isBackingUp,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isBackingUp) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Backing up...", fontFamily = appFont, fontSize = 13.sp)
                                        } else {
                                            Text("☁️ Back Up Now to Google Cloud", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Cloud Snapshots / Restore History
                        item {
                            Text(
                                text = "CLOUD SNAPSHOTS & RESTORE",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }

                        if (snapshots.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.5f)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "No cloud snapshots found yet.\nTap 'Back Up Now' to create your first cloud restore point.",
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
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(snap.displayDate, fontFamily = appFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                Text("Device: ${snap.deviceName}", fontFamily = appFont, fontSize = 11.sp, color = secondaryTextColor)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { snapshotToRestore = snap },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Text("📥", fontSize = 16.sp)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteCloudSnapshot(snap.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(primaryColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                Text("${snap.totalTrackers} Habits", fontFamily = appFont, fontSize = 10.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                                            }
                                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(secondaryTextColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                Text("${snap.totalEntries} Total Logs", fontFamily = appFont, fontSize = 10.sp, color = textColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Local File Backup Options
                        item {
                            Text(
                                text = "LOCAL FILE BACKUP (JSON)",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        rawExportJson = viewModel.exportBackupJson(currentEntries)
                                        showRawJsonDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Text("📄 Export JSON", fontFamily = appFont, fontSize = 12.sp, color = textColor)
                                }

                                OutlinedButton(
                                    onClick = {
                                        importJsonText = ""
                                        showImportDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                                ) {
                                    Text("📂 Import JSON", fontFamily = appFont, fontSize = 12.sp, color = textColor)
                                }
                            }
                        }
                    }
                }

                // Connect Google Account Dialog
                if (showConnectDialog) {
                    var emailInput by remember { mutableStateOf(googleEmail ?: "user@gmail.com") }
                    var nameInput by remember { mutableStateOf(googleName ?: "Habit Builder") }

                    AlertDialog(
                        onDismissRequest = { showConnectDialog = false },
                        containerColor = cardBgColor,
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐 ", fontSize = 18.sp)
                                Text("Connect Google Account", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Link your Google Account to automatically mirror habits, check-ins, journal reflections, and settings securely to Google Cloud.",
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = secondaryTextColor
                                )
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Google Email", fontFamily = appFont, fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Display Name", fontFamily = appFont, fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank()) {
                                        viewModel.connectGoogleAccount(emailInput, nameInput)
                                        showConnectDialog = false
                                        statusMessage = "Google Account linked: $emailInput"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White)
                            ) {
                                Text("Link & Sync", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConnectDialog = false }) {
                                Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                            }
                        }
                    )
                }

                // Restore Confirmation Dialog
                snapshotToRestore?.let { snap ->
                    AlertDialog(
                        onDismissRequest = { snapshotToRestore = null },
                        containerColor = cardBgColor,
                        title = {
                            Text("Restore from Cloud Snapshot?", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                        },
                        text = {
                            Text(
                                "This will restore your habits (${snap.totalTrackers} habits, ${snap.totalEntries} entries) to the state saved on ${snap.displayDate}.\n\nExisting local data will be merged and updated.",
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
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White)
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

                // Raw JSON Export Dialog
                if (showRawJsonDialog) {
                    AlertDialog(
                        onDismissRequest = { showRawJsonDialog = false },
                        containerColor = cardBgColor,
                        title = { Text("Backup JSON", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor) },
                        text = {
                            Column {
                                Text("Copy this JSON text or save it for offline safekeeping:", fontFamily = appFont, fontSize = 12.sp, color = secondaryTextColor)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = rawExportJson,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFont, fontSize = 10.sp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(rawExportJson))
                                    showRawJsonDialog = false
                                    statusMessage = "Backup JSON copied to clipboard!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White)
                            ) {
                                Text("Copy to Clipboard", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRawJsonDialog = false }) {
                                Text("Close", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                            }
                        }
                    )
                }

                // Raw JSON Import Dialog
                if (showImportDialog) {
                    AlertDialog(
                        onDismissRequest = { showImportDialog = false },
                        containerColor = cardBgColor,
                        title = { Text("Import Backup JSON", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor) },
                        text = {
                            Column {
                                Text("Paste your backup JSON below to restore:", fontFamily = appFont, fontSize = 12.sp, color = secondaryTextColor)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = importJsonText,
                                    onValueChange = { importJsonText = it },
                                    placeholder = { Text("Paste JSON here...", fontFamily = appFont, fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFont, fontSize = 11.sp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (importJsonText.isNotBlank()) {
                                        val ok = viewModel.importBackupJson(importJsonText)
                                        showImportDialog = false
                                        statusMessage = if (ok) "Backup imported successfully!" else "Invalid JSON format"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = if (ThemeStyles.isThemeDark(selectedTheme)) Color.Black else Color.White)
                            ) {
                                Text("Import Data", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportDialog = false }) {
                                Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }
        }
    }
}
