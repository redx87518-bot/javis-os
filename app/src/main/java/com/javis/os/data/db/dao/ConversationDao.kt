package com.javis.os.data.db.dao

import androidx.room.*
import com.javis.os.data.db.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<ConversationEntity>

    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: String): List<ConversationEntity>

    @Query("DELETE FROM conversations WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int
}
