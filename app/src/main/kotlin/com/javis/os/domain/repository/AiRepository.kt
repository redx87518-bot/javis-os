package com.javis.os.domain.repository

import com.javis.os.data.remote.dto.ChatMessage

interface AiRepository {
    suspend fun chat(messages: List<ChatMessage>): Result<String>
    fun getActiveProvider(): String
    suspend fun switchProvider(provider: String)
}
