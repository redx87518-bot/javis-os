package com.javis.os.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.javis.os.data.local.dao.*
import com.javis.os.data.local.entities.*

@Database(
    entities = [
        ConversationEntity::class,
        MemoryEntity::class,
        AppEntity::class,
        AlarmEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JavisDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun appDao(): AppDao
    abstract fun alarmDao(): AlarmDao
}
