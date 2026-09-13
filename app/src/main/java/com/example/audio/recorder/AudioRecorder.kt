package com.example.audio.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

class AudioRecorder(
    private val onAudioChunk: (ByteArray) -> Unit,
    private val onUserSpeechDetected: (() -> Unit)? = null
) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val TAG = "AudioRecorder"
        private const val SPEECH_AMPLITUDE_THRESHOLD = 1800 // Amplitude threshold for interruption
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (_isRecording.value) return true

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = max(minBufferSize, 2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch {
                val readBuffer = ByteArray(1024) // ~32ms of 16kHz 16-bit mono audio
                val shortBuffer = ShortArray(512)

                while (isActive && _isRecording.value) {
                    val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                    if (bytesRead > 0) {
                        // Copy exact buffer to send
                        val chunk = readBuffer.copyOf(bytesRead)
                        onAudioChunk(chunk)

                        // Calculate RMS amplitude for visualization & interruption detection
                        ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer, 0, bytesRead / 2)
                        var sum = 0L
                        var maxAmp = 0
                        val samples = bytesRead / 2
                        for (i in 0 until samples) {
                            val sample = abs(shortBuffer[i].toInt())
                            sum += sample
                            if (sample > maxAmp) maxAmp = sample
                        }
                        val avgAmp = if (samples > 0) (sum / samples).toFloat() else 0f
                        val normalized = (avgAmp / 8000f).coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized

                        // If significant speech detected during recording
                        if (maxAmp > SPEECH_AMPLITUDE_THRESHOLD) {
                            onUserSpeechDetected?.invoke()
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio record", e)
            stopRecording()
            return false
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio record", e)
        } finally {
            audioRecord = null
            _audioAmplitude.value = 0f
        }
    }
}
