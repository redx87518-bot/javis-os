package com.javis.os

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.os.service.JavisForegroundService
import com.javis.os.ui.JavisNavHost
import com.javis.os.ui.theme.JavisTheme
import com.javis.os.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled in VM */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            JavisTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val startVoiceMode by viewModel.startVoiceMode.collectAsState()

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                    )
                    startForegroundService()
                    viewModel.onAppStarted()
                }

                JavisNavHost(
                    startVoiceMode = startVoiceMode,
                    onVoiceModeHandled = viewModel::onVoiceModeHandled
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == "com.javis.os.ACTIVATE_VOICE") {
            // handled by nav host
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, JavisForegroundService::class.java).apply {
            action = JavisForegroundService.ACTION_START
        }
        startForegroundService(intent)
    }
}
