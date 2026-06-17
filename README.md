# JAVIS OS — AI Companion for Android

> An intelligent, voice-first AI companion that understands natural language, remembers the user, controls Android apps, and operates online and offline.

[![Android Build](https://github.com/redx87518-bot/javis-os/actions/workflows/android-build.yml/badge.svg)](https://github.com/redx87518-bot/javis-os/actions/workflows/android-build.yml)

---

## Features

| Feature | Status |
|---|---|
| Natural language conversation | ✅ |
| Voice input (Android SpeechRecognizer) | ✅ |
| Voice output (ElevenLabs + Android TTS fallback) | ✅ |
| AI providers: Groq + DeepSeek with auto-fallback | ✅ |
| Memory engine (remembers user name, prefs, habits) | ✅ |
| App launcher (opens any installed app by name) | ✅ |
| YouTube search via voice | ✅ |
| Web search | ✅ |
| Alarm & reminder setting | ✅ |
| Contact calling (via dialer) | ✅ |
| Notification reader | ✅ |
| Accessibility Service | ✅ |
| Quick Settings Tile | ✅ |
| Persistent foreground notification | ✅ |
| Offline fallback mode | ✅ |
| Background service (stays alive) | ✅ |

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3, dark futuristic theme)
- **Architecture**: MVVM + Repository Pattern
- **DI**: Hilt
- **DB**: Room + DataStore
- **Network**: Retrofit + OkHttp
- **Async**: Coroutines + StateFlow

---

## First-Time Setup

### 1. Get API Keys

| Service | Where to get it | Required? |
|---|---|---|
| **Groq** | [console.groq.com](https://console.groq.com) — free tier available | Recommended |
| **DeepSeek** | [platform.deepseek.com](https://platform.deepseek.com) | Fallback |
| **ElevenLabs** | [elevenlabs.io](https://elevenlabs.io) | Optional (for natural voice) |

### 2. Install & Grant Permissions

When you first open the app, grant:
- **Microphone** — for voice input
- **Contacts & Phone** — for calling
- **Notifications** — for persistent notification
- **Notification Access** — Settings → Apps → Special App Access → Notification Access → JAVIS
- **Accessibility Service** — Settings → Accessibility → JAVIS (optional, for app control)

### 3. Configure in Settings

1. Enter your name
2. Paste your Groq API key (or DeepSeek)
3. Optionally add ElevenLabs API key + Voice ID for a natural voice
4. Tap **Save Settings**

---

## Example Commands

```
"Call Musa"
"Open YouTube and search Kano news"
"Set alarm for 7 AM tomorrow"
"Remind me at 8 to take medicine"
"Search Google for weather today"
"Open WhatsApp"
"Tell me a joke"
"My name is Ibrahim"
"What do you remember about me?"
"What's the time?"
```

---

## Build

### Debug APK
```bash
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (unsigned)
```bash
./gradlew assembleRelease
```

### GitHub Actions
Every push to `main` automatically builds both debug and release APKs. Download them from the **Actions** tab → latest workflow run → **Artifacts**.

---

## Minimum Requirements

- Android 8.0 (API 26) or higher
- ~50 MB storage
- Internet for AI features (offline mode available for basic tasks)
- Optimized for low-end devices (Redmi A1 and similar)

---

## Architecture

```
com.javis.os/
├── agent/          AgentRouter — classifies intent, dispatches to correct agent
├── data/
│   ├── local/      Room DB: conversations, memories, apps, alarms
│   └── remote/     Retrofit: Groq, DeepSeek, ElevenLabs APIs
├── di/             Hilt dependency injection modules
├── domain/         Models + repository interfaces
├── memory/         MemoryEngine — learns and recalls user info
├── planner/        TaskPlanner — parses alarms, timers, multi-step tasks
├── service/        Android services: foreground, accessibility, notification listener, tile
├── ui/
│   ├── screens/    Voice, Chat, Memory, Notifications, Settings
│   ├── theme/      Dark futuristic Compose theme
│   └── viewmodel/  AssistantViewModel, SettingsViewModel
├── util/           PreferencesManager, AppScanner
└── voice/          SpeechRecognitionManager, TtsManager
```

---

## Roadmap

- [ ] Wake word detection ("Hey JAVIS")
- [ ] WhatsApp message drafting with confirmation
- [ ] Multi-turn task execution via Accessibility Service
- [ ] Routine learning (auto-detect daily patterns)
- [ ] Local on-device LLM (Gemma / Phi-3 via MediaPipe)
- [ ] Conversation export

---

## License

MIT — free to use, modify, and distribute.
