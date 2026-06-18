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

    // ── Query ─────────────────────────────────────────────────────────────────

    suspend fun handleMemoryQuery(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("my name is") -> {
                val name = extractName(input, "my name is")
                if (name.isNotBlank()) {
                    memoryRepository.remember("user_name", name, "user_info", 5)
                    prefs.setUserName(name)
                    "Got it — I'll call you $name."
                } else "What name should I use?"
            }
            lower.contains("call me ") -> {
                val name = input.substringAfter("call me ").trim().split(" ").first()
                if (name.isNotBlank()) {
                    prefs.setUserName(name)
                    memoryRepository.remember("user_name", name, "user_info", 5)
                    "Done — you're $name to me."
                } else "What would you like to be called?"
            }
            lower.hasAny("who am i", "my name", "what's my name") -> {
                val name = prefs.getUserName()
                if (name.isNotBlank()) "Your name is $name."
                else "I don't know your name yet. Tell me by saying 'my name is…'"
            }
            lower.hasAny("what do you know about me", "what do you remember", "my info") -> {
                val name = prefs.getUserName()
                val summary = memoryRepository.getContextSummary()
                buildString {
                    if (name.isNotBlank()) appendLine("Name: $name")
                    if (summary.isNotBlank()) appendLine(summary)
                    if (isEmpty()) appendLine("I don't have much stored yet. Tell me things and I'll remember them.")
                }.trim()
            }
            lower.hasAny("forget everything", "delete my memory", "clear memory") -> {
                memoryRepository.clearAll()
                prefs.setUserName("")
                "Done. I've cleared everything I knew about you."
            }
            lower.hasAny("forget that", "don't remember that") ->
                "Understood. I'll try to disregard that."
            lower.contains("remember that") -> storeUserFact(input)
            else -> {
                val summary = memoryRepository.getContextSummary()
                if (summary.isNotBlank()) "Here's what I remember:\n$summary"
                else "Nothing stored yet. Tell me things and I'll keep track."
            }
        }
    }

    // ── Store ─────────────────────────────────────────────────────────────────

    /**
     * Explicit "remember that X" command from AgentRouter.
     */
    suspend fun storeUserFact(input: String): String {
        val lower = input.lowercase()
        val fact = when {
            lower.contains("remember that") -> input.substringAfter("remember that").trim()
            lower.contains("note that") -> input.substringAfter("note that").trim()
            lower.contains("save that") -> input.substringAfter("save that").trim()
            lower.contains("keep in mind") -> input.substringAfter("keep in mind").trim()
            else -> input.trim()
        }
        if (fact.isBlank()) return "What should I remember?"
        memoryRepository.remember(
            key = "fact_${System.currentTimeMillis()}",
            value = fact,
            category = "user_note",
            importance = 3
        )
        return "Remembered: \"$fact\"."
    }

    // ── Passive learning ──────────────────────────────────────────────────────

    suspend fun learnFromConversation(userInput: String, assistantReply: String) {
        val lower = userInput.lowercase()

        // Name extraction
        if (lower.contains("my name is") || lower.contains("i am called") || lower.contains("call me ")) {
            val name = when {
                lower.contains("my name is") -> extractName(userInput, "my name is")
                lower.contains("i am called") -> extractName(userInput, "i am called")
                lower.contains("call me ") -> userInput.substringAfter("call me ").trim().split(" ").first()
                else -> ""
            }
            if (name.isNotBlank()) {
                memoryRepository.remember("user_name", name, "user_info", 5)
                prefs.setUserName(name)
            }
        }

        // Preferences
        if (lower.contains("i like ") || lower.contains("i love ")) {
            val pref = userInput
                .substringAfterLast("i like ", "")
                .substringAfterLast("i love ", "")
                .trim().take(80)
            if (pref.isNotBlank()) {
                memoryRepository.remember("likes_${pref.take(20)}", pref, "preference", 2)
            }
        }
        if (lower.contains("i hate ") || lower.contains("i don't like ")) {
            val thing = userInput
                .substringAfterLast("i hate ", "")
                .substringAfterLast("i don't like ", "")
                .trim().take(80)
            if (thing.isNotBlank()) {
                memoryRepository.remember("dislikes_${thing.take(20)}", thing, "preference", 2)
            }
        }

        // Location
        if (lower.contains("i live in") || lower.contains("i'm from") || lower.contains("i am from")) {
            val location = userInput
                .substringAfterLast("i live in", "")
                .substringAfterLast("i'm from", "")
                .substringAfterLast("i am from", "")
                .trim().split(".").first().take(60)
            if (location.isNotBlank()) {
                memoryRepository.remember("location", location, "user_info", 3)
            }
        }

        // Job / occupation
        if (lower.contains("i work as") || lower.contains("i am a ") || lower.contains("my job is")) {
            val job = userInput
                .substringAfterLast("i work as", "")
                .substringAfterLast("i am a ", "")
                .substringAfterLast("my job is", "")
                .trim().split(" ").take(3).joinToString(" ").take(40)
            if (job.isNotBlank()) {
                memoryRepository.remember("occupation", job, "user_info", 3)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun extractName(input: String, after: String): String =
        input.substringAfter(after, "").trim()
            .split(" ").firstOrNull { it.isNotBlank() }
            ?.filter { it.isLetter() }
            ?.replaceFirstChar { it.uppercase() } ?: ""

    private fun String.hasAny(vararg kw: String) = kw.any { this.contains(it) }
}
