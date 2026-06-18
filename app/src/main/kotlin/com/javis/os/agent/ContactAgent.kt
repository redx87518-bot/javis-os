package com.javis.os.agent

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class Contact(
    val name: String,
    val phoneNumber: String,
    val normalizedName: String = name.lowercase()
)

@Singleton
class ContactAgent @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Fuzzy contact search — checks exact, then contains, then word-level. */
    fun searchContact(query: String): Contact? {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return null

        // 1. Try direct LIKE query first
        val direct = queryContacts("%$cleanQuery%")
        if (direct.isNotEmpty()) return direct.first()

        // 2. Fuzzy — each word in query against all contacts
        val words = cleanQuery.split(" ").filter { it.length > 2 }
        if (words.isEmpty()) return null

        val allContacts = queryContacts("%")
        return allContacts.firstOrNull { contact ->
            words.any { word -> contact.normalizedName.contains(word) }
        }
    }

    private fun queryContacts(pattern: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf(pattern),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: continue
                    val number = it.getString(numIdx) ?: continue
                    contacts.add(Contact(name = name, phoneNumber = number.replace(" ", "")))
                }
            }
        } catch (e: Exception) {
            Log.e("ContactAgent", "Contact query error: ${e.message}")
        }
        return contacts.distinctBy { it.phoneNumber }
    }

    fun getAllContacts(): List<Contact> = queryContacts("%")

    /**
     * Directly dials the contact using ACTION_CALL (requires CALL_PHONE permission).
     * If permission is missing at runtime, falls back to ACTION_DIAL (shows dialer).
     */
    fun initiateCall(contact: Contact): String {
        return try {
            // ACTION_CALL dials immediately — no user confirmation step in dialer
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            "Calling ${contact.name} now…"
        } catch (e: SecurityException) {
            // CALL_PHONE permission denied at runtime — fall back to dialer
            Log.w("ContactAgent", "CALL_PHONE denied, falling back to DIAL")
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contact.phoneNumber}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                "I've pre-filled ${contact.name}'s number in the dialer — tap the call button to connect."
            } catch (e2: Exception) {
                "Couldn't initiate call: ${e2.message}"
            }
        } catch (e: Exception) {
            Log.e("ContactAgent", "Call failed: ${e.message}")
            "I had trouble calling ${contact.name}. ${e.message}"
        }
    }

    /**
     * Full call flow: parse name, search, dial (or show dialer if not found).
     */
    fun handleCallRequest(nameQuery: String): String {
        val trimmed = nameQuery.trim()

        // Direct number
        if (trimmed.matches(Regex("[0-9+\\-\\s()]{7,}"))) {
            return try {
                context.startActivity(Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${trimmed.replace(" ", "")}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "Dialing $trimmed…"
            } catch (e: SecurityException) {
                context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${trimmed.replace(" ", "")}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "Dialer opened for $trimmed."
            } catch (e: Exception) {
                "Couldn't dial $trimmed: ${e.message}"
            }
        }

        val contact = searchContact(trimmed)
        return if (contact != null) {
            initiateCall(contact)
        } else {
            // Open contacts picker as last resort
            try {
                context.startActivity(Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "I couldn't find \"$trimmed\" in your contacts. I've opened your contacts — pick the person to call."
            } catch (e: Exception) {
                "I couldn't find \"$trimmed\" in your contacts. Please check the name and try again."
            }
        }
    }

    /**
     * Open WhatsApp or SMS compose for a contact.
     */
    fun openMessageCompose(nameQuery: String, message: String): String {
        val contact = searchContact(nameQuery) ?: return "I couldn't find \"$nameQuery\" in your contacts."
        val phone = contact.phoneNumber

        val whatsappOpened = tryOpenWhatsApp(phone, message)
        if (!whatsappOpened) openSms(phone, message)

        return "I've opened a message to ${contact.name} with: \"$message\"\nPlease review and send — I never send without your confirmation."
    }

    private fun tryOpenWhatsApp(phone: String, message: String): Boolean {
        return try {
            val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) { false }
    }

    private fun openSms(phone: String, message: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("ContactAgent", "SMS open failed: ${e.message}")
        }
    }
}
