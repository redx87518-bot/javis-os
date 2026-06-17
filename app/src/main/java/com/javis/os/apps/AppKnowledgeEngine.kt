package com.javis.os.apps

import javax.inject.Inject
import javax.inject.Singleton

data class AppProfile(
    val packageName: String,
    val appName: String,
    val categories: List<String>,
    val capabilities: List<String>
)

@Singleton
class AppKnowledgeEngine @Inject constructor() {

    private val knowledgeBase: Map<String, AppProfile> = mapOf(
        "com.whatsapp" to AppProfile("com.whatsapp", "WhatsApp", listOf("messaging"), listOf("messaging", "voice_call", "video_call", "media_share")),
        "com.google.android.youtube" to AppProfile("com.google.android.youtube", "YouTube", listOf("entertainment"), listOf("video_search", "playback", "subscribe")),
        "com.android.chrome" to AppProfile("com.android.chrome", "Chrome", listOf("browser"), listOf("web_search", "navigation", "bookmarks")),
        "com.google.android.googlequicksearchbox" to AppProfile("com.google.android.googlequicksearchbox", "Google", listOf("search"), listOf("web_search", "voice_search", "news")),
        "com.zhiliaoapp.musically" to AppProfile("com.zhiliaoapp.musically", "TikTok", listOf("entertainment"), listOf("video_search", "playback", "create")),
        "com.instagram.android" to AppProfile("com.instagram.android", "Instagram", listOf("social"), listOf("messaging", "stories", "reels", "photos")),
        "com.facebook.katana" to AppProfile("com.facebook.katana", "Facebook", listOf("social"), listOf("news_feed", "messaging", "groups")),
        "com.twitter.android" to AppProfile("com.twitter.android", "Twitter", listOf("social"), listOf("tweets", "search", "dm")),
        "com.spotify.music" to AppProfile("com.spotify.music", "Spotify", listOf("music"), listOf("music_playback", "search", "playlist")),
        "com.google.android.gm" to AppProfile("com.google.android.gm", "Gmail", listOf("email"), listOf("send_email", "read_email", "compose")),
        "com.google.android.apps.maps" to AppProfile("com.google.android.apps.maps", "Google Maps", listOf("navigation"), listOf("navigation", "search_places", "directions")),
        "com.google.android.calendar" to AppProfile("com.google.android.calendar", "Calendar", listOf("productivity"), listOf("create_event", "view_schedule", "reminders")),
        "com.netflix.mediaclient" to AppProfile("com.netflix.mediaclient", "Netflix", listOf("entertainment"), listOf("video_search", "playback", "downloads")),
        "org.telegram.messenger" to AppProfile("org.telegram.messenger", "Telegram", listOf("messaging"), listOf("messaging", "voice_call", "channels", "bots")),
        "com.google.android.apps.photos" to AppProfile("com.google.android.apps.photos", "Google Photos", listOf("photos"), listOf("view_photos", "albums", "share")),
        "com.amazon.mShop.android.shopping" to AppProfile("com.amazon.mShop.android.shopping", "Amazon", listOf("shopping"), listOf("search", "purchase", "orders")),
    )

    fun getProfile(packageName: String, appName: String): AppProfile {
        return knowledgeBase[packageName] ?: inferProfile(packageName, appName)
    }

    private fun inferProfile(packageName: String, appName: String): AppProfile {
        val categories = mutableListOf<String>()
        val capabilities = mutableListOf<String>()

        val nameLower = appName.lowercase()
        when {
            nameLower.contains("music") || nameLower.contains("player") || nameLower.contains("audio") -> {
                categories.add("music")
                capabilities.addAll(listOf("music_playback", "search"))
            }
            nameLower.contains("map") || nameLower.contains("navig") -> {
                categories.add("navigation")
                capabilities.addAll(listOf("navigation", "search_places"))
            }
            nameLower.contains("mail") || nameLower.contains("email") -> {
                categories.add("email")
                capabilities.addAll(listOf("send_email", "read_email"))
            }
            nameLower.contains("camera") || nameLower.contains("photo") -> {
                categories.add("camera")
                capabilities.addAll(listOf("take_photo", "view_photos"))
            }
            nameLower.contains("shop") || nameLower.contains("store") || nameLower.contains("buy") -> {
                categories.add("shopping")
                capabilities.addAll(listOf("search", "purchase"))
            }
            nameLower.contains("bank") || nameLower.contains("pay") || nameLower.contains("finance") -> {
                categories.add("finance")
                capabilities.addAll(listOf("payments", "balance"))
            }
            else -> {
                categories.add("general")
                capabilities.add("launch")
            }
        }

        return AppProfile(packageName, appName, categories, capabilities)
    }

    fun canHandleAction(profile: AppProfile, action: String): Boolean {
        return profile.capabilities.any { it.contains(action.lowercase()) }
    }

    fun getAppsForAction(action: String, allProfiles: List<AppProfile>): List<AppProfile> {
        return allProfiles.filter { canHandleAction(it, action) }
    }
}
