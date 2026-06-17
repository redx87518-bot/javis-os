package com.javis.os.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.javis.os.service.CapturedNotification
import com.javis.os.service.JavisNotificationListener
import com.javis.os.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(navController: NavController) {
    val notifications by JavisNotificationListener.notifications.collectAsState()
    val grouped = notifications.groupBy { it.appName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Notifications", style = MaterialTheme.typography.titleLarge, color = CyanPrimary)
            Text(
                "${notifications.size} unread",
                style = MaterialTheme.typography.labelMedium,
                color = if (notifications.isNotEmpty()) WarningAmber else Color(0xFF546E7A),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        HorizontalDivider(color = CyanPrimary.copy(alpha = 0.2f))

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = CyanPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notifications captured",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceLight.copy(alpha = 0.5f)
                    )
                    Text(
                        "Enable Notification Access in Android\nSettings > Apps > Special Access",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF546E7A),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (appName, notifs) ->
                    item {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                    items(notifs) { notification ->
                        NotificationCard(notification = notification)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: CapturedNotification) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeFormat.format(Date(notification.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF546E7A)
                )
            }
            if (notification.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceLight.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
        }
    }
}
