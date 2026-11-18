package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.example.ui.BackupImportMode
import com.example.ui.BackupParseResult
import com.example.ui.BackupPreview
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import java.io.File
import java.time.LocalDate

@Stable
class JsonBackupFlowController(
    private val viewModel: HabitViewModel,
    private val onStatus: (String) -> Unit
) {
    var showExportDialog by mutableStateOf(false)
    var exportJsonText by mutableStateOf("")
    var exportHabitCount by mutableIntStateOf(0)
    var exportEntryCount by mutableIntStateOf(0)

    var showImportDialog by mutableStateOf(false)
    var importJsonInput by mutableStateOf("")
    var importError by mutableStateOf<String?>(null)
    var pendingPreview by mutableStateOf<BackupPreview?>(null)
    var isApplyingImport by mutableStateOf(false)

    fun openExport() {
        viewModel.prepareFullExportJson { json, habits, entries ->
            exportJsonText = json
            exportHabitCount = habits
            exportEntryCount = entries
            showExportDialog = true
        }
    }

    fun openImportPaste() {
        importJsonInput = ""
        importError = null
        pendingPreview = null
        showImportDialog = true
    }

    fun validateAndPreview(rawJson: String) {
        when (val result = viewModel.previewBackupImport(rawJson)) {
            is BackupParseResult.Valid -> {
                importError = null
                showImportDialog = false
                pendingPreview = result.preview
            }
            is BackupParseResult.Error -> {
                importError = result.message
                showImportDialog = true
            }
        }
    }

    fun applyImport(mode: BackupImportMode) {
        val preview = pendingPreview ?: return
        isApplyingImport = true
        viewModel.applyBackupImport(preview, mode) { _, message ->
            isApplyingImport = false
            pendingPreview = null
            onStatus(message)
        }
    }
}

@Composable
fun rememberJsonBackupController(
    viewModel: HabitViewModel,
    onStatus: (String) -> Unit
): JsonBackupFlowController {
    return remember(viewModel) { JsonBackupFlowController(viewModel, onStatus) }
}

@Composable
fun JsonBackupDialogsHost(
    controller: JsonBackupFlowController,
    selectedTheme: String,
    primaryColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    appFont: FontFamily,
    onStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isLightCard = ThemeStyles.isLightColor(cardBgColor.copy(alpha = 1f))
    val onPrimaryColor = ThemeStyles.contrastOn(primaryColor)
    val accentTextColor = ThemeStyles.accentOnSurface(primaryColor, cardBgColor, fallbackInk = textColor)
    val dangerColor = ThemeStyles.semanticDangerColor(isLightCard)

    // SAF Save File launcher (Zero storage permissions)
    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(controller.exportJsonText.toByteArray(Charsets.UTF_8))
                }
                controller.showExportDialog = false
                onStatus("Saved backup JSON file successfully.")
            } catch (e: Exception) {
                onStatus("Failed to write file: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    // SAF Open File launcher (Zero storage permissions)
    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val readText = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                controller.importJsonInput = readText
                controller.validateAndPreview(readText)
            } catch (e: Exception) {
                controller.importError = "Could not read file: ${e.localizedMessage ?: "Unknown error"}"
                controller.showImportDialog = true
            }
        }
    }

    // 1. Export JSON Dialog
    if (controller.showExportDialog) {
        AlertDialog(
            onDismissRequest = { controller.showExportDialog = false },
            containerColor = cardBgColor,
            shape = DesignTokens.CardShape,
            title = {
                Text(
                    text = "Export Full Backup JSON",
                    fontFamily = appFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(DesignTokens.SmallBadgeRadius.let(::RoundedCornerShape))
                                .background(primaryColor.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${controller.exportHabitCount} Habits",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentTextColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(DesignTokens.SmallBadgeRadius))
                                .background(borderColor.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${controller.exportEntryCount} Logs & Settings",
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                color = textColor
                            )
                        }
                    }

                    OutlinedTextField(
                        value = controller.exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = DesignTokens.ControlShape,
                        textStyle = TextStyle(fontFamily = appFont, fontSize = 10.sp, color = textColor)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val fileName = "etcha_backup_${LocalDate.now()}.json"
                                saveJsonLauncher.launch(fileName)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = onPrimaryColor
                            ),
                            shape = DesignTokens.ControlShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("export_json_save_file_button")
                        ) {
                            Text("Save .json File", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(controller.exportJsonText))
                                controller.showExportDialog = false
                                onStatus("Full backup JSON copied to clipboard.")
                            },
                            shape = DesignTokens.ControlShape,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("export_json_copy_button")
                        ) {
                            Text("Copy JSON", fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = textColor)
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        try {
                            val file = File(context.cacheDir, "etcha_backup_${LocalDate.now()}.json")
                            file.writeText(controller.exportJsonText)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = ShareCompat.IntentBuilder(context)
                                .setType("application/json")
                                .setStream(uri)
                                .setChooserTitle("Share Etcha Backup")
                                .createChooserIntent()
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(intent)
                            controller.showExportDialog = false
                        } catch (e: Exception) {
                            onStatus("Share failed: ${e.localizedMessage ?: "Unknown error"}")
                        }
                    },
                    shape = DesignTokens.ControlShape,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.testTag("export_json_share_button")
                ) {
                    Text("Share File", fontFamily = appFont, fontSize = 12.sp, color = textColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { controller.showExportDialog = false }) {
                    Text("Close", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                }
            }
        )
    }

    // 2. Import JSON Input Dialog (Pick File or Paste JSON)
    if (controller.showImportDialog) {
        AlertDialog(
            onDismissRequest = { controller.showImportDialog = false },
            containerColor = cardBgColor,
            shape = DesignTokens.CardShape,
            title = {
                Text(
                    text = "Import Backup JSON",
                    fontFamily = appFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = DesignTokens.ControlShape,
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("import_json_pick_file_button")
                    ) {
                        Text(
                            text = "📂 Pick .json Backup File",
                            fontFamily = appFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = accentTextColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = borderColor.copy(alpha = 0.5f))
                        Text("OR PASTE JSON", fontFamily = appFont, fontSize = 10.sp, color = secondaryTextColor)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = borderColor.copy(alpha = 0.5f))
                    }

                    OutlinedTextField(
                        value = controller.importJsonInput,
                        onValueChange = {
                            controller.importJsonInput = it
                            controller.importError = null
                        },
                        placeholder = { Text("Paste Etcha or HabitTracker JSON here...", fontFamily = appFont, fontSize = 12.sp) },
                        shape = DesignTokens.ControlShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_json_text_field"),
                        textStyle = TextStyle(fontFamily = appFont, fontSize = 11.sp, color = textColor)
                    )

                    controller.importError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DesignTokens.ChipRadius))
                                .background(dangerColor.copy(alpha = 0.14f))
                                .border(0.8.dp, dangerColor.copy(alpha = 0.5f), RoundedCornerShape(DesignTokens.ChipRadius))
                                .padding(10.dp)
                                .testTag("import_json_error_banner")
                        ) {
                            Text(
                                text = err,
                                fontFamily = appFont,
                                fontSize = 11.5.sp,
                                color = dangerColor
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { controller.validateAndPreview(controller.importJsonInput) },
                    enabled = controller.importJsonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = onPrimaryColor
                    ),
                    shape = DesignTokens.ControlShape,
                    modifier = Modifier.testTag("import_json_review_button")
                ) {
                    Text("Review Backup", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { controller.showImportDialog = false }) {
                    Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                }
            }
        )
    }

    // 3. Import Preview & Merge / Replace Confirmation Dialog
    controller.pendingPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!controller.isApplyingImport) controller.pendingPreview = null },
            containerColor = cardBgColor,
            shape = DesignTokens.CardShape,
            title = {
                Text(
                    text = "Confirm Backup Import",
                    fontFamily = appFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = preview.formatLabel,
                        fontFamily = appFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentTextColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreviewStatPill("${preview.habitCount} Habits", primaryColor, textColor, borderColor, appFont, Modifier.weight(1f))
                        PreviewStatPill("${preview.entryCount} Logs", primaryColor, textColor, borderColor, appFont, Modifier.weight(1f))
                        PreviewStatPill("${preview.preferenceCount} Settings", primaryColor, textColor, borderColor, appFont, Modifier.weight(1f))
                    }

                    if (preview.isLegacySingleTrackerOnly) {
                        Text(
                            text = "This legacy backup contains entry logs without habit definitions. Choosing Merge adds logs into your selected habit.",
                            fontFamily = appFont,
                            fontSize = 11.5.sp,
                            color = secondaryTextColor
                        )
                    } else {
                        Text(
                            text = "Choose how to apply this backup:\n• Merge adds missing habits and keeps your highest log counts.\n• Replace overwrites all current habits, logs, and settings.",
                            fontFamily = appFont,
                            fontSize = 11.5.sp,
                            color = secondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { controller.applyImport(BackupImportMode.MERGE) },
                            enabled = !controller.isApplyingImport,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = onPrimaryColor
                            ),
                            shape = DesignTokens.ControlShape,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("import_preview_merge_button")
                        ) {
                            Text("Merge Data", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (!preview.isLegacySingleTrackerOnly) {
                            OutlinedButton(
                                onClick = { controller.applyImport(BackupImportMode.REPLACE) },
                                enabled = !controller.isApplyingImport,
                                shape = DesignTokens.ControlShape,
                                border = BorderStroke(1.dp, dangerColor.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("import_preview_replace_button")
                            ) {
                                Text(
                                    text = "Replace All",
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = dangerColor
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { controller.pendingPreview = null },
                    enabled = !controller.isApplyingImport
                ) {
                    Text("Cancel", fontFamily = appFont, color = secondaryTextColor, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun PreviewStatPill(
    label: String,
    primaryColor: Color,
    textColor: Color,
    borderColor: Color,
    appFont: FontFamily,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DesignTokens.ChipRadius))
            .background(primaryColor.copy(alpha = 0.10f))
            .border(0.5.dp, borderColor, RoundedCornerShape(DesignTokens.ChipRadius))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = appFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
