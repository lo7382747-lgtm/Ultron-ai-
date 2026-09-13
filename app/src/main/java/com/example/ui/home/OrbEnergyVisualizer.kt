package com.example.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.domain.model.VoiceState
import com.example.ui.theme.UltronAccent
import com.example.ui.theme.UltronAmber
import com.example.ui.theme.UltronCyan
import com.example.ui.theme.UltronCyanGlow
import com.example.ui.theme.UltronElectricBlue
import com.example.ui.theme.UltronRedAlert
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbEnergyVisualizer(
    voiceState: VoiceState,
    amplitude: Float, // 0f to 1f
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    // Base colors according to state
    val primaryColor = when (voiceState) {
        VoiceState.ERROR -> UltronRedAlert
        VoiceState.INTERRUPTED -> UltronRedAlert
        VoiceState.THINKING -> UltronAmber
        VoiceState.SPEAKING -> UltronAccent
        VoiceState.LISTENING -> UltronCyan
        VoiceState.CONNECTING, VoiceState.RECONNECTING -> UltronAmber
        else -> UltronCyan
    }

    val secondaryColor = when (voiceState) {
        VoiceState.ERROR -> Color(0xFF880022)
        VoiceState.THINKING -> UltronElectricBlue
        VoiceState.SPEAKING -> UltronCyan
        VoiceState.LISTENING -> UltronElectricBlue
        else -> UltronElectricBlue
    }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.72f

            // Dynamic scaling based on state and live amplitude
            val dynamicBoost = amplitude * 35f
            val dynamicRadius = (baseRadius * pulse + dynamicBoost).coerceAtMost(size.minDimension / 2f - 4f)

            // 1. Outer ambient glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f + amplitude * 0.35f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.35f
                ),
                radius = dynamicRadius * 1.35f,
                center = center
            )

            // 2. Central energy core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        primaryColor.copy(alpha = 0.75f),
                        secondaryColor.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 0.65f
                ),
                radius = dynamicRadius * 0.65f,
                center = center
            )

            // 3. Inner cyber ring with dashes (rotates clockwise)
            rotate(rotation1, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.8f),
                    radius = dynamicRadius * 0.78f,
                    center = center,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f, 10f, 15f), 0f)
                    )
                )
            }

            // 4. Middle cyber ring with distinct dashes (rotates counter-clockwise)
            rotate(rotation2, pivot = center) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.65f),
                    radius = dynamicRadius * 0.92f,
                    center = center,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(45f, 25f, 15f, 25f), 0f)
                    )
                )
            }

            // 5. Outer orbit ring with glowing node satellites
            rotate(rotation1 * 0.6f, pivot = center) {
                drawCircle(
                    color = UltronCyanGlow,
                    radius = dynamicRadius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // 4 orbital nodes
                val nodeCount = 4
                for (i in 0 until nodeCount) {
                    val angle = (i * (360f / nodeCount) + (waveOffset * 20f)) * (Math.PI / 180f)
                    val nx = center.x + dynamicRadius * cos(angle).toFloat()
                    val ny = center.y + dynamicRadius * sin(angle).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 4f + amplitude * 3f,
                        center = Offset(nx, ny)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 8f + amplitude * 6f,
                        center = Offset(nx, ny),
                        style = Stroke(width = 2f)
                    )
                }
            }

            // 6. Particle pulse dots around circumference
            val particleCount = 12
            for (i in 0 until particleCount) {
                val pAngle = (i * (360f / particleCount)) * (Math.PI / 180f)
                val dist = dynamicRadius * 0.45f + (sin(waveOffset + i) * 12f)
                val px = center.x + dist * cos(pAngle).toFloat()
                val py = center.y + dist * sin(pAngle).toFloat()
                drawCircle(
                    color = primaryColor.copy(alpha = 0.4f + amplitude * 0.4f),
                    radius = 2.5f,
                    center = Offset(px, py)
                )
            }
        }
    }
}
