package com.example.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.domain.model.VoiceState
import com.example.ui.theme.UltronAccent
import com.example.ui.theme.UltronAmber
import com.example.ui.theme.UltronCyan
import com.example.ui.theme.UltronRedAlert
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    voiceState: VoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barColor = when (voiceState) {
        VoiceState.ERROR -> UltronRedAlert
        VoiceState.INTERRUPTED -> UltronRedAlert
        VoiceState.THINKING -> UltronAmber
        VoiceState.SPEAKING -> UltronAccent
        VoiceState.LISTENING -> UltronCyan
        else -> UltronCyan.copy(alpha = 0.5f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val barCount = 36
        val barSpacing = size.width / barCount
        val centerY = size.height / 2f
        val maxHeight = (size.height / 2f) - 4f

        for (i in 0 until barCount) {
            val x = i * barSpacing + (barSpacing / 2f)

            // Normalized sine wave factor combined with real audio amplitude
            val normalizedX = (i.toFloat() / barCount.toFloat()) * 3.14159f * 2f
            val wave = sin(normalizedX * 2f + phase) * 0.5f + 0.5f
            val baseScale = if (voiceState == VoiceState.IDLE) 0.12f else 0.25f
            val ampMultiplier = if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                baseScale + (amplitude * 0.75f * (0.4f + wave * 0.6f))
            } else if (voiceState == VoiceState.THINKING) {
                baseScale + (wave * 0.35f)
            } else {
                baseScale * (0.5f + wave * 0.5f)
            }

            val currentHeight = (maxHeight * ampMultiplier).coerceIn(3f, maxHeight)

            // Draw audio bar
            drawLine(
                color = barColor,
                start = Offset(x, centerY - currentHeight),
                end = Offset(x, centerY + currentHeight),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }
    }
}
