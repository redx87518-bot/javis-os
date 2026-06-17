package com.javis.os.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.javis.os.data.db.dao.AppInfoDao
import com.javis.os.data.db.dao.ConversationDao
import com.javis.os.data.db.dao.MemoryDao
import com.javis.os.data.db.entities.AppInfoEntity
import com.javis.os.data.db.entities.ConversationEntity
import com.javis.os.data.db.entities.MemoryEntity

@Database(
    entities = [ConversationEntity::class, MemoryEntity::class, AppInfoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun appInfoDao(): AppInfoDao

    companion object {
        const val DATABASE_NAME = "javis_db"
    }
}
