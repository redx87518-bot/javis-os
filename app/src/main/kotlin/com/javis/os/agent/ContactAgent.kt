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

    fun searchContact(query: String): Contact? {
        val resolver: ContentResolver = context.contentResolver
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return null

        try {
            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$cleanQuery%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = it.getString(nameIdx) ?: return null
                    val number = it.getString(numIdx) ?: return null
                    return Contact(name = name, phoneNumber = number)
                }
            }
        } catch (e: Exception) {
            Log.e("ContactAgent", "Contact search error: ${e.message}")
        }
        return null
    }

    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
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
            Log.e("ContactAgent", "Error listing contacts: ${e.message}")
        }
        return contacts.distinctBy { it.phoneNumber }
    }

    /**
     * Initiate call — uses ACTION_DIAL (shows dialer with number pre-filled, user confirms).
     * Never uses ACTION_CALL directly without user confirmation.
     */
    fun initiateCall(contact: Contact): String {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opening dialer to call ${contact.name} at ${contact.phoneNumber}. Please confirm the call."
        } catch (e: Exception) {
            "I couldn't open the dialer: ${e.message}"
        }
    }

    /**
     * Handle a call request: search for contact, then dial.
     */
    fun handleCallRequest(nameQuery: String): String {
        // Direct number?
        if (nameQuery.matches(Regex("[0-9+\\-\\s()]{7,}"))) {
            return try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${nameQuery.replace(" ", "")}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening dialer for $nameQuery. Please confirm the call."
            } catch (e: Exception) {
                "I couldn't open the dialer: ${e.message}"
            }
        }

        val contact = searchContact(nameQuery)
        return if (contact != null) {
            initiateCall(contact)
        } else {
            // Try opening contacts search as fallback
            try {
                val intent = Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "I couldn't find \"$nameQuery\" in your contacts. I've opened your contacts so you can pick manually."
            } catch (e: Exception) {
                "I couldn't find \"$nameQuery\" in your contacts. Please check the name and try again."
            }
        }
    }

    /**
     * Open messaging app for a contact (WhatsApp first, then SMS).
     * Never sends without user confirmation — just opens the compose window.
     */
    fun openMessageCompose(nameQuery: String, message: String): String {
        val contact = searchContact(nameQuery)
        val phone = contact?.phoneNumber

        return if (phone != null) {
            // Try WhatsApp first
            val whatsappOpened = tryOpenWhatsApp(phone, message)
            if (!whatsappOpened) {
                // Fall back to SMS
                openSms(phone, message)
            }
            "I've opened a message to ${contact.name} with: \"$message\"\nPlease review and send it yourself — I never send without your confirmation."
        } else {
            "I couldn't find \"$nameQuery\" in your contacts."
        }
    }

    private fun tryOpenWhatsApp(phone: String, message: String): Boolean {
        return try {
            val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openSms(phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ContactAgent", "SMS open failed: ${e.message}")
        }
    }
}
