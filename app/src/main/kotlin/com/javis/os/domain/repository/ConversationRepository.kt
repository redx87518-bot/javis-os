package com.javis.os.domain.repository

import com.javis.os.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getRecentMessages(): Flow<List<Message>>
    suspend fun addMessage(role: Message.Role, content: String): Long
    suspend fun getContextMessages(limit: Int = 12): List<Message>
    suspend fun clearOldMessages(keepDays: Int = 30)
}
