package com.javis.os.ai

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class GeminiProvider(private val apiKey: String) : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    override suspend fun chat(messages: List<AiMessage>, systemPrompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val contents = messages.map { msg ->
                    GeminiContent(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(GeminiPart(text = msg.content))
                    )
                }

                val requestBody = GeminiRequest(
                    contents = contents,
                    systemInstruction = GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = systemPrompt))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.9f,
                        maxOutputTokens = 512,
                        topP = 0.95f
                    )
                )

                val json = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("API error ${response.code}: $body"))
                }

                val geminiResponse = gson.fromJson(body, GeminiResponse::class.java)
                val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: return@withContext Result.failure(Exception("No response text"))

                Result.success(text.trim())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun streamChat(
        messages: List<AiMessage>,
        systemPrompt: String,
        onToken: (String) -> Unit
    ): Result<Unit> {
        val result = chat(messages, systemPrompt)
        result.getOrNull()?.let { onToken(it) }
        return result.map { }
    }

    override fun getName() = "gemini"

    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        @SerializedName("system_instruction") val systemInstruction: GeminiContent,
        @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig
    )

    private data class GeminiContent(
        val role: String,
        val parts: List<GeminiPart>
    )

    private data class GeminiPart(val text: String)

    private data class GeminiGenerationConfig(
        val temperature: Float = 0.9f,
        @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 512,
        val topP: Float = 0.95f
    )

    private data class GeminiResponse(
        val candidates: List<GeminiCandidate>?
    )

    private data class GeminiCandidate(
        val content: GeminiContent?
    )
}
