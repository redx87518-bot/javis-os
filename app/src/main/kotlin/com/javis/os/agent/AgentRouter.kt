package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.util.Log
import com.javis.os.data.local.dao.AppDao
import com.javis.os.domain.repository.AiRepository
import com.javis.os.domain.repository.ConversationRepository
import com.javis.os.domain.repository.MemoryRepository
import com.javis.os.memory.MemoryEngine
import com.javis.os.planner.TaskPlanner
import com.javis.os.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AgentType {
    CONVERSATION, APP, CONTACT, SEARCH, ALARM, MEMORY, NOTIFICATION
}

data class AgentTask(
    val type: AgentType,
    val intent: String,
    val params: Map<String, String> = emptyMap()
)

@Singleton
class AgentRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiRepository: AiRepository,
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val memoryEngine: MemoryEngine,
    private val taskPlanner: TaskPlanner,
    private val prefs: PreferencesManager,
    private val appDao: AppDao
) {

    suspend fun route(userInput: String): String {
        val intent = classifyIntent(userInput)
        Log.d("AgentRouter", "Intent classified: ${intent.type} | $userInput")

        return when (intent.type) {
            AgentType.APP -> handleAppTask(intent, userInput)
            AgentType.CONTACT -> handleContactTask(intent, userInput)
            AgentType.SEARCH -> handleSearchTask(intent, userInput)
            AgentType.ALARM -> handleAlarmTask(intent, userInput)
            AgentType.MEMORY -> handleMemoryTask(intent, userInput)
            AgentType.NOTIFICATION -> handleNotificationTask(intent, userInput)
            AgentType.CONVERSATION -> handleConversation(userInput)
        }
    }

    private suspend fun classifyIntent(input: String): AgentTask {
        val lower = input.lowercase()
        return when {
            // App launch patterns
            lower.contains("open ") || lower.contains("launch ") || lower.contains("start ") ->
                AgentTask(AgentType.APP, "open_app", mapOf("query" to extractAppName(lower)))

            // Contact/call patterns
            lower.matches(Regex(".*\\b(call|phone|dial|ring)\\b.*")) ->
                AgentTask(AgentType.CONTACT, "call", mapOf("name" to extractContactName(lower)))

            // Messaging patterns
            lower.matches(Regex(".*\\b(tell|message|text|send|whatsapp)\\b.*")) ->
                AgentTask(AgentType.CONTACT, "message", mapOf("raw" to input))

            // Search patterns
            lower.matches(Regex(".*\\b(search|find|look up|google)\\b.*")) ->
                AgentTask(AgentType.SEARCH, "web_search", mapOf("query" to extractSearchQuery(lower)))

            // YouTube
            lower.contains("youtube") ->
                AgentTask(AgentType.APP, "youtube_search", mapOf("query" to extractYoutubeQuery(lower)))

            // Alarm patterns
            lower.matches(Regex(".*\\b(alarm|remind|reminder|timer|wake)\\b.*")) ->
                AgentTask(AgentType.ALARM, "set_alarm", mapOf("raw" to input))

            // Memory patterns
            lower.matches(Regex(".*\\b(remember|forget|my name|who am i)\\b.*")) ->
                AgentTask(AgentType.MEMORY, "memory_op", mapOf("raw" to input))

            // Default: conversation
            else -> AgentTask(AgentType.CONVERSATION, "chat")
        }
    }

    private suspend fun handleAppTask(task: AgentTask, original: String): String {
        val appQuery = task.params["query"] ?: return handleConversation(original)

        // Special case: YouTube search
        if (task.intent == "youtube_search") {
            val searchQuery = task.params["query"] ?: ""
            return launchYoutubeSearch(searchQuery)
        }

        // Look up app in database
        val matchedApps = appDao.searchApps(appQuery)
        if (matchedApps.isEmpty()) {
            // Try fallback via AI
            return handleConversation(original)
        }

        val app = matchedApps.first()
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                "Opening ${app.appName} for you."
            } else {
                "I found ${app.appName} but couldn't open it right now."
            }
        } catch (e: Exception) {
            "I had trouble opening ${app.appName}: ${e.message}"
        }
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
            // Fallback to web
            val webIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webIntent)
            "Opening YouTube search for \"$query\"."
        }
    }

    private suspend fun handleContactTask(task: AgentTask, original: String): String {
        return if (task.intent == "call") {
            val name = task.params["name"] ?: return handleConversation(original)
            initiateCall(name)
        } else {
            // Message intent — let AI handle drafting
            handleConversation(original)
        }
    }

    private fun initiateCall(name: String): String {
        return try {
            val uri = Uri.encode(name)
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:")
                putExtra(ContactsContract.Intents.Insert.NAME, name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Use ACTION_DIAL (not CALL) to respect user confirmation
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            "Opening the dialer to call $name. Please confirm the call."
        } catch (e: Exception) {
            "I couldn't initiate the call: ${e.message}"
        }
    }

    private suspend fun handleSearchTask(task: AgentTask, original: String): String {
        val query = task.params["query"] ?: return handleConversation(original)
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
            "Opening search results for \"$query\"."
        }
    }

    private suspend fun handleAlarmTask(task: AgentTask, original: String): String {
        // Delegate to AI to parse the time then schedule
        return taskPlanner.planAlarm(original)
    }

    private suspend fun handleMemoryTask(task: AgentTask, original: String): String {
        return memoryEngine.handleMemoryQuery(original)
    }

    private suspend fun handleNotificationTask(task: AgentTask, original: String): String {
        return handleConversation(original)
    }

    private suspend fun handleConversation(userInput: String): String {
        val contextMessages = conversationRepository.getContextMessages(12)
        val memorySummary = memoryRepository.getContextSummary()
        val userName = prefs.getUserName()

        val systemPrompt = buildString {
            appendLine("You are JAVIS, an intelligent and friendly AI companion running on Android.")
            if (userName.isNotBlank()) appendLine("The user's name is $userName.")
            appendLine("Be natural, witty, and helpful. Keep responses concise for voice delivery (2-4 sentences unless more is needed).")
            appendLine("You have access to Android features: calls, apps, alarms, search, and more.")
            appendLine("Current time: ${java.util.Date()}")
            if (memorySummary.isNotBlank()) {
                appendLine("\nWhat you know about the user:")
                appendLine(memorySummary)
            }
        }

        val messages = mutableListOf(
            com.javis.os.data.remote.dto.ChatMessage("system", systemPrompt)
        )
        contextMessages.takeLast(10).forEach {
            messages.add(
                com.javis.os.data.remote.dto.ChatMessage(
                    role = it.role.name.lowercase(),
                    content = it.content
                )
            )
        }
        messages.add(com.javis.os.data.remote.dto.ChatMessage("user", userInput))

        return aiRepository.chat(messages).getOrElse { e ->
            // Offline fallback
            handleOfflineFallback(userInput)
        }
    }

    private fun handleOfflineFallback(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") -> "Hello! I'm JAVIS, running in offline mode. Some features are limited without internet."
            lower.contains("time") -> "The current time is ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}."
            lower.contains("date") -> "Today is ${java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}."
            lower.contains("joke") -> "I'd tell you a joke, but I'm offline and my humor needs an internet connection. Try again when you're connected!"
            else -> "I'm currently in offline mode. I can still help with calls, alarms, and local tasks. Connect to the internet for full AI capabilities."
        }
    }

    // Helpers
    private fun extractAppName(input: String): String {
        val patterns = listOf("open ", "launch ", "start ")
        for (p in patterns) {
            if (input.contains(p)) {
                return input.substringAfter(p).trim().split(" ").first()
            }
        }
        return input
    }

    private fun extractContactName(input: String): String {
        val triggers = listOf("call ", "phone ", "dial ", "ring ")
        for (t in triggers) {
            if (input.contains(t)) return input.substringAfter(t).trim()
        }
        return input
    }

    private fun extractSearchQuery(input: String): String {
        val triggers = listOf("search for ", "search ", "google ", "find ", "look up ")
        for (t in triggers) {
            if (input.contains(t)) return input.substringAfter(t).trim()
        }
        return input
    }

    private fun extractYoutubeQuery(input: String): String {
        return when {
            input.contains("search") -> input.substringAfter("search").replace("youtube", "").trim()
            input.contains("youtube") -> input.substringAfter("youtube").trim()
            else -> input
        }
    }
}
