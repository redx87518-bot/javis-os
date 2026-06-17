package com.javis.os.domain.model

data class Message(
    val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
) {
    val isUser get() = role == "user"
    val isAssistant get() = role == "assistant"
    val isSystem get() = role == "system"
}
