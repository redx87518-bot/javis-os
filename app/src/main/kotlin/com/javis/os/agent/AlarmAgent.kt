package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmAgent @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun handleAlarmRequest(input: String): String {
        val lower = input.lowercase().trim()
        return when {
            lower.hasAny("timer", "count down", "countdown") -> handleTimer(lower)
            lower.hasAny("remind", "reminder") -> handleReminder(input, lower)
            else -> handleAlarm(input, lower)
        }
    }

    // ── Alarm ─────────────────────────────────────────────────────────────────

    private fun handleAlarm(original: String, lower: String): String {
        val parsed = parseTime(lower)
        return if (parsed != null) {
            val (hour, minute) = parsed
            val label = extractLabel(lower) ?: "JAVIS Alarm"
            setAlarm(hour, minute, label)
            "Alarm set for ${formatTime(hour, minute)}."
        } else {
            "I couldn't figure out the time. Say something like 'set alarm for 7 AM' or 'wake me up at 6:30'."
        }
    }

    // ── Reminder ──────────────────────────────────────────────────────────────

    private fun handleReminder(original: String, lower: String): String {
        val parsed = parseTime(lower)
        return if (parsed != null) {
            val (hour, minute) = parsed
            val label = extractLabel(lower) ?: "JAVIS Reminder"
            setAlarm(hour, minute, label)
            "I'll remind you at ${formatTime(hour, minute)}: $label."
        } else if (lower.hasAny("in ", "after ")) {
            val minutes = extractRelativeMinutes(lower)
            if (minutes > 0) {
                setTimer(minutes)
                "Reminder set for $minutes minutes from now."
            } else {
                "How many minutes? Say 'remind me in 20 minutes'."
            }
        } else {
            "When should I remind you? Say 'remind me at 3 PM' or 'remind me in 30 minutes'."
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun handleTimer(lower: String): String {
        val seconds = extractTimerSeconds(lower)
        return if (seconds > 0) {
            setTimer(seconds / 60)
            val display = when {
                seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
                seconds >= 60 -> "${seconds / 60} minute${if (seconds / 60 != 1) "s" else ""}"
                else -> "$seconds seconds"
            }
            "Timer started for $display."
        } else {
            "How long should the timer be? Say 'set timer for 10 minutes'."
        }
    }

    // ── AlarmManager intents ──────────────────────────────────────────────────

    private fun setAlarm(hour: Int, minute: Int, label: String) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i("AlarmAgent", "Alarm set: $hour:$minute '$label'")
        } catch (e: Exception) {
            Log.e("AlarmAgent", "Failed to set alarm: ${e.message}")
        }
    }

    private fun setTimer(minutes: Int) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AlarmAgent", "Failed to set timer: ${e.message}")
        }
    }

    // ── Time parsing ──────────────────────────────────────────────────────────

    /**
     * Parses time expressions like:
     *  "7 AM", "7:30 PM", "at 8", "19:00", "half past 3", "quarter to 5"
     */
    fun parseTime(input: String): Pair<Int, Int>? {
        val lower = input.lowercase()

        // HH:MM with optional AM/PM
        val colonRegex = Regex("""(\d{1,2}):(\d{2})\s*(am|pm)?""")
        colonRegex.find(lower)?.let { m ->
            var h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            val mer = m.groupValues[3]
            h = adjustMeridiem(h, mer)
            return Pair(h, min)
        }

        // "X AM/PM" or "at X AM/PM"
        val simpleRegex = Regex("""(?:at\s+)?(\d{1,2})\s*(am|pm|a\.m|p\.m)""")
        simpleRegex.find(lower)?.let { m ->
            var h = m.groupValues[1].toInt()
            val mer = m.groupValues[2].replace(".", "")
            h = adjustMeridiem(h, mer)
            return Pair(h, 0)
        }

        // Bare number "at 8" / "at 20" (24h if >= 13)
        val bareRegex = Regex("""(?:at|for)\s+(\d{1,2})(?!\s*:)(?!\s*\d)""")
        bareRegex.find(lower)?.let { m ->
            val h = m.groupValues[1].toInt()
            if (h in 0..23) return Pair(h, 0)
        }

        // Natural language
        return when {
            lower.contains("midnight") -> Pair(0, 0)
            lower.contains("noon") || lower.contains("midday") -> Pair(12, 0)
            lower.contains("morning") && !lower.contains("every") -> Pair(8, 0)
            lower.contains("afternoon") -> Pair(14, 0)
            lower.contains("evening") -> Pair(19, 0)
            lower.contains("night") && !lower.contains("good night") -> Pair(21, 0)
            else -> null
        }
    }

    private fun adjustMeridiem(hour: Int, mer: String): Int = when {
        mer == "pm" && hour < 12 -> hour + 12
        mer == "am" && hour == 12 -> 0
        else -> hour
    }

    private fun extractRelativeMinutes(input: String): Int {
        val regex = Regex("""(\d+)\s*(minute|min|hour|hr|second|sec)""")
        val match = regex.find(input) ?: return 0
        val value = match.groupValues[1].toIntOrNull() ?: return 0
        return when {
            match.groupValues[2].startsWith("hour") || match.groupValues[2].startsWith("hr") -> value * 60
            match.groupValues[2].startsWith("sec") -> maxOf(1, value / 60)
            else -> value
        }
    }

    private fun extractTimerSeconds(input: String): Int {
        var total = 0
        val hourRegex = Regex("""(\d+)\s*(?:hour|hr)""")
        val minRegex = Regex("""(\d+)\s*(?:minute|min)""")
        val secRegex = Regex("""(\d+)\s*(?:second|sec)""")
        hourRegex.find(input)?.let { total += it.groupValues[1].toInt() * 3600 }
        minRegex.find(input)?.let { total += it.groupValues[1].toInt() * 60 }
        secRegex.find(input)?.let { total += it.groupValues[1].toInt() }
        return total
    }

    private fun extractLabel(input: String): String? {
        val patterns = listOf(
            Regex("""remind(?:er)?\s+(?:me\s+)?(?:to\s+)?(.+?)(?:\s+at|\s+in|\s+for|$)"""),
            Regex("""alarm\s+(?:for\s+)?(.+?)(?:\s+at|\s+in|$)"""),
            Regex("""wake\s+me\s+up\s+(?:to\s+)?(.+?)(?:\s+at|\s+in|$)""")
        )
        for (p in patterns) {
            val m = p.find(input.lowercase())
            val label = m?.groupValues?.getOrNull(1)?.trim()
            if (!label.isNullOrBlank() && label.length > 2) {
                return label.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)
    }

    private fun String.hasAny(vararg kw: String) = kw.any { this.contains(it) }
}
