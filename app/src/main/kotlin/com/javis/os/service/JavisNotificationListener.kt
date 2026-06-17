package com.javis.os.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CapturedNotification(
    val id: Int,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class JavisNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            packageManager.getApplicationInfo(sbn.packageName, 0).loadLabel(packageManager).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val captured = CapturedNotification(
            id = sbn.id,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text
        )

        Log.d("NotificationListener", "Captured: $appName — $title")
        val current = _notifications.value.toMutableList()
        current.add(0, captured)
        _notifications.value = current.take(50) // Keep last 50
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        _notifications.value = _notifications.value.filter { it.id != sbn.id }
    }

    companion object {
        private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val notifications: StateFlow<List<CapturedNotification>> = _notifications
    }
}
