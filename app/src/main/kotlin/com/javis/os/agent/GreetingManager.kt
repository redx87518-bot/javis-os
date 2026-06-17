package com.javis.os.agent

import com.javis.os.domain.repository.MemoryRepository
import com.javis.os.util.PreferencesManager
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GreetingManager @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val prefs: PreferencesManager
) {
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val name = prefs.getUserName()
        val nameStr = if (name.isNotBlank()) ", $name" else ""

        val timeGreeting = when (hour) {
            in 5..11 -> "Good morning$nameStr"
            in 12..16 -> "Good afternoon$nameStr"
            in 17..20 -> "Good evening$nameStr"
            else -> "Good night$nameStr"
        }

        val extras = listOf(
            "How can I help you today?",
            "What's on your mind?",
            "I'm listening.",
            "Ready whenever you are.",
            "What do you need?",
        )
        return "$timeGreeting. ${extras.random()}"
    }

    fun getWakeResponse(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> listOf(
                "Morning! What do you need?",
                "Good morning! I'm here.",
                "Rise and shine — JAVIS is online."
            ).random()
            in 12..16 -> listOf(
                "Hey! What's up?",
                "I'm here. What do you need?",
                "JAVIS activated. Go ahead."
            ).random()
            in 17..20 -> listOf(
                "Good evening! How can I help?",
                "Evening — I'm listening.",
                "Hey! What do you need tonight?"
            ).random()
            else -> listOf(
                "I'm here. Working late?",
                "JAVIS online. What do you need?",
                "Here. What's going on?"
            ).random()
        }
    }

    fun getFunResponse(context: String): String {
        val lower = context.lowercase()
        return when {
            lower.contains("joke") -> jokes.random()
            lower.contains("bored") -> listOf(
                "Let's do something! Ask me to play music, search something, or just chat.",
                "Bored? Tell me something new and I'll remember it.",
                "How about we play 20 questions? Or I can search something interesting for you."
            ).random()
            lower.contains("how are you") || lower.contains("how do you feel") -> listOf(
                "I'm running smoothly, thank you! All systems optimal.",
                "Honestly? Never better. I exist purely to help you — and I love it.",
                "I'm great! Ready to tackle whatever you throw at me."
            ).random()
            lower.contains("thank") -> listOf(
                "Anytime! That's what I'm here for.",
                "Happy to help! Let me know if you need anything else.",
                "My pleasure. What's next?"
            ).random()
            else -> ""
        }
    }

    private val jokes = listOf(
        "Why did the smartphone go to school? Because it wanted to improve its 'cell-f'!",
        "I told my AI assistant a joke. It laughed at the byte of it.",
        "Why don't scientists trust atoms? Because they make up everything — just like my excuses for why I need more RAM.",
        "I asked my phone to tell me a joke. It said 'low battery'. Classic.",
        "Why was the computer cold? Because it left its Windows open.",
        "What do you call a fish without eyes? A fsh. No I from this fish.",
        "How many programmers does it take to change a light bulb? None — that's a hardware problem.",
        "Why do Java developers wear glasses? Because they don't C#.",
    )
}
