package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.javis.os.data.local.dao.AppDao
import com.javis.os.data.remote.dto.ChatMessage
import com.javis.os.domain.repository.AiRepository
import com.javis.os.domain.repository.ConversationRepository
import com.javis.os.domain.repository.MemoryRepository
import com.javis.os.memory.MemoryEngine
import com.javis.os.planner.TaskPlanner
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
    private val taskPlanner: TaskPlanner,
    private val contactAgent: ContactAgent,
    private val weatherAgent: WeatherAgent,
    private val greetingManager: GreetingManager,
    private val notificationSummaryAgent: NotificationSummaryAgent,
    private val prefs: PreferencesManager,
    private val appDao: AppDao
) {

    suspend fun route(userInput: String): String {
        val lower = userInput.lowercase().trim()
        Log.d("AgentRouter", "Routing: $lower")

        // Quick personality shortcuts — no AI call needed
        greetingManager.getFunResponse(lower).let { if (it.isNotBlank()) return it }

        return when {
            // ── Greetings ────────────────────────────────────────────────────
            isGreeting(lower) -> greetingManager.getWakeResponse()

            // ── Weather ──────────────────────────────────────────────────────
            lower.hasAny("weather", "temperature", "hot outside", "cold outside", "rain today", "forecast") ->
                weatherAgent.getWeatherResponse()

            // ── Notifications ────────────────────────────────────────────────
            lower.hasAny("notification", "messages", "what did i miss", "any messages", "unread") ->
                notificationSummaryAgent.getSummary()

            // ── Call ─────────────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(call|phone|dial|ring|call up)\\b.*")) ->
                contactAgent.handleCallRequest(extractContactName(lower))

            // ── Messaging ────────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(tell|message|text|whatsapp|send|msg)\\b.*")) ->
                handleMessagingIntent(userInput, lower)

            // ── YouTube ──────────────────────────────────────────────────────
            lower.contains("youtube") ->
                launchYoutubeSearch(extractYoutubeQuery(lower))

            // ── App launch ───────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(open|launch|start|run|go to)\\b.*")) ->
                handleAppLaunch(lower, userInput)

            // ── Web search ───────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(search|google|look up|find|browse)\\b.*")) ->
                handleWebSearch(lower)

            // ── Alarm / reminder ─────────────────────────────────────────────
            lower.matches(Regex(".*\\b(alarm|remind|reminder|timer|wake me|wake up)\\b.*")) ->
                taskPlanner.planAlarm(userInput)

            // ── Memory queries ───────────────────────────────────────────────
            lower.matches(Regex(".*\\b(remember|forget|my name|who am i|what do you know)\\b.*")) ->
                memoryEngine.handleMemoryQuery(userInput)

            // ── Time / date ──────────────────────────────────────────────────
            lower.matches(Regex(".*\\b(time|what time|date|today|day is it)\\b.*")) ->
                getTimeDate()

            // ── Battery / system info ────────────────────────────────────────
            lower.hasAny("battery", "charge", "charging") ->
                "I can't read battery level directly, but check your status bar. Want me to open battery settings?"

            // ── Default: AI conversation ─────────────────────────────────────
            else -> handleConversation(userInput)
        }
    }

    // ── Intent handlers ──────────────────────────────────────────────────────

    private suspend fun handleMessagingIntent(original: String, lower: String): String {
        // Pattern: "tell [name] [message]"
        val toPatterns = listOf("tell ", "message ", "text ", "whatsapp ", "send to ", "msg ")
        for (p in toPatterns) {
            if (lower.contains(p)) {
                val after = lower.substringAfter(p).trim()
                // Try to split name from message
                val words = after.split(" ")
                if (words.size >= 2) {
                    val possibleName = words.first()
                    val possibleMessage = words.drop(1).joinToString(" ")
                    // Check if "that" or "saying" is in there
                    val cleanMsg = possibleMessage
                        .removePrefix("that ")
                        .removePrefix("saying ")
                        .removePrefix("to say ")
                        .trim()
                    return contactAgent.openMessageCompose(possibleName, cleanMsg.ifBlank { original })
                }
            }
        }
        return handleConversation(original)
    }

    private suspend fun handleAppLaunch(lower: String, original: String): String {
        val appName = extractAppName(lower)
        if (appName.isBlank()) return handleConversation(original)

        // Check DB first
        val matched = appDao.searchApps(appName)
        if (matched.isNotEmpty()) {
            val app = matched.first()
            return try {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?: return "I found ${app.appName} but couldn't open it — it may not have a launcher."
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening ${app.appName}."
            } catch (e: Exception) {
                "I had trouble opening ${app.appName}."
            }
        }
        // Fallback: AI handles
        return handleConversation(original)
    }

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
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching for \"$query\"."
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webIntent)
            "Searching Google for \"$query\"."
        }
    }

    private fun getTimeDate(): String {
        val now = java.util.Date()
        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(now)
        val date = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(now)
        return "It's $time on $date."
    }

    // ── Core AI conversation ──────────────────────────────────────────────────

    suspend fun handleConversation(userInput: String): String {
        if (prefs.isOfflineMode()) return handleOfflineFallback(userInput)

        val contextMessages = conversationRepository.getContextMessages(14)
        val memorySummary = memoryRepository.getContextSummary()
        val userName = prefs.getUserName()

        val systemPrompt = buildString {
            appendLine("You are JAVIS — a sharp, witty, intelligent AI companion running on Android.")
            if (userName.isNotBlank()) appendLine("The user's name is $userName. Use it naturally, not in every sentence.")
            appendLine("""
                Personality:
                - Friendly but not sycophantic. Don't start every reply with "Of course!" or "Sure!"
                - Occasionally funny and playful, especially on light topics
                - Direct and concise — voice delivery means 1-4 sentences unless the user asks for detail
                - Honest: if you don't know something, say so clearly
                - You care about the user and remember context across the conversation
                - You're aware you run on an Android phone and can control it
                
                Android capabilities you have:
                - Make calls (via dialer, user confirms)
                - Open apps by name
                - Search YouTube, Google, the web
                - Set alarms and reminders
                - Read notifications
                - Send messages (user always confirms before sending)
                - Get weather (open-meteo, no API key)
                - Remember things the user tells you
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
            Log.e("AgentRouter", "AI error: ${e.message}")
            handleOfflineFallback(userInput)
        }
    }

    private fun handleOfflineFallback(input: String): String {
        val lower = input.lowercase()
        return when {
            isGreeting(lower) -> greetingManager.getWakeResponse()
            lower.hasAny("time", "what time") -> getTimeDate()
            lower.hasAny("date", "today", "day") -> getTimeDate()
            lower.hasAny("joke") -> greetingManager.getFunResponse("joke")
            else -> "I'm in offline mode right now. I can still help with calls, alarms, and local tasks. Connect to the internet for full AI conversation."
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isGreeting(lower: String) =
        lower.matches(Regex("^(hello|hi|hey|yo|sup|what's up|howdy|javis)[^a-z]*$"))

    private fun String.hasAny(vararg keywords: String) = keywords.any { this.contains(it) }

    private fun extractAppName(input: String): String {
        val triggers = listOf("open ", "launch ", "start ", "run ", "go to ")
        for (t in triggers) {
            if (input.contains(t)) {
                val after = input.substringAfter(t).trim()
                return after.split(" ").take(2).joinToString(" ")
            }
        }
        return input
    }

    private fun extractContactName(input: String): String {
        val triggers = listOf("call ", "phone ", "dial ", "ring ", "call up ")
        for (t in triggers) {
            if (input.contains(t)) return input.substringAfter(t).trim()
        }
        return input
    }

    private fun extractSearchQuery(input: String): String {
        val triggers = listOf("search for ", "google ", "look up ", "search ", "find ", "browse ")
        for (t in triggers) {
            if (input.contains(t)) return input.substringAfter(t).trim()
        }
        return input
    }

    private fun extractYoutubeQuery(input: String): String {
        return when {
            input.contains("search") -> input.substringAfter("search").replace("youtube", "").trim()
            input.contains("play") -> input.substringAfter("play").trim()
            input.contains("youtube") -> input.substringAfter("youtube").trim()
            else -> input
        }.trim().ifBlank { "trending" }
    }
}
