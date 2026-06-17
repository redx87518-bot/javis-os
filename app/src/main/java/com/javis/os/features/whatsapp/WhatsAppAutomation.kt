package com.javis.os.features.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class WhatsAppDraft(
    val contactName: String,
    val phoneNumber: String,
    val message: String
)

@Singleton
class WhatsAppAutomation @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val whatsappPackage = "com.whatsapp"

    fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(whatsappPackage, 0)
            true
        } catch (e: Exception) { false }
    }

    fun openWhatsAppChat(phoneNumber: String, message: String = "") {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        try {
            val uri = if (message.isNotBlank()) {
                Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://wa.me/$cleanNumber")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(whatsappPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openWhatsAppFallback(phoneNumber, message)
        }
    }

    private fun openWhatsAppFallback(phoneNumber: String, message: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val uri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openWhatsApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(whatsappPackage)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun shareTextViaWhatsApp(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(whatsappPackage)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun buildConfirmationMessage(draft: WhatsAppDraft): String {
        return "I'll send \"${draft.message}\" to ${draft.contactName} on WhatsApp. Should I go ahead?"
    }
}
