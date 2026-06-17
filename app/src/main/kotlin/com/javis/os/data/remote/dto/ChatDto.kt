package com.javis.os.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Float = 0.8f,
    val stream: Boolean = false
)

data class ChatResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

data class Choice(
    val message: ChatMessage = ChatMessage("assistant", ""),
    @SerializedName("finish_reason") val finishReason: String = ""
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0
)
