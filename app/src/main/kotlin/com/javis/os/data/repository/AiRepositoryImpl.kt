package com.javis.os.data.repository

import android.util.Log
import com.javis.os.data.remote.api.DeepSeekApi
import com.javis.os.data.remote.api.GroqApi
import com.javis.os.data.remote.dto.ChatMessage
import com.javis.os.data.remote.dto.ChatRequest
import com.javis.os.domain.repository.AiRepository
import com.javis.os.util.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val groqApi: GroqApi,
    private val deepSeekApi: DeepSeekApi,
    private val prefs: PreferencesManager
) : AiRepository {

    private val groqModels = listOf("llama3-70b-8192", "llama3-8b-8192", "mixtral-8x7b-32768")
    private val deepSeekModels = listOf("deepseek-chat", "deepseek-coder")

    override suspend fun chat(messages: List<ChatMessage>): Result<String> {
        val provider = prefs.getAiProvider()
        return when (provider) {
            "groq" -> chatWithGroq(messages)
            "deepseek" -> chatWithDeepSeek(messages)
            else -> chatWithGroq(messages)
        }
    }

    private suspend fun chatWithGroq(messages: List<ChatMessage>): Result<String> {
        val apiKey = prefs.getGroqApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("Groq API key not set"))
        return try {
            val model = prefs.getGroqModel().ifBlank { groqModels[0] }
            val response = groqApi.chat(
                auth = "Bearer $apiKey",
                request = ChatRequest(model = model, messages = messages)
            )
            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                Result.success(reply)
            } else {
                // Fallback to deepseek if groq fails
                Log.w("AI", "Groq failed ${response.code()}, trying DeepSeek")
                chatWithDeepSeek(messages)
            }
        } catch (e: Exception) {
            Log.e("AI", "Groq error: ${e.message}")
            chatWithDeepSeek(messages)
        }
    }

    private suspend fun chatWithDeepSeek(messages: List<ChatMessage>): Result<String> {
        val apiKey = prefs.getDeepSeekApiKey()
        if (apiKey.isBlank()) return Result.failure(Exception("No AI provider configured. Please add an API key in Settings."))
        return try {
            val model = deepSeekModels[0]
            val response = deepSeekApi.chat(
                auth = "Bearer $apiKey",
                request = ChatRequest(model = model, messages = messages)
            )
            if (response.isSuccessful) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                Result.success(reply)
            } else {
                Result.failure(Exception("DeepSeek error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getActiveProvider(): String = prefs.getAiProvider()

    override suspend fun switchProvider(provider: String) {
        prefs.setAiProvider(provider)
    }
}
