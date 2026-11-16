package com.example.ui

import androidx.compose.ui.graphics.Color

data class ThemeCategorySpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String
)

data class AppTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val primary: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val category: String = if (isDark) "dark" else "light", // "light", "dark", "glass", "elemental", "retro", "vip"
    val isSupporterOnly: Boolean = false,
    val isGlass: Boolean = false,
    val ambientStart: Color = background,
    val ambientEnd: Color = background,
    val ambientHighlight: Color = primary,
    val glassAlpha: Float = 1f,
    val effectStyle: String = if (isGlass) "glass_silk" else "solid", // "solid", "glass_silk", "frozen_liquid", "volcano_magma", "aurora_plasma", "cosmic_prism"
    val ambientTertiary: Color = ambientHighlight,
    val badgeLabel: String? = null
)
