package com.example.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.VoiceState
import com.example.ui.chat.ChatSheet
import com.example.ui.settings.SettingsSheet
import com.example.ui.theme.UltronAccent
import com.example.ui.theme.UltronAmber
import com.example.ui.theme.UltronCyan
import com.example.ui.theme.UltronCyanGlow
import com.example.ui.theme.UltronDarkNavy
import com.example.ui.theme.UltronElectricBlue
import com.example.ui.theme.UltronRedAlert
import com.example.ui.theme.UltronSurfaceCard
import com.example.ui.theme.UltronSurfaceDark
import com.example.ui.theme.UltronTextPrimary
import com.example.ui.theme.UltronTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val transcript by viewModel.lastTranscript.collectAsState()
    val amplitude by viewModel.compositeAmplitude.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isTextLoading by viewModel.isTextLoading.collectAsState()
    val activeToolEvent by viewModel.activeToolEvent.collectAsState()

    var showChatSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.toggleVoiceAssistant(true)
        }
    }

    val insets = WindowInsets.systemBars.asPaddingValues()

    // Status Glow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "halo_transition")
    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    val activeStateColor by animateColorAsState(
        targetValue = when (voiceState) {
            VoiceState.ERROR -> UltronRedAlert
            VoiceState.INTERRUPTED -> UltronRedAlert
            VoiceState.THINKING -> UltronAmber
            VoiceState.SPEAKING -> UltronAccent
            VoiceState.LISTENING -> UltronCyan
            VoiceState.CONNECTING, VoiceState.RECONNECTING -> UltronAmber
            else -> UltronCyan.copy(alpha = 0.5f)
        },
        label = "stateColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        UltronDarkNavy,
                        Color(0xFF03070E),
                        UltronDarkNavy
                    )
                )
            )
            .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding())
    ) {
        // Subtle cybernetic grid lines in background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: System ID, User Welcome, Secondary actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ULTRON",
                            color = UltronCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(activeStateColor)
                        )
                    }
                    Text(
                        text = "System Operator: ${settings.userName}",
                        color = UltronTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showChatSheet = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(UltronSurfaceDark)
                            .border(1.dp, UltronCyan.copy(alpha = 0.2f), CircleShape)
                            .testTag("chat_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Text conversation",
                            tint = UltronCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(UltronSurfaceDark)
                            .border(1.dp, UltronCyan.copy(alpha = 0.2f), CircleShape)
                            .testTag("settings_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuration",
                            tint = UltronTextSecondary
                        )
                    }
                }
            }

            // State Badge Pill
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = UltronSurfaceDark,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, activeStateColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(activeStateColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = voiceState.name,
                        color = activeStateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Active Tool Banner
            AnimatedVisibility(
                visible = activeToolEvent != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = UltronSurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(1.dp, UltronAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = activeToolEvent ?: "",
                        color = UltronAccent,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Central Animated Energy Orb/Ring (Interactive to Voice Activity)
            OrbEnergyVisualizer(
                voiceState = voiceState,
                amplitude = amplitude,
                modifier = Modifier.testTag("orb_energy_visualizer")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Dynamic Waveform
            VoiceWaveform(
                voiceState = voiceState,
                amplitude = amplitude,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("voice_waveform")
            )

            // Status message & Live Transcript preview
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusText,
                color = UltronTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("assistant_status_text")
            )

            if (transcript.isNotBlank()) {
                Text(
                    text = "\"$transcript\"",
                    color = UltronCyan,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("transcript_text")
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Quick Prompt Suggestions
            Text(
                text = "SUGGESTED COMMANDS",
                color = UltronTextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val suggestions = listOf(
                "Open YouTube",
                "Toggle Flashlight",
                "What's the weather today?",
                "Open Settings",
                "Current time & battery"
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(suggestions) { item ->
                    Surface(
                        color = UltronSurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .border(1.dp, UltronCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.sendTextMessage(item)
                            }
                    ) {
                        Text(
                            text = item,
                            color = UltronTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Glowing Power / Microphone Control Button
            val isLiveActive = voiceState == VoiceState.LISTENING ||
                    voiceState == VoiceState.SPEAKING ||
                    voiceState == VoiceState.THINKING ||
                    voiceState == VoiceState.CONNECTING

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .scale(if (isLiveActive) buttonPulse else 1f),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeStateColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Main circular activation button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isLiveActive) {
                                    listOf(activeStateColor, UltronElectricBlue)
                                } else {
                                    listOf(UltronSurfaceCard, UltronSurfaceDark)
                                }
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = if (isLiveActive) Color.White.copy(alpha = 0.8f) else UltronCyan.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.toggleVoiceAssistant(true)
                            }
                        }
                        .testTag("microphone_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLiveActive) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isLiveActive) "Deactivate ULTRON" else "Activate ULTRON",
                        tint = if (isLiveActive) UltronDarkNavy else UltronCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isLiveActive) "TAP TO DISENGAGE" else "TAP TO ENGAGE ULTRON",
                color = if (isLiveActive) activeStateColor else UltronTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // ChatGPT-Style Chat Sheet
        if (showChatSheet) {
            ChatSheet(
                messages = chatMessages,
                isLoading = isTextLoading,
                onSendMessage = { prompt ->
                    viewModel.sendTextMessage(prompt)
                },
                onClearHistory = {
                    viewModel.clearChatHistory()
                },
                onDismiss = {
                    showChatSheet = false
                }
            )
        }

        // Settings Sheet
        if (showSettingsSheet) {
            SettingsSheet(
                settings = settings,
                onSaveSettings = { updated ->
                    viewModel.updateSettings(updated)
                },
                onDismiss = {
                    showSettingsSheet = false
                }
            )
        }
    }
}
