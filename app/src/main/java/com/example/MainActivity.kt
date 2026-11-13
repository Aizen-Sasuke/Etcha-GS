package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HabitViewModel
import com.example.ui.screens.MainContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.worker.ReminderManager

class MainActivity : ComponentActivity() {
    private var launchedTrackerId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.ThemeStyles.init(applicationContext)
        launchedTrackerId = intent?.getStringExtra(ReminderManager.EXTRA_TRACKER_ID)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: HabitViewModel = viewModel()
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
