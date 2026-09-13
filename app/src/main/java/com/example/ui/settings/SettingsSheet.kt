package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ActivationMode
import com.example.domain.model.AssistantSettings
import com.example.ui.theme.UltronAccent
import com.example.ui.theme.UltronCyan
import com.example.ui.theme.UltronDarkNavy
import com.example.ui.theme.UltronElectricBlue
import com.example.ui.theme.UltronSurfaceCard
import com.example.ui.theme.UltronTextPrimary
import com.example.ui.theme.UltronTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AssistantSettings,
    onSaveSettings: (AssistantSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentVoice by remember { mutableStateOf(settings.voiceName) }
    var currentMode by remember { mutableStateOf(settings.activationMode) }
    var currentDeviceControl by remember { mutableStateOf(settings.deviceControlEnabled) }
    var currentMemory by remember { mutableStateOf(settings.memoryEnabled) }

    val voices = listOf("Puck", "Charon", "Aoede", "Fenrir", "Kore")

    ModalBottomSheet(
        onDismissRequest = {
            onSaveSettings(
                settings.copy(
                    voiceName = currentVoice,
                    activationMode = currentMode,
                    deviceControlEnabled = currentDeviceControl,
                    memoryEnabled = currentMemory
                )
            )
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = UltronDarkNavy,
        contentColor = UltronTextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(UltronCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = UltronCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ULTRON CONFIGURATION",
                        color = UltronCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.2.sp
                    )
                }

                IconButton(
                    onClick = {
                        onSaveSettings(
                            settings.copy(
                                voiceName = currentVoice,
                                activationMode = currentMode,
                                deviceControlEnabled = currentDeviceControl,
                                memoryEnabled = currentMemory
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = UltronTextSecondary
                    )
                }
            }

            // Section 1: Voice & Speech Engine
            SettingsSectionHeader(icon = Icons.AutoMirrored.Filled.VolumeUp, title = "VOICE ARCHITECTURE")
            Card(
                colors = CardDefaults.cardColors(containerColor = UltronSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, UltronCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Neural Vocalization Profile",
                        color = UltronTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select high-fidelity synthesized voice tone",
                        color = UltronTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        voices.forEach { v ->
                            FilterChip(
                                selected = currentVoice == v,
                                onClick = { currentVoice = v },
                                label = { Text(v, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UltronCyan,
                                    selectedLabelColor = UltronDarkNavy,
                                    containerColor = UltronDarkNavy,
                                    labelColor = UltronTextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 2: Activation & Listening Mode
            SettingsSectionHeader(icon = Icons.Default.Mic, title = "SUMMON & ACTIVATION")
            Card(
                colors = CardDefaults.cardColors(containerColor = UltronSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, UltronCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Interaction Trigger",
                        color = UltronTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentMode == ActivationMode.TAP_TO_TALK,
                            onClick = { currentMode = ActivationMode.TAP_TO_TALK },
                            label = { Text("Tap To Talk", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UltronCyan,
                                selectedLabelColor = UltronDarkNavy,
                                containerColor = UltronDarkNavy,
                                labelColor = UltronTextSecondary
                            )
                        )
                        FilterChip(
                            selected = currentMode == ActivationMode.SUMMON_MODE,
                            onClick = { currentMode = ActivationMode.SUMMON_MODE },
                            label = { Text("Summon (Ultron)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UltronCyan,
                                selectedLabelColor = UltronDarkNavy,
                                containerColor = UltronDarkNavy,
                                labelColor = UltronTextSecondary
                            )
                        )
                        FilterChip(
                            selected = currentMode == ActivationMode.CONTINUOUS_LISTENING,
                            onClick = { currentMode = ActivationMode.CONTINUOUS_LISTENING },
                            label = { Text("Continuous", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UltronCyan,
                                selectedLabelColor = UltronDarkNavy,
                                containerColor = UltronDarkNavy,
                                labelColor = UltronTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 3: Android Device Controls & Tools
            SettingsSectionHeader(icon = Icons.Default.SmartToy, title = "DEVICE TOOLS & AUTOMATION")
            Card(
                colors = CardDefaults.cardColors(containerColor = UltronSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, UltronCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Device Control Tools",
                                color = UltronTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Allow ULTRON to open apps, control flashlight, search web, query time/battery",
                                color = UltronTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = currentDeviceControl,
                            onCheckedChange = { currentDeviceControl = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = UltronDarkNavy,
                                checkedTrackColor = UltronCyan,
                                uncheckedThumbColor = UltronTextSecondary,
                                uncheckedTrackColor = UltronDarkNavy
                            ),
                            modifier = Modifier.testTag("device_control_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 4: Privacy & Identity
            SettingsSectionHeader(icon = Icons.Default.Security, title = "IDENTITY & PRIVACY")
            Card(
                colors = CardDefaults.cardColors(containerColor = UltronSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, UltronCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "User Identity: Asik",
                        color = UltronAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Language: English / Hindi / Hinglish auto-switch",
                        color = UltronTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Session Memory",
                                color = UltronTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Retain contextual conversational memory across turns",
                                color = UltronTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = currentMemory,
                            onCheckedChange = { currentMemory = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = UltronDarkNavy,
                                checkedTrackColor = UltronCyan,
                                uncheckedThumbColor = UltronTextSecondary,
                                uncheckedTrackColor = UltronDarkNavy
                            ),
                            modifier = Modifier.testTag("memory_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = UltronCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = UltronCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}
