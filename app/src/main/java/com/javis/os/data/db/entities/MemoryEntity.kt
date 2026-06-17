package com.javis.os.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String,
    val confidence: Float = 1.0f,
    val updatedAt: Long = System.currentTimeMillis()
)
