package com.javis.os.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val categories: String = "",
    val capabilities: String = "",
    val lastUsed: Long = 0L
)
