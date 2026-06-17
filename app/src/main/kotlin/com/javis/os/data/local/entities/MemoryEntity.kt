package com.javis.os.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String,   // "user_info" | "preference" | "habit" | "contact" | "routine"
    val timestamp: Long = System.currentTimeMillis(),
    val importance: Int = 1 // 1-5
)
