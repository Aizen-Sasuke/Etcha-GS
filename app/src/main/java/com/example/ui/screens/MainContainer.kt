package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles
import com.example.ui.components.LiquidAmbientBackdrop

@Composable
fun MainContainer(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    openSettingsRequest: Boolean = false,
    onOpenSettingsConsumed: () -> Unit = {}
) {
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    val saveableStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(openSettingsRequest) {
        if (openSettingsRequest) {
            activeTab = 3
            onOpenSettingsConsumed()
        }
    }

    // Intercept back presses on secondary tabs to navigate back to Home first
    BackHandler(enabled = activeTab != 0) {
        activeTab = 0
    }
    val isExpandedLayout = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val customHex by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
    val customBgHex by viewModel.customBackgroundColor.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val ambientMotionEnabled by viewModel.ambientMotionEnabled.collectAsStateWithLifecycle()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)

    val palette = remember(selectedTheme, customHex, customBgHex) {
        ThemeStyles.resolvePalette(selectedTheme, customHex, customBgHex)
    }
    val isGlass = palette.isGlass
    val basePrimaryColor = palette.primary
    val bgColor = palette.background
    val navBarSurfaceColor = if (isGlass) palette.cardSurface else palette.background
    val navTextSurface = if (isGlass) palette.modalSurface else palette.background
    val navSelectedColor = remember(palette) {
        ThemeStyles.accentOnSurface(palette.primary, navTextSurface, fallbackInk = palette.textPrimary)
    }
    val unselectedColor = palette.textSecondary

    val navigationBarColors = NavigationBarItemDefaults.colors(
        selectedIconColor = navSelectedColor,
        selectedTextColor = navSelectedColor,
        indicatorColor = basePrimaryColor.copy(alpha = 0.16f),
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor
    )
    val navigationRailColors = NavigationRailItemDefaults.colors(
        selectedIconColor = navSelectedColor,
        selectedTextColor = navSelectedColor,
        indicatorColor = basePrimaryColor.copy(alpha = 0.16f),
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor
    )

    val homeLabel = stringResource(R.string.nav_home)
    val analyticsLabel = stringResource(R.string.nav_analytics)
    val historyLabel = stringResource(R.string.nav_history)
    val settingsLabel = stringResource(R.string.nav_settings)

    val navItems = remember(homeLabel, analyticsLabel, historyLabel, settingsLabel) {
        listOf(
            Triple(0, "nav_home", homeLabel to Icons.Default.Home),
            Triple(1, "nav_analytics", analyticsLabel to Icons.Default.DateRange),
            Triple(2, "nav_history", historyLabel to Icons.AutoMirrored.Filled.List),
            Triple(3, "nav_settings", settingsLabel to Icons.Default.Settings)
        )
    }

    LiquidAmbientBackdrop(
        selectedTheme = selectedTheme,
        customPrimaryHex = customHex,
        customBackgroundHex = customBgHex,
        animateMotion = ambientMotionEnabled,
        modifier = modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isExpandedLayout) {
                Surface(
                    color = navBarSurfaceColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = ThemeStyles.getGlassBorderStroke(selectedTheme, customBgHex, widthDp = 0.5f),
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("main_navigation_rail")
                ) {
                    NavigationRail(
                        containerColor = Color.Transparent,
                        windowInsets = NavigationRailDefaults.windowInsets
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { (tabIdx, tag, labelAndIcon) ->
                            val (label, icon) = labelAndIcon
                            val isSel = activeTab == tabIdx
                            NavigationRailItem(
                                modifier = Modifier.testTag(tag),
                                selected = isSel,
                                onClick = { activeTab = tabIdx },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSel) navSelectedColor else unselectedColor
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        color = if (isSel) navSelectedColor else unselectedColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = appFont
                                    )
                                },
                                colors = navigationRailColors
                            )
                        }
                    }
                }
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = Color.Transparent,
                bottomBar = {
                    if (!isExpandedLayout) {
                        val isLightNav = ThemeStyles.isLightTheme(selectedTheme, customBgHex)
                        Surface(
                            color = navBarSurfaceColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = ThemeStyles.getGlassBorderStroke(selectedTheme, customBgHex, widthDp = 0.5f),
                            modifier = Modifier
                                .drawBehind {
                                    drawLine(
                                        color = Color.White.copy(alpha = if (isLightNav) 0.65f else 0.14f),
                                        start = Offset(0f, 0.5f),
                                        end = Offset(size.width, 0.5f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .testTag("main_navigation_bar")
                        ) {
                            NavigationBar(
                                windowInsets = NavigationBarDefaults.windowInsets,
                                containerColor = Color.Transparent
                            ) {
                                navItems.forEach { (tabIdx, tag, labelAndIcon) ->
                                    val (label, icon) = labelAndIcon
                                    val isSel = activeTab == tabIdx
                                    NavigationBarItem(
                                        modifier = Modifier.testTag(tag),
                                        selected = isSel,
                                        onClick = { activeTab = tabIdx },
                                        icon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSel) navSelectedColor else unselectedColor
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                color = if (isSel) navSelectedColor else unselectedColor,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = appFont
                                            )
                                        },
                                        colors = navigationBarColors
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    color = if (isGlass) Color.Transparent else bgColor
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                            ) togetherWith fadeOut(
                                animationSpec = tween(durationMillis = 70, easing = LinearEasing)
                            )
                        },
                        label = "main_tab_animated_content"
                    ) { tab ->
                        saveableStateHolder.SaveableStateProvider(key = tab) {
                            when (tab) {
                                0 -> HomeScreen(
                                    viewModel = viewModel,
                                    isTwoPane = isExpandedLayout,
                                    onNavigateToReports = { trackerId ->
                                        viewModel.selectReportTracker(trackerId)
                                        activeTab = 1
                                    },
                                    onNavigateToHistory = { trackerId ->
                                        viewModel.selectTracker(trackerId)
                                        activeTab = 2
                                    }
                                )
                                1 -> ReportScreen(viewModel = viewModel, isTwoPane = isExpandedLayout)
                                2 -> UsageScreen(viewModel = viewModel, isTwoPane = isExpandedLayout)
                                3 -> PreferencesScreen(viewModel = viewModel, isTwoPane = isExpandedLayout)
                            }
                        }
                    }
                }
            }
        }
    }
}
