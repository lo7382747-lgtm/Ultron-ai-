package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.ActivationMode
import com.example.domain.model.AssistantSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "ultron_prefs"
        private const val KEY_VOICE = "voice_name"
        private const val KEY_MODEL = "model_name"
        private const val KEY_ACTIVATION_MODE = "activation_mode"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_DEVICE_CONTROL = "device_control_enabled"
        private const val KEY_MEMORY = "memory_enabled"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private fun loadSettings(): AssistantSettings {
        val voice = prefs.getString(KEY_VOICE, "Puck") ?: "Puck"
        val model = prefs.getString(KEY_MODEL, "models/gemini-2.5-flash-native-audio-preview-12-2025")
            ?: "models/gemini-2.5-flash-native-audio-preview-12-2025"
        val modeStr = prefs.getString(KEY_ACTIVATION_MODE, ActivationMode.TAP_TO_TALK.name)
        val mode = try {
            ActivationMode.valueOf(modeStr ?: ActivationMode.TAP_TO_TALK.name)
        } catch (e: Exception) {
            ActivationMode.TAP_TO_TALK
        }
        val userName = prefs.getString(KEY_USER_NAME, "Asik") ?: "Asik"
        val devControl = prefs.getBoolean(KEY_DEVICE_CONTROL, true)
        val memory = prefs.getBoolean(KEY_MEMORY, true)

        return AssistantSettings(
            voiceName = voice,
            modelName = model,
            activationMode = mode,
            userName = userName,
            deviceControlEnabled = devControl,
            memoryEnabled = memory
        )
    }

    fun updateSettings(newSettings: AssistantSettings) {
        prefs.edit()
            .putString(KEY_VOICE, newSettings.voiceName)
            .putString(KEY_MODEL, newSettings.modelName)
            .putString(KEY_ACTIVATION_MODE, newSettings.activationMode.name)
            .putString(KEY_USER_NAME, newSettings.userName)
            .putBoolean(KEY_DEVICE_CONTROL, newSettings.deviceControlEnabled)
            .putBoolean(KEY_MEMORY, newSettings.memoryEnabled)
            .apply()
        _settings.value = newSettings
    }
}
