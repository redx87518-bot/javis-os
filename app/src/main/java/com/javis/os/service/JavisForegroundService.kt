package com.javis.os.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.javis.os.JavisApplication
import com.javis.os.MainActivity
import com.javis.os.R
import com.javis.os.voice.TextToSpeechManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class JavisForegroundService : Service() {

    @Inject lateinit var ttsManager: TextToSpeechManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ttsManager.initialize()
        startForeground(JavisApplication.NOTIFICATION_ID_FOREGROUND, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> { /* already started */ }
            ACTION_STOP -> stopSelf()
            ACTION_LISTEN -> {
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    action = "com.javis.os.ACTIVATE_VOICE"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(mainIntent)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val listenIntent = PendingIntent.getService(
            this, 1,
            Intent(this, JavisForegroundService::class.java).apply { action = ACTION_LISTEN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, JavisForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JavisApplication.CHANNEL_FOREGROUND)
            .setContentTitle("JAVIS ONLINE")
            .setContentText("Your AI companion is ready")
            .setSmallIcon(R.drawable.ic_javis)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_javis, "Listen", listenIntent)
            .addAction(R.drawable.ic_javis, "Stop", stopIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.javis.os.START"
        const val ACTION_STOP = "com.javis.os.STOP"
        const val ACTION_LISTEN = "com.javis.os.LISTEN"
    }
}
