package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.AppTheme
import com.example.ui.HabitViewModel
import com.example.ui.ThemeData
import com.example.ui.ThemeStyles
import com.example.ui.components.LiquidAmbientBackdrop
import com.example.ui.components.MotionSpecs
import com.example.ui.components.ThemeEffectPreviewCanvas
import com.example.ui.components.recessedWell
import com.example.ui.components.surface3DCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val calendarCellShapeName by viewModel.calendarCellShape.collectAsState()
    val isSupporter by viewModel.isSupporter.collectAsState()
    val favoriteThemes by viewModel.favoriteThemes.collectAsState()
    val ambientMotionEnabled by viewModel.ambientMotionEnabled.collectAsState()

    val palette = remember(selectedTheme, customHex, customBgHex) {
        ThemeStyles.resolvePalette(selectedTheme, customHex, customBgHex)
    }
    val isCurrentLight = !palette.isDark

    var previewThemeId by remember(selectedTheme) { mutableStateOf(selectedTheme) }
    val activePreviewThemeId = previewThemeId
    val heroAppTheme = remember(activePreviewThemeId, customHex, customBgHex) {
        if (activePreviewThemeId == "custom") {
            val isLight = ThemeStyles.isLightTheme("custom", customBgHex)
            AppTheme(
                id = "custom",
                name = "Custom Studio Palette",
                isDark = !isLight,
                background = ThemeStyles.getBackgroundColor("custom", customBgHex),
                card = ThemeStyles.getCardBackgroundColor("custom", customBgHex),
                primary = ThemeStyles.getPrimaryColor("custom", customHex),
                border = ThemeStyles.getBorderColor("custom", customBgHex),
                textPrimary = ThemeStyles.getTextColor("custom", customBgHex),
                textSecondary = ThemeStyles.getSecondaryTextColor("custom", customBgHex),
                category = if (isLight) "light" else "dark",
                badgeLabel = "CUSTOM"
            )
        } else {
            ThemeStyles.getAppTheme(activePreviewThemeId)
        }
    }

    val cardBgColor = palette.cardSurface
    val modalCardColor = palette.modalSurface
    val borderColor = palette.border
    val primaryColor = palette.primary
    val accentTextColor = palette.accentOnSurface
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val textColor = palette.textPrimary
    val secondaryTextColor = palette.textSecondary
    val activeCellShape = ThemeStyles.getCalendarCellShape(calendarCellShapeName)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var lockedThemeToPreview by remember { mutableStateOf<AppTheme?>(null) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scope = rememberCoroutineScope()
    val closeAndDismiss = {
        visible = false
        scope.launch {
            delay(170)
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
                animationSpec = tween(170),
                targetOffsetY = { it / 2 }
            ) + fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            LiquidAmbientBackdrop(
                selectedTheme = activePreviewThemeId,
                customPrimaryHex = customHex,
                customBackgroundHex = customBgHex,
                animateMotion = ambientMotionEnabled,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Studio Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { closeAndDismiss() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = textColor
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Theme Studio",
                                fontFamily = appFont,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ThemeData.ALL_THEMES.size} curated themes & live shaders",
                                fontFamily = appFont,
                                fontSize = 10.5.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (ambientMotionEnabled) primaryColor.copy(alpha = 0.2f) else cardBgColor)
                                .border(0.8.dp, if (ambientMotionEnabled) primaryColor else borderColor, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setAmbientMotionEnabled(!ambientMotionEnabled) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("theme_studio_motion_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (ambientMotionEnabled) "✨ Motion On" else "Motion Off",
                                fontFamily = appFont,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (ambientMotionEnabled) accentTextColor else secondaryTextColor
                            )
                        }
                        IconButton(
                            onClick = {
                                closeAndDismiss()
                                onOpenDonate()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text("👑", fontSize = 18.sp)
                        }
                    }

                    // Category Filter Chips (Pinned directly beneath Studio header)
                    val chipScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(chipScroll)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeCategoryFilterChip(
                            label = "✨ All (${ThemeData.ALL_THEMES.size})",
                            selected = selectedCategory == "all",
                            isSpecialHighlight = false,
                            primaryColor = primaryColor,
                            accentTextColor = accentTextColor,
                            isLightSurface = isCurrentLight,
                            cardBgColor = cardBgColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            appFont = appFont,
                            testTag = "theme_filter_all",
                            onClick = { selectedCategory = "all" }
                        )

                        ThemeCategoryFilterChip(
                            label = "❤️ Favorites (${favoriteThemes.size})",
                            selected = selectedCategory == "favorites",
                            isSpecialHighlight = false,
                            primaryColor = primaryColor,
                            accentTextColor = accentTextColor,
                            isLightSurface = isCurrentLight,
                            cardBgColor = cardBgColor,
                            borderColor = borderColor,
                            textColor = textColor,
                            appFont = appFont,
                            testTag = "theme_filter_favorites",
                            onClick = { selectedCategory = "favorites" }
                        )

                        ThemeData.CATEGORIES.forEach { cat ->
                            val count = ThemeData.ALL_THEMES.count { it.category == cat.id }
                            val isSelected = selectedCategory == cat.id
                            val isVipOrElemental = cat.id == "vip" || cat.id == "elemental"
                            ThemeCategoryFilterChip(
                                label = "${cat.emoji} ${cat.title} ($count)",
                                selected = isSelected,
                                isSpecialHighlight = isVipOrElemental,
                                primaryColor = primaryColor,
                                accentTextColor = accentTextColor,
                                isLightSurface = isCurrentLight,
                                cardBgColor = cardBgColor,
                                borderColor = borderColor,
                                textColor = textColor,
                                appFont = appFont,
                                testTag = "theme_filter_${cat.id}",
                                onClick = { selectedCategory = cat.id }
                            )
                        }
                    }

                    // Categorized 2-Column Rich Showcase Grid (Hero Preview + Search + Cards scroll fluidly together)
                    val visibleCategories = remember(selectedCategory) {
                        when (selectedCategory) {
                            "all" -> ThemeData.CATEGORIES
                            "favorites" -> emptyList()
                            else -> ThemeData.CATEGORIES.filter { it.id == selectedCategory }
                        }
                    }

                    val favoriteThemeItems = remember(favoriteThemes, searchQuery) {
                        ThemeData.ALL_THEMES.filter { theme ->
                            theme.id in favoriteThemes &&
                                (searchQuery.isBlank() ||
                                    theme.name.contains(searchQuery, ignoreCase = true) ||
                                    (theme.badgeLabel?.contains(searchQuery, ignoreCase = true) == true))
                        }
                    }

                    val includeCustom = remember(searchQuery, selectedCategory, favoriteThemes) {
                        val matchesQuery = searchQuery.isBlank() ||
                            "custom studio palette".contains(searchQuery, ignoreCase = true)
                        when (selectedCategory) {
                            "favorites" -> matchesQuery && ("custom" in favoriteThemes)
                            "all", "light", "dark" -> matchesQuery
                            else -> false
                        }
                    }

                    val gridState = rememberLazyGridState()
                    LaunchedEffect(selectedCategory) {
                        gridState.scrollToItem(0)
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .testTag("theme_picker_grid"),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(
                            key = "studio_hero_preview",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            ThemeHeroPreviewCard(
                                heroTheme = heroAppTheme,
                                isApplied = selectedTheme == heroAppTheme.id,
                                isLocked = heroAppTheme.isSupporterOnly && !isSupporter,
                                isFavorite = heroAppTheme.id in favoriteThemes,
                                animatePreview = ambientMotionEnabled,
                                cellShape = activeCellShape,
                                appFont = appFont,
                                onToggleFavorite = { viewModel.toggleFavoriteTheme(heroAppTheme.id) },
                                onUnlockOrApply = {
                                    if (heroAppTheme.isSupporterOnly && !isSupporter) {
                                        lockedThemeToPreview = heroAppTheme
                                    } else {
                                        viewModel.setSelectedTheme(heroAppTheme.id)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item(
                            key = "studio_search_bar",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search Volcano, Glacier, Glass, Cream, VIP...",
                                        fontFamily = appFont,
                                        fontSize = 12.5.sp,
                                        color = secondaryTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
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
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFont, fontSize = 13.5.sp),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .recessedWell(RoundedCornerShape(14.dp), isLight = isCurrentLight)
                            )
                        }

                        if (selectedCategory == "favorites") {
                            if (favoriteThemeItems.isEmpty() && !includeCustom) {
                                item(
                                    key = "favorites_empty_state",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                            .surface3DCard(
                                                shape = RoundedCornerShape(18.dp),
                                                isLight = isCurrentLight,
                                                primaryColor = primaryColor,
                                                isGlass = ThemeStyles.isGlassTheme(selectedTheme),
                                                elevationDp = 4.dp
                                            )
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(cardBgColor)
                                            .border(0.8.dp, borderColor, RoundedCornerShape(18.dp))
                                            .padding(horizontal = 20.dp, vertical = 24.dp)
                                            .testTag("favorites_empty_state"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("🤍", fontSize = 30.sp)
                                            Text(
                                                text = if (searchQuery.isBlank()) "No Favorite Themes Yet" else "No matching favorites",
                                                fontFamily = appFont,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = if (searchQuery.isBlank()) {
                                                    "Tap the heart icon on any theme card or in the live preview above to pin your favorites here."
                                                } else {
                                                    "Try clearing your search filter to see all pinned favorite themes."
                                                },
                                                fontFamily = appFont,
                                                fontSize = 12.sp,
                                                color = secondaryTextColor,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                item(
                                    key = "header_favorites",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    ThemeSectionHeader(
                                        sectionTag = "theme_section_favorites",
                                        emoji = "❤️",
                                        title = "Favorite Themes",
                                        subtitle = "Your pinned studios & shaders for instant switching",
                                        count = favoriteThemeItems.size + if (includeCustom) 1 else 0,
                                        appFont = appFont,
                                        accentTextColor = accentTextColor,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        cardBgColor = cardBgColor,
                                        borderColor = borderColor
                                    )
                                }

                                items(
                                    items = favoriteThemeItems,
                                    key = { "fav_${it.id}" }
                                ) { theme ->
                                    ThemeSwatchCard(
                                        theme = theme,
                                        selectedTheme = selectedTheme,
                                        isSupporter = isSupporter,
                                        isFavorite = true,
                                        appFont = appFont,
                                        screenTextColor = textColor,
                                        screenSecondaryTextColor = secondaryTextColor,
                                        isPreviewed = activePreviewThemeId == theme.id,
                                        onToggleFavorite = { viewModel.toggleFavoriteTheme(theme.id) },
                                        onSelect = {
                                            previewThemeId = theme.id
                                            if (theme.isSupporterOnly && !isSupporter) {
                                                lockedThemeToPreview = theme
                                            } else {
                                                viewModel.setSelectedTheme(theme.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        visibleCategories.forEach { category ->
                            val catThemes = ThemeData.ALL_THEMES.filter { theme ->
                                theme.category == category.id &&
                                    (searchQuery.isBlank() ||
                                        theme.name.contains(searchQuery, ignoreCase = true) ||
                                        (theme.badgeLabel?.contains(searchQuery, ignoreCase = true) == true) ||
                                        category.title.contains(searchQuery, ignoreCase = true))
                            }
                            if (catThemes.isNotEmpty()) {
                                item(
                                    key = "header_${category.id}",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    ThemeSectionHeader(
                                        sectionTag = "theme_section_${category.id}",
                                        emoji = category.emoji,
                                        title = category.title,
                                        subtitle = category.subtitle,
                                        count = catThemes.size,
                                        appFont = appFont,
                                        accentTextColor = accentTextColor,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                        cardBgColor = cardBgColor,
                                        borderColor = borderColor
                                    )
                                }

                                items(
                                    items = catThemes,
                                    key = { it.id }
                                ) { theme ->
                                    ThemeSwatchCard(
                                        theme = theme,
                                        selectedTheme = selectedTheme,
                                        isPreviewed = activePreviewThemeId == theme.id,
                                        isSupporter = isSupporter,
                                        isFavorite = theme.id in favoriteThemes,
                                        appFont = appFont,
                                        screenTextColor = textColor,
                                        screenSecondaryTextColor = secondaryTextColor,
                                        onToggleFavorite = { viewModel.toggleFavoriteTheme(theme.id) },
                                        onSelect = {
                                            previewThemeId = theme.id
                                            if (theme.isSupporterOnly && !isSupporter) {
                                                lockedThemeToPreview = theme
                                            } else {
                                                viewModel.setSelectedTheme(theme.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (includeCustom) {
                            item(
                                key = "header_custom",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Text(
                                    text = "🎨 Custom Studio Palette",
                                    fontFamily = appFont,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            item(
                                key = "swatch_custom",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                CustomThemeSwatchCard(
                                    selectedTheme = selectedTheme,
                                    isFavorite = "custom" in favoriteThemes,
                                    appFont = appFont,
                                    primaryColor = primaryColor,
                                    customBgColor = ThemeStyles.getBackgroundColor("custom", customBgHex),
                                    textColor = textColor,
                                    secondaryTextColor = secondaryTextColor,
                                    onToggleFavorite = { viewModel.toggleFavoriteTheme("custom") },
                                    onSelect = {
                                        previewThemeId = "custom"
                                        viewModel.setSelectedTheme("custom")
                                    }
                                )
                            }
                        }
                    }
                }

                // Supporter Theme Locked Dialog / Preview
                lockedThemeToPreview?.let { lockedTheme ->
                    AlertDialog(
                        onDismissRequest = { lockedThemeToPreview = null },
                        containerColor = modalCardColor,
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👑 ", fontSize = 20.sp)
                                Text(
                                    text = "Bonus Supporter Theme",
                                    fontFamily = appFont,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentLight) Color(0xFF9A6700) else Color(0xFFFFD700),
                                    fontSize = 17.sp
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "«${lockedTheme.name}» is previewing live in the Theme Studio banner.",
                                    fontFamily = appFont,
                                    fontSize = 13.sp,
                                    color = textColor
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = lockedTheme.card),
                                    border = BorderStroke(1.dp, lockedTheme.primary.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Preview Palette" + if (lockedTheme.isGlass) " · Live Shader & Glass" else "",
                                            fontFamily = appFont,
                                            fontSize = 12.sp,
                                            color = lockedTheme.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(lockedTheme.ambientStart)
                                                    .border(1.dp, lockedTheme.border, CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(lockedTheme.card)
                                                    .border(1.dp, lockedTheme.border, CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(lockedTheme.primary)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Activate Supporter Edition (100% free & offline) to apply all VIP themes and extra streak freezes.",
                                    fontFamily = appFont,
                                    fontSize = 12.sp,
                                    color = secondaryTextColor
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    lockedThemeToPreview = null
                                    viewModel.setSupporter(true)
                                    viewModel.setSelectedTheme(lockedTheme.id)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700),
                                    contentColor = Color(0xFF1E1400)
                                )
                            ) {
                                Text("✨ Unlock & Apply", fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    lockedThemeToPreview = null
                                    closeAndDismiss()
                                    onOpenDonate()
                                }
                            ) {
                                Text("View Vault", fontFamily = appFont, color = accentTextColor, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteHeartPillButton(
    isFavorite: Boolean,
    contentDescription: String,
    accentColor: Color,
    isDarkSurface: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartColor = if (isFavorite) Color(0xFFFF4D6D) else (if (isDarkSurface) Color.White.copy(alpha = 0.85f) else Color(0xFF1F2430))
    val pillBg = if (isFavorite) Color(0xFFFF4D6D).copy(alpha = 0.18f) else {
        if (isDarkSurface) Color.Black.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.82f)
    }
    val pillBorder = if (isFavorite) Color(0xFFFF4D6D).copy(alpha = 0.65f) else accentColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(pillBg)
            .border(0.8.dp, pillBorder, CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = contentDescription,
            tint = heartColor,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun ThemeHeroPreviewCard(
    heroTheme: AppTheme,
    isApplied: Boolean,
    isLocked: Boolean,
    isFavorite: Boolean,
    animatePreview: Boolean,
    cellShape: Shape,
    appFont: FontFamily,
    onToggleFavorite: () -> Unit,
    onUnlockOrApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = if (heroTheme.isGlass) heroTheme.card.copy(alpha = heroTheme.glassAlpha) else heroTheme.card
    val heroPrimary = heroTheme.primary
    val heroAccentText = ThemeStyles.accentOnSurface(heroPrimary, heroTheme.card, fallbackInk = heroTheme.textPrimary)
    val heroOnPrimary = ThemeStyles.contrastOn(heroPrimary)
    val heroBorder = if (heroTheme.isGlass) {
        Color.White.copy(alpha = if (heroTheme.isDark) 0.34f else 0.85f)
    } else {
        heroTheme.border.copy(alpha = 0.7f)
    }

    val heroShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .heightIn(min = 122.dp)
            .surface3DCard(
                shape = heroShape,
                isLight = !heroTheme.isDark,
                primaryColor = heroPrimary,
                isGlass = heroTheme.isGlass,
                elevationDp = 8.dp
            )
            .clip(heroShape)
            .border(1.2.dp, heroBorder, heroShape)
            .testTag("theme_hero_preview")
    ) {
        ThemeEffectPreviewCanvas(
            theme = heroTheme,
            animated = animatePreview,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardColor)
                            .border(0.8.dp, heroBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧘", fontSize = 16.sp)
                    }
                    Column {
                        AnimatedContent(
                            targetState = heroTheme.name,
                            transitionSpec = {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                            },
                            label = "hero_title"
                        ) { name ->
                            Text(
                                text = name,
                                fontFamily = appFont,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = heroTheme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val styleLabel = heroTheme.badgeLabel ?: when {
                                heroTheme.isGlass -> "GLASSMORPHISM"
                                heroTheme.isDark -> "DARK STUDIO"
                                else -> "EDITORIAL"
                            }
                            Text(
                                text = "LIVE PREVIEW · $styleLabel",
                                fontFamily = appFont,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = heroTheme.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FavoriteHeartPillButton(
                        isFavorite = isFavorite,
                        contentDescription = if (isFavorite) "Remove from favorite themes" else "Add to favorite themes",
                        accentColor = heroPrimary,
                        isDarkSurface = heroTheme.isDark,
                        testTag = "hero_favorite_button",
                        onClick = onToggleFavorite
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isLocked -> Color(0xFFFFD700)
                                    isApplied -> heroTheme.card.copy(alpha = 0.85f)
                                    else -> heroPrimary
                                }
                            )
                            .border(
                                0.8.dp,
                                if (isApplied && !isLocked) heroPrimary else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onUnlockOrApply() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isLocked -> "👑 Unlock"
                                isApplied -> "✓ Active"
                                else -> "Apply"
                            },
                            fontFamily = appFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isLocked -> Color(0xFF1F1600)
                                isApplied -> heroAccentText
                                else -> heroOnPrimary
                            },
                            maxLines = 1
                        )
                    }
                }
            }

            // Frosted miniature preview card with 5 active cell-shape sample days (3D lifted vs recessed)
            val miniCardShape = RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .surface3DCard(
                        shape = miniCardShape,
                        isLight = !heroTheme.isDark,
                        primaryColor = heroPrimary,
                        isGlass = heroTheme.isGlass,
                        elevationDp = 3.dp
                    )
                    .clip(miniCardShape)
                    .background(cardColor)
                    .border(0.8.dp, heroBorder, miniCardShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "🔥 14d Streak",
                        fontFamily = appFont,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = heroTheme.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "86% Consistency",
                        fontFamily = appFont,
                        fontSize = 10.sp,
                        color = heroTheme.textSecondary,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(true, true, false, true, true).forEachIndexed { idx, done ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .then(
                                    if (done) {
                                        Modifier.surface3DCard(
                                            shape = cellShape,
                                            isLight = !heroTheme.isDark,
                                            primaryColor = heroPrimary,
                                            elevationDp = 2.dp
                                        )
                                    } else {
                                        Modifier.recessedWell(cellShape, isLight = !heroTheme.isDark)
                                    }
                                )
                                .clip(cellShape)
                                .background(
                                    if (done) heroPrimary.copy(alpha = if (idx == 4) 0.28f else 0.18f)
                                    else heroTheme.background.copy(alpha = 0.45f)
                                )
                                .border(
                                    width = if (idx == 4) 1.4.dp else 0.8.dp,
                                    color = if (done) heroPrimary.copy(alpha = 0.78f) else heroTheme.border.copy(alpha = 0.45f),
                                    shape = cellShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (done) "✓" else "${11 + idx}",
                                fontFamily = appFont,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (done) heroAccentText else heroTheme.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCategoryFilterChip(
    label: String,
    selected: Boolean,
    isSpecialHighlight: Boolean,
    primaryColor: Color,
    accentTextColor: Color,
    isLightSurface: Boolean,
    cardBgColor: Color,
    borderColor: Color,
    textColor: Color,
    appFont: FontFamily,
    testTag: String,
    onClick: () -> Unit
) {
    val accentGold = Color(0xFFD99B00)
    val bg = when {
        isSpecialHighlight && selected -> Color(0xFFFFD700).copy(alpha = 0.25f)
        isSpecialHighlight -> Color(0xFFFFD700).copy(alpha = 0.11f)
        selected -> primaryColor.copy(alpha = 0.20f)
        else -> cardBgColor
    }
    val stroke = when {
        isSpecialHighlight -> accentGold.copy(alpha = if (selected) 0.92f else 0.45f)
        selected -> primaryColor
        else -> borderColor
    }
    val labelColor = when {
        isSpecialHighlight -> if (selected) textColor else textColor.copy(alpha = 0.88f)
        selected -> accentTextColor
        else -> textColor
    }
    val chipShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .then(
                if (selected) {
                    Modifier.surface3DCard(
                        shape = chipShape,
                        isLight = isLightSurface,
                        primaryColor = primaryColor,
                        elevationDp = 4.dp
                    )
                } else {
                    Modifier
                }
            )
            .clip(chipShape)
            .background(bg)
            .border(if (selected) 1.4.dp else 0.8.dp, stroke, chipShape)
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = appFont,
            fontSize = 12.sp,
            color = labelColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ThemeSectionHeader(
    sectionTag: String,
    emoji: String,
    title: String,
    subtitle: String,
    count: Int,
    appFont: FontFamily,
    accentTextColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    cardBgColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .testTag(sectionTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = "$emoji $title",
                fontFamily = appFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontFamily = appFont,
                fontSize = 11.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(cardBgColor)
                .border(0.6.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$count",
                fontFamily = appFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentTextColor
            )
        }
    }
}

@Composable
fun ThemeSwatchCard(
    theme: AppTheme,
    selectedTheme: String,
    isSupporter: Boolean,
    appFont: FontFamily,
    screenTextColor: Color,
    screenSecondaryTextColor: Color,
    isPreviewed: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onSelect: () -> Unit
) {
    val isSelected = selectedTheme == theme.id
    val isLocked = theme.isSupporterOnly && !isSupporter
    val borderColor = when {
        isSelected -> theme.primary
        isPreviewed -> theme.primary.copy(alpha = 0.65f)
        theme.isGlass -> Color.White.copy(alpha = if (theme.isDark) 0.34f else 0.88f)
        else -> theme.border.copy(alpha = 0.55f)
    }
    val swatchAccentText = ThemeStyles.accentOnSurface(theme.primary, theme.card, fallbackInk = theme.textPrimary)
    val swatchOnPrimary = ThemeStyles.contrastOn(theme.primary)
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_card_${theme.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.42f)
                .surface3DCard(
                    shape = cardShape,
                    isLight = !theme.isDark,
                    primaryColor = theme.primary,
                    isGlass = theme.isGlass,
                    elevationDp = if (isSelected) 8.dp else 5.dp
                )
                .clip(cardShape)
                .border(
                    width = if (isSelected) 2.2.dp else if (theme.isGlass) 1.1.dp else 0.8.dp,
                    color = borderColor,
                    shape = cardShape
                )
                .clickable { onSelect() }
        ) {
            // Static cached gradient/shader preview on grid cards ensures 120Hz zero-stutter scrolling
            ThemeEffectPreviewCanvas(
                theme = theme,
                animated = false,
                modifier = Modifier.matchParentSize()
            )

            // Frosted or solid inner habit card miniature with subtle 3D lift
            val innerCardShape = RoundedCornerShape(11.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .align(Alignment.BottomCenter)
                    .surface3DCard(
                        shape = innerCardShape,
                        isLight = !theme.isDark,
                        primaryColor = theme.primary,
                        isGlass = theme.isGlass,
                        elevationDp = 2.dp
                    )
                    .clip(innerCardShape)
                    .background(if (theme.isGlass) theme.card.copy(alpha = theme.glassAlpha) else theme.card)
                    .border(
                        width = if (theme.isGlass) 0.8.dp else 0.5.dp,
                        color = if (theme.isGlass) Color.White.copy(alpha = if (theme.isDark) 0.28f else 0.82f) else theme.border.copy(alpha = 0.45f),
                        shape = innerCardShape
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(theme.ambientHighlight)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(theme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(theme.textPrimary.copy(alpha = 0.78f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.primary.copy(alpha = 0.22f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Aa",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = appFont,
                            color = swatchAccentText
                        )
                    }
                }
            }

            // Top-Left Favorite Heart Toggle + Effect/Tier Badge
            val topBadge = theme.badgeLabel ?: when {
                theme.isGlass -> "GLASS"
                theme.isSupporterOnly -> "VIP"
                else -> null
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FavoriteHeartPillButton(
                    isFavorite = isFavorite,
                    contentDescription = if (isFavorite) "Unfavorite ${theme.name}" else "Favorite ${theme.name}",
                    accentColor = theme.primary,
                    isDarkSurface = theme.isDark,
                    testTag = "theme_favorite_${theme.id}",
                    onClick = onToggleFavorite
                )

                if (topBadge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (theme.isSupporterOnly) Color(0xFFFFD700).copy(alpha = 0.90f)
                                else theme.card.copy(alpha = if (theme.isGlass) 0.78f else 0.88f)
                            )
                            .border(
                                0.6.dp,
                                if (theme.isSupporterOnly) Color(0xFFFFD700) else Color.White.copy(alpha = if (theme.isDark) 0.28f else 0.7f),
                                RoundedCornerShape(7.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (theme.isSupporterOnly) "★ $topBadge" else topBadge,
                            fontFamily = appFont,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme.isSupporterOnly) Color(0xFF221600) else theme.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Lock or Check badge
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.60f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Supporter Locked",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(theme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = swatchOnPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = theme.name,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = appFont,
            color = if (isSelected) screenTextColor else screenSecondaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun CustomThemeSwatchCard(
    selectedTheme: String,
    appFont: FontFamily,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color = textColor.copy(alpha = 0.7f),
    customBgColor: Color = primaryColor.copy(alpha = 0.10f),
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onSelect: () -> Unit
) {
    val isSelected = selectedTheme == "custom"
    val isCustomLight = ThemeStyles.isLightColor(customBgColor)
    val customInk = ThemeStyles.contrastOn(customBgColor)
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_card_custom")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .surface3DCard(
                    shape = cardShape,
                    isLight = isCustomLight,
                    primaryColor = primaryColor,
                    elevationDp = if (isSelected) 7.dp else 4.dp
                )
                .clip(cardShape)
                .background(customBgColor)
                .clickable { onSelect() }
                .border(
                    width = if (isSelected) 2.2.dp else 1.2.dp,
                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.55f),
                    shape = cardShape
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎨", fontSize = 22.sp)
                    Column {
                        Text(
                            text = "Custom Studio Palette",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = appFont,
                            color = customInk
                        )
                        Text(
                            text = "Adaptive luminance ink on your colors",
                            fontSize = 10.5.sp,
                            fontFamily = appFont,
                            color = customInk.copy(alpha = 0.72f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FavoriteHeartPillButton(
                        isFavorite = isFavorite,
                        contentDescription = if (isFavorite) "Unfavorite Custom Studio Palette" else "Favorite Custom Studio Palette",
                        accentColor = primaryColor,
                        isDarkSurface = !isCustomLight,
                        testTag = "theme_favorite_custom",
                        onClick = onToggleFavorite
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .border(1.dp, customInk.copy(alpha = 0.45f), CircleShape)
                    )
                }
            }
        }
    }
}
