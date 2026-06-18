package com.javis.os.data.repository

import com.javis.os.data.local.dao.MemoryDao
import com.javis.os.data.local.entities.MemoryEntity
import com.javis.os.domain.model.Memory
import com.javis.os.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override fun getAllMemories(): Flow<List<Memory>> =
        memoryDao.getAllMemories().map { list -> list.map { it.toDomain() } }

    override suspend fun remember(key: String, value: String, category: String, importance: Int) {
        memoryDao.insert(MemoryEntity(key = key, value = value, category = category, importance = importance))
    }

    override suspend fun recall(key: String): String? = memoryDao.getByKey(key)?.value

    override suspend fun getContextSummary(): String {
        val memories = memoryDao.getTopMemories(20)
        if (memories.isEmpty()) return ""
        return memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
    }

    override suspend fun forgetKey(key: String) { memoryDao.deleteByKey(key) }

    override suspend fun clearAll() { memoryDao.clearAll() }

    private fun MemoryEntity.toDomain() =
        Memory(id = id, key = key, value = value, category = category, importance = importance, timestamp = timestamp)
}
