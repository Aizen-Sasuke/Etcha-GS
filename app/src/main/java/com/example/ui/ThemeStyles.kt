package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.Font
import com.example.R
import android.content.Context
import androidx.core.content.res.ResourcesCompat
import android.util.Log

object ThemeStyles {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun find(id: String): AppTheme =
        ThemeData.ALL_THEMES.find { it.id == id } ?: ThemeData.ALL_THEMES.first()

    private val fontCache = mutableMapOf<String, FontFamily>()

    fun isLightTheme(theme: String) = find(theme).isDark.not()
    fun isThemeDark(theme: String) = find(theme).isDark

    fun getPrimaryColor(theme: String, customHex: String = ""): Color {
        if (theme == "custom" && customHex.isNotEmpty()) {
            try { return Color(android.graphics.Color.parseColor(customHex)) } catch (e: Exception) {}
        }
        return find(theme).primary
    }

    fun getSecondaryColor(theme: String, customHex: String = ""): Color {
        return getPrimaryColor(theme, customHex)
    }

    fun getBackgroundColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom" && !customBgHex.isNullOrEmpty()) {
            try { return Color(android.graphics.Color.parseColor(customBgHex)) } catch (e: Exception) {}
            return Color(0xFF111111)
        }
        return find(theme).background
    }

    fun getCardBackgroundColor(theme: String, customBgHex: String? = null): Color {
        if (theme == "custom" && !customBgHex.isNullOrEmpty()) {
            try {
                val baseColor = android.graphics.Color.parseColor(customBgHex)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(baseColor, hsv)
                hsv[2] = (hsv[2] + 0.08f).coerceAtMost(1f)
                return Color(android.graphics.Color.HSVToColor(hsv))
            } catch (e: Exception) {}
        }
        return find(theme).card
    }
    fun getBorderColor(theme: String) = find(theme).border
    fun getTextColor(theme: String) = find(theme).textPrimary
    fun getSecondaryTextColor(theme: String) = find(theme).textSecondary
    fun getGlowColor(theme: String, customHex: String = "") = getPrimaryColor(theme, customHex)
    fun getAccentRippleColor(theme: String, customHex: String = "") = getPrimaryColor(theme, customHex).copy(alpha = 0.15f)

    fun getSelectedFontFamily(font: String): FontFamily {
        if (fontCache.containsKey(font)) return fontCache[font]!!
        val f = when (font) {
            "cursive"        -> FontFamily.Cursive
            "serif"          -> FontFamily.Serif
            "monospace"      -> FontFamily.Monospace
            "sans_serif"     -> FontFamily.SansSerif
            "jetbrains_mono" -> safeFontFamily(R.font.jetbrains_mono)
            "space_grotesk"  -> safeFontFamily(R.font.space_grotesk)
            "comfortaa"      -> safeFontFamily(R.font.comfortaa)
            "nunito"         -> safeFontFamily(R.font.nunito)
            else             -> FontFamily.Default
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

    fun getCalendarCellShape(shape: String): Shape = when (shape) {
        "rounded_square" -> RoundedCornerShape(10.dp)
        "square"         -> RoundedCornerShape(2.dp)
        "circle"         -> CircleShape
        "squircle"       -> RoundedCornerShape(16.dp)
        "pill"           -> RoundedCornerShape(18.dp)
        else             -> RoundedCornerShape(10.dp)
    }
}
