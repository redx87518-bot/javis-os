package com.javis.os.domain.repository

import com.javis.os.domain.model.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getAllMemories(): Flow<List<Memory>>
    suspend fun remember(key: String, value: String, category: String, importance: Int = 1)
    suspend fun recall(key: String): String?
    suspend fun getContextSummary(): String
    suspend fun forgetKey(key: String)
}
