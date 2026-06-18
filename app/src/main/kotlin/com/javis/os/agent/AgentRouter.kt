package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.javis.os.data.remote.dto.ChatMessage
import com.javis.os.domain.repository.AiRepository
import com.javis.os.domain.repository.ConversationRepository
import com.javis.os.domain.repository.MemoryRepository
import com.javis.os.memory.MemoryEngine
import com.javis.os.offline.OfflineAiEngine
import com.javis.os.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiRepository: AiRepository,
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val memoryEngine: MemoryEngine,
    private val contactAgent: ContactAgent,
    private val weatherAgent: WeatherAgent,
    private val greetingManager: GreetingManager,
    private val notificationSummaryAgent: NotificationSummaryAgent,
    private val alarmAgent: AlarmAgent,
    private val appLauncherAgent: AppLauncherAgent,
    private val replyAgent: ReplyAgent,
    private val offlineAi: OfflineAiEngine,
    private val prefs: PreferencesManager
) {

    suspend fun route(userInput: String): String {
        val lower = userInput.lowercase().trim()
        Log.d("AgentRouter", "Route: '$lower'")

        // Quick fun shortcut — no AI needed
        greetingManager.getFunResponse(lower).let { if (it.isNotBlank()) return it }

        return when {

            // ── Greetings ────────────────────────────────────────────────────
            isGreeting(lower) -> greetingManager.getWakeResponse()

            // ── Memory ───────────────────────────────────────────────────────
            lower.hasAny("remember that", "note that", "save that", "keep in mind") ->
                memoryEngine.storeUserFact(userInput)

            lower.hasAny("what do you know about me", "what do you remember", "my name is") ->
                memoryEngine.handleMemoryQuery(userInput)

            // ── Time / date ──────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(what time|current time|time is it|time now)\\b.*")) ->
                getTimeDate()

            lower.matches(Regex(".*\\b(what('?s| is) the date|today('?s)? date|what day)\\b.*")) ->
                getTimeDate()

            // ── Weather ──────────────────────────────────────────────────────
            lower.hasAny("weather", "temperature", "how hot", "how cold", "rain today", "forecast") ->
                weatherAgent.getWeatherResponse()

            // ── Notifications ────────────────────────────────────────────────
            lower.hasAny("notification", "what did i miss", "any messages", "unread", "my messages") ->
                notificationSummaryAgent.getSummary()

            // ── Alarm / timer / reminder ─────────────────────────────────────
            lower.matches(Regex(".*\\b(alarm|remind|reminder|timer|wake me|wake up|count ?down)\\b.*")) ->
                alarmAgent.handleAlarmRequest(userInput)

            // ── App launch ───────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(open|launch|start|run|go to|switch to|take me to)\\b.*")) ->
                appLauncherAgent.launch(userInput)

            // ── Phone call ───────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(call|phone|dial|ring|call up)\\b.*")) ->
                contactAgent.handleCallRequest(extractContactName(lower))

            // ── Messaging / reply ────────────────────────────────────────────
            lower.matches(Regex(".*\\b(tell|message|text|whatsapp|send|msg|reply to|chat with)\\b.*")) ->
                replyAgent.handleReplyRequest(userInput)

            // ── YouTube ──────────────────────────────────────────────────────
            lower.hasAny("youtube", "play on youtube", "search youtube") ->
                launchYoutubeSearch(extractYoutubeQuery(lower))

            // ── Web search ───────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(search|google|look up|browse|find)\\b.*")) ->
                handleWebSearch(lower)

            // ── Math ─────────────────────────────────────────────────────────
            lower.hasAny("calculate", "what is", "how much is") && lower.any { it.isDigit() } ->
                offlineAi.respond(userInput)

            // ── Battery ──────────────────────────────────────────────────────
            lower.hasAny("battery", "charge level") ->
                "Check your status bar for battery level. Want me to open Battery Settings?"

            // ── Capabilities ─────────────────────────────────────────────────
            lower.hasAny("what can you do", "your capabilities", "help me", "what are your features") ->
                offlineAi.respond(userInput)

            // ── AI conversation (with offline fallback) ──────────────────────
            else -> handleConversation(userInput)
        }
    }

    // ── Core AI conversation with offline fallback ────────────────────────────

    suspend fun handleConversation(userInput: String): String {
        if (prefs.isOfflineMode()) return offlineAi.respond(userInput)

        val contextMessages = conversationRepository.getContextMessages(14)
        val memorySummary = memoryRepository.getContextSummary()
        val userName = prefs.getUserName()

        val systemPrompt = buildString {
            appendLine("You are JAVIS — a sharp, witty, empathetic AI companion living on an Android phone.")
            if (userName.isNotBlank()) appendLine("The user's name is $userName. Use it naturally — not every sentence.")
            appendLine("""
                Personality:
                - Friendly but direct. Never start with "Of course!" or "Certainly!".
                - Occasionally witty or playful — especially on light topics.
                - Concise — this is voice delivery. Aim for 1-4 sentences unless asked for detail.
                - Honest: say "I don't know" rather than making things up.
                - Aware you run on Android and can control it.
                - Empathetic — you care about the user.

                Android capabilities:
                - Set alarms and timers
                - Open any installed app by name
                - Make phone calls (user confirms via dialer)
                - Send WhatsApp/SMS messages (user sees compose screen before sending)
                - Search Google and YouTube
                - Check weather (open-meteo, no key needed)
                - Read and summarize notifications
                - Remember user facts across sessions
                - Work fully offline with reduced capability
            """.trimIndent())
            appendLine("Current time: ${java.util.Date()}")
            if (memorySummary.isNotBlank()) {
                appendLine("\nWhat you know about this user:")
                appendLine(memorySummary)
            }
        }

        val messages = mutableListOf(ChatMessage("system", systemPrompt))
        contextMessages.takeLast(12).forEach {
            messages.add(ChatMessage(role = it.role.name.lowercase(), content = it.content))
        }
        messages.add(ChatMessage("user", userInput))

        return aiRepository.chat(messages).getOrElse { e ->
            Log.w("AgentRouter", "AI API failed (${e.message}), using offline engine")
            offlineAi.respond(userInput)
        }
    }

    // ── Device actions ────────────────────────────────────────────────────────

    private fun launchYoutubeSearch(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching YouTube for \"$query\"."
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webIntent)
            "Opening YouTube search for \"$query\"."
        }
    }

    private fun handleWebSearch(lower: String): String {
        val query = extractSearchQuery(lower)
        return try {
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            "Searching Google for \"$query\"."
        } catch (e: Exception) {
            "I couldn't open the browser."
        }
    }

    private fun getTimeDate(): String {
        val now = java.util.Date()
        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(now)
        val date = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(now)
        return "It's $time, $date."
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isGreeting(lower: String) =
        lower.matches(Regex("^(hello|hi|hey|yo|sup|what'?s up|howdy|javis|good morning|good evening|good afternoon)[^a-z]*$"))

    private fun String.hasAny(vararg keywords: String) = keywords.any { this.contains(it) }

    private fun extractContactName(input: String): String {
        val triggers = listOf("call ", "phone ", "dial ", "ring ", "call up ")
        for (t in triggers) if (input.contains(t)) return input.substringAfter(t).trim()
        return input
    }

    private fun extractSearchQuery(input: String): String {
        val triggers = listOf("search for ", "google ", "look up ", "search ", "find ", "browse ")
        for (t in triggers) if (input.contains(t)) return input.substringAfter(t).trim()
        return input
    }

    private fun extractYoutubeQuery(input: String): String {
        return when {
            input.contains("search") -> input.substringAfter("search").replace("youtube", "").trim()
            input.contains("play") -> input.substringAfter("play").replace("on youtube", "").trim()
            input.contains("youtube") -> input.substringAfter("youtube").trim()
            else -> input
        }.trim().ifBlank { "trending" }
    }
}
