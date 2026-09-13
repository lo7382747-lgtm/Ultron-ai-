package com.example.domain.model

data class AssistantSettings(
    val voiceName: String = "Puck", // Puck, Charon, Aoede, Fenrir, Kore
    val modelName: String = "models/gemini-2.5-flash-native-audio-preview-12-2025",
    val activationMode: ActivationMode = ActivationMode.TAP_TO_TALK,
    val userName: String = "Asik",
    val language: String = "English / Hinglish",
    val personality: String = "Intelligent, confident, fast, slightly witty",
    val deviceControlEnabled: Boolean = true,
    val memoryEnabled: Boolean = true,
    val particleIntensity: Float = 1.0f
)
