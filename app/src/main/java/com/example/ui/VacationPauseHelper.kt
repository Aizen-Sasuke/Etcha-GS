package com.example.ui

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class VacationPauseState(
    val globalEnabled: Boolean = false,
    val globalStartDate: String? = null,
    val globalEndDate: String? = null
)

object VacationPauseHelper {
    const val KEY_VACATION_ENABLED = "vacation_enabled"
    const val KEY_VACATION_START = "vacation_start_date"
    const val KEY_VACATION_END = "vacation_end_date"

    private val DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun fromPreferences(preferences: Map<String, String>): VacationPauseState {
        return VacationPauseState(
            globalEnabled = preferences[KEY_VACATION_ENABLED] == "true",
            globalStartDate = preferences[KEY_VACATION_START]?.takeIf { it.isNotBlank() },
            globalEndDate = preferences[KEY_VACATION_END]?.takeIf { it.isNotBlank() }
        )
    }

    fun isDateInGlobalVacation(date: LocalDate, vacation: VacationPauseState): Boolean {
        if (!vacation.globalEnabled) return false
        val start = parseOrNull(vacation.globalStartDate)
        val end = parseOrNull(vacation.globalEndDate)
        return when {
            start != null && end != null -> !date.isBefore(start) && !date.isAfter(end)
            start != null -> !date.isBefore(start)
            end != null -> !date.isAfter(end)
            else -> true
        }
    }

    fun isDatePausedForTracker(
        date: LocalDate,
        pausedUntilDate: String?,
        vacation: VacationPauseState,
        anchorToday: LocalDate = LocalDate.now()
    ): Boolean {
        if (isDateInGlobalVacation(date, vacation)) return true
        val pausedUntil = parseOrNull(pausedUntilDate) ?: return false
        return !date.isAfter(pausedUntil) && !date.isBefore(anchorToday.minusDays(365)) &&
            !pausedUntil.isBefore(anchorToday.minusDays(1))
    }

    fun isPausedOnEffectiveDate(
        date: LocalDate,
        pausedUntilDate: String?,
        vacation: VacationPauseState
    ): Boolean {
        if (isDateInGlobalVacation(date, vacation)) return true
        val pausedUntil = parseOrNull(pausedUntilDate) ?: return false
        return !date.isAfter(pausedUntil)
    }

    fun formatVacationSummary(vacation: VacationPauseState, today: LocalDate): String {
        if (!vacation.globalEnabled) return "Off"
        val end = parseOrNull(vacation.globalEndDate)
        return when {
            end != null && end.isBefore(today) -> "Ended ${end.format(DISPLAY_FMT)}"
            end != null -> "Active until ${end.format(DISPLAY_FMT)}"
            else -> "Active indefinitely"
        }
    }

    fun formatTrackerPauseBadge(pausedUntilDate: String?, vacation: VacationPauseState, today: LocalDate): String? {
        if (isDateInGlobalVacation(today, vacation)) {
            val end = parseOrNull(vacation.globalEndDate)
            return if (end != null) "Vacation until ${end.format(DISPLAY_FMT)}" else "Vacation active"
        }
        val pausedUntil = parseOrNull(pausedUntilDate) ?: return null
        if (today.isAfter(pausedUntil)) return null
        return "Paused until ${pausedUntil.format(DISPLAY_FMT)}"
    }

    private fun parseOrNull(raw: String?): LocalDate? = try {
        raw?.trim()?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
    } catch (_: Exception) {
        null
    }
}
