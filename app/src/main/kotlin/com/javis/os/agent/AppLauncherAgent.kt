package com.javis.os.agent

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppLauncherAgent — opens any installed app by name or alias.
 *
 * Strategy (in order):
 * 1. Known ALIASES map (instant, no scan needed)
 * 2. Exact / contains / fuzzy match against ALL installed packages (not just launcher apps)
 * 3. System-action handlers (Settings, WiFi, Camera, Browser, etc.)
 *
 * Cache is rebuilt every 3 min OR when [refreshCache] is called explicitly.
 */
@Singleton
class AppLauncherAgent @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** label.lowercase() → packageName */
    private var appCache: Map<String, String> = emptyMap()
    private var lastScanTime = 0L

    fun launch(input: String): String {
        val appName = extractAppName(input.lowercase().trim())
        if (appName.isBlank()) return "Which app should I open?"

        ensureCache()

        // 1. Alias map — exact key
        ALIASES[appName]?.let { return launchPackage(it, appName) }
        // Alias — partial key match
        ALIASES.entries.firstOrNull { (k, _) ->
            appName.contains(k) || k.contains(appName)
        }?.let { return launchPackage(it.value, it.key) }

        // 2. Cache — exact
        appCache[appName]?.let { return launchPackage(it, appName) }

        // 3. Cache — contains
        appCache.entries.firstOrNull { (label, _) ->
            label.contains(appName) || appName.contains(label)
        }?.let { return launchPackage(it.value, it.key) }

        // 4. Cache — word fuzzy
        val words = appName.split(" ").filter { it.length > 2 }
        if (words.isNotEmpty()) {
            appCache.entries.firstOrNull { (label, _) ->
                words.any { label.contains(it) }
            }?.let { return launchPackage(it.value, it.key) }
        }

        // 5. System actions
        handleSystemAction(appName)?.let { return it }

        return "I couldn't find \"$appName\" on your phone. Make sure it's installed."
    }

    /** Force a fresh scan (e.g. after a new app is installed). */
    fun refreshCache() {
        lastScanTime = 0L
        ensureCache()
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun launchPackage(packageName: String, label: String): String {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
                ?: Intent(Intent.ACTION_MAIN).also {
                    it.setPackage(packageName)
                    it.addCategory(Intent.CATEGORY_LAUNCHER)
                }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
            "Opening ${label.replaceFirstChar { it.uppercase() }}."
        } catch (e: Exception) {
            Log.e("AppLauncherAgent", "Launch failed $packageName: ${e.message}")
            "I found ${label.replaceFirstChar { it.uppercase() }} but couldn't open it. Try from your home screen."
        }
    }

    private fun handleSystemAction(name: String): String? = when {
        name.hasAny("setting", "config", "system") -> {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening Settings."
        }
        name.hasAny("wifi", "wi-fi", "wireless") -> {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening Wi-Fi settings."
        }
        name.hasAny("bluetooth") -> {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening Bluetooth settings."
        }
        name.hasAny("camera") -> {
            context.startActivity(
                Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening Camera."
        }
        name.hasAny("calculator", "calc") -> {
            listOf(
                "com.miui.calculator" to "com.miui.calculator.cal.CalculatorActivity",
                "com.android.calculator2" to "com.android.calculator2.Calculator"
            ).firstOrNull { (pkg, cls) ->
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_MAIN).setClassName(pkg, cls)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ); true
                } catch (e: Exception) { false }
            }?.let { return "Opening Calculator." }
            null
        }
        name.hasAny("browser", "internet", "chrome") -> {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening browser."
        }
        name.hasAny("play store", "playstore", "market") -> {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening Play Store."
        }
        name.hasAny("notification", "status bar") -> {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening Notification settings."
        }
        name.hasAny("accessibility") -> {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ); "Opening Accessibility settings."
        }
        else -> null
    }

    private fun extractAppName(lower: String): String {
        val triggers = listOf(
            "open ", "launch ", "start ", "run ", "go to ", "switch to ",
            "take me to ", "show me ", "bring up "
        )
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
        rebuildCache()
    }

    private fun rebuildCache() {
        try {
            val pm = context.packageManager
            val cache = mutableMapOf<String, String>()

            // --- Pass 1: launcher apps (highest priority — these have proper labels) ---
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launcherApps = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in launcherApps) {
                val label = info.loadLabel(pm).toString().lowercase().trim()
                if (label.isNotBlank()) cache[label] = info.activityInfo.packageName
            }

            // --- Pass 2: ALL installed user packages (catches apps without launcher icons) ---
            val allPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in allPackages) {
                // Skip system apps (they're rarely useful to open by name)
                if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
                val label = pm.getApplicationLabel(appInfo).toString().lowercase().trim()
                if (label.isNotBlank()) cache.putIfAbsent(label, appInfo.packageName)
            }

            appCache = cache
            lastScanTime = System.currentTimeMillis()
            Log.d("AppLauncherAgent", "Cache built: ${appCache.size} apps")
        } catch (e: Exception) {
            Log.e("AppLauncherAgent", "Cache build failed: ${e.message}")
        }
    }

    private fun String.hasAny(vararg kw: String) = kw.any { this.contains(it) }

    companion object {
        private const val CACHE_TTL_MS = 3 * 60_000L

        /** Common apps — resolved instantly without a PackageManager scan */
        val ALIASES: Map<String, String> = mapOf(
            // Messaging
            "whatsapp" to "com.whatsapp",
            "wa" to "com.whatsapp",
            "whatsapp business" to "com.whatsapp.w4b",
            "telegram" to "org.telegram.messenger",
            "tg" to "org.telegram.messenger",
            "messages" to "com.android.mms",
            "sms" to "com.android.mms",
            "messenger" to "com.facebook.orca",
            "signal" to "org.thoughtcrime.securesms",
            "viber" to "com.viber.voip",
            "skype" to "com.skype.raider",
            "discord" to "com.discord",
            "slack" to "com.Slack",
            "teams" to "com.microsoft.teams",

            // Social
            "instagram" to "com.instagram.android",
            "insta" to "com.instagram.android",
            "ig" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "fb" to "com.facebook.katana",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "snapchat" to "com.snapchat.android",
            "snap" to "com.snapchat.android",
            "linkedin" to "com.linkedin.android",
            "pinterest" to "com.pinterest",
            "reddit" to "com.reddit.frontpage",

            // Google
            "youtube" to "com.google.android.youtube",
            "yt" to "com.google.android.youtube",
            "gmail" to "com.google.android.gm",
            "google maps" to "com.google.android.apps.maps",
            "maps" to "com.google.android.apps.maps",
            "google drive" to "com.google.android.apps.docs",
            "drive" to "com.google.android.apps.docs",
            "google photos" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "google meet" to "com.google.android.apps.meetings",
            "meet" to "com.google.android.apps.meetings",
            "google docs" to "com.google.android.apps.docs.editors.docs",
            "docs" to "com.google.android.apps.docs.editors.docs",
            "google sheets" to "com.google.android.apps.docs.editors.sheets",
            "sheets" to "com.google.android.apps.docs.editors.sheets",
            "google calendar" to "com.google.android.calendar",
            "calendar" to "com.google.android.calendar",
            "google assistant" to "com.google.android.googlequicksearchbox",
            "google" to "com.google.android.googlequicksearchbox",
            "chrome" to "com.android.chrome",
            "google chrome" to "com.android.chrome",
            "play store" to "com.android.vending",
            "playstore" to "com.android.vending",

            // MIUI / Xiaomi
            "gallery" to "com.miui.gallery",
            "mi gallery" to "com.miui.gallery",
            "music" to "com.miui.player",
            "mi music" to "com.miui.player",
            "video" to "com.miui.videoplayer",
            "mi video" to "com.miui.videoplayer",
            "file manager" to "com.android.fileexplorer",
            "files" to "com.android.fileexplorer",
            "mi browser" to "com.mi.globalbrowser",
            "mi store" to "com.xiaomi.market",
            "themes" to "com.miui.thememanager",
            "security" to "com.miui.securitycenter",
            "cleaner" to "com.miui.securitycenter",
            "mi home" to "com.xiaomi.smarthome",
            "scanner" to "com.xiaomi.scanner",
            "notes" to "com.miui.notes",
            "mi notes" to "com.miui.notes",
            "clock" to "com.android.deskclock",
            "contacts" to "com.android.contacts",
            "phone" to "com.android.dialer",
            "dialer" to "com.android.dialer",

            // Entertainment
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient",
            "amazon" to "com.amazon.mShop.android.shopping",
            "amazon music" to "com.amazon.mp3",
            "prime video" to "com.amazon.avod.thirdpartyclient",
            "prime" to "com.amazon.avod.thirdpartyclient",
            "zoom" to "us.zoom.videomeetings",
            "shazam" to "com.shazam.android",

            // Finance
            "gpay" to "com.google.android.apps.nbu.paisa.user",
            "google pay" to "com.google.android.apps.nbu.paisa.user",
            "paytm" to "net.one97.paytm",
            "phonepe" to "com.phonepe.app",
        )
    }
}
