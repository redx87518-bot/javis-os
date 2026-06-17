package com.javis.os.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.javis.os.JavisApplication
import com.javis.os.MainActivity
import com.javis.os.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JavisAssistantService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val mainIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val listenIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).apply {
                action = "com.javis.os.ACTION_START_LISTENING"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, JavisAssistantService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JavisApplication.CHANNEL_ASSISTANT)
            .setContentTitle("JAVIS ONLINE")
            .setContentText("Tap to open · Long press to listen")
            .setSmallIcon(R.drawable.ic_javis_tile)
            .setContentIntent(mainIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "Listen", listenIntent)
            .addAction(0, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.javis.os.ACTION_STOP_SERVICE"
    }
}
