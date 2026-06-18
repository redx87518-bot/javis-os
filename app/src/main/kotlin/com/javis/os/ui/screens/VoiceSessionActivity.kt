package com.javis.os.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.javis.os.domain.model.Message
import com.javis.os.ui.theme.CyanPrimary
import com.javis.os.ui.viewmodel.AssistantStatus
import com.javis.os.ui.viewmodel.AssistantUiState
import com.javis.os.ui.viewmodel.AssistantViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * VoiceSessionActivity — persistent conversational voice screen.
 *
 * Launched from the home screen shortcut. Auto-loops:
 *   Greet → Listen → Think → Speak → Listen → …
 *
 * No overlay permission needed. Works on Redmi A1 / MIUI.
 */
@AndroidEntryPoint
class VoiceSessionActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val micPermissionLauncher = registerForActivityResult(
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
        if (savedInstanceState == null) {
            viewModel.greetAndListen()
        }
    }

    override fun onResume() {
        super.onResume()
        val state = viewModel.uiState.value
        if (!state.isListening && !state.isThinking && !state.isSpeaking) {
            requestMicAndListen()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopListening()
    }

    private fun requestMicAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PermissionChecker.PERMISSION_GRANTED
        ) {
            viewModel.startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
private fun VoiceSessionScreen(
    viewModel: AssistantViewModel,
    onClose: () -> Unit
) {
    val uiState: AssistantUiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-listen after TTS finishes
    LaunchedEffect(uiState.isSpeaking, uiState.isThinking) {
        if (!uiState.isSpeaking && !uiState.isThinking && !uiState.isListening) {
            delay(500)
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
            Icon(
                Icons.Default.Close, "Close",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedOrb(status = uiState.assistantStatus)

            Spacer(Modifier.height(32.dp))

            Text(
                "JAVIS",
                color = CyanPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Spacer(Modifier.height(8.dp))

            val statusLabel = when (uiState.assistantStatus) {
                AssistantStatus.LISTENING -> "Listening…"
                AssistantStatus.THINKING -> "Thinking…"
                AssistantStatus.SPEAKING -> "Speaking…"
                AssistantStatus.ERROR -> uiState.lastError ?: "Error"
                AssistantStatus.IDLE -> "Tap mic to speak"
            }
            Text(
                statusLabel,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(24.dp))

            // Show partial transcript or last assistant reply
            val displayText = when {
                uiState.partialText.isNotBlank() -> uiState.partialText
                uiState.messages.isNotEmpty() -> {
                    val last = uiState.messages.last()
                    if (last.role == Message.Role.ASSISTANT) last.content else ""
                }
                else -> ""
            }
            if (displayText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        displayText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isSpeaking) {
                    FilledIconButton(
                        onClick = { viewModel.stopSpeaking() },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF7C3AED)
                        )
                    ) {
                        Icon(
                            Icons.Default.VolumeOff, "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        if (uiState.isListening) viewModel.stopListening()
                        else viewModel.startListening()
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
                        if (uiState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        if (uiState.isListening) "Stop" else "Speak",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                if (uiState.isListening) "Tap to stop" else "Tap to speak",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AnimatedOrb(status: AssistantStatus) {
    val inf = rememberInfiniteTransition(label = "orb")

    val pulse by inf.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.LISTENING -> 500
                    AssistantStatus.SPEAKING -> 350
                    AssistantStatus.THINKING -> 700
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val ringAlpha by inf.animateFloat(
        initialValue = 0.12f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "ring"
    )

    val orbColor = when (status) {
        AssistantStatus.LISTENING -> CyanPrimary
        AssistantStatus.SPEAKING -> Color(0xFF7C3AED)
        AssistantStatus.THINKING -> Color(0xFF2563EB)
        AssistantStatus.ERROR -> Color(0xFFEF4444)
        AssistantStatus.IDLE -> Color(0xFF1E3A5F)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        Box(
            Modifier.size(160.dp).scale(pulse * 1.1f)
                .background(orbColor.copy(alpha = ringAlpha * 0.35f), CircleShape)
        )
        Box(
            Modifier.size(126.dp).scale(pulse)
                .background(orbColor.copy(alpha = ringAlpha * 0.55f), CircleShape)
        )
        Box(
            modifier = Modifier.size(92.dp).background(
                Brush.radialGradient(
                    listOf(orbColor.copy(alpha = 0.9f), orbColor.copy(alpha = 0.45f))
                ), CircleShape
            )
        ) {
            if (status == AssistantStatus.LISTENING) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        val dot by inf.animateFloat(
                            initialValue = 0.4f, targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(380, delayMillis = i * 110),
                                repeatMode = RepeatMode.Reverse
                            ), label = "d$i"
                        )
                        Box(Modifier.scale(dot).size(8.dp).background(Color.White, CircleShape))
                    }
                }
            }
        }
    }
}
