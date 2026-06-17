package com.javis.os.data.repository

import com.javis.os.data.db.dao.MemoryDao
import com.javis.os.data.db.entities.MemoryEntity
import com.javis.os.domain.model.UserMemory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao
) {
    fun getAllFlow(): Flow<List<UserMemory>> = dao.getAllFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun remember(key: String, value: String, category: String = "general") {
        val existing = dao.getByKey(key)
        if (existing != null) {
            dao.update(existing.copy(value = value, updatedAt = System.currentTimeMillis()))
        } else {
            dao.insert(MemoryEntity(key = key, value = value, category = category))
        }
    }

    suspend fun recall(key: String): String? = dao.getByKey(key)?.value

    suspend fun recallByCategory(category: String): List<UserMemory> =
        dao.getByCategory(category).map { it.toModel() }

    suspend fun getAll(): List<UserMemory> = dao.getAll().map { it.toModel() }

    suspend fun forget(key: String) = dao.deleteByKey(key)

    suspend fun clearAll() = dao.clearAll()

    suspend fun buildMemoryContext(): String {
        val memories = dao.getAll()
        if (memories.isEmpty()) return ""
        return "User memory: " + memories.joinToString(", ") { "${it.key}: ${it.value}" }
    }

    private fun MemoryEntity.toModel() = UserMemory(
        id = id,
        key = key,
        value = value,
        category = category,
        updatedAt = updatedAt
    )
}
