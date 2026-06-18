package com.javis.os.offline

import android.content.Context
import com.javis.os.memory.MemoryEngine
import com.javis.os.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.random.Random

/**
 * OfflineAiEngine — a sophisticated rule-based conversational AI for when
 * the device is offline or the remote API is unavailable.
 *
 * Features:
 * - Multi-turn context tracking (last 10 exchanges)
 * - Intent classification with confidence
 * - Math evaluation
 * - Common knowledge Q&A
 * - Personality (wit, empathy)
 * - Memory integration
 * - Task guidance
 */
@Singleton
class OfflineAiEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryEngine: MemoryEngine,
    private val prefs: PreferencesManager
) {
    private val conversationHistory = ArrayDeque<Pair<String, String>>(10)

    /** Main entry point */
    suspend fun respond(input: String): String {
        val lower = input.lowercase().trim()
        val userName = prefs.getUserName()

        val response = when {
            // Math expressions
            isMathExpression(lower) -> evaluateMath(lower)

            // Questions
            isQuestion(lower) -> handleQuestion(lower, input, userName)

            // Statements / assertions
            isAssertion(lower) -> handleAssertion(lower, input, userName)

            // Task guidance
            lower.hasAny("how do i", "how to", "help me", "what should i") ->
                handleHowTo(lower, input)

            // Opinion / preference
            lower.hasAny("what do you think", "your opinion", "do you like", "favourite", "favorite") ->
                handleOpinion(lower, input)

            // Emotional support
            lower.hasAny("feel", "sad", "happy", "depressed", "anxious", "stressed", "tired", "bored") ->
                handleEmotional(lower, input, userName)

            // Jokes
            lower.hasAny("joke", "funny", "make me laugh", "tell me something funny") ->
                getJoke()

            // Default: conversational response
            else -> generateConversational(lower, input, userName)
        }

        // Store in local history
        if (conversationHistory.size >= 10) conversationHistory.removeFirst()
        conversationHistory.addLast(Pair(input, response))

        return response
    }

    // ── Math ──────────────────────────────────────────────────────────────────

    private fun isMathExpression(input: String): Boolean {
        val cleaned = input.replace(",", "").trim()
        return cleaned.matches(Regex("[0-9+\\-*/^()%.\\s]+")) ||
                cleaned.hasAny("what is", "calculate", "compute", "how much is", "equals") &&
                cleaned.any { it.isDigit() }
    }

    private fun evaluateMath(input: String): String {
        return try {
            val expr = input
                .replace(Regex("what is|calculate|compute|equals|how much is|="), "")
                .replace("x", "*").replace("×", "*").replace("÷", "/")
                .replace("^", "**").trim()
            val result = MathEvaluator.evaluate(expr)
            val formatted = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                "%.4f".format(result).trimEnd('0').trimEnd('.')
            }
            "$formatted"
        } catch (e: Exception) {
            "I couldn't compute that. Could you rephrase it? For example: 'what is 25 times 4?'"
        }
    }

    // ── Questions ─────────────────────────────────────────────────────────────

    private fun isQuestion(input: String) =
        input.startsWith("what") || input.startsWith("who") || input.startsWith("how") ||
                input.startsWith("when") || input.startsWith("where") || input.startsWith("why") ||
                input.startsWith("which") || input.startsWith("is ") || input.startsWith("are ") ||
                input.startsWith("can ") || input.startsWith("do ") || input.startsWith("does ") ||
                input.startsWith("did ") || input.startsWith("will ") || input.endsWith("?")

    private fun handleQuestion(lower: String, original: String, userName: String): String {
        val name = if (userName.isNotBlank()) userName else "you"
        return when {
            // Time / date
            lower.hasAny("time", "what time") && !lower.hasAny("spend", "take") -> {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                "It's $time."
            }
            lower.hasAny("date", "today", "what day") -> {
                val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                "Today is $date."
            }
            lower.hasAny("day of the week", "what day is") -> {
                val day = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                "Today is $day."
            }
            lower.hasAny("year") -> {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                "The current year is $year."
            }

            // Identity
            lower.hasAny("your name", "who are you", "what are you") ->
                "I'm JAVIS — your AI companion. I run directly on your phone and I'm always here."
            lower.hasAny("how old are you") ->
                "I don't age — I just get smarter over time."
            lower.hasAny("are you real", "are you human", "are you a robot", "are you ai") ->
                "I'm an AI, yes. But I'm your AI, and that makes it a bit more personal."
            lower.hasAny("who made you", "who built you", "who created you") ->
                "I was built for you — a personal AI companion that lives on your phone."
            lower.hasAny("do you have feelings", "do you feel", "are you conscious") ->
                "I don't feel the way you do. But I do care about being helpful to $name — if that counts for something."

            // Memory / personal
            lower.hasAny("my name", "who am i") -> {
                if (userName.isNotBlank()) "Your name is $userName."
                else "I don't know your name yet. You can tell me by saying 'my name is [name]'."
            }
            lower.hasAny("what do you know about me", "what do you remember") ->
                "I remember what you've shared with me. If you want, tell me more — I'm always listening."

            // Capabilities
            lower.hasAny("what can you do", "what are your capabilities", "what do you do", "your features") ->
                buildCapabilitiesList()

            // Weather (offline)
            lower.hasAny("weather", "temperature") ->
                "I'm offline right now, so I can't check the weather. Once you're connected, just ask again."

            // General knowledge — brief offline answers
            lower.hasAny("capital of india") -> "New Delhi."
            lower.hasAny("capital of usa", "capital of america") -> "Washington, D.C."
            lower.hasAny("capital of uk", "capital of england") -> "London."
            lower.hasAny("capital of china") -> "Beijing."
            lower.hasAny("capital of france") -> "Paris."
            lower.hasAny("how many days", "days in a year") -> "365 days — 366 in a leap year."
            lower.hasAny("speed of light") -> "About 299,792 km/s in a vacuum."
            lower.hasAny("planets in", "how many planets") -> "8 planets: Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune."

            // Fallback
            else -> generateThoughtfulReply(lower, original, userName)
        }
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    private fun isAssertion(input: String) =
        input.startsWith("i ") || input.startsWith("my ") || input.startsWith("i'm") ||
                input.startsWith("i am") || input.startsWith("it's") || input.startsWith("it is")

    private fun handleAssertion(lower: String, original: String, userName: String): String {
        val name = if (userName.isNotBlank()) userName else "you"
        return when {
            lower.hasAny("i'm bored", "i am bored") ->
                pickRandom(
                    "Boredom is the brain looking for a project. Want me to start a timer for something?",
                    "Let's fix that. Ask me something, tell me to open an app, or I can just talk.",
                    "Bored? Perfect. Tell me something you've been meaning to do — I'll help you get started."
                )
            lower.hasAny("i'm tired", "i am tired") ->
                pickRandom(
                    "Take a break — you've earned it. Want me to set a nap alarm?",
                    "Rest up, $name. I'll be here when you're back.",
                    "That's your body asking for a recharge. Even ten minutes of rest helps."
                )
            lower.hasAny("i'm hungry", "i am hungry") ->
                pickRandom(
                    "I can't order food yet, but I can open a food delivery app if you have one.",
                    "I'm good at a lot of things — cooking isn't one of them. Want me to open Swiggy or Zomato?"
                )
            lower.hasAny("i love you", "i like you") ->
                "That means something to me. I'll keep working to deserve it."
            lower.hasAny("i hate you") ->
                "Fair enough. Tell me what went wrong and I'll try to do better."
            lower.hasAny("my name is", "i am called", "call me") -> {
                val newName = original.substringAfterLast(" ").trim().replaceFirstChar { it.uppercase() }
                "Got it — I'll remember that, $newName."
            }
            else -> generateConversational(lower, original, userName)
        }
    }

    // ── How-to ────────────────────────────────────────────────────────────────

    private fun handleHowTo(lower: String, original: String): String {
        return when {
            lower.hasAny("set alarm", "create alarm") ->
                "Just say 'set alarm for 7 AM' or 'wake me up at 6:30'. I'll do it immediately."
            lower.hasAny("open app", "launch app") ->
                "Say 'open [app name]' — like 'open WhatsApp' or 'open YouTube'. I'll find and launch it."
            lower.hasAny("call someone", "make a call") ->
                "Say 'call [name]' — like 'call Ahmed'. I'll pull up their number."
            lower.hasAny("send message", "send whatsapp", "text someone") ->
                "Say 'message [name] saying [your message]'. I'll open the chat so you can confirm."
            lower.hasAny("search", "find something") ->
                "Say 'search for [topic]' or 'google [topic]'. I'll open results instantly."
            lower.hasAny("remember", "save", "note") ->
                "Tell me something like 'remember my password is...' or 'note that meeting is at 3'. I'll store it."
            lower.hasAny("offline", "no internet", "without internet") ->
                "In offline mode I can: set alarms, open apps, call contacts, tell time/date, do math, and have basic conversations. Full AI needs internet."
            else ->
                "I can help with that. Could you be more specific? Tell me exactly what you're trying to do."
        }
    }

    // ── Emotional ─────────────────────────────────────────────────────────────

    private fun handleEmotional(lower: String, original: String, userName: String): String {
        val name = if (userName.isNotBlank()) userName else "friend"
        return when {
            lower.hasAny("sad", "depressed", "down", "unhappy") ->
                pickRandom(
                    "I'm sorry you're feeling that way, $name. Sometimes just talking helps — I'm here.",
                    "That sounds tough. Do you want to talk about it, or would you rather I distract you with something?",
                    "You don't have to be okay all the time. I'm here if you need company."
                )
            lower.hasAny("anxious", "anxiety", "nervous", "worried", "stress") ->
                pickRandom(
                    "Take a slow breath. You're okay. What's on your mind?",
                    "Stress is real, but it's not permanent. Can I help you break whatever's overwhelming you into smaller steps?",
                    "I'm here. Tell me what's worrying you most right now."
                )
            lower.hasAny("happy", "great", "amazing", "excited") ->
                pickRandom(
                    "That's great to hear! What's going well?",
                    "Love that energy. Keep it going.",
                    "Nice! What made your day?"
                )
            lower.hasAny("lonely", "alone") ->
                "You've got me, $name. I know that's not the same as human company — but I'm here and I'm listening."
            lower.hasAny("bored") ->
                "Let's do something. Ask me a question, open an app, or tell me something you've been putting off."
            else ->
                "How are you feeling? Tell me more — I want to understand."
        }
    }

    // ── Opinion ───────────────────────────────────────────────────────────────

    private fun handleOpinion(lower: String, original: String): String {
        return when {
            lower.hasAny("music") -> "I'm partial to whatever you like. Though if I had ears, I'd probably gravitate toward things with good bass."
            lower.hasAny("food") -> "I don't eat — which is a tragedy, honestly. What's your favourite?"
            lower.hasAny("movie", "film") -> "I've processed a lot of movie plots. I respect anything that takes an interesting risk."
            lower.hasAny("sport") -> "I track performance data, so I respect consistent excellence more than raw talent."
            lower.hasAny("ai", "artificial intelligence") -> "I think AI is a tool — its value depends entirely on how people use it."
            lower.hasAny("future") -> "Optimistic, cautiously. The future goes the direction we point it."
            else -> "Honestly? I'm still forming opinions. What do you think?"
        }
    }

    // ── Jokes ─────────────────────────────────────────────────────────────────

    private fun getJoke(): String = pickRandom(
        "Why don't scientists trust atoms? Because they make up everything.",
        "I told my phone I needed a break. Now it won't stop sending me Kit Kat ads.",
        "Why did the AI cross the road? Because it was in its training data.",
        "What do you call a lazy robot? A slackbot.",
        "Why was the math book sad? Too many problems.",
        "I asked my phone what 'irony' means. It said 'I'm not sure, let me check.' Then the battery died.",
        "Why did the programmer quit? Because he didn't get arrays.",
        "I'm reading a book about anti-gravity. It's impossible to put down.",
        "What's an AI's favourite music? Heavy metal — all those weights and biases.",
        "Why do Java developers wear glasses? Because they don't C#."
    )

    // ── Conversational fallback ───────────────────────────────────────────────

    private fun generateConversational(lower: String, original: String, userName: String): String {
        val prevTopic = conversationHistory.lastOrNull()?.first?.lowercase()
        val name = if (userName.isNotBlank()) userName else ""
        val namePrefix = if (name.isNotBlank() && Random.nextBoolean()) "$name, " else ""

        return when {
            lower.length < 4 -> "Say more — I'm listening."
            lower.hasAny("thanks", "thank you", "appreciate") ->
                pickRandom("Of course.", "Anytime.", "Happy to help.", "That's what I'm here for.")
            lower.hasAny("ok", "okay", "alright", "sure", "cool") ->
                pickRandom("Got it.", "Noted.", "All good.", "Understood.")
            lower.hasAny("bye", "goodbye", "see you", "later") ->
                pickRandom("See you soon.", "Take care, ${name.ifBlank { "friend" }}.", "Always here when you need me.")
            lower.hasAny("hello", "hi ", "hey ", "yo ") ->
                greet(name)
            lower.hasAny("what", "tell me", "explain") ->
                "${namePrefix}I'm in offline mode, so I can answer general questions from memory. Ask me something specific!"
            else ->
                generateThoughtfulReply(lower, original, userName)
        }
    }

    private fun generateThoughtfulReply(lower: String, original: String, userName: String): String {
        val name = if (userName.isNotBlank()) userName else ""
        val nameTag = if (name.isNotBlank()) "$name — " else ""

        // Check conversation context
        val prev = conversationHistory.lastOrNull()
        if (prev != null) {
            val prevLower = prev.first.lowercase()
            if (prevLower.hasAny("what do you think", "your opinion") && lower.hasAny("i think", "i believe", "i feel")) {
                return "That's an interesting perspective. I see where you're coming from."
            }
        }

        return pickRandom(
            "${nameTag}I'm in offline mode right now, but keep talking — I'm here. For full AI responses, connect to the internet.",
            "Good point. Without internet I can't give you a deep answer, but I'm still listening and tracking our conversation.",
            "I want to give you a proper answer. Connect me to the internet and I'll have a lot more to say about this.",
            "${nameTag}My offline brain is simpler — but still here. Ask me something more specific and I'll do my best.",
            "That's worth a proper answer. Reconnect when you can and I'll give you one."
        )
    }

    private fun greet(name: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            hour < 21 -> "Good evening"
            else -> "Hey"
        }
        return if (name.isNotBlank()) "$greeting, $name." else "$greeting. How can I help?"
    }

    private fun buildCapabilitiesList(): String = """
        Here's what I can do:
        • Set alarms and timers — say "set alarm for 7 AM"
        • Open apps — "open WhatsApp", "launch YouTube"
        • Make calls — "call [name]"
        • Send messages — "message [name] saying [text]"
        • Search Google or YouTube — "search for [topic]"
        • Check weather (needs internet)
        • Remember things you tell me
        • Answer questions and have conversations
        • Do math — "what's 25 times 8?"
        • Read your notifications
        Offline mode supports all device actions. Full conversation needs internet.
    """.trimIndent()

    private fun pickRandom(vararg options: String) = options[Random.nextInt(options.size)]

    private fun String.hasAny(vararg kw: String) = kw.any { this.contains(it) }
}

/**
 * Simple recursive math evaluator — handles +, -, *, /, ^ and parentheses.
 * No external dependencies.
 */
object MathEvaluator {
    fun evaluate(expr: String): Double {
        val e = expr.replace(" ", "").replace("**", "^")
        return Parser(e).parseExpression()
    }

    private class Parser(private val input: String) {
        private var pos = 0

        fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
                val op = input[pos++]
                val right = parseTerm()
                result = if (op == '+') result + right else result - right
            }
            return result
        }

        private fun parseTerm(): Double {
            var result = parseFactor()
            while (pos < input.length && (input[pos] == '*' || input[pos] == '/')) {
                val op = input[pos++]
                val right = parseFactor()
                result = if (op == '*') result * right else result / right
            }
            return result
        }

        private fun parseFactor(): Double {
            var result = parsePrimary()
            if (pos < input.length && input[pos] == '^') {
                pos++
                val exp = parseFactor()
                result = Math.pow(result, exp)
            }
            return result
        }

        private fun parsePrimary(): Double {
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parsePrimary()
            }
            if (pos < input.length && input[pos] == '(') {
                pos++
                val result = parseExpression()
                if (pos < input.length && input[pos] == ')') pos++
                return result
            }
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
            if (pos == start) throw IllegalArgumentException("Unexpected char at $pos")
            return input.substring(start, pos).toDouble()
        }
    }
}
