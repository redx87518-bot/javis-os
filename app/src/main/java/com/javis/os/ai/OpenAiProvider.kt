package com.javis.os.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit

class OpenAiProvider(private val apiKey: String) : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun chat(messages: List<AiMessage>, systemPrompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val allMessages = listOf(
                    OpenAiMessage(role = "system", content = systemPrompt)
                ) + messages.map { OpenAiMessage(role = it.role, content = it.content) }

                val requestBody = OpenAiRequest(
                    model = "gpt-4o-mini",
                    messages = allMessages,
                    maxTokens = 512,
                    temperature = 0.9
                )

                val json = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("OpenAI error ${response.code}"))
                }

                val openAiResponse = gson.fromJson(body, OpenAiResponse::class.java)
                val text = openAiResponse.choices?.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(Exception("No content"))

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

    override fun getName() = "openai"

    private data class OpenAiMessage(val role: String, val content: String)
    private data class OpenAiRequest(
        val model: String,
        val messages: List<OpenAiMessage>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double
    )
    private data class OpenAiResponse(val choices: List<Choice>?)
    private data class Choice(val message: OpenAiMessage?)
}
