package com.javis.os.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.javis.os.R
import com.javis.os.ShortcutActivity

object JavisShortcutManager {

    const val SHORTCUT_ID = "javis_listen_shortcut"

    /**
     * Adds a pinned shortcut to the home screen.
     * Works on all Android 8+ launchers including MIUI (Redmi A1).
     * Shows the system dialog asking user to confirm adding the shortcut.
     */
    fun addToHomeScreen(activity: Activity) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(activity)) {
            val shortcutIntent = Intent(activity, ShortcutActivity::class.java).apply {
                action = ShortcutActivity.ACTION_START_LISTENING
            }

            val shortcut = ShortcutInfoCompat.Builder(activity, SHORTCUT_ID)
                .setShortLabel("JAVIS")
                .setLongLabel("Activate JAVIS")
                .setIcon(IconCompat.createWithResource(activity, R.drawable.ic_javis_tile))
                .setIntent(shortcutIntent)
                .build()

            // Request the system to pin it — shows a dialog to the user
            ShortcutManagerCompat.requestPinShortcut(activity, shortcut, null)
        } else {
            // Fallback for older launchers: broadcast the old-style shortcut intent
            addLegacyShortcut(activity)
        }
    }

    private fun addLegacyShortcut(context: Context) {
        val shortcutIntent = Intent(context, ShortcutActivity::class.java).apply {
            action = ShortcutActivity.ACTION_START_LISTENING
        }

        @Suppress("DEPRECATION")
        val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, "JAVIS")
            putExtra("duplicate", false)
        }
        context.sendBroadcast(addIntent)
    }
}
