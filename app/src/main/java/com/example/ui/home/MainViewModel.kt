package com.example.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.ai.live.LiveSessionManager
import com.example.ai.text.TextAIEngine
import com.example.ai.tools.ToolManager
import com.example.audio.player.AudioPlayer
import com.example.audio.recorder.AudioRecorder
import com.example.data.preferences.PreferencesManager
import com.example.domain.model.AssistantSettings
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageSender
import com.example.domain.model.VoiceState
import com.example.service.VoiceAssistantService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val preferencesManager = PreferencesManager(application)
    val settings: StateFlow<AssistantSettings> = preferencesManager.settings

    private val toolManager = ToolManager(application)
    private val textAIEngine = TextAIEngine(BuildConfig.GEMINI_API_KEY)

    private val _voiceState = MutableStateFlow(VoiceState.DISCONNECTED)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _statusText = MutableStateFlow("Tap to activate ULTRON")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isTextLoading = MutableStateFlow(false)
    val isTextLoading: StateFlow<Boolean> = _isTextLoading.asStateFlow()

    private val _activeToolEvent = MutableStateFlow<String?>(null)
    val activeToolEvent: StateFlow<String?> = _activeToolEvent.asStateFlow()

    // Dedicated Audio Player
    private val audioPlayer: AudioPlayer = AudioPlayer(
        onPlaybackStarted = {
            if (_voiceState.value != VoiceState.INTERRUPTED) {
                _voiceState.value = VoiceState.SPEAKING
                _statusText.value = "ULTRON speaking"
            }
        },
        onPlaybackEnded = {
            if (_voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
                _statusText.value = "Standby"
            }
        }
    )

    // Dedicated Live Session Manager
    private val liveSessionManager: LiveSessionManager = LiveSessionManager(
        apiKey = BuildConfig.GEMINI_API_KEY,
        toolManager = toolManager,
        onAudioReceived = { pcm24Bytes ->
            audioPlayer.queueAudio(pcm24Bytes)
        },
        onTranscriptReceived = { text, isUser ->
            _lastTranscript.value = text
            appendMessage(
                ChatMessage(
                    sender = if (isUser) MessageSender.USER else MessageSender.ASSISTANT,
                    text = text
                )
            )
        },
        onInterrupted = {
            handleUserInterruption()
        }
    )

    // Dedicated Audio Recorder
    private val audioRecorder: AudioRecorder = AudioRecorder(
        onAudioChunk = { chunk ->
            liveSessionManager.sendAudioChunk(chunk)
        },
        onUserSpeechDetected = {
            // Natural user interruption: stop assistant speech immediately
            if (_voiceState.value == VoiceState.SPEAKING) {
                handleUserInterruption()
            }
        }
    )

    // Dynamic visualization amplitude: combines recorder mic amplitude or player audio amplitude
    private val _compositeAmplitude = MutableStateFlow(0f)
    val compositeAmplitude: StateFlow<Float> = _compositeAmplitude.asStateFlow()

    init {
        // Collect live session state
        viewModelScope.launch {
            liveSessionManager.voiceState.collect { state ->
                // Do not override SPEAKING unless session indicates otherwise
                if (state != VoiceState.IDLE || !audioPlayer.isPlaying.value) {
                    _voiceState.value = state
                }
            }
        }

        viewModelScope.launch {
            liveSessionManager.statusMessage.collect { msg ->
                _statusText.value = msg
            }
        }

        viewModelScope.launch {
            liveSessionManager.toolEvents.collect { evt ->
                _activeToolEvent.value = evt
                appendMessage(
                    ChatMessage(
                        sender = MessageSender.TOOL,
                        text = evt,
                        toolName = "ULTRON Android Control"
                    )
                )
            }
        }

        // Combine amplitudes for UI visualization
        viewModelScope.launch {
            combine(
                audioRecorder.audioAmplitude,
                audioPlayer.outputAmplitude
            ) { recAmp, playAmp ->
                if (_voiceState.value == VoiceState.SPEAKING) {
                    playAmp
                } else {
                    recAmp
                }
            }.collect { amp ->
                _compositeAmplitude.value = amp
            }
        }
    }

    fun toggleVoiceAssistant(hasMicPermission: Boolean) {
        if (!hasMicPermission) {
            _voiceState.value = VoiceState.ERROR
            _statusText.value = "Microphone permission required"
            return
        }

        when (_voiceState.value) {
            VoiceState.DISCONNECTED, VoiceState.ERROR -> {
                activateAssistant()
            }
            VoiceState.LISTENING, VoiceState.SPEAKING, VoiceState.THINKING, VoiceState.IDLE -> {
                deactivateAssistant()
            }
            else -> {
                deactivateAssistant()
            }
        }
    }

    private fun activateAssistant() {
        val app = getApplication<Application>()
        VoiceAssistantService.start(app)

        liveSessionManager.connect(settings.value)
        val started = audioRecorder.startRecording()
        if (started) {
            _voiceState.value = VoiceState.CONNECTING
            _statusText.value = "Activating ULTRON..."
        } else {
            _voiceState.value = VoiceState.ERROR
            _statusText.value = "Failed to initialize microphone"
        }
    }

    private fun deactivateAssistant() {
        audioRecorder.stopRecording()
        audioPlayer.stopAndClear()
        liveSessionManager.disconnect()
        _voiceState.value = VoiceState.DISCONNECTED
        _statusText.value = "ULTRON Standby"
        _activeToolEvent.value = null

        val app = getApplication<Application>()
        VoiceAssistantService.stop(app)
    }

    fun handleUserInterruption() {
        Log.d(TAG, "Interruption triggered: stopping assistant audio")
        audioPlayer.stopAndClear()
        _voiceState.value = VoiceState.INTERRUPTED
        _statusText.value = "Listening to Asik..."
        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            if (_voiceState.value == VoiceState.INTERRUPTED) {
                _voiceState.value = VoiceState.LISTENING
            }
        }
    }

    fun sendTextMessage(prompt: String) {
        if (prompt.isBlank()) return
        appendMessage(ChatMessage(sender = MessageSender.USER, text = prompt))

        // If Live session is connected, route through live session
        if (_voiceState.value != VoiceState.DISCONNECTED && _voiceState.value != VoiceState.ERROR) {
            liveSessionManager.sendTextMessage(prompt)
            return
        }

        // Otherwise use Text AI Engine
        viewModelScope.launch(Dispatchers.IO) {
            _isTextLoading.value = true
            val result = textAIEngine.generateTextResponse(
                prompt = prompt,
                history = _chatMessages.value,
                userName = settings.value.userName
            )
            _isTextLoading.value = false
            result.onSuccess { text ->
                appendMessage(ChatMessage(sender = MessageSender.ASSISTANT, text = text))
            }.onFailure { err ->
                appendMessage(
                    ChatMessage(
                        sender = MessageSender.SYSTEM,
                        text = "Error: ${err.message ?: "Could not process request"}"
                    )
                )
            }
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        _lastTranscript.value = ""
    }

    fun updateSettings(newSettings: AssistantSettings) {
        preferencesManager.updateSettings(newSettings)
        // If session was running, reconnect with updated settings
        if (_voiceState.value != VoiceState.DISCONNECTED && _voiceState.value != VoiceState.ERROR) {
            liveSessionManager.connect(newSettings)
        }
    }

    private fun appendMessage(message: ChatMessage) {
        _chatMessages.value = _chatMessages.value + message
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
        audioPlayer.release()
        liveSessionManager.disconnect()
    }
}
