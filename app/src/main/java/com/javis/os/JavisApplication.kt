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
        val notificationManager = getSystemService(NotificationManager::class.java)

        val foregroundChannel = NotificationChannel(
            CHANNEL_FOREGROUND,
            "JAVIS Assistant",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "JAVIS is running in the background"
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "JAVIS Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "JAVIS important alerts and responses"
        }

        val notifChannel = NotificationChannel(
            CHANNEL_NOTIFICATIONS,
            "Notification Summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Summaries of your notifications"
        }

        notificationManager.createNotificationChannels(
            listOf(foregroundChannel, alertChannel, notifChannel)
        )
    }

    companion object {
        const val CHANNEL_FOREGROUND = "javis_foreground"
        const val CHANNEL_ALERTS = "javis_alerts"
        const val CHANNEL_NOTIFICATIONS = "javis_notifications"
        const val NOTIFICATION_ID_FOREGROUND = 1001
    }
}
