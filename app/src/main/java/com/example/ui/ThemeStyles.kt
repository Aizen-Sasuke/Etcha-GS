package com.example.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import com.example.R

data class ResolvedThemeArgb(
    val isDark: Boolean,
    val background: Int,
    val card: Int,
    val primary: Int,
    val onPrimary: Int,
    val border: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val pillBackground: Int
)

object ThemeStyles {
    const val DEFAULT_THEME_ID = "revolutionary_cream"
    const val DEFAULT_FONT_ID = "space_grotesk"

    val FONT_OPTIONS: List<Pair<String, String>> = listOf(
        "space_grotesk"     to "Space Grotesk",
        "plus_jakarta_sans" to "Plus Jakarta Sans",
        "outfit"            to "Outfit",
        "inter"             to "Inter",
        "sora"              to "Sora",
        "jetbrains_mono"    to "JetBrains Mono",
        "serif"             to "Serif",
        "default"           to "System Default"
    )

    val FONT_ROLE_TAGS: Map<String, String> = mapOf(
        "space_grotesk"     to "Geometric Display",
        "plus_jakarta_sans" to "Modern Editorial",
        "outfit"            to "Clean Geometric",
        "inter"             to "Crisp UI",
        "sora"              to "Futuristic Sans",
        "jetbrains_mono"    to "Mono & Metrics",
        "serif"             to "Classic Editorial",
        "default"           to "OS Native"
    )

    fun getFontDisplayName(fontId: String): String =
        FONT_OPTIONS.find { it.first == fontId }?.second ?: "Space Grotesk"

    private var appContext: Context? = null

    @Volatile
    private var cachedCustomBgHex: String? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getAppTheme(id: String): AppTheme =
        ThemeData.ALL_THEMES.find { it.id == id }
            ?: ThemeData.ALL_THEMES.find { it.id == DEFAULT_THEME_ID }
            ?: ThemeData.ALL_THEMES.first()

    private fun find(id: String): AppTheme = getAppTheme(id)

    private val fontCache = mutableMapOf<String, FontFamily>()

    private fun resolveCustomBg(customBgHex: String?): Color? {
        val hex = (customBgHex ?: cachedCustomBgHex)?.takeIf { it.isNotBlank() } ?: return null
        if (!customBgHex.isNullOrBlank()) {
            cachedCustomBgHex = customBgHex
        }
        return try {
            Color(hex.toColorInt())
        } catch (_: Exception) {
            null
        }
    }

    fun isGlassTheme(theme: String): Boolean = theme != "custom" && find(theme).isGlass

    fun isLightTheme(theme: String, customBgHex: String? = null): Boolean {
        if (theme == "custom") {
            val customBg = resolveCustomBg(customBgHex) ?: return false
            return isLightColor(customBg)
        }
        return !find(theme).isDark
    }

    fun isThemeDark(theme: String, customBgHex: String? = null): Boolean =
        !isLightTheme(theme, customBgHex)

    fun isLightColor(color: Color): Boolean {
        val luminance = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
        return luminance > 0.6f
    }

    fun getPrimaryColor(theme: String, customHex: String = ""): Color {
        if (theme == "custom" && customHex.isNotEmpty()) {
            try { return Color(customHex.toColorInt()) } catch (_: Exception) {}
        }
        return find(theme).primary
    }

    fun getSecondaryColor(theme: String, customHex: String = ""): Color {
        return getPrimaryColor(theme, customHex)
    }

    fun getBackgroundColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom") {
            val resolved = resolveCustomBg(customBgHex)
            if (resolved != null) return resolved
            return Color(0xFF111111)
        }
        return find(theme).background
    }

    fun getCardBackgroundColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom") {
            val base = resolveCustomBg(customBgHex) ?: Color(0xFF111111)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(base.toArgb(), hsv)
            hsv[2] = if (isLightColor(base)) (hsv[2] - 0.05f).coerceAtLeast(0.82f) else (hsv[2] + 0.08f).coerceAtMost(1f)
            return Color(android.graphics.Color.HSVToColor(hsv))
        }
        val appTheme = find(theme)
        return if (appTheme.isGlass) {
            appTheme.card.copy(alpha = appTheme.glassAlpha)
        } else {
            appTheme.card
        }
    }

    /**
     * Returns an opaque card surface color safe for dialogs, popups, or bitmap widgets even on Glass themes.
     */
    fun getOpaqueCardColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom") return getCardBackgroundColor(theme, customBgHex)
        val appTheme = find(theme)
        return if (appTheme.isGlass) {
            appTheme.card.copy(alpha = (appTheme.glassAlpha + 0.22f).coerceAtMost(0.96f))
                .compositeOver(appTheme.background)
        } else {
            appTheme.card
        }
    }

    fun getBorderColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom") {
            val bg = resolveCustomBg(customBgHex) ?: Color(0xFF111111)
            return if (isLightColor(bg)) Color(0xFFBDBDBD) else Color(0xFF333333)
        }
        val appTheme = find(theme)
        return if (appTheme.isGlass) {
            appTheme.border.copy(alpha = if (appTheme.isDark) 0.42f else 0.65f)
        } else {
            appTheme.border
        }
    }

    fun getGlassBorderStroke(theme: String, customBgHex: String? = null, widthDp: Float = 0.75f): BorderStroke {
        val appTheme = if (theme == "custom") null else find(theme)
        if (appTheme != null && appTheme.isGlass) {
            val topTint = when (appTheme.effectStyle) {
                "volcano_magma" -> Color(0xFFFFB284).copy(alpha = 0.36f)
                else -> if (appTheme.isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.85f)
            }
            val bottomTint = appTheme.border.copy(alpha = if (appTheme.isDark) 0.30f else 0.42f)
            return BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(topTint, bottomTint))
            )
        }
        return BorderStroke(widthDp.dp, getBorderColor(theme, customBgHex))
    }

    fun getTextColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom") {
            val bg = resolveCustomBg(customBgHex) ?: Color(0xFF111111)
            return if (isLightColor(bg)) Color(0xFF141414) else Color.White
        }
        return find(theme).textPrimary
    }

    fun getSecondaryTextColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom") {
            val bg = resolveCustomBg(customBgHex) ?: Color(0xFF111111)
            return if (isLightColor(bg)) Color(0xFF555555) else Color(0xFFA6AEBF)
        }
        return find(theme).textSecondary
    }

    fun getGlowColor(theme: String, customHex: String = "") = getPrimaryColor(theme, customHex)
    fun getAccentRippleColor(theme: String, customHex: String = "") = getPrimaryColor(theme, customHex).copy(alpha = 0.15f)

    fun resolveArgbPalette(
        theme: String,
        customPrimaryHex: String = "#FFFFFF",
        customBackgroundHex: String = "#111111"
    ): ResolvedThemeArgb {
        val bg = getBackgroundColor(theme, customBackgroundHex)
        val card = getOpaqueCardColor(theme, customBackgroundHex)
        val primary = getPrimaryColor(theme, customPrimaryHex)
        val border = getBorderColor(theme, customBackgroundHex)
        val textPrimary = getTextColor(theme, customBackgroundHex)
        val textSecondary = getSecondaryTextColor(theme, customBackgroundHex)
        val onPrimary = if (isLightColor(primary)) Color.Black else Color.White
        val isDark = isThemeDark(theme, customBackgroundHex)
        val pillBg = if (isDark) {
            card.copy(alpha = 0.95f)
        } else {
            border.copy(alpha = 0.18f)
        }
        return ResolvedThemeArgb(
            isDark = isDark,
            background = bg.toArgb(),
            card = card.toArgb(),
            primary = primary.toArgb(),
            onPrimary = onPrimary.toArgb(),
            border = border.toArgb(),
            textPrimary = textPrimary.toArgb(),
            textSecondary = textSecondary.toArgb(),
            pillBackground = pillBg.toArgb()
        )
    }

    fun getSelectedFontFamily(font: String): FontFamily {
        if (fontCache.containsKey(font)) return fontCache[font]!!
        val f = when (font) {
            "space_grotesk"     -> safeFontFamily(R.font.space_grotesk)
            "plus_jakarta_sans" -> safeFontFamily(R.font.plus_jakarta_sans)
            "outfit"            -> safeFontFamily(R.font.outfit)
            "inter"             -> safeFontFamily(R.font.inter)
            "sora"              -> safeFontFamily(R.font.sora)
            "jetbrains_mono"    -> safeFontFamily(R.font.jetbrains_mono)
            "serif"             -> FontFamily.Serif
            "default"           -> FontFamily.Default
            // Graceful fallback for previously persisted retired font IDs
            "monospace"         -> safeFontFamily(R.font.jetbrains_mono)
            "comfortaa", "nunito", "sans_serif", "cursive" -> safeFontFamily(R.font.space_grotesk)
            else                -> FontFamily.Default
        }
        fontCache[font] = f
        return f
    }

    private fun safeFontFamily(resId: Int): FontFamily {
        val ctx = appContext
        if (ctx != null) {
            try {
                val tf = ResourcesCompat.getFont(ctx, resId)
                if (tf != null) {
                    return FontFamily(tf)
                }
            } catch (e: Throwable) {
                Log.e("ThemeStyles", "Failed to load font from resource $resId:, falling back to system default.", e)
            }
        }
        return FontFamily.Default
    }

    /**
     * Softly rounded faceted diamond fitting symmetrically inside each square/rectangular cell.
     */
    val RoundedDiamondShape: Shape = GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val cornerRound = 0.15f // Fractional curve radius along each edge from apex
        val inv = 1f - cornerRound

        // Start just past Left apex heading toward Top apex
        moveTo(cx * cornerRound, cy * inv)
        lineTo(cx * inv, cy * cornerRound)
        // Top apex curve
        quadraticTo(cx, 0f, cx + cx * cornerRound, cy * cornerRound)
        // Edge to Right apex
        lineTo(w - cx * cornerRound, cy * inv)
        // Right apex curve
        quadraticTo(w, cy, w - cx * cornerRound, cy + cy * cornerRound)
        // Edge to Bottom apex
        lineTo(cx + cx * cornerRound, h - cy * cornerRound)
        // Bottom apex curve
        quadraticTo(cx, h, cx * inv, h - cy * cornerRound)
        // Edge to Left apex
        lineTo(cx * cornerRound, cy + cy * cornerRound)
        // Left apex curve
        quadraticTo(0f, cy, cx * cornerRound, cy * inv)
        close()
    }

    fun getCalendarCellShape(shape: String): Shape = when (shape) {
        "rounded_square" -> RoundedCornerShape(10.dp)
        "square"         -> RoundedCornerShape(2.dp)
        "circle"         -> CircleShape
        "squircle"       -> RoundedCornerShape(16.dp)
        "pill"           -> RoundedCornerShape(18.dp)
        "diamond"        -> RoundedDiamondShape
        else             -> RoundedCornerShape(10.dp)
    }
}
