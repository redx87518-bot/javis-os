package com.javis.os.data.repository

import com.javis.os.data.local.dao.ConversationDao
import com.javis.os.data.local.entities.ConversationEntity
import com.javis.os.domain.model.Message
import com.javis.os.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao
) : ConversationRepository {

    override fun getRecentMessages(): Flow<List<Message>> =
        conversationDao.getRecentMessages(200).map { list ->
            list.reversed().map { it.toDomain() }
        }

    override suspend fun addMessage(role: Message.Role, content: String): Long =
        conversationDao.insert(
            ConversationEntity(
                role = role.name.lowercase(),
                content = content
            )
        )

    override suspend fun getContextMessages(limit: Int): List<Message> =
        conversationDao.getRecentMessagesSync(limit).reversed().map { it.toDomain() }

    override suspend fun clearOldMessages(keepDays: Int) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(keepDays.toLong())
        conversationDao.deleteOlderThan(cutoff)
    }

    private fun ConversationEntity.toDomain() = Message(
        id = id,
        role = when (role) {
            "user" -> Message.Role.USER
            "assistant" -> Message.Role.ASSISTANT
            else -> Message.Role.SYSTEM
        },
        content = content,
        timestamp = timestamp
    )
}
