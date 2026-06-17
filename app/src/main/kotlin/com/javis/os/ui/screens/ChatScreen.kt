package com.javis.os.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.os.domain.model.Message
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp) // Nav bar height
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "JAVIS Chat",
                style = MaterialTheme.typography.titleLarge,
                color = CyanPrimary
            )
            if (uiState.isThinking) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterEnd),
                    color = CyanPrimary,
                    strokeWidth = 2.dp
                )
            }
        }

        HorizontalDivider(color = CyanPrimary.copy(alpha = 0.2f))

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Hello! I'm JAVIS.",
                                style = MaterialTheme.typography.headlineMedium,
                                color = CyanPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "How can I help you today?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnSurfaceLight.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            items(uiState.messages) { message ->
                MessageBubble(message = message)
            }
            if (uiState.isThinking) {
                item {
                    ThinkingIndicator()
                }
            }
        }

        // Input
        HorizontalDivider(color = CyanPrimary.copy(alpha = 0.15f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask JAVIS anything...", color = Color(0xFF546E7A)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendTextMessage(inputText)
                        inputText = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = Color(0xFF263238),
                    focusedTextColor = OnSurfaceLight,
                    unfocusedTextColor = OnSurfaceLight,
                    cursorColor = CyanPrimary,
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendTextMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (inputText.isNotBlank()) CyanPrimary else SurfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) BackgroundDark else Color(0xFF546E7A)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == Message.Role.USER
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(CyanPrimary.copy(alpha = 0.15f), CircleShape)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text("J", color = CyanPrimary, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) CyanPrimary.copy(alpha = 0.15f) else SurfaceVariant
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) CyanPrimary else OnSurfaceLight
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF546E7A)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PurpleAccent.copy(alpha = 0.15f), CircleShape)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = PurpleAccent, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    Row(
        modifier = Modifier.padding(start = 40.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(CyanPrimary.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

private val CircleShape = androidx.compose.foundation.shape.CircleShape
