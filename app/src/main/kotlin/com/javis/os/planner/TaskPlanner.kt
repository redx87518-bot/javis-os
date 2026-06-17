package com.javis.os.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import com.javis.os.service.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPlanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun planAlarm(userInput: String): String {
        val lower = userInput.lowercase()
        return try {
            val (hour, minute, label) = parseAlarmTime(lower)
            if (hour >= 0) {
                setAlarmViaIntent(hour, minute, label)
                "Setting alarm for ${formatTime(hour, minute)}: $label"
            } else if (lower.contains("timer")) {
                val minutes = extractMinutes(lower)
                if (minutes > 0) {
                    setTimer(minutes)
                    "Starting a $minutes-minute timer."
                } else {
                    "Could you tell me how long the timer should be?"
                }
            } else {
                "I couldn't understand the time. Could you say it again? For example: 'Remind me at 8 AM' or 'Set alarm for 7:30'."
            }
        } catch (e: Exception) {
            Log.e("TaskPlanner", "Alarm error: ${e.message}")
            "I had trouble setting the alarm. Please try again."
        }
    }

    private fun setAlarmViaIntent(hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun setTimer(minutes: Int) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun parseAlarmTime(input: String): Triple<Int, Int, String> {
        // Pattern: "at HH:MM" or "at H AM/PM" or "at H"
        val timeRegex = Regex("""at\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val match = timeRegex.find(input)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: -1
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val meridiem = match.groupValues[3]
            if (meridiem == "pm" && hour < 12) hour += 12
            if (meridiem == "am" && hour == 12) hour = 0
            val label = extractAlarmLabel(input)
            return Triple(hour, minute, label)
        }

        // Pattern: "tomorrow at X" - use next day at that time
        if (input.contains("tomorrow")) {
            val numRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
            val nm = numRegex.find(input)
            if (nm != null) {
                var hour = nm.groupValues[1].toIntOrNull() ?: -1
                val minute = nm.groupValues[2].toIntOrNull() ?: 0
                val meridiem = nm.groupValues[3]
                if (meridiem == "pm" && hour < 12) hour += 12
                if (meridiem == "am" && hour == 12) hour = 0
                return Triple(hour, minute, "JAVIS Reminder")
            }
        }

        // Common words: "morning" = 8, "afternoon" = 14, "evening" = 19, "night" = 22
        return when {
            input.contains("morning") -> Triple(8, 0, "Morning reminder")
            input.contains("afternoon") -> Triple(14, 0, "Afternoon reminder")
            input.contains("evening") -> Triple(19, 0, "Evening reminder")
            input.contains("night") -> Triple(21, 0, "Night reminder")
            else -> Triple(-1, 0, "")
        }
    }

    private fun extractMinutes(input: String): Int {
        val regex = Regex("""(\d+)\s*(minute|min|hour|hr)""")
        val match = regex.find(input) ?: return 0
        val value = match.groupValues[1].toIntOrNull() ?: return 0
        return if (match.groupValues[2].startsWith("hour")) value * 60 else value
    }

    private fun extractAlarmLabel(input: String): String {
        val triggers = listOf("remind me to ", "reminder to ", "remind me ", "alarm for ")
        for (t in triggers) {
            if (input.contains(t)) {
                val label = input.substringAfter(t).substringBefore(" at").trim()
                if (label.isNotBlank()) return label.replaceFirstChar { it.uppercase() }
            }
        }
        return "JAVIS Alarm"
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)
    }
}
