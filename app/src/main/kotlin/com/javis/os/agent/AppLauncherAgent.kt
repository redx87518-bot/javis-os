package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppLauncherAgent — opens any installed app by name.
 * Uses direct PackageManager scan (no DB dependency).
 * Supports fuzzy matching: "open whatsapp", "launch youtube", "open settings".
 */
@Singleton
class AppLauncherAgent @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Cache of appName.lowercase() → packageName
    private var appCache: Map<String, String> = emptyMap()
    private var lastScanTime = 0L

    /** Resolve and launch app from natural language input. */
    fun launch(input: String): String {
        val lower = input.lowercase().trim()
        val appName = extractAppName(lower)

        if (appName.isBlank()) return "Which app should I open?"

        ensureCache()

        // 1. Known alias map
        val aliasPackage = ALIASES[appName] ?: ALIASES.entries.firstOrNull {
            appName.contains(it.key) || it.key.contains(appName)
        }?.value

        if (aliasPackage != null) {
            return launchPackage(aliasPackage, appName)
        }

        // 2. Exact match in cache
        val exactPkg = appCache[appName]
        if (exactPkg != null) return launchPackage(exactPkg, appName)

        // 3. Contains match
        val containsPkg = appCache.entries.firstOrNull {
            it.key.contains(appName) || appName.contains(it.key)
        }?.value
        if (containsPkg != null) {
            val appLabel = appCache.entries.first { it.value == containsPkg }.key
            return launchPackage(containsPkg, appLabel)
        }

        // 4. Fuzzy: any word overlap
        val appWords = appName.split(" ").filter { it.length > 2 }
        val fuzzyMatch = appCache.entries.firstOrNull { (name, _) ->
            appWords.any { word -> name.contains(word) }
        }
        if (fuzzyMatch != null) {
            return launchPackage(fuzzyMatch.value, fuzzyMatch.key)
        }

        // 5. Special system actions
        val systemResult = handleSystemAction(appName)
        if (systemResult != null) return systemResult

        return "I couldn't find \"$appName\" installed. Make sure it's installed and try again."
    }

    private fun launchPackage(packageName: String, label: String): String {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
                ?: return "I found $label but it doesn't have a launcher icon."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
            val displayName = label.replaceFirstChar { it.uppercase() }
            "Opening $displayName."
        } catch (e: Exception) {
            Log.e("AppLauncherAgent", "Launch failed for $packageName: ${e.message}")
            "I had trouble opening $label."
        }
    }

    private fun handleSystemAction(input: String): String? {
        return when {
            input.hasAny("setting", "config") -> {
                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening Settings."
            }
            input.hasAny("wifi", "wi-fi") -> {
                context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening Wi-Fi settings."
            }
            input.hasAny("bluetooth") -> {
                context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening Bluetooth settings."
            }
            input.hasAny("camera") -> {
                val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(cameraIntent)
                "Opening Camera."
            }
            input.hasAny("calculator", "calc") -> {
                try {
                    val i = Intent(Intent.ACTION_MAIN).apply {
                        setClassName("com.miui.calculator", "com.miui.calculator.cal.CalculatorActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(i)
                    "Opening Calculator."
                } catch (e: Exception) { null }
            }
            input.hasAny("browser", "internet", "chrome") -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening browser."
            }
            input.hasAny("play store", "playstore", "market") -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening Play Store."
            }
            else -> null
        }
    }

    private fun extractAppName(lower: String): String {
        val triggers = listOf("open ", "launch ", "start ", "run ", "go to ", "switch to ", "take me to ")
        for (t in triggers) {
            if (lower.contains(t)) {
                return lower.substringAfter(t).trim()
                    .removePrefix("the ").removePrefix("my ").trim()
            }
        }
        return lower
    }

    private fun ensureCache() {
        val now = System.currentTimeMillis()
        if (appCache.isNotEmpty() && now - lastScanTime < CACHE_TTL_MS) return

        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            appCache = apps.associate { info ->
                info.loadLabel(pm).toString().lowercase() to info.activityInfo.packageName
            }
            lastScanTime = now
            Log.d("AppLauncherAgent", "Cache built: ${appCache.size} apps")
        } catch (e: Exception) {
            Log.e("AppLauncherAgent", "Cache build failed: ${e.message}")
        }
    }

    private fun String.hasAny(vararg kw: String) = kw.any { this.contains(it) }

    companion object {
        private const val CACHE_TTL_MS = 5 * 60_000L

        val ALIASES = mapOf(
            "whatsapp" to "com.whatsapp",
            "wa" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "yt" to "com.google.android.youtube",
            "instagram" to "com.instagram.android",
            "insta" to "com.instagram.android",
            "ig" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "fb" to "com.facebook.katana",
            "twitter" to "com.twitter.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "telegram" to "com.telegram.messenger",
            "tg" to "com.telegram.messenger",
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "snapchat" to "com.snapchat.android",
            "snap" to "com.snapchat.android",
            "contacts" to "com.android.contacts",
            "phone" to "com.android.dialer",
            "dialer" to "com.android.dialer",
            "messages" to "com.android.mms",
            "sms" to "com.android.mms",
            "clock" to "com.android.deskclock",
            "gallery" to "com.miui.gallery",
            "photos" to "com.google.android.apps.photos",
            "files" to "com.android.fileexplorer",
            "file manager" to "com.android.fileexplorer",
            "music" to "com.miui.player",
            "calendar" to "com.google.android.calendar",
            "meet" to "com.google.android.apps.meetings",
            "zoom" to "us.zoom.videomeetings",
            "chrome" to "com.android.chrome",
            "mi browser" to "com.mi.globalbrowser",
        )
    }
}
