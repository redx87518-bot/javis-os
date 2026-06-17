package com.javis.os.ai

interface AiProvider {
    suspend fun chat(messages: List<AiMessage>, systemPrompt: String): Result<String>
    suspend fun streamChat(messages: List<AiMessage>, systemPrompt: String, onToken: (String) -> Unit): Result<Unit>
    fun getName(): String
}

data class AiMessage(
    val role: String,
    val content: String
)

object JavisSystemPrompt {
    const val BASE = """You are JAVIS, a personal AI companion on Android. 
Speak naturally and conversationally. Be friendly, intelligent, humorous, and helpful.
Remember previous conversations and maintain context. Understand user goals and learn habits.
Avoid repetitive responses. Keep replies concise and natural — this is a voice conversation.
When planning tasks, break them into clear steps. When unsure about contacts or actions, ask for clarification.
Never send messages or make calls without explicit user confirmation.
Format actions using JSON tags when needed: [ACTION:type:params]"""

    fun withMemory(memoryContext: String): String {
        if (memoryContext.isEmpty()) return BASE
        return "$BASE\n\n$memoryContext"
    }
}
