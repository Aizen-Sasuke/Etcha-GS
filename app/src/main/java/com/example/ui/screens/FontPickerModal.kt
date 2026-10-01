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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun FontPickerModal(
    viewModel: HabitViewModel,
    onClose: () -> Unit
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

    val fonts = listOf(
        "cursive"        to "Cursive",
        "serif"          to "Serif",
        "monospace"      to "Monospace",
        "sans_serif"     to "Sans Serif",
        "default"        to "Default",
        "jetbrains_mono" to "JetBrains Mono",
        "space_grotesk"  to "Space Grotesk",
        "comfortaa"      to "Comfortaa",
        "nunito"         to "Nunito"
    )

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
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { closeAndDismiss() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                        }
                        Text(
                            text = "Font",
                            fontFamily = appFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(48.dp)) // Balance back button
                    }

                    // Font list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(fonts) { (fontId, fontName) ->
                            val isSelected = selectedFont == fontId
                            val actualFont = ThemeStyles.getSelectedFontFamily(fontId)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor.copy(alpha = 0.08f) else cardBgColor)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) primaryColor else borderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setSelectedFont(fontId) }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = fontName,
                                    fontFamily = actualFont,
                                    fontSize = 18.sp,
                                    color = if (isSelected) primaryColor else textColor
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Aa 123",
                                        fontFamily = actualFont,
                                        fontSize = 14.sp,
                                        color = secondaryTextColor,
                                        modifier = Modifier.padding(end = if (isSelected) 16.dp else 0.dp)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
