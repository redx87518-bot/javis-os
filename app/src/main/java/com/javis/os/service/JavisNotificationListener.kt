package com.javis.os.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class JavisNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class JavisNotificationListener : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<JavisNotification>>(emptyList())
        val notifications: StateFlow<List<JavisNotification>> = _notifications

        private val _latestNotification = MutableStateFlow<JavisNotification?>(null)
        val latestNotification: StateFlow<JavisNotification?> = _latestNotification

        private val monitoredApps = setOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.google.android.gm",
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return
        val packageName = sbn.packageName

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) { packageName }

        val notification = JavisNotification(
            key = sbn.key,
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        val current = _notifications.value.toMutableList()
        current.removeAll { it.key == sbn.key }
        current.add(0, notification)
        if (current.size > 100) current.removeAt(current.size - 1)
        _notifications.value = current

        if (packageName in monitoredApps) {
            _latestNotification.value = notification
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        _notifications.value = _notifications.value.filter { it.key != sbn.key }
    }

    fun getSummary(): String {
        val grouped = _notifications.value.groupBy { it.appName }
        if (grouped.isEmpty()) return "No new notifications."
        return grouped.entries.joinToString(". ") { (app, notifs) ->
            "$app: ${notifs.size} notification${if (notifs.size > 1) "s" else ""}"
        }
    }
}
