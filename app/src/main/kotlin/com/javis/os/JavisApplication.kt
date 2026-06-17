package com.javis.os

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JavisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val assistantChannel = NotificationChannel(
                CHANNEL_ASSISTANT,
                getString(R.string.notification_channel_assistant),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "JAVIS assistant persistent notification"
                setShowBadge(false)
            }

            val alarmsChannel = NotificationChannel(
                CHANNEL_ALARMS,
                getString(R.string.notification_channel_alarms),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "JAVIS alarms and reminders"
            }

            manager.createNotificationChannel(assistantChannel)
            manager.createNotificationChannel(alarmsChannel)
        }
    }

    companion object {
        const val CHANNEL_ASSISTANT = "javis_assistant"
        const val CHANNEL_ALARMS = "javis_alarms"
    }
}
