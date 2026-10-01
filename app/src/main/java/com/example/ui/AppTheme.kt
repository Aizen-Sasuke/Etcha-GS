package com.example.ui

import androidx.compose.ui.graphics.Color

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
    val category: String = if (isDark) "dark" else "light", // "dark", "light", "special", "supporter"
    val isSupporterOnly: Boolean = false
)

