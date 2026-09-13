package com.example.audio.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs

class AudioPlayer(
    private val onPlaybackStarted: (() -> Unit)? = null,
    private val onPlaybackEnded: (() -> Unit)? = null
) {
    companion object {
        const val SAMPLE_RATE = 24000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val TAG = "AudioPlayer"
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private var playerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    @Synchronized
    fun initTrack() {
        if (audioTrack != null && audioTrack?.state == AudioTrack.STATE_INITIALIZED) return

        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audioTrack", e)
        }

        startPlaybackLoop()
    }

    private fun startPlaybackLoop() {
        playerJob?.cancel()
        playerJob = scope.launch {
            val shortBuffer = ShortArray(2048)
            while (isActive) {
                val chunk = audioQueue.poll()
                if (chunk != null && chunk.isNotEmpty()) {
                    if (!_isPlaying.value) {
                        _isPlaying.value = true
                        onPlaybackStarted?.invoke()
                    }

                    // Compute amplitude
                    val sampleCount = (chunk.size / 2).coerceAtMost(shortBuffer.size)
                    ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer, 0, sampleCount)
                    var sum = 0L
                    for (i in 0 until sampleCount) {
                        sum += abs(shortBuffer[i].toInt())
                    }
                    val avg = if (sampleCount > 0) sum / sampleCount else 0L
                    _outputAmplitude.value = (avg / 8000f).coerceIn(0f, 1f)

                    // Write to AudioTrack
                    audioTrack?.write(chunk, 0, chunk.size)
                } else {
                    if (_isPlaying.value && audioQueue.isEmpty()) {
                        _isPlaying.value = false
                        _outputAmplitude.value = 0f
                        onPlaybackEnded?.invoke()
                    }
                    kotlinx.coroutines.delay(20)
                }
            }
        }
    }

    fun queueAudio(data: ByteArray) {
        initTrack()
        audioQueue.offer(data)
    }

    /**
     * Immediately stop/fade audio on interruption
     */
    @Synchronized
    fun stopAndClear() {
        audioQueue.clear()
        _isPlaying.value = false
        _outputAmplitude.value = 0f
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing/flushing audioTrack", e)
        }
    }

    fun release() {
        playerJob?.cancel()
        playerJob = null
        audioQueue.clear()
        _isPlaying.value = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audioTrack", e)
        } finally {
            audioTrack = null
        }
    }
}
