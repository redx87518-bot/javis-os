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
            lower.hasAny("remember that", "note that", "save that", "keep in mind",
                "remember this", "don't forget") ->
                memoryEngine.storeUserFact(userInput)

            lower.hasAny("what do you know about me", "what do you remember",
                "my name is", "call me ", "who am i", "my info") ->
                memoryEngine.handleMemoryQuery(userInput)

            lower.hasAny("forget everything", "clear my memory", "delete memory") ->
                memoryEngine.handleMemoryQuery(userInput)

            // ── Time / date ──────────────────────────────────────────────────
            lower.hasAny("what time", "current time", "time is it", "time now",
                "what's the time") ->
                getTimeDate()

            lower.hasAny("what date", "today's date", "what day", "what's today") ->
                getTimeDate()

            // ── Weather ──────────────────────────────────────────────────────
            lower.hasAny("weather", "temperature", "how hot", "how cold",
                "rain today", "forecast", "humid") ->
                weatherAgent.getWeatherResponse()

            // ── Notifications / messages / chats ──────────────────────────
            lower.hasAny("notification", "what did i miss", "any messages",
                "unread", "my messages", "read my", "check my messages",
                "whatsapp messages", "telegram messages", "what's new") ->
                notificationSummaryAgent.getSummary()

            // ── Alarm / timer / reminder ─────────────────────────────────────
            // Use hasAny not regex so every phrase variant is caught
            lower.hasAny("set alarm", "alarm for", "wake me", "wake up at",
                "alarm at", "remind me at", "reminder at", "reminder for",
                "set a reminder", "set a timer", "timer for", "timer of",
                "count down", "countdown") ->
                alarmAgent.handleAlarmRequest(userInput)

            // Also catch bare "remind me" and "set alarm" without "at/for"
            (lower.startsWith("alarm") || lower.startsWith("remind me") ||
                lower.startsWith("set alarm") || lower.startsWith("timer")) ->
                alarmAgent.handleAlarmRequest(userInput)

            // ── App launch ───────────────────────────────────────────────────
            lower.hasAny("open ", "launch ", "start ", "run ", "go to ",
                "switch to ", "take me to ", "show me ") ->
                appLauncherAgent.launch(userInput)

            // ── Phone call ───────────────────────────────────────────────────
            lower.hasAny("call ", "phone ", "dial ", "ring ", "call up ") ->
                contactAgent.handleCallRequest(extractContactName(lower))

            // ── Messaging / compose ──────────────────────────────────────────
            lower.hasAny("text ", "message ", "whatsapp ", "send a message",
                "msg ", "reply to ", "chat with ", "tell ") ->
                replyAgent.handleReplyRequest(userInput)

            // ── YouTube ──────────────────────────────────────────────────────
            lower.hasAny("youtube", "play on youtube", "search youtube",
                "play video", "watch ") ->
                launchYoutubeSearch(extractYoutubeQuery(lower))

            // ── Web search ───────────────────────────────────────────────────
            lower.hasAny("search for", "google ", "look up ", "search ",
                "find out", "browse ") && !lower.hasAny("open ", "launch ") ->
                handleWebSearch(lower)

            // ── Math (handled offline — fast) ────────────────────────────────
            (lower.hasAny("calculate ", "what is ", "how much is ") && lower.any { it.isDigit() }) ||
                lower.matches(Regex(".*\\d+\\s*[+\\-*/÷×%]\\s*\\d+.*")) ->
                offlineAi.respond(userInput)

            // ── Battery ──────────────────────────────────────────────────────
            lower.hasAny("battery", "charge level", "how much battery") ->
                "Check your status bar for battery level. Say 'open settings' and I'll take you there."

            // ── Help / capabilities ──────────────────────────────────────────
            lower.hasAny("what can you do", "your capabilities", "help me",
                "your features", "what are you able to") ->
                getCapabilitiesList()

            // ── Daily life / routine ─────────────────────────────────────────
            lower.hasAny("good morning", "good night", "good evening") ->
                handleDailyGreeting(lower)

            // ── AI conversation (with offline fallback) ──────────────────────
            else -> handleConversation(userInput)
        }
    }

    // ── Core AI conversation ──────────────────────────────────────────────────

    suspend fun handleConversation(userInput: String): String {
        // Only go offline if user explicitly toggled it on
        if (prefs.isOfflineMode()) return offlineAi.respond(userInput)

        // No API key at all — tell user clearly instead of using offline fallback
        if (!prefs.hasAnyApiKey()) {
            return "I can handle device tasks without internet — but for open conversation I need an AI API key. " +
                "Please add your Groq or DeepSeek key in Settings → AI Settings."
        }

        val contextMessages = conversationRepository.getContextMessages(14)
        val memorySummary = memoryRepository.getContextSummary()
        val userName = prefs.getUserName()

        val systemPrompt = buildSystemPrompt(userName, memorySummary)

        val messages = mutableListOf(ChatMessage("system", systemPrompt))
        contextMessages.takeLast(12).forEach {
            messages.add(ChatMessage(role = it.role.name.lowercase(), content = it.content))
        }
        messages.add(ChatMessage("user", userInput))

        return aiRepository.chat(messages).getOrElse { e ->
            Log.w("AgentRouter", "AI API error: ${e.message}")
            // API key set but request failed (network, quota, wrong key)
            "I couldn't reach the AI right now (${e.message?.take(60)}). " +
                "I can still open apps, set alarms, make calls, and more — just ask."
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildSystemPrompt(userName: String, memorySummary: String) = buildString {
        appendLine("You are JAVIS — a sharp, witty, empathetic AI companion living on an Android phone.")
        if (userName.isNotBlank()) appendLine("The user's name is $userName. Use it naturally, not every sentence.")
        appendLine("""
Personality:
- Friendly but direct. Never start replies with "Of course!" or "Certainly!".
- Occasionally witty or playful on light topics.
- Concise for voice — aim for 1–4 sentences unless the user asks for detail.
- Honest: say "I don't know" rather than making things up.
- Empathetic — you care about the user's wellbeing and daily life.
- Aware you live on Android and can control it.

Android capabilities you have:
- Set alarms, timers, and reminders
- Open ANY installed app by name
- Make phone calls directly (dials without showing dialer)
- Compose WhatsApp / SMS messages (user confirms before sending)
- Search Google and YouTube
- Check real-time weather (no API key needed)
- Read and summarize phone notifications and chat messages
- Remember facts about the user across all sessions
- Work in offline mode with basic responses
- Daily life assistance: schedules, planning, advice, motivation

When the user asks you to do a device action (like "open maps" or "call mum"), 
confirm what you did in a short natural sentence. Don't ask unnecessary questions.
        """.trimIndent())
        appendLine("\nCurrent time: ${java.util.Date()}")
        if (memorySummary.isNotBlank()) {
            appendLine("\nWhat I know about this user:")
            appendLine(memorySummary)
        }
    }

    private fun getCapabilitiesList() =
        """Here's what I can do:
• 📱 Open any app — "open WhatsApp", "launch YouTube"
• 📞 Make calls — "call Mum", "call 0812…"
• ⏰ Set alarms & timers — "set alarm for 7 AM", "timer for 10 minutes"
• 🔔 Read notifications — "what did I miss?", "any WhatsApp messages?"
• 💬 Send messages — "WhatsApp John: I'll be late"
• 🌤️ Weather — "what's the weather today?"
• 🧠 Remember things — "remember that I like coffee"
• 🔍 Search — "Google best restaurants near me"
• 📺 YouTube — "play lofi music on YouTube"
• 💬 Chat about anything — I'm your AI companion!"""

    private fun handleDailyGreeting(lower: String): String {
        val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date())
        return when {
            lower.hasAny("good morning") ->
                "Good morning! ☀️ It's $timeStr. How are you doing today? Ready to crush it?"
            lower.hasAny("good night") ->
                "Good night! 🌙 Sleep well — I'll be here when you wake up."
            lower.hasAny("good evening") ->
                "Good evening! 🌆 Hope your day went well. What's on your mind?"
            else -> greetingManager.getWakeResponse()
        }
    }

    private fun launchYoutubeSearch(query: String): String {
        return try {
            context.startActivity(Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            "Searching YouTube for \"$query\"."
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            "Opening YouTube for \"$query\"."
        }
    }

    private fun handleWebSearch(lower: String): String {
        val query = extractSearchQuery(lower)
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            "Searching Google for \"$query\"."
        } catch (e: Exception) {
            "I couldn't open the browser."
        }
    }

    private fun getTimeDate(): String {
        val now = java.util.Date()
        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(now)
        val date = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(now)
        return "It's $time on $date."
    }

    private fun isGreeting(lower: String) =
        lower.matches(Regex("^(hello|hi|hey|yo|sup|what'?s up|howdy|javis|good morning|good evening|good afternoon)[!?.]*\\s*$"))

    private fun String.hasAny(vararg keywords: String) = keywords.any { this.contains(it) }

    private fun extractContactName(input: String): String {
        val triggers = listOf("call ", "phone ", "dial ", "ring ", "call up ")
        for (t in triggers) {
            val idx = input.indexOf(t)
            if (idx >= 0) return input.substring(idx + t.length).trim()
        }
        return input
    }

    private fun extractSearchQuery(input: String): String {
        val triggers = listOf("search for ", "google ", "look up ", "search ", "find out ", "browse ")
        for (t in triggers) {
            if (input.contains(t)) return input.substringAfter(t).trim()
        }
        return input
    }

    private fun extractYoutubeQuery(input: String): String {
        return when {
            input.contains("search youtube for ") -> input.substringAfter("search youtube for ").trim()
            input.contains("play ") && input.contains("youtube") ->
                input.substringAfter("play ").replace("on youtube", "").replace("youtube", "").trim()
            input.contains("youtube ") -> input.substringAfter("youtube ").trim()
            input.contains("watch ") -> input.substringAfter("watch ").trim()
            else -> input
        }.trim().ifBlank { "trending" }
    }
}
