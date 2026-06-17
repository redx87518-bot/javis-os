package com.javis.os.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val userName by viewModel.userName.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val voiceSpeed by viewModel.voiceSpeed.collectAsState()
    val voicePitch by viewModel.voicePitch.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    var userNameInput by remember(userName) { mutableStateOf(userName) }
    var apiKeyInput by remember(aiApiKey) { mutableStateOf(aiApiKey) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection("Profile") {
                SettingsTextField(
                    label = "Your Name",
                    value = userNameInput,
                    onValueChange = { userNameInput = it },
                    onDone = { viewModel.setUserName(userNameInput) },
                    icon = Icons.Default.Person
                )
            }

            SettingsSection("AI Provider") {
                SettingsDropdown(
                    label = "Provider",
                    value = aiProvider,
                    options = listOf("gemini" to "Google Gemini (Recommended)", "openai" to "OpenAI GPT"),
                    onSelect = { viewModel.setAiProvider(it) },
                    icon = Icons.Default.SmartToy
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsTextField(
                    label = "API Key",
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    onDone = { viewModel.setAiApiKey(apiKeyInput) },
                    icon = Icons.Default.Key,
                    isPassword = true,
                    hint = "Leave empty to use built-in key"
                )
            }

            SettingsSection("Voice") {
                SettingsSlider(
                    label = "Speech Speed",
                    value = voiceSpeed,
                    range = 0.5f..2.0f,
                    onValueChange = viewModel::setVoiceSpeed
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSlider(
                    label = "Speech Pitch",
                    value = voicePitch,
                    range = 0.5f..2.0f,
                    onValueChange = viewModel::setVoicePitch
                )
            }

            SettingsSection("Notifications") {
                SettingsSwitch(
                    label = "Enable Notification Reading",
                    checked = notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                    icon = Icons.Default.Notifications
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsActionButton(
                    label = "Grant Notification Access",
                    icon = Icons.Default.NotificationsActive,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                )
            }

            SettingsSection("Shortcuts") {
                SettingsActionButton(
                    label = "Add JAVIS to Home Screen",
                    icon = Icons.Default.AddToHomeScreen,
                    onClick = { viewModel.addHomeScreenShortcut() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsActionButton(
                    label = "Accessibility Settings",
                    icon = Icons.Default.Accessibility,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                )
            }

            SettingsSection("Data") {
                SettingsActionButton(
                    label = "Clear Memory",
                    icon = Icons.Default.Psychology,
                    onClick = { viewModel.clearMemory() },
                    tint = JavisOrange
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsActionButton(
                    label = "Clear Conversation History",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { showClearHistoryDialog = true },
                    tint = JavisRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History", color = TextPrimary) },
            text = { Text("All conversation history will be deleted.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear", color = JavisRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = JavisCyan,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    icon: ImageVector,
    isPassword: Boolean = false,
    hint: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = JavisCyan, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JavisCyan,
            unfocusedBorderColor = BorderDark,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = JavisCyan,
        ),
        trailingIcon = {
            TextButton(onClick = onDone) {
                Text("Save", color = JavisCyan, style = MaterialTheme.typography.labelMedium)
            }
        },
        singleLine = true,
        placeholder = if (hint.isNotBlank()) {
            { Text(hint, color = TextDim, style = MaterialTheme.typography.bodySmall) }
        } else null
    )
}

@Composable
private fun SettingsDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    icon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = options.find { it.first == value }?.second ?: value

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            label = { Text(label, color = TextSecondary) },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = JavisCyan, modifier = Modifier.size(20.dp))
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JavisCyan,
                unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardDark)
        ) {
            options.forEach { (key, display) ->
                DropdownMenuItem(
                    text = { Text(display, color = if (key == value) JavisCyan else TextPrimary) },
                    onClick = { onSelect(key); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(String.format("%.1f", value), style = MaterialTheme.typography.bodySmall, color = JavisCyan)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = JavisCyan,
                activeTrackColor = JavisCyan,
                inactiveTrackColor = BorderDark
            )
        )
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = JavisCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundDark,
                checkedTrackColor = JavisCyan
            )
        )
    }
}

@Composable
private fun SettingsActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = JavisCyan
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = tint, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}
