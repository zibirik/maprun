package com.example.runner

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.runner.presentation.AppViewModel
import com.example.runner.presentation.navigation.AppNavGraph
import com.example.runner.ui.theme.MapRunTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private var startFromWidget by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeStartIntent(intent)
        keepScreenOnWhileRunning()
        setContent {
            val settings by appViewModel.settings.collectAsStateWithLifecycle()
            val runActive by appViewModel.runActive.collectAsStateWithLifecycle()
            MapRunTheme(themeMode = settings.themeMode) {
                AppNavGraph(
                    themeMode = settings.themeMode,
                    startFromWidget = startFromWidget,
                    runActive = runActive,
                    onConsumedWidgetStart = { startFromWidget = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeStartIntent(intent)
    }

    private fun consumeStartIntent(intent: Intent?) {
        if (intent?.data?.host == "start") {
            startFromWidget = true
        }
    }

    private fun keepScreenOnWhileRunning() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appViewModel.runActive.collect { active ->
                    if (active) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
    }
}
