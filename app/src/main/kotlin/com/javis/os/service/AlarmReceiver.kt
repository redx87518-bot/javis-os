package com.javis.os.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.javis.os.JavisApplication
import com.javis.os.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label") ?: "JAVIS Reminder"
        val notificationId = intent.getIntExtra("notification_id", 2000)

        val notification = NotificationCompat.Builder(context, JavisApplication.CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_javis_tile)
            .setContentTitle("JAVIS Reminder")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
