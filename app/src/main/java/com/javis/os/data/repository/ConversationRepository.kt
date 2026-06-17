package com.javis.os.data.repository

import com.javis.os.data.db.dao.ConversationDao
import com.javis.os.data.db.entities.ConversationEntity
import com.javis.os.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val dao: ConversationDao
) {
    fun getAllFlow(): Flow<List<Message>> = dao.getAllFlow().map { list ->
        list.map { it.toMessage() }
    }

    suspend fun saveMessage(role: String, content: String, sessionId: String = "") {
        dao.insert(ConversationEntity(role = role, content = content, sessionId = sessionId))
    }

    suspend fun getRecentHistory(limit: Int = 20): List<Message> =
        dao.getRecent(limit).reversed().map { it.toMessage() }

    suspend fun clearAll() = dao.clearAll()

    suspend fun deleteOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        dao.deleteOlderThan(cutoff)
    }

    private fun ConversationEntity.toMessage() = Message(
        id = id,
        role = role,
        content = content,
        timestamp = timestamp
    )
}
