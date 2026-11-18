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
import androidx.compose.ui.graphics.lerp
import com.example.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class ResolvedThemeArgb(
    val isDark: Boolean,
    val background: Int,
    val card: Int,
    val primary: Int,
    val onPrimary: Int,
    val border: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val pillBackground: Int,
    val accentOnCard: Int = primary
)

@androidx.compose.runtime.Immutable
data class ResolvedThemePalette(
    val isDark: Boolean,
    val isGlass: Boolean,
    val background: Color,
    val cardSurface: Color,
    val modalSurface: Color,
    val neutralWell: Color,
    val primary: Color,
    val onPrimary: Color,
    val accentOnSurface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val danger: Color,
    val onDanger: Color,
    val positive: Color
)

object ThemeStyles {
    const val DEFAULT_THEME_ID = "revolutionary_cream"
    const val DEFAULT_FONT_ID = "space_grotesk"
    private val CONTRAST_DARK_INK = Color(0xFF0A0B0E)

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

    private fun linearizeChannel(channel: Float): Float {
        val c = channel.coerceIn(0f, 1f)
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    }

    fun relativeLuminance(color: Color): Float {
        val r = linearizeChannel(color.red)
        val g = linearizeChannel(color.green)
        val b = linearizeChannel(color.blue)
        return (0.2126f * r) + (0.7152f * g) + (0.0722f * b)
    }

    fun contrastRatio(foreground: Color, background: Color): Float {
        val fg = if (foreground.alpha < 1f) foreground.compositeOver(background) else foreground
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    fun isLightColor(color: Color): Boolean {
        return contrastRatio(Color.Black, color) > contrastRatio(Color.White, color)
    }

    /**
     * Picks pure white or high-contrast dark ink for text/icons rendered on top of [background].
     */
    fun contrastOn(background: Color): Color {
        val whiteContrast = contrastRatio(Color.White, background)
        val darkContrast = contrastRatio(CONTRAST_DARK_INK, background)
        return if (darkContrast > whiteContrast) CONTRAST_DARK_INK else Color.White
    }

    /**
     * Ensures an accent used for foreground text or thin icons maintains >= [minRatio] contrast
     * against [surface] by progressively mixing toward [fallbackInk] only when needed.
     */
    fun accentOnSurface(
        accent: Color,
        surface: Color,
        fallbackInk: Color = contrastOn(surface),
        minRatio: Float = 4.5f
    ): Color {
        val opaqueSurface = if (surface.alpha < 1f) surface.copy(alpha = 1f) else surface
        if (contrastRatio(accent, opaqueSurface) >= minRatio) {
            return accent
        }
        var best = fallbackInk
        for (step in 1..12) {
            val fraction = step / 12f
            val candidate = lerp(accent, fallbackInk, fraction)
            if (contrastRatio(candidate, opaqueSurface) >= minRatio) {
                return candidate
            }
            best = candidate
        }
        return best
    }

    fun semanticDangerColor(isLight: Boolean): Color =
        if (isLight) Color(0xFFB91C1C) else Color(0xFFFF6B6B)

    fun semanticPositiveColor(isLight: Boolean): Color =
        if (isLight) Color(0xFF15803D) else Color(0xFF4ADE80)

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

    /**
     * Theme-derived tonal neutral container for segmented trays, input fields, and sub-cards.
     */
    fun getNeutralWellColor(theme: String, customBgHex: String? = null): Color {
        val bg = getBackgroundColor(theme, customBgHex)
        val card = getOpaqueCardColor(theme, customBgHex)
        val border = getBorderColor(theme, customBgHex).copy(alpha = 1f)
        val isLight = isLightTheme(theme, customBgHex)
        return if (isLight) {
            lerp(card, border, 0.24f)
        } else {
            lerp(card, bg, 0.45f)
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
        val bg = getOpaqueCardColor(theme, customBgHex)
        val raw = if (theme == "custom") {
            if (isLightColor(bg)) Color(0xFF141414) else Color.White
        } else {
            find(theme).textPrimary
        }
        return if (contrastRatio(raw, bg) >= 4.5f) raw else contrastOn(bg)
    }

    fun getSecondaryTextColor(theme: String, customBgHex: String? = null): Color {
        val bg = getOpaqueCardColor(theme, customBgHex)
        val primaryInk = getTextColor(theme, customBgHex)
        val raw = if (theme == "custom") {
            if (isLightColor(bg)) Color(0xFF4B5563) else Color(0xFFA6AEBF)
        } else {
            find(theme).textSecondary
        }
        return accentOnSurface(raw, bg, fallbackInk = primaryInk, minRatio = 4.5f)
    }

    fun resolvePalette(
        theme: String,
        customPrimaryHex: String = "#C32021",
        customBackgroundHex: String = "#EBE4D3"
    ): ResolvedThemePalette {
        val isDark = isThemeDark(theme, customBackgroundHex)
        val isGlass = isGlassTheme(theme)
        val bg = getBackgroundColor(theme, customBackgroundHex)
        val cardSurface = getCardBackgroundColor(theme, customBackgroundHex)
        val modalSurface = getOpaqueCardColor(theme, customBackgroundHex)
        val neutralWell = getNeutralWellColor(theme, customBackgroundHex)
        val primary = getPrimaryColor(theme, customPrimaryHex)
        val textPrimary = getTextColor(theme, customBackgroundHex)
        val textSecondary = getSecondaryTextColor(theme, customBackgroundHex)
        val border = getBorderColor(theme, customBackgroundHex)
        val onPrimary = contrastOn(primary)
        val accentOnSurface = accentOnSurface(primary, modalSurface, fallbackInk = textPrimary)
        val danger = semanticDangerColor(!isDark)
        val positive = semanticPositiveColor(!isDark)
        return ResolvedThemePalette(
            isDark = isDark,
            isGlass = isGlass,
            background = bg,
            cardSurface = cardSurface,
            modalSurface = modalSurface,
            neutralWell = neutralWell,
            primary = primary,
            onPrimary = onPrimary,
            accentOnSurface = accentOnSurface,
            border = border,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            danger = danger,
            onDanger = contrastOn(danger),
            positive = positive
        )
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
        val onPrimary = contrastOn(primary)
        val accentOnCard = accentOnSurface(primary, card, fallbackInk = textPrimary)
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
            pillBackground = pillBg.toArgb(),
            accentOnCard = accentOnCard.toArgb()
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
