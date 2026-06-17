package com.javis.os.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.javis.os.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onFinished: (String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("") }

    val totalSteps = 4

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        },
        label = "onboarding_step"
    ) { currentStep ->
        when (currentStep) {
            0 -> OnboardingStepBoot(onNext = { step++ })
            1 -> OnboardingStepName(
                name = userName,
                onNameChange = { userName = it },
                onNext = { step++ }
            )
            2 -> OnboardingStepPermissions(onNext = { step++ })
            3 -> OnboardingStepReady(name = userName, onStart = { onFinished(userName) })
        }
    }
}

@Composable
private fun OnboardingStepBoot(onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "boot")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        Brush.radialGradient(
                            listOf(JavisCyan.copy(alpha = pulseAlpha * 0.3f), BackgroundDark)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(listOf(JavisCyan.copy(alpha = 0.2f), SurfaceVariantDark)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("J", style = MaterialTheme.typography.displayLarge.copy(
                        color = JavisCyan.copy(alpha = pulseAlpha)
                    ))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "JAVIS OS",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Your Personal AI Companion",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Intelligent • Conversational • Always Ready",
                style = MaterialTheme.typography.bodySmall,
                color = JavisCyan.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = JavisCyan),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text("Get Started", color = BackgroundDark, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun OnboardingStepName(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("👋", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "What should I call you?",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "I'll remember your name and personalize our conversations.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Your name", color = TextDim) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onNext() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JavisCyan,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = JavisCyan
                ),
                textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onNext() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.weight(1f)
                ) { Text("Skip") }

                Button(
                    onClick = { if (name.isNotBlank()) onNext() else onNext() },
                    colors = ButtonDefaults.buttonColors(containerColor = JavisCyan),
                    modifier = Modifier.weight(1f)
                ) { Text("Continue", color = BackgroundDark) }
            }
        }
    }
}

@Composable
private fun OnboardingStepPermissions(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔐", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Permissions",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "JAVIS needs these to work properly. All usage is private and stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            val permissions = listOf(
                "🎤" to "Microphone — for voice commands",
                "👥" to "Contacts — to find and call people",
                "📱" to "Phone — to make calls after your confirmation",
                "🔔" to "Notifications — to read and summarize alerts",
                "♿" to "Accessibility — for app automation (optional)"
            )

            permissions.forEach { (emoji, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = JavisCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) { Text("I Understand", color = BackgroundDark, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun OnboardingStepReady(name: String, onStart: () -> Unit) {
    val greeting = if (name.isNotBlank()) "Hello, $name!" else "Hello!"

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
        onStart()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "ready")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.5f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                label = "a"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(listOf(JavisCyan.copy(alpha = alpha * 0.4f), BackgroundDark)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("J", style = MaterialTheme.typography.displayLarge, color = JavisCyan.copy(alpha = alpha))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(greeting, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "JAVIS online and ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = JavisCyan,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "How can I help you today?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = JavisCyan),
                modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) { Text("Let's Go", color = BackgroundDark, style = MaterialTheme.typography.titleMedium) }
        }
    }
}
