package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.LiquidAmbientBackdrop
import com.example.ui.components.MotionSpecs
import com.example.ui.components.recessedWell
import com.example.ui.components.surface3DCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FontPickerModal(
    viewModel: HabitViewModel,
    onClose: () -> Unit
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val ambientMotionEnabled by viewModel.ambientMotionEnabled.collectAsState()

    val palette = remember(selectedTheme, customHex, customBgHex) {
        ThemeStyles.resolvePalette(selectedTheme, customHex, customBgHex)
    }
    val isLight = !palette.isDark
    val isGlass = palette.isGlass
    val cardBgColor = palette.cardSurface
    val borderColor = palette.border
    val primaryColor = palette.primary
    val onPrimaryColor = palette.onPrimary
    val accentTextColor = palette.accentOnSurface
    val neutralWell = palette.neutralWell
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = palette.textPrimary
    val secondaryTextColor = palette.textSecondary

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scope = rememberCoroutineScope()
    val closeAndDismiss = {
        visible = false
        scope.launch {
            delay(180)
            onClose()
        }
    }

    Dialog(
        onDismissRequest = { closeAndDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = MotionSpecs.SlideIntOffsetSpring,
                initialOffsetY = { it / 3 }
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(160),
                targetOffsetY = { it / 2 }
            ) + fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            LiquidAmbientBackdrop(
                selectedTheme = selectedTheme,
                customPrimaryHex = customHex,
                customBackgroundHex = customBgHex,
                animateMotion = ambientMotionEnabled,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { closeAndDismiss() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Typography Studio",
                                fontFamily = appFont,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${ThemeStyles.FONT_OPTIONS.size} curated modern typefaces",
                                fontFamily = appFont,
                                fontSize = 10.5.sp,
                                color = secondaryTextColor
                            )
                        }
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("font_picker_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Specimen Hero Card
                        item(key = "font_specimen_hero") {
                            val heroShape = RoundedCornerShape(18.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .surface3DCard(
                                        shape = heroShape,
                                        isLight = isLight,
                                        primaryColor = primaryColor,
                                        isGlass = isGlass,
                                        elevationDp = 7.dp
                                    )
                                    .clip(heroShape)
                                    .background(cardBgColor)
                                    .border(1.dp, primaryColor.copy(alpha = 0.45f), heroShape)
                                    .padding(16.dp)
                                    .testTag("font_specimen_preview")
                            ) {
                                AnimatedContent(
                                    targetState = selectedFont,
                                    transitionSpec = {
                                        fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                                    },
                                    label = "font_hero_specimen"
                                ) { fontKey ->
                                    val previewFont = ThemeStyles.getSelectedFontFamily(fontKey)
                                    val fontTitle = ThemeStyles.getFontDisplayName(fontKey)
                                    val roleLabel = ThemeStyles.FONT_ROLE_TAGS[fontKey] ?: "Curated Typeface"

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "LIVE TYPE SPECIMEN",
                                                fontFamily = previewFont,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = accentTextColor
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(primaryColor.copy(alpha = 0.16f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = roleLabel,
                                                    fontFamily = previewFont,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = accentTextColor
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Morning Walk · 14d Streak",
                                            fontFamily = previewFont,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Active Font: $fontTitle — Daily clarity, numbers 0123456789 & calm momentum.",
                                            fontFamily = previewFont,
                                            fontSize = 12.sp,
                                            color = secondaryTextColor
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = ThemeStyles.FONT_OPTIONS,
                            key = { it.first }
                        ) { (fontId, fontName) ->
                            val isSelected = selectedFont == fontId ||
                                (fontId == ThemeStyles.DEFAULT_FONT_ID && ThemeStyles.FONT_OPTIONS.none { it.first == selectedFont })
                            val actualFont = ThemeStyles.getSelectedFontFamily(fontId)
                            val roleTag = ThemeStyles.FONT_ROLE_TAGS[fontId] ?: "Studio Type"
                            val rowBg = if (isSelected) primaryColor.copy(alpha = 0.14f) else cardBgColor
                            val cardShape = RoundedCornerShape(16.dp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 82.dp)
                                    .surface3DCard(
                                        shape = cardShape,
                                        isLight = isLight,
                                        primaryColor = primaryColor,
                                        isGlass = isGlass,
                                        elevationDp = if (isSelected) 7.dp else 4.dp
                                    )
                                    .clip(cardShape)
                                    .background(rowBg)
                                    .border(
                                        width = if (isSelected) 1.6.dp else 0.9.dp,
                                        color = if (isSelected) primaryColor else borderColor,
                                        shape = cardShape
                                    )
                                    .clickable { viewModel.setSelectedFont(fontId) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("font_row_$fontId"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = fontName,
                                            fontFamily = actualFont,
                                            fontSize = 17.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) accentTextColor else textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (fontId == ThemeStyles.DEFAULT_FONT_ID) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(primaryColor.copy(alpha = 0.16f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "DEFAULT",
                                                    fontFamily = actualFont,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentTextColor
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "$roleTag · Daily clarity & momentum",
                                        fontFamily = actualFont,
                                        fontSize = 11.5.sp,
                                        color = secondaryTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .recessedWell(RoundedCornerShape(10.dp), isLight = isLight)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(neutralWell)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Aa 123",
                                            fontFamily = actualFont,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = onPrimaryColor,
                                                modifier = Modifier.size(15.dp)
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
}
