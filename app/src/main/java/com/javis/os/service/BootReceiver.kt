package com.javis.os.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val serviceIntent = Intent(context, JavisForegroundService::class.java).apply {
                action = JavisForegroundService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
