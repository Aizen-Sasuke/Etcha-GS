package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HabitViewModel
import com.example.ui.ThemeStyles

@Composable
fun MainContainer(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) }

    // Intercept back presses on secondary tabs to navigate back to Home first
    BackHandler(enabled = activeTab != 0) {
        activeTab = 0
    }
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val customHex by viewModel.customPrimaryColor.collectAsState()
    val customBgHex by viewModel.customBackgroundColor.collectAsState()
    val selectedFont by viewModel.selectedFont.collectAsState()
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)

    // Determine primary accent color based on theme selection
    val basePrimaryColor = ThemeStyles.getPrimaryColor(selectedTheme, customHex)
    val activeTracker by viewModel.activeTracker.collectAsState()
    val accentColor = activeTracker.accentColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch(e: Exception) { basePrimaryColor }
    } ?: basePrimaryColor
    
    val bgColor = ThemeStyles.getBackgroundColor(selectedTheme, customBgHex)
    val unselectedColor = ThemeStyles.getSecondaryTextColor(selectedTheme)

    // High contrast navigation palette
    val navigationBarColors = NavigationBarItemDefaults.colors(
        selectedIconColor = accentColor,
        selectedTextColor = accentColor,
        indicatorColor = accentColor.copy(alpha = 0.15f),
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = bgColor,
                tonalElevation = 8.dp
            ) {
                // Tab 1: Home
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_home"),
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (activeTab == 0) accentColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
                            color = if (activeTab == 0) accentColor else unselectedColor,
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = appFont
                        )
                    },
                    colors = navigationBarColors
                )

                // Tab 2: Analytics & Heatmap
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_analytics"),
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Analytics",
                            tint = if (activeTab == 1) accentColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            text = "Analytics",
                            color = if (activeTab == 1) accentColor else unselectedColor,
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = appFont
                        )
                    },
                    colors = navigationBarColors
                )

                // Tab 3: History & Logs
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_history"),
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "History",
                            tint = if (activeTab == 2) accentColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            text = "History",
                            color = if (activeTab == 2) accentColor else unselectedColor,
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = appFont
                        )
                    },
                    colors = navigationBarColors
                )

                // Tab 4: Settings & Preferences
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_settings"),
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (activeTab == 3) accentColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            color = if (activeTab == 3) accentColor else unselectedColor,
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = appFont
                        )
                    },
                    colors = navigationBarColors
                )
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            color = bgColor
        ) {
            when (activeTab) {
                0 -> HomeScreen(viewModel = viewModel)
                1 -> ReportScreen(viewModel = viewModel)
                2 -> UsageScreen(viewModel = viewModel)
                3 -> PreferencesScreen(viewModel = viewModel)
            }
        }
    }

    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()
    if (!hasSeenOnboarding) {
        OnboardingModal(
            viewModel = viewModel,
            onDismiss = { viewModel.setHasSeenOnboarding(true) }
        )
    }
}
