package com.javis.os.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.AssistantStatus
import com.javis.os.ui.viewmodel.AssistantViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * VoiceSessionActivity — persistent conversational voice screen.
 *
 * Launched from the home screen shortcut. Auto-loops:
 *   Listen → Think → Speak → Listen → ...
 * User taps mic to pause/resume, taps X to close.
 * Works without overlay permissions (Redmi A1 / MIUI compatible).
 */
@AndroidEntryPoint
class VoiceSessionActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                VoiceSessionScreen(
                    viewModel = viewModel,
                    onClose = { finish() }
                )
            }
        }

        // Greet on first open, then auto-listen
        if (savedInstanceState == null) {
            viewModel.greetAndListen()
        }
    }

    fun requestMicAndListen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED -> viewModel.startListening()
            else -> requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onPause() {
        super.onPause()
        // Keep speaking in background but stop listening
        viewModel.stopListening()
    }

    override fun onResume() {
        super.onResume()
        // Resume listening if idle
        val state = viewModel.uiState.value
        if (!state.isListening && !state.isThinking && !state.isSpeaking) {
            viewModel.startListening()
        }
    }
}

@Composable
private fun VoiceSessionScreen(
    viewModel: AssistantViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-listen after speaking finishes
    LaunchedEffect(uiState.isSpeaking, uiState.isThinking) {
        if (!uiState.isSpeaking && !uiState.isThinking && !uiState.isListening) {
            kotlinx.coroutines.delay(500)
            viewModel.startListening()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF050E17)),
                    radius = 1200f
                )
            )
    ) {
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated orb
            AnimatedOrb(status = uiState.assistantStatus)

            Spacer(modifier = Modifier.height(32.dp))

            // JAVIS label
            Text(
                text = "JAVIS",
                color = CyanPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status text
            val statusText = when (uiState.assistantStatus) {
                AssistantStatus.LISTENING -> "Listening..."
                AssistantStatus.THINKING -> "Thinking..."
                AssistantStatus.SPEAKING -> "Speaking..."
                AssistantStatus.ERROR -> uiState.lastError ?: "Error"
                AssistantStatus.IDLE -> "Tap mic to speak"
            }
            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Partial / last response text
            val displayText = when {
                uiState.partialText.isNotBlank() -> uiState.partialText
                uiState.messages.isNotEmpty() -> {
                    val last = uiState.messages.last()
                    if (last.role == com.javis.os.domain.model.Message.Role.ASSISTANT)
                        last.content else ""
                }
                else -> ""
            }
            if (displayText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = displayText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Control row: Stop Speaking | Mic toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop speaking button
                if (uiState.isSpeaking) {
                    FilledIconButton(
                        onClick = { viewModel.stopSpeaking() },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF7C3AED)
                        )
                    ) {
                        Icon(Icons.Default.VolumeOff, contentDescription = "Stop speaking",
                            tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // Main mic button
                FilledIconButton(
                    onClick = {
                        when {
                            uiState.isListening -> viewModel.stopListening()
                            else -> viewModel.startListening()
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            uiState.isListening -> CyanPrimary
                            uiState.isThinking -> Color(0xFF7C3AED)
                            else -> Color.White.copy(alpha = 0.15f)
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (uiState.isListening) "Stop" else "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (uiState.isListening) "Tap to stop" else "Tap to speak",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AnimatedOrb(status: AssistantStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.LISTENING -> 600
                    AssistantStatus.SPEAKING -> 400
                    AssistantStatus.THINKING -> 800
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring"
    )

    val orbColor = when (status) {
        AssistantStatus.LISTENING -> CyanPrimary
        AssistantStatus.SPEAKING -> Color(0xFF7C3AED)
        AssistantStatus.THINKING -> Color(0xFF2563EB)
        AssistantStatus.ERROR -> Color(0xFFEF4444)
        AssistantStatus.IDLE -> Color(0xFF1E3A5F)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        // Outer ring 2
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulseScale * 1.1f)
                .background(orbColor.copy(alpha = ringAlpha * 0.4f), CircleShape)
        )
        // Outer ring 1
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(pulseScale)
                .background(orbColor.copy(alpha = ringAlpha * 0.6f), CircleShape)
        )
        // Core orb
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            orbColor.copy(alpha = 0.9f),
                            orbColor.copy(alpha = 0.5f)
                        )
                    ),
                    CircleShape
                )
        ) {
            // Inner dots for listening animation
            if (status == AssistantStatus.LISTENING) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val dotScale by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, delayMillis = index * 120),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot$index"
                        )
                        Box(
                            modifier = Modifier
                                .scale(dotScale)
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
