package com.javis.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.javis.os.ui.navigation.JavisNavGraph
import com.javis.os.ui.theme.JavisTheme
import com.javis.os.util.AppScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appScanner: AppScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Scan installed apps in background on first launch
        lifecycleScope.launch(Dispatchers.IO) {
            appScanner.scanAndStore()
        }

        setContent {
            JavisTheme {
                JavisNavGraph()
            }
        }
    }
}
