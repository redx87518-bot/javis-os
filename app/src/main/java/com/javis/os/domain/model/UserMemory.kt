package com.javis.os.domain.model

data class UserMemory(
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general",
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_NAME = "identity"
        const val CATEGORY_HABITS = "habits"
        const val CATEGORY_CONTACTS = "contacts"
        const val CATEGORY_PREFERENCES = "preferences"
        const val CATEGORY_ROUTINES = "routines"
    }
}
