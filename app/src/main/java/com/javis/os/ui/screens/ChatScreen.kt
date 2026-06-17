package com.javis.os.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.os.domain.model.Message
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.ChatViewModel
import com.javis.os.voice.RecognitionState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val recognitionState by viewModel.recognitionState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val pendingWhatsApp by viewModel.pendingWhatsApp.collectAsState()
    val pendingCall by viewModel.pendingCall.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isListening = recognitionState is RecognitionState.Listening || recognitionState is RecognitionState.Hearing

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(Unit) { viewModel.initializeVoice() }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            JavisTopBar(isSpeaking = isSpeaking, isListening = isListening)

            if (messages.isEmpty()) {
                JavisWelcome(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                        ) {
                            MessageBubble(message = message)
                        }
                    }
                    if (isProcessing) {
                        item { TypingIndicator() }
                    }
                }
            }

            // Confirmation banners (priority order)
            AnimatedVisibility(visible = pendingCall != null) {
                pendingCall?.let { (name, number) ->
                    ConfirmationBanner(
                        icon = Icons.Default.Phone,
                        message = "Call $name ($number)?",
                        confirmLabel = "Call",
                        confirmColor = JavisGreen,
                        onConfirm = viewModel::confirmPendingCall,
                        onCancel = viewModel::cancelPendingCall
                    )
                }
            }
            AnimatedVisibility(visible = pendingWhatsApp != null) {
                pendingWhatsApp?.let { draft ->
                    ConfirmationBanner(
                        icon = Icons.Default.Message,
                        message = "Send to ${draft.contactName}: \"${draft.message.take(40)}${if (draft.message.length > 40) "..." else ""}\"",
                        confirmLabel = "Send",
                        confirmColor = JavisGreen,
                        onConfirm = viewModel::confirmPendingWhatsApp,
                        onCancel = viewModel::cancelPendingWhatsApp
                    )
                }
            }
            AnimatedVisibility(visible = pendingAction != null) {
                ConfirmationBanner(
                    icon = Icons.Default.PlayArrow,
                    message = "Proceed with this action?",
                    confirmLabel = "Confirm",
                    confirmColor = JavisBlue,
                    onConfirm = viewModel::confirmPendingAction,
                    onCancel = viewModel::cancelPendingAction
                )
            }

            InputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" }
                },
                onVoice = { viewModel.startVoiceInput() },
                recognitionState = recognitionState,
                isProcessing = isProcessing
            )
        }
    }
}

@Composable
private fun JavisTopBar(isSpeaking: Boolean, isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "topbar")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    Brush.radialGradient(listOf(JavisCyan.copy(alpha = 0.3f), BackgroundDark)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("J", style = MaterialTheme.typography.titleMedium, color = JavisCyan)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("JAVIS", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            when {
                                isListening -> JavisCyan.copy(alpha = dotAlpha)
                                isSpeaking -> JavisGreen.copy(alpha = dotAlpha)
                                else -> JavisGreen.copy(alpha = 0.6f)
                            },
                            CircleShape
                        )
                )
                Text(
                    when {
                        isListening -> "Listening..."
                        isSpeaking -> "Speaking..."
                        else -> "Online"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isListening -> JavisCyan
                        isSpeaking -> JavisGreen
                        else -> TextSecondary
                    }
                )
            }
        }
    }
}

@Composable
private fun JavisWelcome(modifier: Modifier) {
    val suggestions = listOf(
        "What's the weather?",
        "Call someone",
        "Set an alarm",
        "Open YouTube",
        "Check battery",
        "Toggle flashlight"
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "welcome")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "a"
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(listOf(JavisCyan.copy(alpha = alpha * 0.25f), BackgroundDark)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("J", style = MaterialTheme.typography.headlineLarge, color = JavisCyan.copy(alpha = alpha))
        }
        Spacer(Modifier.height(20.dp))
        Text("JAVIS OS", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Your AI companion is ready", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(40.dp))
        Text("Try asking:", style = MaterialTheme.typography.labelMedium, color = TextDim)
        Spacer(Modifier.height(12.dp))
        suggestions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { suggestion ->
                    SuggestionChip(
                        modifier = Modifier.weight(1f),
                        onClick = {},
                        label = { Text(suggestion, style = MaterialTheme.typography.bodySmall, color = TextSecondary) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CardDark),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = BorderDark
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(JavisCyan.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("J", style = MaterialTheme.typography.labelMedium, color = JavisCyan) }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            if (message.isLoading) {
                TypingIndicator()
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            )
                        )
                        .background(if (isUser) UserBubble else AssistantBubble)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(message.content, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier
            .background(AssistantBubble, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600), RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot$index"
            )
            Box(modifier = Modifier.size(8.dp).background(JavisCyan.copy(alpha = alpha), CircleShape))
        }
    }
}

@Composable
private fun ConfirmationBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    confirmLabel: String,
    confirmColor: androidx.compose.ui.graphics.Color,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariantDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = JavisCyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f), maxLines = 2)
        TextButton(onClick = onCancel) { Text("No", color = JavisRed, style = MaterialTheme.typography.labelMedium) }
        TextButton(onClick = onConfirm) { Text(confirmLabel, color = confirmColor, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    recognitionState: RecognitionState,
    isProcessing: Boolean
) {
    val isListening = recognitionState is RecognitionState.Listening || recognitionState is RecognitionState.Hearing
    val micScale by animateFloatAsState(
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "mic"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    if (isListening) "Listening..." else "Message JAVIS...",
                    style = MaterialTheme.typography.bodyMedium, color = TextDim
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JavisCyan, unfocusedBorderColor = BorderDark,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = JavisCyan, focusedContainerColor = CardDark, unfocusedContainerColor = CardDark
            ),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(8.dp))
        if (value.isNotBlank()) {
            IconButton(
                onClick = onSend,
                modifier = Modifier.size(44.dp).background(JavisBlue, CircleShape)
            ) {
                Icon(Icons.Default.Send, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
        } else {
            Box(modifier = Modifier.scale(micScale)) {
                IconButton(
                    onClick = onVoice,
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (isListening) JavisCyan else CardDark, CircleShape)
                ) {
                    Icon(
                        if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        null,
                        tint = if (isListening) BackgroundDark else JavisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
