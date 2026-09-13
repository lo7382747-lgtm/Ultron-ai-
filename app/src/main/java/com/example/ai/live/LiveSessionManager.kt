package com.example.ai.live

import android.util.Base64
import android.util.Log
import com.example.ai.tools.ToolManager
import com.example.domain.model.AssistantSettings
import com.example.domain.model.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LiveSessionManager(
    private val apiKey: String,
    private val toolManager: ToolManager,
    private val onAudioReceived: (ByteArray) -> Unit,
    private val onTranscriptReceived: (String, Boolean) -> Unit, // text, isUser
    private val onInterrupted: () -> Unit
) {
    companion object {
        private const val TAG = "LiveSessionManager"
        private const val BASE_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var isManuallyClosed = false

    private val _voiceState = MutableStateFlow(VoiceState.DISCONNECTED)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap to activate ULTRON")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _toolEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val toolEvents: SharedFlow<String> = _toolEvents.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // Keep-alive for streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    fun connect(settings: AssistantSettings) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            _voiceState.value = VoiceState.ERROR
            _statusMessage.value = "Gemini API key missing. Configure in AI Studio Secrets panel."
            return
        }

        isManuallyClosed = false
        _voiceState.value = VoiceState.CONNECTING
        _statusMessage.value = "Connecting to ULTRON core..."

        val url = "$BASE_WS_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket?.cancel()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully")
                _voiceState.value = VoiceState.IDLE
                _statusMessage.value = "ULTRON online. Speak or tap microphone."
                sendInitialSetup(webSocket, settings)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(webSocket, text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                if (!isManuallyClosed) {
                    _voiceState.value = VoiceState.RECONNECTING
                    _statusMessage.value = "Connection closing: $reason. Reconnecting..."
                    attemptReconnect(settings)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                if (!isManuallyClosed) {
                    _voiceState.value = VoiceState.DISCONNECTED
                    _statusMessage.value = "Disconnected"
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                if (!isManuallyClosed) {
                    _voiceState.value = VoiceState.ERROR
                    val err = t.message ?: "Network failure"
                    _statusMessage.value = "Connection error: $err"
                    attemptReconnect(settings)
                }
            }
        })
    }

    private fun sendInitialSetup(ws: WebSocket, settings: AssistantSettings) {
        try {
            val setupJson = JSONObject()
            val setupBody = JSONObject()

            // Configured model: default to supported native audio or user preference
            val modelStr = if (settings.modelName.startsWith("models/")) settings.modelName else "models/${settings.modelName}"
            setupBody.put("model", modelStr)

            // Generation config
            val genConfig = JSONObject()
            val responseModalities = JSONArray().apply {
                put("AUDIO")
            }
            genConfig.put("responseModalities", responseModalities)

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuilt = JSONObject()
            prebuilt.put("voiceName", settings.voiceName)
            voiceConfig.put("prebuiltVoiceConfig", prebuilt)
            speechConfig.put("voiceConfig", voiceConfig)
            genConfig.put("speechConfig", speechConfig)
            setupBody.put("generationConfig", genConfig)

            // System Instruction
            val systemInstruction = JSONObject()
            val parts = JSONArray()
            val sysPart = JSONObject()
            val promptText = """
                You are ULTRON, a fast, intelligent, confident, slightly witty, emotionally responsive, and helpful personal AI assistant.
                You recognize the user as ${settings.userName}. Address the user as ${settings.userName} naturally when appropriate, but do not repeat the name in every sentence.
                Never display or say the word 'Gemini'.
                You can communicate fluently in English, Hindi, and Hinglish depending on how ${settings.userName} speaks to you.
                Keep responses concise, natural, and conversational.
                You have real Android tools available:
                - open_app: Opens installed Android apps such as YouTube, Maps, Settings, Camera, Chrome, etc.
                - get_device_time: Returns current time, date, battery status.
                - search_web: Searches the web.
                - open_website: Opens URLs in browser.
                - control_device: Toggles flashlight or opens volume/wifi/battery settings.
                - calculate: Computes mathematical expressions.
                When the user requests an action matching these tools, invoke the appropriate tool immediately.
            """.trimIndent()
            sysPart.put("text", promptText)
            parts.put(sysPart)
            systemInstruction.put("parts", parts)
            setupBody.put("systemInstruction", systemInstruction)

            // Tools
            if (settings.deviceControlEnabled) {
                setupBody.put("tools", toolManager.getToolsDeclarationJson())
            }

            setupJson.put("setup", setupBody)
            ws.send(setupJson.toString())
            Log.d(TAG, "Sent setup configuration: ${setupJson.toString().take(120)}...")
        } catch (e: Exception) {
            Log.e(TAG, "Error building setup JSON", e)
        }
    }

    fun sendAudioChunk(pcm16Bytes: ByteArray) {
        val ws = webSocket ?: return
        if (_voiceState.value == VoiceState.DISCONNECTED || _voiceState.value == VoiceState.CONNECTING) return

        try {
            val base64Data = Base64.encodeToString(pcm16Bytes, Base64.NO_WRAP)
            val root = JSONObject()
            val realtimeInput = JSONObject()
            val mediaChunks = JSONArray()
            val chunk = JSONObject()
            chunk.put("mimeType", "audio/pcm;rate=16000")
            chunk.put("data", base64Data)
            mediaChunks.put(chunk)
            realtimeInput.put("mediaChunks", mediaChunks)
            root.put("realtimeInput", realtimeInput)

            ws.send(root.toString())
            if (_voiceState.value != VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.LISTENING
                _statusMessage.value = "ULTRON is listening..."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio chunk", e)
        }
    }

    fun sendTextMessage(text: String) {
        val ws = webSocket ?: return
        try {
            val root = JSONObject()
            val clientContent = JSONObject()
            val turns = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", text)
            parts.put(part)
            turn.put("parts", parts)
            turns.put(turn)
            clientContent.put("turns", turns)
            clientContent.put("turnComplete", true)
            root.put("clientContent", clientContent)

            ws.send(root.toString())
            _voiceState.value = VoiceState.THINKING
            _statusMessage.value = "Processing request..."
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text message", e)
        }
    }

    private fun handleServerMessage(ws: WebSocket, text: String) {
        try {
            val json = JSONObject(text)

            // 1. Check for serverContent
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                // Interrupted
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Server signaled interruption")
                    _voiceState.value = VoiceState.INTERRUPTED
                    _statusMessage.value = "Interrupted"
                    onInterrupted()
                    scope.launch {
                        kotlinx.coroutines.delay(200)
                        if (_voiceState.value == VoiceState.INTERRUPTED) {
                            _voiceState.value = VoiceState.LISTENING
                        }
                    }
                    return
                }

                // Model turn
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts") ?: JSONArray()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)

                        // Text transcript
                        if (part.has("text")) {
                            val chunkText = part.getString("text")
                            onTranscriptReceived(chunkText, false)
                        }

                        // Audio output
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val mimeType = inlineData.optString("mimeType", "")
                            if (mimeType.startsWith("audio/pcm")) {
                                val b64 = inlineData.getString("data")
                                val audioBytes = Base64.decode(b64, Base64.DEFAULT)
                                _voiceState.value = VoiceState.SPEAKING
                                _statusMessage.value = "ULTRON speaking"
                                onAudioReceived(audioBytes)
                            }
                        }
                    }
                }

                // Turn complete
                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.d(TAG, "Turn complete")
                    if (_voiceState.value == VoiceState.THINKING) {
                        _voiceState.value = VoiceState.IDLE
                        _statusMessage.value = "Ready"
                    }
                }
            }

            // 2. Tool calls
            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls") ?: JSONArray()
                for (i in 0 until functionCalls.length()) {
                    val fc = functionCalls.getJSONObject(i)
                    val callId = fc.optString("id", "")
                    val name = fc.getString("name")
                    val args = fc.optJSONObject("args") ?: JSONObject()

                    _voiceState.value = VoiceState.THINKING
                    _statusMessage.value = "Executing tool: $name"
                    _toolEvents.tryEmit("Executing: $name")

                    scope.launch {
                        val execResult = toolManager.executeTool(name, args, callId)
                        _toolEvents.tryEmit("Result: ${execResult.output}")
                        sendToolResponse(ws, callId, execResult.output)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling server message", e)
        }
    }

    private fun sendToolResponse(ws: WebSocket, callId: String, outputText: String) {
        try {
            val root = JSONObject()
            val toolResponse = JSONObject()
            val functionResponses = JSONArray()
            val fr = JSONObject()
            fr.put("id", callId)
            val responseObj = JSONObject()
            val outputObj = JSONObject()
            outputObj.put("result", outputText)
            responseObj.put("output", outputObj)
            fr.put("response", responseObj)
            functionResponses.put(fr)
            toolResponse.put("functionResponses", functionResponses)
            root.put("toolResponse", toolResponse)

            ws.send(root.toString())
            Log.d(TAG, "Sent tool response for $callId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tool response", e)
        }
    }

    private fun attemptReconnect(settings: AssistantSettings) {
        if (isManuallyClosed) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _voiceState.value = VoiceState.RECONNECTING
            _statusMessage.value = "Connection lost. Reconnecting in 3s..."
            kotlinx.coroutines.delay(3000)
            if (!isManuallyClosed) {
                connect(settings)
            }
        }
    }

    fun disconnect() {
        isManuallyClosed = true
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing webSocket", e)
        } finally {
            webSocket = null
            _voiceState.value = VoiceState.DISCONNECTED
            _statusMessage.value = "ULTRON Standby"
        }
    }

    fun setThinkingState() {
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.THINKING
            _statusMessage.value = "Analyzing voice command..."
        }
    }

    fun setSpeakingState() {
        _voiceState.value = VoiceState.SPEAKING
        _statusMessage.value = "ULTRON is speaking"
    }

    fun setIdleState() {
        if (_voiceState.value != VoiceState.DISCONNECTED && _voiceState.value != VoiceState.ERROR) {
            _voiceState.value = VoiceState.IDLE
            _statusMessage.value = "Ready"
        }
    }
}
