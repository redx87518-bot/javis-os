package com.javis.os.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val capabilities: String = "",  // JSON array of capability strings
    val launchCategory: String = "",
    val lastUsed: Long = 0L
)
