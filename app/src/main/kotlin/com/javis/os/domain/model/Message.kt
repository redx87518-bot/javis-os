package com.javis.os.domain.model

data class Message(
    val id: Long = 0,
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
}

data class Memory(
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String,
    val importance: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val capabilities: List<String> = emptyList()
)

data class JavisAlarm(
    val id: Long = 0,
    val label: String,
    val triggerTimeMillis: Long,
    val isReminder: Boolean = false
)
