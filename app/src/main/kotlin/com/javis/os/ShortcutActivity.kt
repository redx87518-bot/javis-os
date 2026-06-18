package com.javis.os

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.javis.os.service.JavisAssistantService
import com.javis.os.ui.screens.VoiceSessionActivity

/**
 * ShortcutActivity — transparent trampoline for the home screen shortcut.
 *
 * Tapping the home screen icon does NOT open the full app.
 * Instead it launches VoiceSessionActivity — JAVIS greets you, listens,
 * responds and stays alive for a full back-and-forth conversation.
 *
 * Works on Redmi A1 / MIUI without overlay permissions.
 */
class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the background service alive
        val serviceIntent = Intent(this, JavisAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Open the persistent conversational voice screen
        val voiceIntent = Intent(this, VoiceSessionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(voiceIntent)

        finish() // Close this transparent trampoline immediately
    }

    companion object {
        const val ACTION_START_LISTENING = "com.javis.os.ACTION_START_LISTENING"
    }
}
