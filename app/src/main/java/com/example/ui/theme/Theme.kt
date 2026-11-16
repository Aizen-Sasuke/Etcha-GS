package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.ThemeStyles

@Composable
fun MyApplicationTheme(
    selectedTheme: String = ThemeStyles.DEFAULT_THEME_ID,
    customPrimaryColor: String = "#C32021",
    customBackgroundColor: String = "#EBE4D3",
    selectedFont: String = ThemeStyles.DEFAULT_FONT_ID,
    content: @Composable () -> Unit
) {
    val isDark = ThemeStyles.isThemeDark(selectedTheme, customBackgroundColor)
    val primary = ThemeStyles.getPrimaryColor(selectedTheme, customPrimaryColor)
    val background = ThemeStyles.getBackgroundColor(selectedTheme, customBackgroundColor)
    val surface = ThemeStyles.getCardBackgroundColor(selectedTheme, customBackgroundColor)
    val outline = ThemeStyles.getBorderColor(selectedTheme, customBackgroundColor)
    val onBackground = ThemeStyles.getTextColor(selectedTheme, customBackgroundColor)
    val onSurfaceVariant = ThemeStyles.getSecondaryTextColor(selectedTheme, customBackgroundColor)
    val onPrimary = if (ThemeStyles.isLightColor(primary)) Color.Black else Color.White
    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)

    val colorScheme = remember(selectedTheme, primary, background, surface, outline, onBackground, onSurfaceVariant) {
        if (isDark) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                secondary = primary,
                onSecondary = onPrimary,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surface,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outline.copy(alpha = 0.55f)
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                secondary = primary,
                onSecondary = onPrimary,
                background = background,
                onBackground = onBackground,
                surface = surface,
                onSurface = onBackground,
                surfaceVariant = surface,
                onSurfaceVariant = onSurfaceVariant,
                outline = outline,
                outlineVariant = outline.copy(alpha = 0.55f)
            )
        }
    }

    val typography = remember(appFont) {
        if (appFont == FontFamily.Default) Typography else Typography.withFontFamily(appFont)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily)
)
