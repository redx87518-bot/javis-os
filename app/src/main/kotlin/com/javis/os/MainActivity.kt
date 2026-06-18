package com.javis.os

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.javis.os.ui.navigation.JavisNavGraph
import com.javis.os.ui.theme.JavisTheme
import com.javis.os.ui.theme.CyanPrimary
import com.javis.os.util.AppScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appScanner: AppScanner

    private val essentialPermissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
    }

    private var showPermissionScreen by mutableStateOf(false)
    private var missingPermissions by mutableStateOf<List<String>>(emptyList())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val stillMissing = results.entries
            .filter { !it.value }
            .map { formatPermissionName(it.key) }
        missingPermissions = stillMissing
        if (stillMissing.isEmpty()) showPermissionScreen = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Background: scan installed apps
        lifecycleScope.launch(Dispatchers.IO) {
            appScanner.scanAndStore()
        }

        // Check permissions
        val missing = essentialPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PermissionChecker.PERMISSION_GRANTED
        }

        showPermissionScreen = missing.isNotEmpty()
        missingPermissions = missing.map { formatPermissionName(it) }

        setContent {
            JavisTheme {
                if (showPermissionScreen) {
                    PermissionsScreen(
                        missingPermissions = missingPermissions,
                        onGrantAll = {
                            permissionLauncher.launch(
                                essentialPermissions
                                    .filter { perm ->
                                        ContextCompat.checkSelfPermission(
                                            this@MainActivity, perm
                                        ) != PermissionChecker.PERMISSION_GRANTED
                                    }
                                    .toTypedArray()
                            )
                        },
                        onOpenSettings = {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        },
                        onContinueAnyway = {
                            showPermissionScreen = false
                        }
                    )
                } else {
                    JavisNavGraph()
                }
            }
        }
    }

    private fun formatPermissionName(perm: String): String = when {
        perm.contains("RECORD_AUDIO") -> "Microphone"
        perm.contains("READ_CONTACTS") -> "Contacts"
        perm.contains("CALL_PHONE") -> "Phone Calls"
        perm.contains("PHONE_STATE") -> "Phone State"
        perm.contains("NOTIFICATIONS") -> "Notifications"
        perm.contains("SCHEDULE_EXACT_ALARM") -> "Exact Alarms"
        perm.contains("SEND_SMS") -> "Send SMS"
        perm.contains("READ_SMS") -> "Read SMS"
        perm.contains("MEDIA_IMAGES") -> "Media Access"
        else -> perm.substringAfterLast(".").replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun PermissionsScreen(
    missingPermissions: List<String>,
    onGrantAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinueAnyway: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050E17)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Text("JAVIS needs a few permissions", color = CyanPrimary,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            Text("These let JAVIS call contacts, set alarms, listen to your voice, and more.",
                color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)

            Spacer(Modifier.height(24.dp))

            // Permission list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val allPermissions = listOf(
                    "Microphone" to "Voice input — core feature",
                    "Contacts" to "Call and message people by name",
                    "Phone Calls" to "Make calls via dialer",
                    "Notifications" to "Read and summarize your notifications",
                    "Exact Alarms" to "Set alarms at precise times"
                )
                allPermissions.forEach { (name, desc) ->
                    val missing = missingPermissions.contains(name)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (missing) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (missing) Color(0xFFF59E0B) else Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name, color = Color.White, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onGrantAll,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Grant All Permissions", color = Color.Black, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Open App Settings", color = Color.White)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onContinueAnyway) {
                Text("Continue with limited features", color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
