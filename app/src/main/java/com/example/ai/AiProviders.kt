package com.example.ai

import android.content.Context
import com.example.core.command.CommandResult
import com.example.core.command.LocalCommandRouter
import com.example.core.model.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

interface AiChatProvider {
    val name: String
    val isConfigured: Boolean
    val isLocal: Boolean
    suspend fun generateResponse(prompt: String, conversationContext: List<Pair<String, String>>): Result<String>
}

interface VisionProvider {
    val name: String
    val isConfigured: Boolean
    suspend fun analyzeImage(description: String): Result<String>
}

interface ImageGenerationProvider {
    val name: String
    val isConfigured: Boolean
    suspend fun generateImage(prompt: String, style: String): Result<String>
}

interface ThreeDGenerationProvider {
    val name: String
    val isConfigured: Boolean
    suspend fun generate3D(prompt: String): Result<String>
}

class LocalKnowledgeChatProvider : AiChatProvider {
    override val name: String = "Nila Local Engine (Offline-First)"
    override val isConfigured: Boolean = true
    override val isLocal: Boolean = true

    override suspend fun generateResponse(
        prompt: String,
        conversationContext: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.Default) {
        val query = prompt.trim().lowercase(Locale.ROOT)

        val response = when {
            query == "nila" -> "I am here."
            query.contains("how are you") -> "I am operating optimally on your device, ready to assist."
            query.contains("privacy") -> "Nila runs on a free-first, privacy-conscious architecture. Your conversations, memories, and personal profiles are stored locally in your private Room database."
            query.contains("offline") -> "Nila supports complete offline operation for local commands, memory recall, study sessions, and voice synthesis without requiring internet access."
            query.contains("gesture") -> "Nila's vision engine uses camera frame analysis for gestures like Thumbs Up (confirm), Thumbs Down (cancel), and Open Palm (pause)."
            query.contains("study") || query.contains("learn") -> "Nila Study Assistant can help organize your study topics, generate review questions, and track mastery progression."
            query.contains("3d") -> "Real 3D generation requires an integrated 3D provider (such as Tripo or CSM API). When not configured, Nila reports status truthfully rather than simulating output."
            query.contains("code") || query.contains("kotlin") || query.contains("android") -> "I can assist with Kotlin, Jetpack Compose, Room, MVVM architecture, and coroutine pipelines."
            else -> "I processed your request locally. Connect the Nila Backend server to enable Gemini Cloud AI reasoning."
        }
        Result.success(response)
    }
}

/**
 * Secure production chat client communicating exclusively with the Nila Backend API.
 * Never stores or requests the Gemini API key in Android APK or source code.
 */
class GeminiCloudChatProvider(
    private val getBackendUrl: () -> String = { "http://10.0.2.2:8080" },
    private val getAuthToken: () -> String = { "" }
) : AiChatProvider {
    override val name: String = "Nila Backend API (Gemini Production Proxy)"
    override val isLocal: Boolean = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    override val isConfigured: Boolean
        get() = true

    override suspend fun generateResponse(
        prompt: String,
        conversationContext: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = getBackendUrl().trimEnd('/')
        val endpointUrl = "$baseUrl/api/ai/chat"

        try {
            val contextJsonArray = JSONArray()
            conversationContext.takeLast(10).forEach { (role, text) ->
                val obj = JSONObject().apply {
                    put("role", if (role.equals("USER", ignoreCase = true)) "user" else "model")
                    put("text", text)
                }
                contextJsonArray.put(obj)
            }

            val requestJson = JSONObject().apply {
                put("prompt", prompt.trim())
                put("conversationContext", contextJsonArray)
            }

            val requestBuilder = Request.Builder()
                .url(endpointUrl)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Accept", "application/json")

            val token = getAuthToken().trim()
            if (token.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            // Strict Content-Type validation
            val contentType = response.header("Content-Type") ?: ""
            if (!contentType.contains("application/json") && !bodyString.trim().startsWith("{")) {
                return@withContext Result.failure(
                    AppError.ProviderError("Unexpected response format from Nila Backend (HTTP $code).")
                )
            }

            val json = try {
                JSONObject(bodyString)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    AppError.ProviderError("Malformed response received from Nila Backend.")
                )
            }

            val isSuccess = json.optBoolean("success", false)
            val message = json.optString("message", "")

            if (response.isSuccessful && isSuccess && message.isNotBlank()) {
                Result.success(message)
            } else {
                val cleanError = if (message.isNotBlank()) message else "AI service is temporarily unavailable."
                Result.failure(AppError.ProviderError(cleanError))
            }
        } catch (e: Exception) {
            Result.failure(
                AppError.NetworkError("Cannot reach Nila Backend at $baseUrl. Ensure backend server is running.")
            )
        }
    }
}

class HonestImageGenerationProvider : ImageGenerationProvider {
    override val name: String = "Nila Studio Image Engine"
    override val isConfigured: Boolean = false

    override suspend fun generateImage(prompt: String, style: String): Result<String> {
        return Result.failure(
            AppError.ProviderError("Image generation requires an external generation provider. No API endpoint is currently configured.")
        )
    }
}

class HonestThreeDGenerationProvider : ThreeDGenerationProvider {
    override val name: String = "Nila Neural 3D Engine"
    override val isConfigured: Boolean = false

    override suspend fun generate3D(prompt: String): Result<String> {
        return Result.failure(
            AppError.ProviderError("Real 3D generation is unavailable because no 3D mesh provider is active. Nila will not fake 3D generation.")
        )
    }
}

class AiRouter(
    private val context: Context,
    private val localCommandRouter: LocalCommandRouter,
    private val localEngine: LocalKnowledgeChatProvider = LocalKnowledgeChatProvider(),
    private val cloudEngine: GeminiCloudChatProvider = GeminiCloudChatProvider(
        getBackendUrl = {
            val prefs = context.getSharedPreferences("nila_settings_cache", Context.MODE_PRIVATE)
            prefs.getString("backend_url", "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
        },
        getAuthToken = {
            val prefs = context.getSharedPreferences("nila_settings_cache", Context.MODE_PRIVATE)
            prefs.getString("backend_token", "") ?: ""
        }
    )
) {
    suspend fun routeQuery(
        prompt: String,
        isOfflineEnforced: Boolean = false,
        conversationContext: List<Pair<String, String>> = emptyList()
    ): Pair<String, String> {
        // 1. Local Command Router evaluation (for on-device app launches, emergency stops, etc.)
        val cmdResult = localCommandRouter.evaluate(prompt)
        when (cmdResult) {
            is CommandResult.Handled -> {
                return Pair(cmdResult.reply, "Local Command Router")
            }
            is CommandResult.Blocked -> {
                return Pair(cmdResult.reason, "Security Policy")
            }
            is CommandResult.NeedsConfirmation -> {
                return Pair("⚠️ Confirmation Required: ${cmdResult.actionDescription}", "Safety Confirmation")
            }
            is CommandResult.NotACommand -> {
                // Continue to AI backend
            }
        }

        // 2. If user has explicitly enforced Offline Mode in Settings
        if (isOfflineEnforced) {
            val localRes = localEngine.generateResponse(prompt, conversationContext)
            return Pair(localRes.getOrDefault("Offline mode active. Connect to Nila Backend to enable Gemini."), "Offline Mode")
        }

        // 3. Dispatch query to Nila Backend API (which securely proxies to Gemini)
        val cloudRes = cloudEngine.generateResponse(prompt, conversationContext)
        return if (cloudRes.isSuccess) {
            Pair(cloudRes.getOrThrow(), "Nila Backend (Gemini)")
        } else {
            val failure = cloudRes.exceptionOrNull()
            val errorMsg = failure?.localizedMessage ?: "AI service is temporarily unavailable."
            Pair(errorMsg, "Backend Service Notice")
        }
    }
}
