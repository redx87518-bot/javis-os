package com.javis.os

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.javis.os.service.JavisAssistantService

/**
 * Transparent launcher activity for Redmi A1 and devices without overlay/accessibility shortcut support.
 *
 * Add this to your home screen via Settings → "Add Home Screen Shortcut".
 * Tapping it immediately opens JAVIS in listening mode.
 */
class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the assistant service (keep-alive)
        val serviceIntent = Intent(this, JavisAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Launch MainActivity in listening mode
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_START_LISTENING
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(mainIntent)
        finish() // Close this transparent activity immediately
    }

    companion object {
        const val ACTION_START_LISTENING = "com.javis.os.ACTION_START_LISTENING"
    }
}
