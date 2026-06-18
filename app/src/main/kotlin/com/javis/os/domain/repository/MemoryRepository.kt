package com.javis.os.domain.repository

import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getAllMemories(): Flow<List<com.javis.os.domain.model.Memory>>
    suspend fun remember(key: String, value: String, category: String, importance: Int = 1)
    suspend fun recall(key: String): String?
    suspend fun getContextSummary(): String
    suspend fun forgetKey(key: String)
    suspend fun clearAll()
}
