package com.javis.os.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.os.ui.components.CircularWaveform
import com.javis.os.ui.components.VoiceWaveform
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.ChatViewModel
import com.javis.os.voice.RecognitionState

@Composable
fun VoiceModeScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val recognitionState by viewModel.recognitionState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    val isListening = recognitionState is RecognitionState.Listening ||
            recognitionState is RecognitionState.Hearing

    LaunchedEffect(Unit) { viewModel.initializeVoice() }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val orbScale by rememberInfiniteTransition(label = "orb").animateFloat(
        initialValue = 1f,
        targetValue = when {
            isListening -> 1.12f
            isSpeaking -> 1.08f
            else -> 1.02f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isListening -> 500
                    isSpeaking -> 700
                    else -> 2500
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BackgroundDark, Color(0xFF080810), BackgroundDark)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("JAVIS", style = MaterialTheme.typography.titleLarge, color = JavisCyan)
                AssistantStatusChip(isListening = isListening, isSpeaking = isSpeaking, isProcessing = isProcessing)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                contentAlignment = Alignment.Center
            ) {
                CircularWaveform(
                    isActive = isListening || isSpeaking,
                    modifier = Modifier.size(240.dp),
                    color = when {
                        isListening -> JavisCyan
                        isSpeaking -> JavisGreen
                        else -> JavisCyan.copy(alpha = 0.4f)
                    }
                )

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(orbScale)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    when {
                                        isListening -> JavisCyan.copy(alpha = 0.25f)
                                        isSpeaking -> JavisGreen.copy(alpha = 0.2f)
                                        else -> JavisCyan.copy(alpha = 0.1f)
                                    },
                                    BackgroundDark
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            when {
                                isListening -> JavisCyan.copy(alpha = 0.8f)
                                isSpeaking -> JavisGreen.copy(alpha = 0.8f)
                                else -> JavisCyan.copy(alpha = 0.3f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing && !isListening && !isSpeaking) {
                        CircularProgressIndicator(
                            color = JavisCyan,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            when {
                                isSpeaking -> Icons.Default.VolumeUp
                                isListening -> Icons.Default.MicNone
                                else -> Icons.Default.Mic
                            },
                            contentDescription = null,
                            tint = when {
                                isSpeaking -> JavisGreen
                                isListening -> JavisCyan
                                else -> JavisCyan.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            VoiceWaveform(
                isActive = isListening || isSpeaking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = if (isSpeaking) JavisGreen else JavisCyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            val recentMessages = messages.takeLast(4)
            if (recentMessages.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    reverseLayout = false
                ) {
                    items(recentMessages) { message ->
                        val isUser = message.isUser
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp, topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        )
                                    )
                                    .background(if (isUser) UserBubble.copy(alpha = 0.8f) else AssistantBubble)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    message.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Tap to speak or say",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim
                        )
                        Text(
                            "\"Hey JAVIS\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = JavisCyan.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isListening || isSpeaking) {
                    OutlinedButton(
                        onClick = {
                            viewModel.stopVoiceInput()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisRed),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.greetUser()
                            viewModel.startVoiceInput()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JavisCyan)
                    ) {
                        Icon(Icons.Default.Mic, null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Activate", color = BackgroundDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantStatusChip(isListening: Boolean, isSpeaking: Boolean, isProcessing: Boolean) {
    val (text, color) = when {
        isListening -> "LISTENING" to JavisCyan
        isSpeaking -> "SPEAKING" to JavisGreen
        isProcessing -> "THINKING" to JavisOrange
        else -> "ONLINE" to TextDim
    }

    val infiniteTransition = rememberInfiniteTransition(label = "chip")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot"
    )

    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color.copy(alpha = dotAlpha), CircleShape)
        )
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
