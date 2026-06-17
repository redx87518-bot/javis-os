package com.javis.os.agent

import com.javis.os.service.CapturedNotification
import com.javis.os.service.JavisNotificationListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSummaryAgent @Inject constructor() {

    fun getSummary(): String {
        val notifications = JavisNotificationListener.notifications.value
        if (notifications.isEmpty()) return "You have no unread notifications right now."

        val grouped = notifications.groupBy { it.appName }
        val total = notifications.size

        return buildString {
            appendLine("You have $total notification${if (total > 1) "s" else ""}:")
            grouped.entries.take(5).forEach { (app, notifs) ->
                val count = notifs.size
                if (count == 1) {
                    val n = notifs.first()
                    appendLine("• $app: ${n.title} — ${n.text.take(60)}${if (n.text.length > 60) "…" else ""}")
                } else {
                    appendLine("• $app: $count messages")
                    notifs.take(2).forEach { n ->
                        appendLine("  - ${n.title}: ${n.text.take(40)}${if (n.text.length > 40) "…" else ""}")
                    }
                }
            }
            if (grouped.size > 5) {
                appendLine("…and ${grouped.size - 5} more apps.")
            }
        }.trim()
    }

    fun getLatestFrom(appName: String): String {
        val notifications = JavisNotificationListener.notifications.value
        val appNotifs = notifications.filter {
            it.appName.lowercase().contains(appName.lowercase()) ||
            it.packageName.lowercase().contains(appName.lowercase())
        }
        if (appNotifs.isEmpty()) return "No recent notifications from $appName."
        val latest = appNotifs.first()
        return "Latest from ${latest.appName}: \"${latest.title}\" — ${latest.text}"
    }
}
