package com.example.domain.model

/**
 * State machine for the real-time voice assistant
 */
enum class VoiceState {
    DISCONNECTED,
    CONNECTING,
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR,
    RECONNECTING
}

enum class ActivationMode {
    TAP_TO_TALK,
    SUMMON_MODE,
    CONTINUOUS_LISTENING
}
