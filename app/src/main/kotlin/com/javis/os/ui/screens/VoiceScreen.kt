package com.javis.os.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.abs

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceScreen(
    navController: NavController,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    val statusColor = when (uiState.assistantStatus) {
        AssistantStatus.LISTENING -> CyanPrimary
        AssistantStatus.SPEAKING  -> PurpleAccent
        AssistantStatus.THINKING  -> WarningAmber
        AssistantStatus.ERROR     -> ErrorRed
        AssistantStatus.IDLE      -> CyanPrimary.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        // Background pulse rings
        AnimatedRings(isActive = uiState.isListening || uiState.isSpeaking, color = statusColor)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top: Brand + Status ───────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "JAVIS OS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CyanPrimary,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusBadge(status = uiState.assistantStatus, color = statusColor)
            }

            // ── Center: Animated waveform + Mic button ────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Waveform when listening or speaking
                AnimatedVisibility(
                    visible = uiState.isListening || uiState.isSpeaking,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    VoiceWaveform(color = statusColor, modifier = Modifier.padding(bottom = 24.dp))
                }

                // Main mic button
                val btnScale by animateFloatAsState(
                    targetValue = if (uiState.isListening) 1.12f else 1f,
                    animationSpec = if (uiState.isListening) infiniteRepeatable(
                        tween(700, easing = EaseInOutSine), RepeatMode.Reverse
                    ) else spring(),
                    label = "btnScale"
                )

                Box(contentAlignment = Alignment.Center) {
                    // Glow ring
                    if (uiState.isListening || uiState.isSpeaking) {
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.12f))
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            when {
                                !micPermission.status.isGranted -> micPermission.launchPermissionRequest()
                                uiState.isSpeaking -> viewModel.stopSpeaking()
                                uiState.isListening -> viewModel.stopListening()
                                else -> viewModel.startListening()
                            }
                        },
                        modifier = Modifier
                            .size(100.dp)
                            .scale(btnScale),
                        shape = CircleShape,
                        containerColor = when (uiState.assistantStatus) {
                            AssistantStatus.LISTENING -> CyanPrimary
                            AssistantStatus.SPEAKING  -> PurpleAccent
                            AssistantStatus.THINKING  -> WarningAmber
                            AssistantStatus.ERROR     -> ErrorRed
                            else -> SurfaceDark
                        },
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 12.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Icon(
                            imageVector = when {
                                uiState.isListening -> Icons.Default.MicOff
                                uiState.isSpeaking  -> Icons.Default.Stop
                                uiState.isThinking  -> Icons.Default.HourglassEmpty
                                else                -> Icons.Default.Mic
                            },
                            contentDescription = "Activate JAVIS",
                            modifier = Modifier.size(42.dp),
                            tint = if (uiState.assistantStatus == AssistantStatus.IDLE)
                                CyanPrimary else BackgroundDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tap hint
                if (uiState.assistantStatus == AssistantStatus.IDLE) {
                    Text(
                        text = "Tap to speak",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF546E7A),
                        letterSpacing = 1.sp
                    )
                }

                // Partial transcript
                AnimatedVisibility(visible = uiState.partialText.isNotBlank()) {
                    Text(
                        text = "\"${uiState.partialText}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanPrimary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp)
                    )
                }
            }

            // ── Bottom: last message + error ──────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Last exchange
                uiState.messages.lastOrNull()?.let { msg ->
                    LastMessageCard(
                        role = msg.role.name,
                        content = msg.content
                    )
                }

                // Error
                uiState.lastError?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(error, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::clearError) {
                                Text("OK", color = ErrorRed)
                            }
                        }
                    }
                }

                // Mic permission warning
                if (!micPermission.status.isGranted) {
                    Text(
                        "⚠ Microphone permission needed for voice input",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: AssistantStatus, color: Color) {
    val label = when (status) {
        AssistantStatus.IDLE      -> "READY"
        AssistantStatus.LISTENING -> "LISTENING"
        AssistantStatus.THINKING  -> "THINKING"
        AssistantStatus.SPEAKING  -> "SPEAKING"
        AssistantStatus.ERROR     -> "ERROR"
    }
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "badgeAlpha"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = if (status == AssistantStatus.IDLE) 0.1f else 0.15f))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (status != AssistantStatus.IDLE) alpha else 0.5f))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun VoiceWaveform(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "waveProgress"
    )
    val bars = 24
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 32.dp)
    ) {
        val barWidth = size.width / (bars * 2f)
        for (i in 0 until bars) {
            val phase = progress + (i * 0.4f)
            val height = (sin(phase.toDouble()) * 0.5 + 0.5).toFloat()
            val barHeight = (height * size.height * 0.8f).coerceAtLeast(6f)
            val x = i * barWidth * 2f + barWidth / 2f
            drawLine(
                color = color.copy(alpha = 0.4f + height * 0.6f),
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = barWidth * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun AnimatedRings(isActive: Boolean, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "rings")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "ringProg"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!isActive) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val max = size.minDimension * 0.52f
        for (i in 0..2) {
            val offset = (progress + i / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - offset) * 0.35f),
                radius = max * offset,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun LastMessageCard(role: String, content: String) {
    val isAssistant = role == "ASSISTANT"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAssistant) SurfaceVariant else CyanPrimary.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (isAssistant) "JAVIS" else "YOU",
                style = MaterialTheme.typography.labelSmall,
                color = if (isAssistant) CyanPrimary else PurpleAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceLight,
                maxLines = 4
            )
        }
    }
}
