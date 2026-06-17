package com.javis.os.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.os.service.JavisNotification
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("${notifications.size}", style = MaterialTheme.typography.bodySmall, color = JavisCyan)
            }

            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = TextDim, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No notifications", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Enable notification access for JAVIS", style = MaterialTheme.typography.bodySmall, color = TextDim)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisCyan)
                        ) {
                            Text("Grant Access")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = notifications.groupBy { it.appName }
                    grouped.forEach { (appName, notifs) ->
                        item {
                            Text(
                                appName,
                                style = MaterialTheme.typography.labelMedium,
                                color = JavisCyan,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(notifs.take(5), key = { it.key }) { notification ->
                            NotificationCard(notification)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: JavisNotification) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(notification.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        }
        if (notification.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(notification.text, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
        }
    }
}
