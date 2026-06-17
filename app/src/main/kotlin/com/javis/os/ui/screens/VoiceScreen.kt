package com.javis.os.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.AssistantStatus
import com.javis.os.ui.viewmodel.AssistantViewModel
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceScreen(
    navController: NavController,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        // Animated rings in background
        AnimatedRings(
            isActive = uiState.isListening || uiState.isSpeaking,
            color = when (uiState.assistantStatus) {
                AssistantStatus.LISTENING -> CyanPrimary
                AssistantStatus.SPEAKING -> PurpleAccent
                AssistantStatus.THINKING -> WarningAmber
                else -> CyanPrimary.copy(alpha = 0.3f)
            }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            // Title
            Text(
                text = "JAVIS OS",
                style = MaterialTheme.typography.headlineLarge,
                color = CyanPrimary,
                letterSpacing = 4.sp
            )

            Text(
                text = when (uiState.assistantStatus) {
                    AssistantStatus.IDLE -> "Ready"
                    AssistantStatus.LISTENING -> "Listening..."
                    AssistantStatus.THINKING -> "Thinking..."
                    AssistantStatus.SPEAKING -> "Speaking..."
                    AssistantStatus.ERROR -> "Error"
                },
                style = MaterialTheme.typography.labelLarge,
                color = when (uiState.assistantStatus) {
                    AssistantStatus.LISTENING -> CyanPrimary
                    AssistantStatus.THINKING -> WarningAmber
                    AssistantStatus.SPEAKING -> PurpleAccent
                    AssistantStatus.ERROR -> ErrorRed
                    else -> Color(0xFF546E7A)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mic Button
            val scale by animateFloatAsState(
                targetValue = if (uiState.isListening) 1.1f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "micScale"
            )

            Button(
                onClick = {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                    } else if (uiState.isListening) {
                        viewModel.stopListening()
                    } else if (uiState.isSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.startListening()
                    }
                },
                modifier = Modifier
                    .size(100.dp)
                    .scale(if (uiState.isListening) scale else 1f),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (uiState.assistantStatus) {
                        AssistantStatus.LISTENING -> CyanPrimary
                        AssistantStatus.SPEAKING -> PurpleAccent
                        AssistantStatus.THINKING -> WarningAmber
                        AssistantStatus.ERROR -> ErrorRed
                        else -> SurfaceDark
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = when {
                        uiState.isListening -> Icons.Default.MicOff
                        uiState.isSpeaking -> Icons.Default.Stop
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Microphone",
                    modifier = Modifier.size(40.dp),
                    tint = if (uiState.assistantStatus == AssistantStatus.IDLE) CyanPrimary else BackgroundDark
                )
            }

            // Partial transcript
            if (uiState.partialText.isNotBlank()) {
                Text(
                    text = "\"${uiState.partialText}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            // Last message preview
            uiState.messages.lastOrNull()?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (msg.role.name == "ASSISTANT") "JAVIS" else "You",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (msg.role.name == "ASSISTANT") CyanPrimary else PurpleAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceLight,
                            maxLines = 4
                        )
                    }
                }
            }

            // Error message
            uiState.lastError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss", color = ErrorRed)
                        }
                    }
                }
            }

            if (!micPermission.status.isGranted) {
                Text(
                    text = "Microphone permission required for voice input",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningAmber,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AnimatedRings(isActive: Boolean, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "rings")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!isActive) return@Canvas
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension * 0.55f
        for (i in 0..2) {
            val offset = (progress + i * 0.33f) % 1f
            val radius = maxRadius * offset
            val alpha = (1f - offset) * 0.4f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
