package com.javis.os.data.db.dao

import androidx.room.*
import com.javis.os.data.db.entities.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Query("SELECT * FROM memories WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE category = :category")
    suspend fun getByCategory(category: String): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("DELETE FROM memories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
