package com.javis.os.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSavedMessage()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 80.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = CyanPrimary)
            }
            HorizontalDivider(color = CyanPrimary.copy(alpha = 0.2f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile
                SettingsSection(title = "Profile", icon = Icons.Default.Person) {
                    JavisTextField(
                        value = state.userName,
                        onValueChange = viewModel::updateUserName,
                        label = "Your Name",
                        placeholder = "e.g. Musa"
                    )
                }

                // AI Provider
                SettingsSection(title = "AI Provider", icon = Icons.Default.SmartToy) {
                    ProviderSelector(
                        selected = state.aiProvider,
                        onSelect = viewModel::updateAiProvider
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    JavisApiKeyField(
                        value = state.groqApiKey,
                        onValueChange = viewModel::updateGroqApiKey,
                        label = "Groq API Key",
                        hint = "Get free key at console.groq.com"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GroqModelSelector(
                        selected = state.groqModel,
                        onSelect = viewModel::updateGroqModel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    JavisApiKeyField(
                        value = state.deepSeekApiKey,
                        onValueChange = viewModel::updateDeepSeekApiKey,
                        label = "DeepSeek API Key (fallback)",
                        hint = "platform.deepseek.com"
                    )
                }

                // Voice Output
                SettingsSection(title = "Voice Output", icon = Icons.Default.RecordVoiceOver) {
                    TtsProviderSelector(
                        selected = state.ttsProvider,
                        onSelect = viewModel::updateTtsProvider
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    JavisApiKeyField(
                        value = state.elevenLabsApiKey,
                        onValueChange = viewModel::updateElevenLabsApiKey,
                        label = "ElevenLabs API Key",
                        hint = "elevenlabs.io — for natural voice"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    JavisTextField(
                        value = state.elevenLabsVoiceId,
                        onValueChange = viewModel::updateElevenLabsVoiceId,
                        label = "Voice ID",
                        placeholder = "e.g. EXAVITQu4vr4xnSDxMaL"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Text(
                            "Speech Speed: ${String.format("%.1f", state.ttsSpeed)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceLight.copy(alpha = 0.7f)
                        )
                        Slider(
                            value = state.ttsSpeed,
                            onValueChange = viewModel::updateTtsSpeed,
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary,
                                inactiveTrackColor = SurfaceVariant
                            )
                        )
                    }
                }

                // Offline Mode
                SettingsSection(title = "Connectivity", icon = Icons.Default.WifiOff) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Offline Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceLight
                            )
                            Text(
                                "Uses local responses only",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF546E7A)
                            )
                        }
                        Switch(
                            checked = state.offlineMode,
                            onCheckedChange = viewModel::updateOfflineMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = CyanPrimary
                            )
                        )
                    }
                }

                // Save Button
                Button(
                    onClick = viewModel::saveSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = BackgroundDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Settings",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = CyanPrimary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CyanPrimary.copy(alpha = 0.15f))
            content()
        }
    }
}

@Composable
private fun JavisTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF546E7A)) },
        placeholder = { Text(placeholder, color = Color(0xFF37474F)) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = Color(0xFF263238),
            focusedTextColor = OnSurfaceLight,
            unfocusedTextColor = OnSurfaceLight,
            cursorColor = CyanPrimary,
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = SurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun JavisApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String = ""
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF546E7A)) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF546E7A)
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = Color(0xFF263238),
            focusedTextColor = OnSurfaceLight,
            unfocusedTextColor = OnSurfaceLight,
            cursorColor = CyanPrimary,
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = SurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        supportingText = if (hint.isNotBlank()) {{ Text(hint, color = Color(0xFF546E7A), style = MaterialTheme.typography.labelSmall) }} else null
    )
}

@Composable
private fun ProviderSelector(selected: String, onSelect: (String) -> Unit) {
    val providers = listOf("groq" to "Groq (Fast, Free tier)", "deepseek" to "DeepSeek")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        providers.forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = CyanPrimary
                )
            )
        }
    }
}

@Composable
private fun GroqModelSelector(selected: String, onSelect: (String) -> Unit) {
    val models = listOf("llama3-70b-8192", "llama3-8b-8192", "mixtral-8x7b-32768")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Groq Model", color = Color(0xFF546E7A)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = Color(0xFF263238),
                focusedTextColor = OnSurfaceLight,
                unfocusedTextColor = OnSurfaceLight,
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model, color = OnSurfaceLight) },
                    onClick = { onSelect(model); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun TtsProviderSelector(selected: String, onSelect: (String) -> Unit) {
    val providers = listOf("android" to "Android TTS", "elevenlabs" to "ElevenLabs (Human)")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        providers.forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurpleAccent.copy(alpha = 0.2f),
                    selectedLabelColor = PurpleAccent
                )
            )
        }
    }
}
