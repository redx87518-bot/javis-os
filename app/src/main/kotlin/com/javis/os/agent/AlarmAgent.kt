package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmAgent @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun handleAlarmRequest(input: String): String {
        val lower = input.lowercase().trim()
        return when {
            lower.hasAny("timer", "countdown", "count down") -> handleTimer(lower)
            lower.hasAny("remind me", "reminder") -> handleReminder(input, lower)
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
            "Alarm set for ${formatTime(hour, minute)}. ✅"
        } else {
            "I couldn't figure out the time from that. Try: 'set alarm for 7 AM' or 'wake me up at 6:30'."
        }
    }

    // ── Reminder ──────────────────────────────────────────────────────────────

    private fun handleReminder(original: String, lower: String): String {
        val parsed = parseTime(lower)
        return if (parsed != null) {
            val (hour, minute) = parsed
            val label = extractLabel(lower) ?: "JAVIS Reminder"
            setAlarm(hour, minute, label)
            "Reminder set for ${formatTime(hour, minute)}: $label. ✅"
        } else if (lower.hasAny("in ", "after ")) {
            val minutes = extractRelativeMinutes(lower)
            if (minutes > 0) {
                setTimer(minutes * 60)
                "Reminder in $minutes minute${if (minutes != 1) "s" else ""}. ✅"
            } else {
                "How many minutes? Say 'remind me in 20 minutes'."
            }
        } else {
            "When should I remind you? Try 'remind me at 3 PM' or 'remind me in 30 minutes'."
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun handleTimer(lower: String): String {
        val seconds = extractTimerSeconds(lower)
        return if (seconds > 0) {
            setTimer(seconds)
            val display = formatDuration(seconds)
            "Timer started for $display. ✅"
        } else {
            "How long should the timer be? Say 'set timer for 10 minutes'."
        }
    }

    // ── AlarmClock intents ────────────────────────────────────────────────────

    /**
     * SKIP_UI = false opens the system clock app so the user can confirm.
     * This is far more reliable on MIUI/Redmi than the silent SKIP_UI=true path,
     * which many MIUI builds silently reject.
     */
    private fun setAlarm(hour: Int, minute: Int, label: String) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Open clock app — MIUI reliable
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i("AlarmAgent", "Alarm intent sent: $hour:${minute.toString().padStart(2,'0')} '$label'")
        } catch (e: Exception) {
            Log.e("AlarmAgent", "Alarm failed: ${e.message}")
        }
    }

    private fun setTimer(seconds: Int) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i("AlarmAgent", "Timer intent sent: ${seconds}s")
        } catch (e: Exception) {
            Log.e("AlarmAgent", "Timer failed: ${e.message}")
        }
    }

    // ── Time parsing ──────────────────────────────────────────────────────────

    fun parseTime(input: String): Pair<Int, Int>? {
        val lower = input.lowercase()

        // HH:MM [AM/PM]
        Regex("""(\d{1,2}):(\d{2})\s*(am|pm)?""").find(lower)?.let { m ->
            val h = adjustMeridiem(m.groupValues[1].toInt(), m.groupValues[3])
            val min = m.groupValues[2].toInt().coerceIn(0, 59)
            return Pair(h, min)
        }

        // X AM/PM
        Regex("""(?:at\s+)?(\d{1,2})\s*(am|pm|a\.m\.?|p\.m\.?)""").find(lower)?.let { m ->
            val h = adjustMeridiem(m.groupValues[1].toInt(), m.groupValues[2].replace(".", ""))
            return Pair(h, 0)
        }

        // Bare number after "at" or "for"
        Regex("""(?:at|for)\s+(\d{1,2})(?!\s*[:\d])""").find(lower)?.let { m ->
            val h = m.groupValues[1].toInt()
            if (h in 0..23) return Pair(h, 0)
        }

        // "half past 3" → 3:30
        Regex("""half past\s+(\d{1,2})""").find(lower)?.let { m ->
            return Pair(m.groupValues[1].toInt(), 30)
        }

        // "quarter to 5" → 4:45
        Regex("""quarter to\s+(\d{1,2})""").find(lower)?.let { m ->
            val h = m.groupValues[1].toInt()
            return Pair(if (h == 0) 23 else h - 1, 45)
        }

        // "quarter past 3" → 3:15
        Regex("""quarter past\s+(\d{1,2})""").find(lower)?.let { m ->
            return Pair(m.groupValues[1].toInt(), 15)
        }

        return when {
            lower.contains("midnight") -> Pair(0, 0)
            lower.contains("noon") || lower.contains("midday") -> Pair(12, 0)
            lower.contains("morning") -> Pair(8, 0)
            lower.contains("afternoon") -> Pair(14, 0)
            lower.contains("evening") -> Pair(19, 0)
            lower.contains("night") -> Pair(21, 0)
            else -> null
        }
    }

    private fun adjustMeridiem(hour: Int, mer: String): Int = when {
        mer.startsWith("pm") && hour < 12 -> hour + 12
        mer.startsWith("am") && hour == 12 -> 0
        else -> hour.coerceIn(0, 23)
    }

    private fun extractRelativeMinutes(input: String): Int {
        val m = Regex("""(\d+)\s*(minute|min|hour|hr|second|sec)""").find(input) ?: return 0
        val v = m.groupValues[1].toIntOrNull() ?: return 0
        return when {
            m.groupValues[2].startsWith("hour") || m.groupValues[2].startsWith("hr") -> v * 60
            m.groupValues[2].startsWith("sec") -> maxOf(1, v / 60)
            else -> v
        }
    }

    private fun extractTimerSeconds(input: String): Int {
        var total = 0
        Regex("""(\d+)\s*(?:hour|hr)""").find(input)?.let { total += it.groupValues[1].toInt() * 3600 }
        Regex("""(\d+)\s*(?:minute|min)""").find(input)?.let { total += it.groupValues[1].toInt() * 60 }
        Regex("""(\d+)\s*(?:second|sec)""").find(input)?.let { total += it.groupValues[1].toInt() }
        return total
    }

    private fun extractLabel(input: String): String? {
        listOf(
            Regex("""remind(?:er)?\s+(?:me\s+)?(?:to\s+)?(.+?)(?:\s+at|\s+in|\s+for|$)"""),
            Regex("""alarm\s+(?:for\s+)?(.+?)(?:\s+at|\s+in|$)"""),
            Regex("""wake\s+me\s+(?:up\s+)?(?:to\s+)?(.+?)(?:\s+at|\s+in|$)""")
        ).forEach { p ->
            val label = p.find(input.lowercase())?.groupValues?.getOrNull(1)?.trim()
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
        return java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    private fun formatDuration(seconds: Int): String = when {
        seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        seconds >= 60 -> "${seconds / 60} minute${if (seconds / 60 != 1) "s" else ""}"
        else -> "$seconds second${if (seconds != 1) "s" else ""}"
    }

    private fun String.hasAny(vararg kw: String) = kw.any { this.contains(it) }
}
