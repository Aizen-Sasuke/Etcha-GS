package com.example.ui

import androidx.compose.runtime.Immutable
import org.json.JSONObject

enum class FocusMode(val label: String) {
    COUNTDOWN("Focus"),
    POMODORO("Pomodoro"),
    STOPWATCH("Stopwatch")
}

enum class FocusPhase {
    IDLE,
    FOCUS_RUNNING,
    FOCUS_PAUSED,
    BREAK_RUNNING,
    BREAK_PAUSED,
    COMPLETED
}

@Immutable
data class FocusSessionState(
    val isSheetVisible: Boolean = false,
    val trackerId: String = "",
    val trackerTitle: String = "",
    val trackerIcon: String = "⏱️",
    val mode: FocusMode = FocusMode.POMODORO,
    val phase: FocusPhase = FocusPhase.IDLE,
    val targetDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val completedPomodoroCycles: Int = 0,
    val startedAtEpochMs: Long = 0L,
    val accumulatedElapsedMs: Long = 0L,
    val lastCompletedLoggedMinutes: Int = 0
) {
    val isActiveOrPaused: Boolean
        get() = trackerId.isNotBlank() && phase != FocusPhase.IDLE && phase != FocusPhase.COMPLETED

    val isRunning: Boolean
        get() = phase == FocusPhase.FOCUS_RUNNING || phase == FocusPhase.BREAK_RUNNING

    val isBreakPhase: Boolean
        get() = phase == FocusPhase.BREAK_RUNNING || phase == FocusPhase.BREAK_PAUSED

    fun phaseTargetMillis(): Long = when (phase) {
        FocusPhase.BREAK_RUNNING, FocusPhase.BREAK_PAUSED -> breakDurationMinutes.coerceAtLeast(1) * 60_000L
        else -> targetDurationMinutes.coerceAtLeast(1) * 60_000L
    }

    fun elapsedMillis(nowMs: Long = System.currentTimeMillis()): Long {
        val activeDelta = if (isRunning && startedAtEpochMs > 0L) {
            (nowMs - startedAtEpochMs).coerceAtLeast(0L)
        } else {
            0L
        }
        return (accumulatedElapsedMs + activeDelta).coerceAtLeast(0L)
    }

    fun remainingMillis(nowMs: Long = System.currentTimeMillis()): Long {
        if (mode == FocusMode.STOPWATCH && !isBreakPhase) {
            return elapsedMillis(nowMs)
        }
        val targetMs = phaseTargetMillis()
        return (targetMs - elapsedMillis(nowMs)).coerceAtLeast(0L)
    }

    fun progressFraction(nowMs: Long = System.currentTimeMillis()): Float {
        if (mode == FocusMode.STOPWATCH && !isBreakPhase) {
            val sixtyMinMs = 60 * 60_000L
            return ((elapsedMillis(nowMs) % sixtyMinMs).toFloat() / sixtyMinMs.toFloat()).coerceIn(0f, 1f)
        }
        val targetMs = phaseTargetMillis().coerceAtLeast(1L)
        return (elapsedMillis(nowMs).toFloat() / targetMs.toFloat()).coerceIn(0f, 1f)
    }

    fun hasReachedPhaseTarget(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (mode == FocusMode.STOPWATCH && !isBreakPhase) return false
        return isRunning && elapsedMillis(nowMs) >= phaseTargetMillis()
    }

    fun toPersistedJson(): String {
        val obj = JSONObject()
        obj.put("trackerId", trackerId)
        obj.put("trackerTitle", trackerTitle)
        obj.put("trackerIcon", trackerIcon)
        obj.put("mode", mode.name)
        obj.put("phase", phase.name)
        obj.put("targetDurationMinutes", targetDurationMinutes)
        obj.put("breakDurationMinutes", breakDurationMinutes)
        obj.put("completedPomodoroCycles", completedPomodoroCycles)
        obj.put("startedAtEpochMs", startedAtEpochMs)
        obj.put("accumulatedElapsedMs", accumulatedElapsedMs)
        return obj.toString()
    }

    companion object {
        const val PREF_KEY_ACTIVE_FOCUS_SESSION = "active_focus_session_json"

        fun formatClockMillis(ms: Long): String {
            val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return if (hours > 0L) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

        fun fromPersistedJson(raw: String?): FocusSessionState? {
            if (raw.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val trackerId = obj.optString("trackerId", "")
                val phaseStr = obj.optString("phase", FocusPhase.IDLE.name)
                val phase = FocusPhase.entries.firstOrNull { it.name == phaseStr } ?: FocusPhase.IDLE
                if (trackerId.isBlank() || phase == FocusPhase.IDLE || phase == FocusPhase.COMPLETED) {
                    return null
                }
                val modeStr = obj.optString("mode", FocusMode.POMODORO.name)
                val mode = FocusMode.entries.firstOrNull { it.name == modeStr } ?: FocusMode.POMODORO
                FocusSessionState(
                    isSheetVisible = false,
                    trackerId = trackerId,
                    trackerTitle = obj.optString("trackerTitle", ""),
                    trackerIcon = obj.optString("trackerIcon", "⏱️"),
                    mode = mode,
                    phase = phase,
                    targetDurationMinutes = obj.optInt("targetDurationMinutes", 25).coerceIn(1, 180),
                    breakDurationMinutes = obj.optInt("breakDurationMinutes", 5).coerceIn(1, 30),
                    completedPomodoroCycles = obj.optInt("completedPomodoroCycles", 0).coerceAtLeast(0),
                    startedAtEpochMs = obj.optLong("startedAtEpochMs", 0L),
                    accumulatedElapsedMs = obj.optLong("accumulatedElapsedMs", 0L)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
