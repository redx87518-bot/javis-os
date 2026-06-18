package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.javis.os.agent.ContactAgent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReplyAgent — composes and sends messages via WhatsApp, SMS, or Telegram.
 * Always opens the compose screen so the user sees and confirms before sending.
 */
@Singleton
class ReplyAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactAgent: ContactAgent
) {

    fun handleReplyRequest(input: String): String {
        val lower = input.lowercase().trim()

        // Detect platform
        val platform = when {
            lower.hasAny("whatsapp", "wa ") -> Platform.WHATSAPP
            lower.hasAny("telegram", "tg ") -> Platform.TELEGRAM
            lower.hasAny("sms", "text message", "message ") -> Platform.SMS
            else -> Platform.ANY
        }

        // Extract recipient and message
        val (recipient, message) = parseReplyIntent(lower, input)

        if (recipient.isBlank()) {
            return "Who should I message? Say 'message John saying hello'."
        }

        return when (platform) {
            Platform.WHATSAPP -> openWhatsApp(recipient, message)
            Platform.TELEGRAM -> openTelegram(recipient, message)
            Platform.SMS -> openSms(recipient, message)
            Platform.ANY -> openWhatsApp(recipient, message)
        }
    }

    private fun openWhatsApp(recipient: String, message: String): String {
        return try {
            // Try by phone number if it looks like one
            if (recipient.replace("+", "").replace(" ", "").all { it.isDigit() }) {
                val phone = recipient.replace(" ", "").removePrefix("0").let { "+91$it" }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening WhatsApp to message $recipient."
            } else {
                // Open WhatsApp search or main screen
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening WhatsApp. Please select $recipient in the contact list."
            }
        } catch (e: Exception) {
            Log.e("ReplyAgent", "WhatsApp open failed: ${e.message}")
            openSms(recipient, message)
        }
    }

    private fun openTelegram(recipient: String, message: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tg://msg?to=$recipient&text=${Uri.encode(message)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening Telegram to message $recipient."
        } catch (e: Exception) {
            "Telegram isn't installed. Falling back to SMS."
        }
    }

    private fun openSms(recipient: String, message: String): String {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$recipient")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening SMS to $recipient."
        } catch (e: Exception) {
            "I couldn't open the messaging app."
        }
    }

    /**
     * Parses: "message John saying I'll be late"
     *         "tell Sarah that dinner is at 7"
     *         "whatsapp Ahmed hello how are you"
     *         "reply to mom I'm on my way"
     */
    private fun parseReplyIntent(lower: String, original: String): Pair<String, String> {
        val keywords = listOf(
            "whatsapp ", "telegram ", "message ", "tell ", "text ", "msg ", "sms ", "send to ", "reply to "
        )
        for (kw in keywords) {
            if (lower.contains(kw)) {
                val after = original.substringAfter(kw, "").trim()
                val words = after.split(" ")
                if (words.size < 2) return Pair(words.firstOrNull() ?: "", "")

                // Find message separator
                val sepIdx = words.indexOfFirst { w ->
                    w.lowercase() in listOf("saying", "say", "that", "with", "and", ":", "-")
                }

                return if (sepIdx > 0) {
                    val recipient = words.take(sepIdx).joinToString(" ")
                    val message = words.drop(sepIdx + 1).joinToString(" ")
                    Pair(recipient, message)
                } else {
                    // Name is first word, rest is message
                    Pair(words.first(), words.drop(1).joinToString(" "))
                }
            }
        }
        return Pair("", "")
    }

    private fun String.hasAny(vararg kw: String) = kw.any { this.contains(it) }

    private enum class Platform { WHATSAPP, TELEGRAM, SMS, ANY }
}
