package com.javis.os.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.javis.os.ui.theme.JavisCyan
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    color: Color = JavisCyan,
    barCount: Int = 32
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.08f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "amp"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barWidth = (width / barCount) * 0.6f
        val barSpacing = width / barCount
        val maxBarHeight = centerY * 0.9f

        for (i in 0 until barCount) {
            val x = i * barSpacing + barSpacing / 2f
            val normalizedPos = i.toFloat() / barCount
            val wave1 = sin((normalizedPos * 4 * Math.PI + phase).toDouble()).toFloat()
            val wave2 = sin((normalizedPos * 2 * Math.PI + phase * 1.3f).toDouble()).toFloat() * 0.5f
            val wave3 = sin((normalizedPos * 6 * Math.PI - phase * 0.7f).toDouble()).toFloat() * 0.3f
            val rawAmplitude = (wave1 + wave2 + wave3) / 1.8f
            val amplitude = rawAmplitude * amplitudeMultiplier
            val barHeight = maxBarHeight * kotlin.math.abs(amplitude).coerceAtLeast(0.05f)

            val alpha = 0.4f + 0.6f * (normalizedPos * (1 - normalizedPos) * 4f).coerceIn(0f, 1f)

            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(x, centerY - barHeight),
                end = Offset(x, centerY + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun CircularWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    color: Color = JavisCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "scale"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.35f
        val pointCount = 60

        for (i in 0 until pointCount) {
            val angle = (i.toFloat() / pointCount) * 2 * Math.PI
            val wave = sin(angle * 8 + phase) * 0.12f + sin(angle * 4 - phase * 1.5f) * 0.08f
            val radius = baseRadius * (1f + wave.toFloat() * scale)
            val x = center.x + (radius * kotlin.math.cos(angle).toFloat())
            val y = center.y + (radius * kotlin.math.sin(angle).toFloat())

            if (i > 0) {
                val prevAngle = ((i - 1).toFloat() / pointCount) * 2 * Math.PI
                val prevWave = sin(prevAngle * 8 + phase) * 0.12f + sin(prevAngle * 4 - phase * 1.5f) * 0.08f
                val prevRadius = baseRadius * (1f + prevWave.toFloat() * scale)
                val prevX = center.x + (prevRadius * kotlin.math.cos(prevAngle).toFloat())
                val prevY = center.y + (prevRadius * kotlin.math.sin(prevAngle).toFloat())

                drawLine(
                    color = color.copy(alpha = 0.7f),
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
