package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
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
import com.example.ui.components.MotionSpecs
import com.example.ui.components.springPressScale

@Composable
fun MainContainer(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    val saveableStateHolder = rememberSaveableStateHolder()

    // Intercept back presses on secondary tabs to navigate back to Home first
    BackHandler(enabled = activeTab != 0) {
        activeTab = 0
    }
    val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
    val customHex by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
    val customBgHex by viewModel.customBackgroundColor.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)
    val isGlass = ThemeStyles.isGlassTheme(selectedTheme)

    val basePrimaryColor by animateColorAsState(
        targetValue = ThemeStyles.getPrimaryColor(selectedTheme, customHex),
        animationSpec = tween(260),
        label = "nav_primary"
    )
    val bgColor by animateColorAsState(
        targetValue = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex),
        animationSpec = tween(260),
        label = "nav_bg"
    )
    val navBarSurfaceColor by animateColorAsState(
        targetValue = if (isGlass) {
            ThemeStyles.getCardBackgroundColor(selectedTheme, customBgHex)
        } else {
            ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
        },
        animationSpec = tween(260),
        label = "nav_bar_surface"
    )
    val unselectedColor = ThemeStyles.getSecondaryTextColor(selectedTheme, customBgHex)

    val navigationBarColors = NavigationBarItemDefaults.colors(
        selectedIconColor = basePrimaryColor,
        selectedTextColor = basePrimaryColor,
        indicatorColor = basePrimaryColor.copy(alpha = 0.16f),
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor
    )

    val homeLabel = stringResource(R.string.nav_home)
    val analyticsLabel = stringResource(R.string.nav_analytics)
    val historyLabel = stringResource(R.string.nav_history)
    val settingsLabel = stringResource(R.string.nav_settings)

    LiquidAmbientBackdrop(
        selectedTheme = selectedTheme,
        customPrimaryHex = customHex,
        customBackgroundHex = customBgHex,
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                val isLightNav = ThemeStyles.isLightTheme(selectedTheme, customBgHex)
                Surface(
                    color = navBarSurfaceColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = ThemeStyles.getGlassBorderStroke(selectedTheme, customBgHex, widthDp = 0.5f),
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = if (isLightNav) 0.65f else 0.14f),
                            start = Offset(0f, 0.5f),
                            end = Offset(size.width, 0.5f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                ) {
                    NavigationBar(
                        windowInsets = NavigationBarDefaults.windowInsets,
                        containerColor = Color.Transparent
                    ) {
                        val navItems = listOf(
                            Triple(0, "nav_home", homeLabel to Icons.Default.Home),
                            Triple(1, "nav_analytics", analyticsLabel to Icons.Default.DateRange),
                            Triple(2, "nav_history", historyLabel to Icons.AutoMirrored.Filled.List),
                            Triple(3, "nav_settings", settingsLabel to Icons.Default.Settings)
                        )
                        navItems.forEach { (tabIdx, tag, labelAndIcon) ->
                            val (label, icon) = labelAndIcon
                            val isSel = activeTab == tabIdx
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSel) 1.16f else 1f,
                                animationSpec = MotionSpecs.BouncyPopSpring,
                                label = "nav_icon_scale_$tabIdx"
                            )
                            val iconOffsetY by animateDpAsState(
                                targetValue = if (isSel) (-2).dp else 0.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "nav_icon_lift_$tabIdx"
                            )
                            NavigationBarItem(
                                modifier = Modifier
                                    .springPressScale(pressedScale = 0.91f)
                                    .testTag(tag),
                                selected = isSel,
                                onClick = { activeTab = tabIdx },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSel) basePrimaryColor else unselectedColor,
                                        modifier = Modifier
                                            .offset(y = iconOffsetY)
                                            .scale(iconScale)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        color = if (isSel) basePrimaryColor else unselectedColor,
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
                                onNavigateToReports = { trackerId ->
                                    viewModel.selectReportTracker(trackerId)
                                    activeTab = 1
                                },
                                onNavigateToHistory = { trackerId ->
                                    viewModel.selectTracker(trackerId)
                                    activeTab = 2
                                }
                            )
                            1 -> ReportScreen(viewModel = viewModel)
                            2 -> UsageScreen(viewModel = viewModel)
                            3 -> PreferencesScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
