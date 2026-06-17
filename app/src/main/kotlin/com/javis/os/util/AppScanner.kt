package com.javis.os.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson
import com.javis.os.data.local.dao.AppDao
import com.javis.os.data.local.entities.AppEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Known capability profiles for popular apps
private val APP_CAPABILITIES = mapOf(
    "com.whatsapp" to listOf("messaging", "voice_calls", "video_calls", "share_media"),
    "com.google.android.youtube" to listOf("video_search", "video_playback", "stream"),
    "com.android.chrome" to listOf("web_search", "web_browse"),
    "com.google.android.apps.chrome" to listOf("web_search", "web_browse"),
    "com.tiktok.musically" to listOf("video_search", "video_playback", "short_videos"),
    "com.instagram.android" to listOf("photos", "stories", "reels", "messaging"),
    "com.twitter.android" to listOf("tweets", "search", "trending"),
    "com.facebook.katana" to listOf("social", "messaging", "photos"),
    "com.spotify.music" to listOf("music_playback", "search_music", "playlists"),
    "com.google.android.apps.maps" to listOf("navigation", "search_places", "directions"),
    "com.google.android.gm" to listOf("email", "compose_email"),
    "com.google.android.calendar" to listOf("calendar", "events", "reminders"),
    "com.netflix.mediaclient" to listOf("video_streaming", "movies", "tv_shows"),
    "com.snapchat.android" to listOf("photos", "stories", "messaging"),
    "com.telegram.messenger" to listOf("messaging", "voice_calls", "channels"),
)

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao
) {
    private val gson = Gson()

    suspend fun scanAndStore() {
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val entities = apps.map { info ->
                val pkg = info.activityInfo.packageName
                val name = info.loadLabel(pm).toString()
                val caps = APP_CAPABILITIES[pkg] ?: inferCapabilities(name, pkg)
                AppEntity(
                    packageName = pkg,
                    appName = name,
                    capabilities = gson.toJson(caps),
                    launchCategory = "launcher"
                )
            }
            appDao.insertAll(entities)
            Log.i("AppScanner", "Scanned ${entities.size} apps")
        } catch (e: Exception) {
            Log.e("AppScanner", "Scan failed: ${e.message}")
        }
    }

    private fun inferCapabilities(name: String, pkg: String): List<String> {
        val lower = name.lowercase()
        return buildList {
            if (lower.contains("mail") || lower.contains("email")) add("email")
            if (lower.contains("music") || lower.contains("audio")) add("music_playback")
            if (lower.contains("video") || lower.contains("player")) add("video_playback")
            if (lower.contains("camera") || lower.contains("photo")) add("camera")
            if (lower.contains("map") || lower.contains("navigation")) add("navigation")
            if (lower.contains("browser") || lower.contains("chrome") || lower.contains("firefox")) add("web_browse")
            if (lower.contains("messenger") || lower.contains("chat") || lower.contains("message")) add("messaging")
            if (lower.contains("call") || lower.contains("phone") || lower.contains("dialer")) add("calls")
        }
    }
}
