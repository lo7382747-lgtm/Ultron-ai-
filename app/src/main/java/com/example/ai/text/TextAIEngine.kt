package com.example.ai.text

import android.util.Log
import com.example.domain.model.ChatMessage
import com.example.domain.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TextAIEngine(private val apiKey: String) {

    companion object {
        private const val TAG = "TextAIEngine"
        private const val BASE_REST_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateTextResponse(
        prompt: String,
        history: List<ChatMessage>,
        userName: String = "Asik"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()

            // System instructions
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().apply {
                put(
                    "text",
                    "You are ULTRON, a fast, intelligent, confident, slightly witty, emotionally responsive, and helpful personal AI assistant. " +
                            "You recognize the user as $userName. Address the user as $userName naturally when appropriate. " +
                            "Never display or mention the word 'Gemini'. Format responses using clean Markdown and code blocks where applicable."
                )
            })
            systemInstruction.put("parts", sysParts)
            root.put("systemInstruction", systemInstruction)

            // Add previous history turns (up to last 10)
            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                val turn = JSONObject()
                turn.put("role", if (msg.sender == MessageSender.USER) "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", msg.text) })
                turn.put("parts", parts)
                contents.put(turn)
            }

            // Current user turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().apply { put("text", prompt) })
            currentTurn.put("parts", currentParts)
            contents.put(currentTurn)

            root.put("contents", contents)

            val url = "$BASE_REST_URL?key=$apiKey"
            val body = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "REST API call failed: ${response.code} $responseBody")
                return@withContext Result.failure(Exception("API Error (${response.code})"))
            }

            val resJson = JSONObject(responseBody)
            val candidates = resJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)
            val replyText = textPart?.optString("text", "I was unable to generate a response.")
                ?: "I was unable to generate a response."

            Result.success(replyText)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during text generation", e)
            Result.failure(e)
        }
    }
}
