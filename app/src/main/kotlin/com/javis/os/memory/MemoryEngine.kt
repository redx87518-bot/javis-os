package com.javis.os.memory

import com.javis.os.domain.repository.MemoryRepository
import com.javis.os.util.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryEngine @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val prefs: PreferencesManager
) {

    suspend fun handleMemoryQuery(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("my name is") -> {
                val name = input.substringAfter("my name is").trim().split(" ").first()
                memoryRepository.remember("user_name", name, "user_info", 5)
                prefs.setUserName(name)
                "Got it! I'll remember your name is $name."
            }
            lower.contains("who am i") || lower.contains("my name") -> {
                val name = prefs.getUserName()
                if (name.isNotBlank()) "Your name is $name." else "I don't know your name yet. Tell me!"
            }
            lower.contains("remember that") -> {
                val fact = input.substringAfter("remember that").trim()
                memoryRepository.remember("fact_${System.currentTimeMillis()}", fact, "preference", 3)
                "Noted! I'll remember that."
            }
            lower.contains("forget") -> {
                "I've noted your request to forget that. Memory management coming soon."
            }
            else -> {
                val summary = memoryRepository.getContextSummary()
                if (summary.isNotBlank()) "Here's what I remember about you:\n$summary"
                else "I don't have any specific memories stored yet. Tell me things and I'll remember them!"
            }
        }
    }

    suspend fun learnFromConversation(userInput: String, assistantReply: String) {
        val lower = userInput.lowercase()
        // Extract name
        if (lower.contains("my name is")) {
            val name = userInput.substringAfter("my name is").trim().split(" ").firstOrNull() ?: return
            memoryRepository.remember("user_name", name, "user_info", 5)
            prefs.setUserName(name)
        }
        // Extract preferences
        if (lower.contains("i like") || lower.contains("i love")) {
            val topic = userInput.substringAfterLast("i like").substringAfterLast("i love").trim()
            if (topic.isNotBlank()) {
                memoryRepository.remember("likes_${topic.take(20)}", topic, "preference", 2)
            }
        }
        // Extract location info
        if (lower.contains("i live in") || lower.contains("i'm from")) {
            val location = userInput.substringAfterLast("i live in").substringAfterLast("i'm from").trim()
            if (location.isNotBlank()) {
                memoryRepository.remember("location", location, "user_info", 3)
            }
        }
    }
}
