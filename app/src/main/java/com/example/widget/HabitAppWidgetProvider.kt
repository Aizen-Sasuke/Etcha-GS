package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.HabitEntry
import com.example.data.SampleTrackerSeeder
import com.example.data.Tracker
import com.example.ui.HabitStatsCalculator
import com.example.ui.ResolvedThemeArgb
import com.example.ui.ThemeStyles
import com.example.ui.TodayDashboardCalculator
import com.example.ui.TodayDashboardState
import com.example.ui.TodayHabitItem
import com.example.ui.TodayHabitStatus
import com.example.ui.TrackerConfig
import com.example.worker.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

internal enum class WidgetDayState {
    DONE,
    PARTIAL,
    FROZEN,
    EMPTY
}

internal data class WidgetDaySlot(
    val date: LocalDate,
    val letter: String,
    val state: WidgetDayState,
    val isToday: Boolean
)

class HabitAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_HABIT_WIDGET = "com.example.ACTION_UPDATE_HABIT_WIDGET"
        const val ACTION_WIDGET_CHECK_IN = "com.example.ACTION_WIDGET_CHECK_IN"
        const val ACTION_WIDGET_CHECK_IN_TRACKER = "com.example.ACTION_WIDGET_CHECK_IN_TRACKER"
        const val ACTION_WIDGET_PREV_HABIT = "com.example.ACTION_WIDGET_PREV_HABIT"
        const val ACTION_WIDGET_NEXT_HABIT = "com.example.ACTION_WIDGET_NEXT_HABIT"
        const val EXTRA_WIDGET_TRACKER_ID = "extra_widget_tracker_id"

        private const val WIDGET_PREFS_NAME = "habit_widget_prefs"
        private const val KEY_WIDGET_TRACKER_ID = "widget_selected_tracker_id"

        internal fun getSelectedWidgetTrackerId(context: Context): String? {
            val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_WIDGET_TRACKER_ID, null)?.takeIf { it.isNotBlank() }
        }

        internal fun setSelectedWidgetTrackerId(context: Context, trackerId: String) {
            context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putString(KEY_WIDGET_TRACKER_ID, trackerId)
                }
        }

        internal suspend fun cycleSelectedTracker(
            context: Context,
            db: AppDatabase,
            step: Int
        ): Tracker? {
            val activeTrackers = db.trackerDao().getAllTrackersList().filter { !it.isArchived }
            if (activeTrackers.isEmpty()) return null
            val currentId = getSelectedWidgetTrackerId(context)
                ?: db.habitDao().getPreferenceValue("selected_tracker_id")
            val currentIndex = activeTrackers.indexOfFirst { it.id == currentId }
                .let { if (it >= 0) it else 0 }
            val nextIndex = Math.floorMod(currentIndex + step, activeTrackers.size)
            val chosen = activeTrackers[nextIndex]
            setSelectedWidgetTrackerId(context, chosen.id)
            return chosen
        }

        internal suspend fun handleWidgetCheckIn(
            context: Context,
            db: AppDatabase,
            now: LocalDateTime = LocalDateTime.now(),
            trackerId: String? = null
        ): Boolean {
            val activeTrackers = db.trackerDao().getAllTrackersList().filter { !it.isArchived }
            if (activeTrackers.isEmpty()) return false
            val preferredId = trackerId?.takeIf { it.isNotBlank() }
                ?: getSelectedWidgetTrackerId(context)
                ?: db.habitDao().getPreferenceValue("selected_tracker_id")
            val tracker = activeTrackers.find { it.id == preferredId }
                ?: if (trackerId != null) return false else activeTrackers.first()

            // Safety rule: never silently log a bad-habit slip from a widget tap
            if (tracker.type == "bad") {
                return false
            }

            val rolloverHour = db.habitDao().getPreferenceValue("day_rollover_hour")?.toIntOrNull() ?: 0
            val effectiveDate = ReminderManager.getEffectiveToday(rolloverHour, now).toString()
            val existing = db.habitDao().getEntriesForTrackerAndDate(tracker.id, effectiveDate)
            val currentTotal = existing.sumOf { it.count }
            val target = tracker.targetCount.coerceAtLeast(1)

            // Prevent over-logging an already completed good habit via duplicate widget taps
            if (tracker.type == "good" && currentTotal >= target) {
                return false
            }

            if (existing.isEmpty()) {
                db.habitDao().insertEntry(
                    HabitEntry(
                        dateString = effectiveDate,
                        count = 1,
                        trackerId = tracker.id
                    )
                )
            } else {
                val last = existing.last()
                db.habitDao().updateEntry(last.copy(count = last.count + 1))
            }
            return true
        }

        internal fun createRoundedRectBitmap(
            widthPx: Int,
            heightPx: Int,
            cornerRadiusPx: Float,
            fillColor: Int,
            strokeColor: Int = Color.TRANSPARENT,
            strokeWidthPx: Float = 0f
        ): Bitmap {
            val w = widthPx.coerceAtLeast(4)
            val h = heightPx.coerceAtLeast(4)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val halfStroke = if (strokeWidthPx > 0f && Color.alpha(strokeColor) > 0) strokeWidthPx / 2f else 0f
            val rect = RectF(halfStroke, halfStroke, w - halfStroke, h - halfStroke)

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, fillPaint)

            if (strokeWidthPx > 0f && Color.alpha(strokeColor) > 0) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = strokeColor
                    strokeWidth = strokeWidthPx
                }
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)
            }
            return bitmap
        }

        internal fun createGlassCardBitmap(
            widthPx: Int,
            heightPx: Int,
            cornerRadiusPx: Float,
            fillColor: Int,
            highlightTopColor: Int,
            highlightBottomColor: Int,
            strokeColor: Int,
            strokeWidthPx: Float
        ): Bitmap {
            val w = widthPx.coerceAtLeast(8)
            val h = heightPx.coerceAtLeast(8)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val halfStroke = if (strokeWidthPx > 0f && Color.alpha(strokeColor) > 0) strokeWidthPx / 2f else 0f
            val rect = RectF(halfStroke, halfStroke, w - halfStroke, h - halfStroke)

            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, basePaint)

            val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    highlightTopColor, highlightBottomColor,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, sheenPaint)

            if (strokeWidthPx > 0f && Color.alpha(strokeColor) > 0) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = strokeColor
                    strokeWidth = strokeWidthPx
                }
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)
            }
            return bitmap
        }

        internal fun createProgressBarBitmap(
            widthPx: Int,
            heightPx: Int,
            progressPercent: Int,
            trackColor: Int,
            fillColor: Int
        ): Bitmap {
            val w = widthPx.coerceAtLeast(12)
            val h = heightPx.coerceAtLeast(4)
            val radius = h / 2f
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = trackColor
            }
            canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, trackPaint)

            val clamped = progressPercent.coerceIn(0, 100)
            if (clamped > 0) {
                val fillW = (w * (clamped / 100f)).coerceAtLeast(h.toFloat()).coerceAtMost(w.toFloat())
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = fillColor
                }
                canvas.drawRoundRect(RectF(0f, 0f, fillW, h.toFloat()), radius, radius, fillPaint)
            }
            return bitmap
        }

        internal fun createTodayRingBitmap(
            sizePx: Int,
            progressPercent: Int,
            trackColor: Int,
            arcColor: Int,
            centerTextColor: Int
        ): Bitmap {
            val s = sizePx.coerceAtLeast(24)
            val bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val strokeW = (s * 0.13f).coerceAtLeast(2.5f)
            val pad = strokeW / 2f + 1.5f
            val arcRect = RectF(pad, pad, s - pad, s - pad)

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                color = trackColor
            }
            canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

            val clamped = progressPercent.coerceIn(0, 100)
            if (clamped > 0) {
                val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = strokeW
                    strokeCap = Paint.Cap.ROUND
                    color = arcColor
                }
                canvas.drawArc(arcRect, -90f, 360f * (clamped / 100f), false, arcPaint)
            }

            val label = if (clamped >= 100) "✓" else "$clamped"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = centerTextColor
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                textSize = if (clamped >= 100) s * 0.36f else s * 0.28f
            }
            val fm = textPaint.fontMetrics
            val baseline = s / 2f - (fm.ascent + fm.descent) / 2f
            canvas.drawText(label, s / 2f, baseline, textPaint)
            return bitmap
        }

        internal fun createSevenDayStripBitmap(
            widthPx: Int,
            heightPx: Int,
            slots: List<WidgetDaySlot>,
            capsuleFill: Int,
            capsuleStroke: Int,
            primaryColor: Int,
            textColor: Int,
            secondaryTextColor: Int,
            isDark: Boolean
        ): Bitmap {
            val w = widthPx.coerceAtLeast(140)
            val h = heightPx.coerceAtLeast(24)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val corner = h * 0.36f
            val strokeW = (h * 0.04f).coerceAtLeast(1f)
            val capsuleRect = RectF(strokeW / 2f, strokeW / 2f, w - strokeW / 2f, h - strokeW / 2f)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = capsuleFill
            }
            canvas.drawRoundRect(capsuleRect, corner, corner, bgPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                color = capsuleStroke
            }
            canvas.drawRoundRect(capsuleRect, corner, corner, borderPaint)

            if (slots.isEmpty()) return bitmap

            val slotCount = slots.size
            val horizPad = w * 0.03f
            val slotW = (w - horizPad * 2f) / slotCount
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = h * 0.34f
                typeface = Typeface.DEFAULT_BOLD
            }
            val dotRadius = (h * 0.16f).coerceAtMost(slotW * 0.20f)
            val dotY = h * 0.69f
            val textY = h * 0.36f

            slots.forEachIndexed { idx, slot ->
                val cx = horizPad + slotW * idx + slotW / 2f

                labelPaint.color = if (slot.isToday) primaryColor else secondaryTextColor
                canvas.drawText(slot.letter, cx, textY, labelPaint)

                when (slot.state) {
                    WidgetDayState.DONE -> {
                        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = primaryColor
                        }
                        canvas.drawCircle(cx, dotY, dotRadius, p)
                    }
                    WidgetDayState.PARTIAL -> {
                        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = (dotRadius * 0.42f).coerceAtLeast(1.5f)
                            color = primaryColor
                        }
                        canvas.drawCircle(cx, dotY, dotRadius * 0.85f, ring)
                        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = ColorUtils.setAlphaComponent(primaryColor, 140)
                        }
                        canvas.drawCircle(cx, dotY, dotRadius * 0.45f, inner)
                    }
                    WidgetDayState.FROZEN -> {
                        val frozenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = Color.parseColor("#38BDF8")
                        }
                        canvas.drawCircle(cx, dotY, dotRadius * 0.92f, frozenPaint)
                    }
                    WidgetDayState.EMPTY -> {
                        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = ColorUtils.setAlphaComponent(textColor, if (isDark) 36 else 28)
                        }
                        canvas.drawCircle(cx, dotY, dotRadius * 0.78f, emptyPaint)
                    }
                }

                if (slot.isToday) {
                    val todayRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (dotRadius * 0.32f).coerceAtLeast(1.2f)
                        color = if (slot.state == WidgetDayState.DONE) textColor else primaryColor
                    }
                    canvas.drawCircle(cx, dotY, dotRadius * 1.28f, todayRing)
                }
            }

            return bitmap
        }

        internal fun buildSevenDaySlots(
            tracker: Tracker,
            trackerEntries: List<HabitEntry>,
            freezeActiveDate: String?,
            freezeHistory: String,
            effectiveToday: LocalDate
        ): List<WidgetDaySlot> {
            val entriesByDate = trackerEntries.groupBy { it.dateString }
            val frozenDates = freezeHistory.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (!freezeActiveDate.isNullOrBlank()) {
                frozenDates.add(freezeActiveDate)
            }
            val target = tracker.targetCount.coerceAtLeast(1)
            return (6 downTo 0).map { offset ->
                val date = effectiveToday.minusDays(offset.toLong())
                val dateStr = date.toString()
                val sum = entriesByDate[dateStr]?.sumOf { it.count } ?: 0
                val state = when {
                    tracker.type == "bad" -> if (sum == 0) WidgetDayState.DONE else WidgetDayState.EMPTY
                    dateStr in frozenDates -> WidgetDayState.FROZEN
                    sum >= target -> WidgetDayState.DONE
                    sum > 0 -> WidgetDayState.PARTIAL
                    else -> WidgetDayState.EMPTY
                }
                val letter = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.US)
                WidgetDaySlot(
                    date = date,
                    letter = letter,
                    state = state,
                    isToday = offset == 0
                )
            }
        }

        internal fun selectChecklistItems(
            dashboard: TodayDashboardState,
            featuredTrackerId: String
        ): List<TodayHabitItem> {
            val dueNonFeatured = (dashboard.attentionItems + dashboard.avoidanceItems)
                .filter { it.tracker.id != featuredTrackerId }
            val doneOrRestNonFeatured = (dashboard.completedItems + dashboard.restAndProtectedItems)
                .filter { it.tracker.id != featuredTrackerId }
            val ordered = dueNonFeatured + doneOrRestNonFeatured
            return if (ordered.isNotEmpty()) {
                ordered.take(3)
            } else {
                dashboard.allItems.take(3)
            }
        }

        private fun applyThemeToRemoteViews(
            views: RemoteViews,
            palette: ResolvedThemeArgb,
            todayRingPercent: Int,
            progressPercent: Int,
            sevenDaySlots: List<WidgetDaySlot>,
            isDoneState: Boolean,
            isBadTracker: Boolean,
            density: Float
        ) {
            val cardStroke = ColorUtils.setAlphaComponent(palette.border, if (palette.isDark) 215 else 185)
            val sheenTop = if (palette.isDark) Color.argb(30, 255, 255, 255) else Color.argb(85, 255, 255, 255)
            val sheenBottom = if (palette.isDark) Color.argb(18, 0, 0, 0) else Color.argb(10, 0, 0, 0)
            val bgBitmap = createGlassCardBitmap(
                widthPx = (340 * density).toInt(),
                heightPx = (240 * density).toInt(),
                cornerRadiusPx = 22f * density,
                fillColor = palette.card,
                highlightTopColor = sheenTop,
                highlightBottomColor = sheenBottom,
                strokeColor = cardStroke,
                strokeWidthPx = 1.4f * density
            )

            val glassPillFill = if (palette.isDark) {
                ColorUtils.setAlphaComponent(palette.background, 180)
            } else {
                ColorUtils.setAlphaComponent(Color.WHITE, 140)
            }
            val glassPillStroke = ColorUtils.setAlphaComponent(
                if (palette.isDark) Color.WHITE else palette.border,
                if (palette.isDark) 42 else 135
            )
            val heroBitmap = createGlassCardBitmap(
                widthPx = (316 * density).toInt(),
                heightPx = (100 * density).toInt(),
                cornerRadiusPx = 16f * density,
                fillColor = glassPillFill,
                highlightTopColor = if (palette.isDark) Color.argb(24, 255, 255, 255) else Color.argb(75, 255, 255, 255),
                highlightBottomColor = Color.TRANSPARENT,
                strokeColor = glassPillStroke,
                strokeWidthPx = 1.1f * density
            )
            val pillBitmap = createRoundedRectBitmap(
                widthPx = (110 * density).toInt(),
                heightPx = (32 * density).toInt(),
                cornerRadiusPx = 12f * density,
                fillColor = glassPillFill,
                strokeColor = glassPillStroke,
                strokeWidthPx = 1f * density
            )
            val navBitmap = createRoundedRectBitmap(
                widthPx = (36 * density).toInt(),
                heightPx = (30 * density).toInt(),
                cornerRadiusPx = 10f * density,
                fillColor = glassPillFill,
                strokeColor = glassPillStroke,
                strokeWidthPx = 1f * density
            )

            val trackColor = ColorUtils.setAlphaComponent(palette.border, if (palette.isDark) 95 else 110)
            val ringBitmap = createTodayRingBitmap(
                sizePx = (36 * density).toInt(),
                progressPercent = todayRingPercent,
                trackColor = trackColor,
                arcColor = palette.primary,
                centerTextColor = palette.textPrimary
            )
            val stripBitmap = createSevenDayStripBitmap(
                widthPx = (288 * density).toInt(),
                heightPx = (24 * density).toInt(),
                slots = sevenDaySlots,
                capsuleFill = ColorUtils.setAlphaComponent(palette.background, if (palette.isDark) 135 else 95),
                capsuleStroke = glassPillStroke,
                primaryColor = palette.primary,
                textColor = palette.textPrimary,
                secondaryTextColor = palette.textSecondary,
                isDark = palette.isDark
            )
            val progressBitmap = createProgressBarBitmap(
                widthPx = (288 * density).toInt(),
                heightPx = (6 * density).toInt(),
                progressPercent = progressPercent,
                trackColor = trackColor,
                fillColor = palette.primary
            )

            val (actionFill, actionStroke, actionTextColor) = when {
                isBadTracker -> Triple(
                    glassPillFill,
                    glassPillStroke,
                    palette.textPrimary
                )
                isDoneState -> Triple(
                    ColorUtils.setAlphaComponent(palette.primary, if (palette.isDark) 52 else 36),
                    ColorUtils.setAlphaComponent(palette.primary, 155),
                    if (palette.primary == Color.WHITE) palette.textPrimary else palette.primary
                )
                else -> Triple(
                    palette.primary,
                    Color.TRANSPARENT,
                    palette.onPrimary
                )
            }
            val actionBitmap = createRoundedRectBitmap(
                widthPx = (110 * density).toInt(),
                heightPx = (36 * density).toInt(),
                cornerRadiusPx = 12f * density,
                fillColor = actionFill,
                strokeColor = actionStroke,
                strokeWidthPx = if (actionStroke == Color.TRANSPARENT) 0f else 1.1f * density
            )

            val streakTextColor = if (palette.primary == Color.WHITE && palette.isDark) {
                palette.textPrimary
            } else if (palette.primary == Color.BLACK && !palette.isDark) {
                palette.textPrimary
            } else {
                palette.primary
            }

            views.apply {
                setImageViewBitmap(R.id.widget_bg_image, bgBitmap)
                setImageViewBitmap(R.id.widget_today_ring, ringBitmap)
                setImageViewBitmap(R.id.widget_hero_bg, heroBitmap)
                setImageViewBitmap(R.id.widget_streak_bg, pillBitmap)
                setImageViewBitmap(R.id.widget_prev_bg, navBitmap)
                setImageViewBitmap(R.id.widget_next_bg, navBitmap)
                setImageViewBitmap(R.id.widget_streak_strip, stripBitmap)
                setImageViewBitmap(R.id.widget_progress, progressBitmap)
                setImageViewBitmap(R.id.widget_action_bg, actionBitmap)

                setTextColor(R.id.widget_app_label, palette.textSecondary)
                setTextColor(R.id.widget_checklist_caption, palette.textSecondary)
                setTextColor(R.id.widget_habit_streak, streakTextColor)
                setTextColor(R.id.widget_btn_prev, palette.textPrimary)
                setTextColor(R.id.widget_btn_next, palette.textPrimary)
                setTextColor(R.id.widget_habit_title, palette.textPrimary)
                setTextColor(R.id.widget_habit_status, palette.textSecondary)
                setTextColor(R.id.widget_btn_action, actionTextColor)
            }
        }

        private fun bindChecklistRows(
            context: Context,
            views: RemoteViews,
            items: List<TodayHabitItem>,
            palette: ResolvedThemeArgb,
            density: Float,
            showChecklist: Boolean,
            buildOpenAppIntent: (String?) -> PendingIntent
        ) {
            val rowIds = listOf(
                R.id.widget_row_0,
                R.id.widget_row_1,
                R.id.widget_row_2
            )
            val rowBgIds = listOf(
                R.id.widget_row_0_bg,
                R.id.widget_row_1_bg,
                R.id.widget_row_2_bg
            )
            val rowIconIds = listOf(
                R.id.widget_row_0_icon,
                R.id.widget_row_1_icon,
                R.id.widget_row_2_icon
            )

            if (!showChecklist || items.isEmpty()) {
                views.setViewVisibility(R.id.widget_checklist_container, View.GONE)
                rowIds.forEach { views.setViewVisibility(it, View.GONE) }
                return
            }

            views.setViewVisibility(R.id.widget_checklist_container, View.VISIBLE)

            val rowTitleViews = intArrayOf(R.id.widget_row_0_title, R.id.widget_row_1_title, R.id.widget_row_2_title)
            val rowMetaViews = intArrayOf(R.id.widget_row_0_meta, R.id.widget_row_1_meta, R.id.widget_row_2_meta)
            val rowBtnBgViews = intArrayOf(R.id.widget_row_0_btn_bg, R.id.widget_row_1_btn_bg, R.id.widget_row_2_btn_bg)
            val rowBtnViews = intArrayOf(R.id.widget_row_0_btn, R.id.widget_row_1_btn, R.id.widget_row_2_btn)

            val rowFill = if (palette.isDark) {
                ColorUtils.setAlphaComponent(palette.background, 165)
            } else {
                ColorUtils.setAlphaComponent(Color.WHITE, 128)
            }
            val rowStroke = ColorUtils.setAlphaComponent(
                if (palette.isDark) Color.WHITE else palette.border,
                if (palette.isDark) 36 else 120
            )
            val rowBgBitmap = createRoundedRectBitmap(
                widthPx = (310 * density).toInt(),
                heightPx = (34 * density).toInt(),
                cornerRadiusPx = 11f * density,
                fillColor = rowFill,
                strokeColor = rowStroke,
                strokeWidthPx = 0.9f * density
            )
            val actionBtnBitmap = createRoundedRectBitmap(
                widthPx = (40 * density).toInt(),
                heightPx = (26 * density).toInt(),
                cornerRadiusPx = 9f * density,
                fillColor = palette.primary,
                strokeColor = Color.TRANSPARENT,
                strokeWidthPx = 0f
            )
            val doneBtnBitmap = createRoundedRectBitmap(
                widthPx = (40 * density).toInt(),
                heightPx = (26 * density).toInt(),
                cornerRadiusPx = 9f * density,
                fillColor = ColorUtils.setAlphaComponent(palette.primary, if (palette.isDark) 48 else 34),
                strokeColor = ColorUtils.setAlphaComponent(palette.primary, 130),
                strokeWidthPx = 1f * density
            )
            val badBtnBitmap = createRoundedRectBitmap(
                widthPx = (40 * density).toInt(),
                heightPx = (26 * density).toInt(),
                cornerRadiusPx = 9f * density,
                fillColor = rowFill,
                strokeColor = rowStroke,
                strokeWidthPx = 1f * density
            )

            for (i in 0 until 3) {
                if (i >= items.size) {
                    views.setViewVisibility(rowIds[i], View.GONE)
                    continue
                }
                val item = items[i]
                val tracker = item.tracker
                val target = tracker.targetCount.coerceAtLeast(1)
                val isDone = item.status == TodayHabitStatus.COMPLETED ||
                    item.status == TodayHabitStatus.QUOTA_MET ||
                    (tracker.type == "good" && item.todayCount >= target)
                val isBad = tracker.type == "bad"

                val metaText = when {
                    isBad -> if (item.todayCount > 0) "Slip" else "Clean"
                    tracker.type == "misc" -> "${item.todayCount} logs"
                    target > 1 -> "${item.todayCount}/$target"
                    isDone -> "Done"
                    item.status == TodayHabitStatus.FROZEN -> "Frozen"
                    else -> "Due"
                }

                val btnText: String
                val btnBg: Bitmap
                val btnTextColor: Int
                val btnPi: PendingIntent

                when {
                    isBad -> {
                        btnText = context.getString(R.string.widget_row_open)
                        btnBg = badBtnBitmap
                        btnTextColor = palette.textPrimary
                        btnPi = buildOpenAppIntent(tracker.id)
                    }
                    isDone -> {
                        btnText = context.getString(R.string.widget_row_done)
                        btnBg = doneBtnBitmap
                        btnTextColor = if (palette.primary == Color.WHITE) palette.textPrimary else palette.primary
                        btnPi = buildOpenAppIntent(tracker.id)
                    }
                    else -> {
                        btnText = context.getString(R.string.widget_row_add)
                        btnBg = actionBtnBitmap
                        btnTextColor = palette.onPrimary
                        btnPi = PendingIntent.getBroadcast(
                            context,
                            200 + i,
                            Intent(context, HabitAppWidgetProvider::class.java).apply {
                                action = ACTION_WIDGET_CHECK_IN_TRACKER
                                putExtra(EXTRA_WIDGET_TRACKER_ID, tracker.id)
                                setPackage(context.packageName)
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                }

                val rowOpenPi = buildOpenAppIntent(tracker.id)
                views.setViewVisibility(rowIds[i], View.VISIBLE)
                views.setImageViewBitmap(rowBgIds[i], rowBgBitmap)
                views.setTextViewText(rowIconIds[i], tracker.icon)
                views.setTextViewText(rowTitleViews[i], tracker.title)
                views.setTextColor(rowTitleViews[i], palette.textPrimary)
                views.setTextViewText(rowMetaViews[i], metaText)
                views.setTextColor(rowMetaViews[i], if (isDone) (if (palette.primary == Color.WHITE) palette.textPrimary else palette.primary) else palette.textSecondary)
                views.setImageViewBitmap(rowBtnBgViews[i], btnBg)
                views.setTextViewText(rowBtnViews[i], btnText)
                views.setTextColor(rowBtnViews[i], btnTextColor)
                views.setOnClickPendingIntent(rowIds[i], rowOpenPi)
                views.setOnClickPendingIntent(rowBtnViews[i], btnPi)
            }
        }

        internal suspend fun performWidgetUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            db: AppDatabase = AppDatabase.getDatabase(context),
            now: LocalDateTime = LocalDateTime.now()
        ) {
            if (appWidgetIds.isEmpty()) return

            val prefsList = db.habitDao().getAllPreferencesFlow().first()
            val prefsMap = prefsList.associate { it.key to it.value }
            val selectedTheme = prefsMap["selected_theme"] ?: ThemeStyles.DEFAULT_THEME_ID
            val customPrimary = prefsMap["custom_primary_color"] ?: "#C32021"
            val customBg = prefsMap["custom_background_color"] ?: "#EBE4D3"
            val palette = ThemeStyles.resolveArgbPalette(selectedTheme, customPrimary, customBg)
            val density = context.resources.displayMetrics.density.coerceIn(1f, 3.5f)

            val allTrackers = db.trackerDao().getAllTrackersList()
            val activeTrackers = allTrackers.filter { !it.isArchived }

            val savedWidgetId = getSelectedWidgetTrackerId(context)
            val appSelectedId = prefsMap["selected_tracker_id"] ?: SampleTrackerSeeder.SAMPLE_TRACKER_ID
            val activeTracker = activeTrackers.find { it.id == savedWidgetId }
                ?: activeTrackers.find { it.id == appSelectedId }
                ?: activeTrackers.firstOrNull()

            if (activeTracker != null && savedWidgetId != activeTracker.id) {
                setSelectedWidgetTrackerId(context, activeTracker.id)
            }

            val checkInPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                Intent(context, HabitAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_CHECK_IN
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                Intent(context, HabitAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_PREV_HABIT
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                103,
                Intent(context, HabitAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_NEXT_HABIT
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            fun buildOpenAppIntent(targetTrackerId: String?): PendingIntent {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (!targetTrackerId.isNullOrBlank()) {
                        putExtra(ReminderManager.EXTRA_TRACKER_ID, targetTrackerId)
                    }
                }
                return PendingIntent.getActivity(
                    context,
                    if (targetTrackerId != null) ReminderManager.habitRequestCode(targetTrackerId) else 0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            if (activeTracker == null) {
                val openPi = buildOpenAppIntent(null)
                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_habit_today).apply {
                        setTextViewText(R.id.widget_app_label, context.getString(R.string.widget_default_today))
                        setTextViewText(R.id.widget_checklist_caption, context.getString(R.string.widget_caption_default))
                        setTextViewText(R.id.widget_habit_streak, "0/0")
                        setTextViewText(R.id.widget_habit_icon, "📦")
                        setTextViewText(R.id.widget_habit_title, context.getString(R.string.widget_empty_title))
                        setTextViewText(R.id.widget_habit_status, context.getString(R.string.widget_empty_subtitle))
                        setViewVisibility(R.id.widget_prev_container, View.GONE)
                        setViewVisibility(R.id.widget_next_container, View.GONE)
                        setViewVisibility(R.id.widget_btn_prev, View.GONE)
                        setViewVisibility(R.id.widget_btn_next, View.GONE)
                        setViewVisibility(R.id.widget_checklist_container, View.GONE)
                        setTextViewText(R.id.widget_btn_action, context.getString(R.string.widget_open_app))
                        setOnClickPendingIntent(R.id.widget_btn_action, openPi)
                        setOnClickPendingIntent(R.id.widget_root, openPi)
                    }
                    applyThemeToRemoteViews(
                        views = views,
                        palette = palette,
                        todayRingPercent = 0,
                        progressPercent = 0,
                        sevenDaySlots = emptyList(),
                        isDoneState = false,
                        isBadTracker = false,
                        density = density
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
                return
            }

            val rolloverHour = prefsMap["day_rollover_hour"]?.toIntOrNull() ?: 0
            val effectiveToday = ReminderManager.getEffectiveToday(rolloverHour, now)
            val weekAgoStr = effectiveToday.minusDays(14).toString()
            val recentEntries = db.habitDao().getEntriesFromDateFlow(weekAgoStr).first()

            val configs = allTrackers.map { t ->
                TrackerConfig(
                    id = t.id,
                    title = t.title,
                    icon = t.icon,
                    accentColor = t.accentColor,
                    type = t.type,
                    frequencyType = t.frequencyType,
                    targetDays = t.targetDays,
                    targetCount = t.targetCount,
                    timeOfDay = t.timeOfDay,
                    weeklyTarget = t.weeklyTarget,
                    isArchived = t.isArchived,
                    reminderEnabled = t.reminderEnabled,
                    reminderTime = t.reminderTime
                )
            }
            val dashboardState = TodayDashboardCalculator.build(
                trackers = configs,
                recentEntries = recentEntries,
                preferences = prefsMap,
                now = now
            )
            val activeItem = dashboardState.allItems.find { it.tracker.id == activeTracker.id }
            val activeEntries = db.habitDao().getEntriesForTrackerFlow(activeTracker.id).first()
            val activeConfig = configs.first { it.id == activeTracker.id }
            val freezeHistory = prefsMap["streak_freeze_history"].orEmpty()
            val freezeActiveDate = prefsMap["streak_freeze_date"]?.takeIf { it.isNotBlank() }
            val stats = HabitStatsCalculator.calculateStats(
                list = activeEntries,
                freezeActive = freezeActiveDate,
                freezeHist = freezeHistory,
                tracker = activeConfig,
                today = effectiveToday
            )

            val summaryHeader = when {
                dashboardState.dueTotalCount > 0 && dashboardState.dueCompletedCount >= dashboardState.dueTotalCount ->
                    "TODAY · ALL ${dashboardState.dueTotalCount} DONE ✓"
                dashboardState.dueTotalCount > 0 ->
                    "TODAY · ${dashboardState.dueCompletedCount}/${dashboardState.dueTotalCount} DONE"
                else -> "TODAY · ALL SET"
            }

            val todayRingPercent = (dashboardState.overallProgress * 100f).toInt().coerceIn(0, 100)
            val captionLabel = "${activeTrackers.size} active · 7d streak strip"

            val weeklyDoneDays = activeItem?.weeklyCompletedDays ?: 0
            val weeklyQuota = activeTracker.weeklyTarget.coerceIn(1, 7)
            val isWeeklyQuota = activeTracker.frequencyType == "weekly_quota" || activeTracker.frequencyType == "3x_week"

            val badgeText = when {
                activeItem?.status == TodayHabitStatus.FROZEN -> "🧊 Frozen"
                isWeeklyQuota -> "🎯 $weeklyDoneDays/${weeklyQuota}w"
                else -> "🔥 ${stats.currentStreak}d"
            }

            val todayCount = activeItem?.todayCount ?: 0
            val targetCount = activeTracker.targetCount.coerceAtLeast(1)
            val progressPercent = when {
                activeTracker.type == "bad" -> if (todayCount == 0) 100 else 0
                isWeeklyQuota -> ((weeklyDoneDays.toFloat() / weeklyQuota.toFloat()) * 100f).toInt().coerceIn(0, 100)
                else -> ((todayCount.toFloat() / targetCount.toFloat()) * 100f).toInt().coerceIn(0, 100)
            }

            val sevenDaySlots = buildSevenDaySlots(
                tracker = activeTracker,
                trackerEntries = activeEntries,
                freezeActiveDate = freezeActiveDate,
                freezeHistory = freezeHistory,
                effectiveToday = effectiveToday
            )

            val statusLine = when {
                activeTracker.type == "bad" -> {
                    if (todayCount > 0) "Slip logged today · Review" else "Clean today · ${stats.currentStreak}d streak"
                }
                else -> activeItem?.statusLabel ?: "Due today"
            }

            val openPi = buildOpenAppIntent(activeTracker.id)
            val isDoneToday = todayCount >= targetCount
            val buttonLabel: String
            val buttonPi: PendingIntent

            when {
                activeTracker.type == "bad" -> {
                    buttonLabel = context.getString(R.string.widget_open_app)
                    buttonPi = openPi
                }
                activeTracker.type == "good" && isDoneToday -> {
                    buttonLabel = "✓ Done"
                    buttonPi = openPi
                }
                targetCount > 1 -> {
                    buttonLabel = "+1 ($todayCount/$targetCount)"
                    buttonPi = checkInPendingIntent
                }
                activeTracker.type == "misc" -> {
                    buttonLabel = if (todayCount > 0) "+1 ($todayCount)" else "⚡ Log +1"
                    buttonPi = checkInPendingIntent
                }
                else -> {
                    buttonLabel = "⚡ Check In"
                    buttonPi = checkInPendingIntent
                }
            }

            val showSwitcher = activeTrackers.size > 1
            val navVisibility = if (showSwitcher) View.VISIBLE else View.GONE
            val checklistItems = if (activeTrackers.size > 1) {
                selectChecklistItems(dashboardState, activeTracker.id)
            } else {
                emptyList()
            }

            for (widgetId in appWidgetIds) {
                val options = try {
                    appWidgetManager.getAppWidgetOptions(widgetId)
                } catch (_: Exception) {
                    null
                }
                val minHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
                val showChecklist = (minHeightDp == 0 || minHeightDp >= 140) && checklistItems.isNotEmpty()

                val views = RemoteViews(context.packageName, R.layout.widget_habit_today).apply {
                    setTextViewText(R.id.widget_app_label, summaryHeader)
                    setTextViewText(R.id.widget_checklist_caption, captionLabel)
                    setTextViewText(R.id.widget_habit_streak, badgeText)
                    setViewVisibility(R.id.widget_prev_container, navVisibility)
                    setViewVisibility(R.id.widget_next_container, navVisibility)
                    setViewVisibility(R.id.widget_btn_prev, navVisibility)
                    setViewVisibility(R.id.widget_btn_next, navVisibility)
                    setTextViewText(R.id.widget_habit_icon, activeTracker.icon)
                    setTextViewText(R.id.widget_habit_title, activeTracker.title)
                    setTextViewText(R.id.widget_habit_status, statusLine)
                    setTextViewText(R.id.widget_btn_action, buttonLabel)
                    setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)
                    setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)
                    setOnClickPendingIntent(R.id.widget_btn_action, buttonPi)
                    setOnClickPendingIntent(R.id.widget_root, openPi)
                }
                applyThemeToRemoteViews(
                    views = views,
                    palette = palette,
                    todayRingPercent = todayRingPercent,
                    progressPercent = progressPercent,
                    sevenDaySlots = sevenDaySlots,
                    isDoneState = activeTracker.type == "good" && isDoneToday,
                    isBadTracker = activeTracker.type == "bad",
                    density = density
                )
                bindChecklistRows(
                    context = context,
                    views = views,
                    items = checklistItems,
                    palette = palette,
                    density = density,
                    showChecklist = showChecklist,
                    buildOpenAppIntent = ::buildOpenAppIntent
                )
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                performWidgetUpdate(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                Log.w("HabitAppWidgetProvider", "Failed updating widget onUpdate", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                performWidgetUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            } catch (e: Exception) {
                Log.w("HabitAppWidgetProvider", "Failed updating widget onAppWidgetOptionsChanged", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_WIDGET_CHECK_IN ||
            action == ACTION_WIDGET_CHECK_IN_TRACKER ||
            action == ACTION_WIDGET_PREV_HABIT ||
            action == ACTION_WIDGET_NEXT_HABIT ||
            action == ACTION_UPDATE_HABIT_WIDGET
        ) {
            val explicitTrackerId = intent.getStringExtra(EXTRA_WIDGET_TRACKER_ID)
            val pendingResult: PendingResult? = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    when (action) {
                        ACTION_WIDGET_CHECK_IN -> handleWidgetCheckIn(context, db)
                        ACTION_WIDGET_CHECK_IN_TRACKER -> handleWidgetCheckIn(context, db, trackerId = explicitTrackerId)
                        ACTION_WIDGET_PREV_HABIT -> cycleSelectedTracker(context, db, step = -1)
                        ACTION_WIDGET_NEXT_HABIT -> cycleSelectedTracker(context, db, step = 1)
                    }
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, HabitAppWidgetProvider::class.java)
                    )
                    performWidgetUpdate(context, appWidgetManager, ids, db)
                } catch (e: Exception) {
                    Log.w("HabitAppWidgetProvider", "Error processing widget action: $action", e)
                } finally {
                    pendingResult?.finish()
                }
            }
            return
        }

        super.onReceive(context, intent)
    }
}
