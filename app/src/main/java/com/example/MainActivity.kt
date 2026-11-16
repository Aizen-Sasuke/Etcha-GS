package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HabitViewModel
import com.example.ui.screens.MainContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.worker.ReminderManager

class MainActivity : ComponentActivity() {
    private var launchedTrackerId by mutableStateOf<String?>(null)

    @Suppress("DEPRECATION")
    private fun enableHighRefreshRate() {
        try {
            val activeDisplay: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display ?: windowManager.defaultDisplay
            } else {
                windowManager.defaultDisplay
            }
            val currentMode = activeDisplay?.mode
            val modes = activeDisplay?.supportedModes
            if (modes != null && modes.isNotEmpty()) {
                val sameResolutionModes = if (currentMode != null) {
                    modes.filter {
                        it.physicalWidth == currentMode.physicalWidth &&
                            it.physicalHeight == currentMode.physicalHeight
                    }
                } else {
                    emptyList()
                }
                val candidates = sameResolutionModes.ifEmpty { modes.toList() }
                val bestMode = candidates.maxByOrNull { it.refreshRate }
                if (bestMode != null && bestMode.refreshRate >= 90f) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = bestMode.modeId
                        preferredRefreshRate = bestMode.refreshRate
                    }
                }
            }
        } catch (_: Throwable) {
            // Safe fallback on environments/emulators without multi-refresh modes
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.ThemeStyles.init(applicationContext)
        launchedTrackerId = intent?.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
        enableEdgeToEdge()
        enableHighRefreshRate()
        setContent {
            val viewModel: HabitViewModel = viewModel()
            val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
            val customPrimaryColor by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
            val customBackgroundColor by viewModel.customBackgroundColor.collectAsStateWithLifecycle()
            val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()

            MyApplicationTheme(
                selectedTheme = selectedTheme,
                customPrimaryColor = customPrimaryColor,
                customBackgroundColor = customBackgroundColor,
                selectedFont = selectedFont
            ) {
                val targetTracker = launchedTrackerId
                LaunchedEffect(targetTracker) {
                    if (!targetTracker.isNullOrBlank()) {
                        viewModel.selectTracker(targetTracker)
                        launchedTrackerId = null
                    }
                }
                MainContainer(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val extraId = intent.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
        if (!extraId.isNullOrBlank()) {
            launchedTrackerId = extraId
        }
    }
}
