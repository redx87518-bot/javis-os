package com.javis.os.data.local.dao

import androidx.room.*
import com.javis.os.data.local.entities.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY importance DESC, timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC")
    fun getByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM memories ORDER BY importance DESC, timestamp DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int = 20): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Query("DELETE FROM memories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
