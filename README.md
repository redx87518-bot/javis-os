# 🤖 JAVIS OS — Android AI Assistant

**J**ust **A** **V**oice **I**ntelligent **S**ystem — A complete Android AI companion built with Kotlin, Jetpack Compose, and modern Android architecture.

---

## Quick Start

### Prerequisites
- Android Studio Hedgehog or newer (2023.1.1+)
- JDK 17
- Android SDK 35
- A Google Gemini API key (free at [aistudio.google.com](https://aistudio.google.com))

### 1. Get a Gemini API Key
1. Go to [aistudio.google.com](https://aistudio.google.com/app/apikey)
2. Create a free API key
3. Copy it

### 2. Configure API Key
Open `app/build.gradle.kts` and replace:
```kotlin
buildConfigField("String", "GEMINI_API_KEY", "\"YOUR_GEMINI_API_KEY_HERE\"")
```
with your actual key:
```kotlin
buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSy...your-key...\"")
```

> **Tip:** You can also set it at runtime in Settings → AI Provider → API Key, which overrides the build-time key.

### 3. Open in Android Studio
```
File → Open → select the javis-os/ folder
```
Let Gradle sync complete.

### 4. Run on Device or Emulator
- Connect an Android device (API 26+) or use AVD
- Press **Run** (Shift+F10)

---

## Building the APK

### Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (requires signing config)
1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore javis-keystore.jks -alias javis -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Add to `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../javis-keystore.jks")
           storePassword = "your_password"
           keyAlias = "javis"
           keyPassword = "your_password"
       }
   }
   ```
3. Build:
   ```bash
   ./gradlew assembleRelease
   ```

---

## Feature Setup Guide

### Enable Accessibility Service (for shortcut activation)
1. Settings → Accessibility → Installed services → JAVIS
2. Toggle ON
3. JAVIS will now be accessible via the accessibility shortcut button

### Enable Notification Listener (for reading notifications)
1. Settings → Notifications → JAVIS → Allow
   OR tap "Grant Notification Access" in JAVIS Settings screen
2. JAVIS can now read and summarize your notifications

### Enable Quick Settings Tile
1. Pull down notification shade twice
2. Tap the edit/pencil icon
3. Find "JAVIS" tile and drag to active tiles
4. Tap the tile to activate voice mode instantly

### Add to Home Screen
- Open JAVIS → Settings → Shortcuts → "Add to Home Screen"
- Tap the created shortcut to launch directly in voice mode

---

## AI Provider Configuration

| Provider | Model | Cost | Setup |
|----------|-------|------|-------|
| **Google Gemini** (default) | gemini-1.5-flash | Free tier available | Get key at aistudio.google.com |
| **OpenAI** | gpt-4o-mini | Pay per use | Get key at platform.openai.com |

Switch providers in JAVIS Settings → AI Provider at any time.

---

## Architecture

```
com.javis.os/
├── JavisApplication.kt         — Hilt entry point, notification channels
├── MainActivity.kt             — Single activity, permission launcher
├── di/
│   └── AppModule.kt            — Hilt dependency injection
├── ai/
│   ├── AiProvider.kt           — AI abstraction interface
│   ├── GeminiProvider.kt       — Google Gemini implementation
│   ├── OpenAiProvider.kt       — OpenAI implementation
│   └── AiProviderFactory.kt    — Provider factory
├── data/
│   ├── db/                     — Room database (conversations, memory, apps)
│   ├── datastore/              — User preferences (DataStore)
│   └── repository/             — Data repositories
├── domain/
│   ├── model/                  — Domain models (Message, Memory, TaskPlan)
│   └── usecase/                — Business logic use cases
├── service/
│   ├── JavisForegroundService  — Persistent background service
│   ├── JavisAccessibilityService — System shortcut + app automation
│   ├── JavisNotificationListener — Reads device notifications
│   ├── JavisQuickSettingsTile  — Quick Settings integration
│   └── BootReceiver            — Auto-start on reboot
├── voice/
│   ├── SpeechRecognitionManager — Android SpeechRecognizer wrapper
│   └── TextToSpeechManager     — Android TTS wrapper
├── contacts/
│   └── ContactsManager         — Search, call, dial contacts
├── apps/
│   ├── AppDiscoveryService     — Scans and indexes installed apps
│   └── AppKnowledgeEngine      — App capability profiles
├── tasks/
│   └── TaskPlanner             — Multi-step task planning and execution
└── ui/
    ├── JavisNavHost.kt         — Navigation + bottom bar
    ├── theme/                  — Futuristic dark theme
    ├── screens/                — Chat, Voice, Memory, Notifications, Settings
    └── viewmodel/              — ViewModels for each screen
```

---

## Voice Command Examples

| Command | JAVIS Action |
|---------|-------------|
| "Call Musa" | Searches contacts, asks confirmation, makes call |
| "Open YouTube and search football highlights" | Launches YouTube with search query |
| "Send WhatsApp to Ibrahim saying I'm on my way" | Drafts message, asks confirmation |
| "Set alarm for 6 AM" | Opens alarm clock with 6:00 set |
| "Set a timer for 20 minutes" | Sets 20-min timer |
| "What's the weather?" | Free weather from wttr.in, no key needed |
| "What's my battery?" | Reads battery percentage + charging status |
| "Volume up / down / mute" | Adjusts media volume immediately |
| "Turn on / off flashlight" | Toggles camera flashlight |
| "What notifications do I have?" | Reads notification summary |
| "My name is Ibrahim" | JAVIS remembers your name |
| "Search Google for weather" | Opens Google/Chrome with weather search |
| "Remind me to call dad tomorrow at 8" | Sets reminder |
| "Hey JAVIS" (wake word) | Activates voice mode hands-free |

---

## Task Action Format

JAVIS uses an internal action tag format in AI responses to trigger real actions:

```
[ACTION:CALL:ContactName:PhoneNumber]
[ACTION:LAUNCH:com.package.name:AppName]
[ACTION:SEARCH:AppName:search query]
[ACTION:MESSAGE:ContactName:WhatsApp:message text]
[ACTION:ALARM:Hour:Minute:Label]
[ACTION:TIMER:minutes]
```

The AI naturally inserts these when it decides an action is needed. The app parses and executes them, always asking for confirmation on messages and calls.

---

## Optimized for Low-End Devices (Redmi A1)

- **Lazy loading**: only last 100 messages kept in memory
- **Gemini Flash model**: lightweight, fast responses
- **START_STICKY service**: auto-restarts if killed by system
- **Room DB**: indexed queries, no full-table scans
- **Coroutines**: non-blocking IO on background dispatcher
- **No heavy animations**: all animations are simple and GPU-accelerated
- **minSdk 26**: runs on Android 8.0+ devices

---

## Permissions Explained

| Permission | Why JAVIS needs it |
|------------|-------------------|
| `RECORD_AUDIO` | Voice input — hearing your commands |
| `FOREGROUND_SERVICE` | Stay active while screen is off |
| `READ_CONTACTS` | "Call Musa" — finding contact by name |
| `CALL_PHONE` | Making calls after your confirmation |
| `POST_NOTIFICATIONS` | Showing "JAVIS ONLINE" persistent notification |
| `BIND_ACCESSIBILITY_SERVICE` | Activation shortcut + in-app actions |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Reading your notifications when asked |
| `SET_ALARM` | Setting alarms and timers |
| `INTERNET` | AI API calls (Gemini/OpenAI) |

JAVIS will **never** send messages or make calls without explicit confirmation. Audio is only recorded when actively listening.

---

## Extending JAVIS

### Add a new AI provider
1. Implement `AiProvider` interface in `ai/`
2. Add case to `AiProviderFactory.create()`
3. Add option to Settings dropdown

### Add a new app capability
Add an entry to `AppKnowledgeEngine.knowledgeBase`:
```kotlin
"com.example.app" to AppProfile(
    "com.example.app", "My App",
    listOf("productivity"),
    listOf("create_note", "search")
)
```

### Add a new action type
1. Add a `data class` to `TaskAction` sealed class in `TaskPlan.kt`
2. Add parsing in `TaskPlanner.parseActionsFromResponse()`
3. Add execution in `TaskPlanner.executeAction()`

---

## License

MIT License — free to use, modify, and distribute.
Built with ❤️ as an open Android AI companion.
