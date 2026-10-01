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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.HabitViewModel
import com.example.ui.ThemeData
import com.example.ui.AppTheme
import com.example.ui.ThemeStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerModal(
    viewModel: HabitViewModel,
    onClose: () -> Unit,
    onOpenDonate: () -> Unit = {}
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val isSupporter by viewModel.isSupporter.collectAsState()
    
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val cardBgColor = ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
    val borderColor = ThemeStyles.getBorderColor(selectedTheme)
    val primaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = ThemeStyles.getTextColor(selectedTheme)
    val secondaryTextColor = ThemeStyles.getSecondaryTextColor(selectedTheme)

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("All") }
    var lockedThemeToPreview by remember { mutableStateOf<AppTheme?>(null) }
    
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
                    // Top Bar
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
                            text = "Themes & Aesthetics",
                            fontFamily = appFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = {
                            closeAndDismiss()
                            onOpenDonate()
                        }) {
                            Text("💖", fontSize = 18.sp)
                        }
                    }

                    // Search Base
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search themes (e.g. Dark, Matrix, Gold)...", fontFamily = appFont, fontSize = 13.sp, color = secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBgColor,
                            unfocusedContainerColor = cardBgColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFont, fontSize = 14.sp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // Horizontally scrollable Tabs
                    val tabScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(tabScroll)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Dark", "Light", "Special & VIP").forEach { tab ->
                            val isSelected = selectedTab == tab
                            val count = when (tab) {
                                "Dark" -> 10
                                "Light" -> 10
                                "Special & VIP" -> 14
                                else -> ThemeData.ALL_THEMES.size
                            }
                            val tabTitle = when (tab) {
                                "Dark" -> "Dark ($count)"
                                "Light" -> "Light ($count)"
                                "Special & VIP" -> "Special & VIP ★ ($count)"
                                else -> "All ($count)"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (tab == "Special & VIP") {
                                            if (isSelected) Color(0xFFFFD700).copy(alpha = 0.25f) else Color(0xFFFFD700).copy(alpha = 0.08f)
                                        } else if (isSelected) {
                                            primaryColor.copy(alpha = 0.15f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (tab == "Special & VIP") Color(0xFFFFD700).copy(alpha = if (isSelected) 0.9f else 0.4f)
                                        else if (isSelected) primaryColor else borderColor,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = if (tab == "Special & VIP") Color(0xFFFFD700) else if (isSelected) primaryColor else textColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Grid
                    val filteredThemes = ThemeData.ALL_THEMES.filter { theme ->
                        val matchesTab = when (selectedTab) {
                            "Dark" -> theme.category == "dark"
                            "Light" -> theme.category == "light"
                            "Special & VIP" -> theme.category == "special"
                            else -> true
                        }
                        val matchesSearch = theme.name.contains(searchQuery, ignoreCase = true)
                        matchesTab && matchesSearch
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredThemes) { theme ->
                            ThemeSwatchCard(
                                theme = theme,
                                selectedTheme = selectedTheme,
                                isSupporter = isSupporter,
                                appFont = appFont,
                                onSelect = {
                                    if (theme.isSupporterOnly && !isSupporter) {
                                        lockedThemeToPreview = theme
                                    } else {
                                        viewModel.setSelectedTheme(theme.id)
                                    }
                                }
                            )
                        }

                        if ("custom".contains(searchQuery, ignoreCase = true) && (selectedTab == "All" || selectedTab == "Special" || selectedTab == "Dark")) {
                            item {
                                CustomThemeSwatchCard(
                                    selectedTheme = selectedTheme,
                                    appFont = appFont,
                                    primaryColor = primaryColor,
                                    textColor = textColor,
                                    onSelect = { viewModel.setSelectedTheme("custom") }
                                )
                            }
                        }
                    }
                }

                // Supporter Theme Locked Dialog / Preview
                lockedThemeToPreview?.let { lockedTheme ->
                    AlertDialog(
                        onDismissRequest = { lockedThemeToPreview = null },
                        containerColor = cardBgColor,
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👑 ", fontSize = 20.sp)
                                Text(
                                    text = "VIP Supporter Theme",
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    fontSize = 17.sp
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "«${lockedTheme.name}» is an exclusive handcrafted theme designed for project supporters!",
                                    fontFamily = appFont,
                                    fontSize = 13.sp,
                                    color = textColor
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = lockedTheme.card),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, lockedTheme.primary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "Preview Palette", fontFamily = appFont, fontSize = 12.sp, color = lockedTheme.textSecondary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(lockedTheme.background).border(1.dp, lockedTheme.border, CircleShape))
                                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(lockedTheme.card).border(1.dp, lockedTheme.border, CircleShape))
                                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(lockedTheme.primary))
                                        }
                                    }
                                }
                                Text(
                                    text = "Support development with a small donation to unlock all VIP themes, Google Cloud auto-backup, bonus streak freezes, and supporter badge!",
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = secondaryTextColor
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val themeId = lockedTheme.id
                                    lockedThemeToPreview = null
                                    closeAndDismiss()
                                    onOpenDonate()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF1E1400))
                            ) {
                                Text("⭐ Unlock All VIP Features", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                // Direct instant try-it unlock
                                viewModel.setSupporter(true)
                                viewModel.setSelectedTheme(lockedTheme.id)
                                lockedThemeToPreview = null
                            }) {
                                Text("Try It Free", fontFamily = appFont, color = primaryColor, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSwatchCard(
    theme: AppTheme,
    selectedTheme: String,
    isSupporter: Boolean,
    appFont: FontFamily,
    onSelect: () -> Unit
) {
    val isSelected = selectedTheme == theme.id
    val isLocked = theme.isSupporterOnly && !isSupporter
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.background)
                .border(
                    width = if (isSelected) 2.5.dp else if (theme.isSupporterOnly) 1.5.dp else 1.dp,
                    color = if (isSelected) theme.primary else if (theme.isSupporterOnly) Color(0xFFFFD700).copy(alpha = 0.6f) else theme.border.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onSelect() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.28f)
                    .align(Alignment.BottomCenter)
                    .background(theme.card)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(theme.primary)
                        .align(Alignment.Center)
                )
            }
            if (theme.isSupporterOnly) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(3.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("★", fontSize = 8.sp, color = Color(0xFF2A1F00), fontWeight = FontWeight.Black)
                }
            }
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Supporter Locked",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                }
            } else if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(theme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = theme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = theme.name,
            fontSize = 9.sp,
            fontFamily = appFont,
            color = theme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CustomThemeSwatchCard(
    selectedTheme: String,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    onSelect: () -> Unit
) {
    val isSelected = selectedTheme == "custom"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Transparent)
                .clickable { onSelect() }
                .border(
                    width = if (isSelected) 2.5.dp else 1.5.dp,
                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🎨", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Custom",
            fontSize = 9.sp,
            fontFamily = appFont,
            color = if (isSelected) textColor else textColor.copy(alpha=0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
