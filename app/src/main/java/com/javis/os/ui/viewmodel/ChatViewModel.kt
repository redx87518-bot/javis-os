package com.javis.os.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.os.ai.AiMessage
import com.javis.os.ai.AiProvider
import com.javis.os.ai.AiProviderFactory
import com.javis.os.ai.JavisSystemPrompt
import com.javis.os.apps.AppDiscoveryService
import com.javis.os.contacts.Contact
import com.javis.os.contacts.ContactsManager
import com.javis.os.data.datastore.UserPreferencesDataStore
import com.javis.os.data.repository.ConversationRepository
import com.javis.os.data.repository.MemoryRepository
import com.javis.os.domain.model.Message
import com.javis.os.domain.model.TaskAction
import com.javis.os.domain.model.UserMemory
import com.javis.os.features.device.DeviceControlManager
import com.javis.os.features.weather.WeatherService
import com.javis.os.features.whatsapp.WhatsAppAutomation
import com.javis.os.features.whatsapp.WhatsAppDraft
import com.javis.os.tasks.ActionResult
import com.javis.os.tasks.TaskPlanner
import com.javis.os.tasks.UserIntent
import com.javis.os.voice.SpeechRecognitionManager
import com.javis.os.voice.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val memoryRepo: MemoryRepository,
    private val prefs: UserPreferencesDataStore,
    private val ttsManager: TextToSpeechManager,
    private val speechManager: SpeechRecognitionManager,
    private val taskPlanner: TaskPlanner,
    private val appDiscovery: AppDiscoveryService,
    private val contactsManager: ContactsManager,
    private val deviceControl: DeviceControlManager,
    private val weatherService: WeatherService,
    private val whatsApp: WhatsAppAutomation,
    private val defaultAiProvider: AiProvider
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _pendingAction = MutableStateFlow<TaskAction?>(null)
    val pendingAction: StateFlow<TaskAction?> = _pendingAction

    private val _pendingWhatsApp = MutableStateFlow<WhatsAppDraft?>(null)
    val pendingWhatsApp: StateFlow<WhatsAppDraft?> = _pendingWhatsApp

    private val _pendingCall = MutableStateFlow<Pair<String, String>?>(null)
    val pendingCall: StateFlow<Pair<String, String>?> = _pendingCall

    val recognitionState = speechManager.state
    val isSpeaking = ttsManager.isSpeaking

    private var currentAiProvider: AiProvider = defaultAiProvider
    private var userName: String = ""
    private var sessionId: String = System.currentTimeMillis().toString()
    private var isVoiceSession = false

    init {
        viewModelScope.launch {
            conversationRepo.getAllFlow().collectLatest { msgs ->
                _messages.value = msgs.takeLast(100)
            }
        }
        viewModelScope.launch { prefs.userName.collectLatest { userName = it } }
        viewModelScope.launch {
            prefs.aiProvider.collectLatest { provider ->
                val apiKey = prefs.aiApiKey.first()
                currentAiProvider = if (apiKey.isNotBlank()) {
                    AiProviderFactory.create(provider, apiKey)
                } else defaultAiProvider
            }
        }
        viewModelScope.launch { prefs.voiceSpeed.collectLatest { ttsManager.speechSpeed = it } }
        viewModelScope.launch { prefs.voicePitch.collectLatest { ttsManager.speechPitch = it } }
        setupSpeechRecognition()
    }

    private fun setupSpeechRecognition() {
        speechManager.onResult = { text ->
            isVoiceSession = true
            sendMessage(text, fromVoice = true)
        }
        speechManager.onError = { /* silent */ }
        speechManager.onPartialResult = { /* could show in UI */ }
    }

    fun startVoiceInput() {
        ttsManager.stop()
        speechManager.startListening()
    }

    fun stopVoiceInput() {
        speechManager.stopListening()
        ttsManager.stop()
    }

    fun sendMessage(text: String, fromVoice: Boolean = false) {
        if (text.isBlank() || _isProcessing.value) return

        viewModelScope.launch {
            if (fromVoice) isVoiceSession = true

            // Handle confirmation/cancellation for pending WhatsApp
            val wpDraft = _pendingWhatsApp.value
            if (wpDraft != null) {
                val lower = text.lowercase()
                when {
                    lower.contains("yes") || lower.contains("send") || lower.contains("go") || lower.contains("ok") -> {
                        _pendingWhatsApp.value = null
                        val contact = contactsManager.findBestMatch(wpDraft.contactName)
                        val number = contact?.phoneNumbers?.firstOrNull() ?: wpDraft.phoneNumber
                        whatsApp.openWhatsAppChat(number, wpDraft.message)
                        val reply = "Opening WhatsApp to send the message to ${wpDraft.contactName}."
                        conversationRepo.saveMessage("user", text, sessionId)
                        conversationRepo.saveMessage("assistant", reply, sessionId)
                        if (fromVoice) ttsManager.speak(reply)
                        return@launch
                    }
                    lower.contains("no") || lower.contains("cancel") || lower.contains("don't") || lower.contains("stop") -> {
                        _pendingWhatsApp.value = null
                        val reply = "Alright, message cancelled."
                        conversationRepo.saveMessage("user", text, sessionId)
                        conversationRepo.saveMessage("assistant", reply, sessionId)
                        if (fromVoice) ttsManager.speak(reply)
                        return@launch
                    }
                }
            }

            // Handle pending call confirmation
            val pendingCallData = _pendingCall.value
            if (pendingCallData != null) {
                val lower = text.lowercase()
                when {
                    lower.contains("yes") || lower.contains("call") || lower.contains("ok") || lower.contains("go") -> {
                        _pendingCall.value = null
                        contactsManager.makeCall(pendingCallData.second)
                        val reply = "Calling ${pendingCallData.first} now."
                        conversationRepo.saveMessage("user", text, sessionId)
                        conversationRepo.saveMessage("assistant", reply, sessionId)
                        if (fromVoice) ttsManager.speak(reply)
                        return@launch
                    }
                    lower.contains("no") || lower.contains("cancel") -> {
                        _pendingCall.value = null
                        val reply = "Call cancelled."
                        conversationRepo.saveMessage("user", text, sessionId)
                        conversationRepo.saveMessage("assistant", reply, sessionId)
                        if (fromVoice) ttsManager.speak(reply)
                        return@launch
                    }
                }
            }

            conversationRepo.saveMessage("user", text, sessionId)
            _isProcessing.value = true
            _messages.value = _messages.value + Message(role = "assistant", content = "", isLoading = true)

            try {
                // Try to handle locally first for speed
                val localResult = tryLocalHandling(text, fromVoice)
                if (localResult) {
                    _messages.value = _messages.value.dropLast(1)
                    _isProcessing.value = false
                    return@launch
                }

                val history = conversationRepo.getRecentHistory(20)
                val memoryContext = memoryRepo.buildMemoryContext()
                val systemPrompt = buildEnhancedSystemPrompt(memoryContext)
                val aiMessages = history
                    .filter { !it.isLoading }
                    .map { AiMessage(role = it.role, content = it.content) }

                val result = currentAiProvider.chat(aiMessages, systemPrompt)

                result.fold(
                    onSuccess = { response ->
                        _messages.value = _messages.value.dropLast(1)
                        conversationRepo.saveMessage("assistant", response, sessionId)
                        extractAndRemember(text, response)

                        val actions = taskPlanner.parseActionsFromResponse(response)
                        val cleanResponse = response.replace(Regex("""\[ACTION:\w+:[^\]]*]"""), "").trim()

                        if (cleanResponse.isNotBlank() && fromVoice) ttsManager.speak(cleanResponse)

                        for (action in actions) {
                            when (val res = taskPlanner.executeAction(action)) {
                                is ActionResult.Success -> {
                                    if (res.message.isNotBlank()) {
                                        if (fromVoice) ttsManager.speakQueued(res.message)
                                        conversationRepo.saveMessage("assistant", res.message, sessionId)
                                    }
                                }
                                is ActionResult.RequiresHandling -> handleSpecialAction(action, fromVoice)
                                is ActionResult.Failure -> {
                                    val errMsg = "Hmm, ${res.error}. Let me try a different approach."
                                    if (fromVoice) ttsManager.speakQueued(errMsg)
                                }
                            }
                        }
                    },
                    onFailure = {
                        _messages.value = _messages.value.dropLast(1)
                        val errMsg = "I'm having trouble connecting right now. Check your API key in Settings."
                        conversationRepo.saveMessage("assistant", errMsg, sessionId)
                        if (fromVoice) ttsManager.speak(errMsg)
                    }
                )
            } catch (e: Exception) {
                _messages.value = _messages.value.dropLast(1)
                val errMsg = "Something went wrong. Let me try again."
                if (fromVoice) ttsManager.speak(errMsg)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private suspend fun tryLocalHandling(text: String, fromVoice: Boolean): Boolean {
        val intent = taskPlanner.extractIntent(text)
        val lower = text.lowercase()

        return when (intent) {
            UserIntent.BATTERY -> {
                val response = deviceControl.formatBatteryResponse()
                conversationRepo.saveMessage("assistant", response, sessionId)
                if (fromVoice) ttsManager.speak(response)
                true
            }
            UserIntent.FLASHLIGHT -> {
                val turnOn = lower.contains("on") || lower.contains("turn on") || !lower.contains("off")
                if (lower.contains("off")) {
                    deviceControl.turnFlashlightOff()
                    val msg = "Flashlight off."
                    conversationRepo.saveMessage("assistant", msg, sessionId)
                    if (fromVoice) ttsManager.speak(msg)
                } else {
                    deviceControl.turnFlashlightOn()
                    val msg = "Flashlight on."
                    conversationRepo.saveMessage("assistant", msg, sessionId)
                    if (fromVoice) ttsManager.speak(msg)
                }
                true
            }
            UserIntent.VOLUME -> {
                when {
                    lower.contains("up") || lower.contains("increase") || lower.contains("louder") || lower.contains("raise") -> {
                        deviceControl.increaseVolume()
                        val msg = "Volume turned up."
                        conversationRepo.saveMessage("assistant", msg, sessionId)
                        if (fromVoice) ttsManager.speak(msg)
                    }
                    lower.contains("down") || lower.contains("decrease") || lower.contains("lower") || lower.contains("quieter") -> {
                        deviceControl.decreaseVolume()
                        val msg = "Volume turned down."
                        conversationRepo.saveMessage("assistant", msg, sessionId)
                        if (fromVoice) ttsManager.speak(msg)
                    }
                    lower.contains("mute") -> {
                        deviceControl.muteVolume()
                        val msg = "Muted."
                        conversationRepo.saveMessage("assistant", msg, sessionId)
                        if (fromVoice) ttsManager.speak(msg)
                    }
                }
                true
            }
            UserIntent.WEATHER -> {
                val locationRegex = Regex("(?:weather|temperature|forecast)(?:\\s+(?:in|at|for))?\\s+([\\w\\s]+)", RegexOption.IGNORE_CASE)
                val location = locationRegex.find(lower)?.groupValues?.get(1)?.trim()?.ifBlank { "auto" } ?: "auto"
                val result = weatherService.getWeather(location)
                val response = result.fold(
                    onSuccess = { weatherService.formatWeatherResponse(it) },
                    onFailure = { "I couldn't get the weather right now. Please check your internet connection." }
                )
                conversationRepo.saveMessage("assistant", response, sessionId)
                if (fromVoice) ttsManager.speak(response)
                true
            }
            UserIntent.CALL -> {
                val contactQuery = extractContactName(text)
                if (contactQuery.isNotBlank()) {
                    val contact = contactsManager.findBestMatch(contactQuery)
                    if (contact != null && contact.phoneNumbers.isNotEmpty()) {
                        val confirmMsg = "I found ${contact.name}. Should I call ${contact.phoneNumbers.first()}?"
                        _pendingCall.value = Pair(contact.name, contact.phoneNumbers.first())
                        conversationRepo.saveMessage("assistant", confirmMsg, sessionId)
                        if (fromVoice) ttsManager.speak(confirmMsg)
                        true
                    } else {
                        val notFound = "I couldn't find $contactQuery in your contacts. Could you check the name?"
                        conversationRepo.saveMessage("assistant", notFound, sessionId)
                        if (fromVoice) ttsManager.speak(notFound)
                        true
                    }
                } else false
            }
            else -> false
        }
    }

    private suspend fun handleSpecialAction(action: TaskAction, fromVoice: Boolean) {
        when (action) {
            is TaskAction.WhatsAppMessage -> {
                val draft = WhatsAppDraft(action.contact, action.phoneNumber, action.message)
                _pendingWhatsApp.value = draft
                val confirmMsg = whatsApp.buildConfirmationMessage(draft)
                conversationRepo.saveMessage("assistant", confirmMsg, sessionId)
                if (fromVoice) ttsManager.speakQueued(confirmMsg)
            }
            is TaskAction.MakeCall -> {
                val contact = contactsManager.findBestMatch(action.contact)
                val number = contact?.phoneNumbers?.firstOrNull() ?: action.number
                if (number.isBlank()) return
                val confirmMsg = "Should I call ${contact?.name ?: action.contact}?"
                _pendingCall.value = Pair(contact?.name ?: action.contact, number)
                conversationRepo.saveMessage("assistant", confirmMsg, sessionId)
                if (fromVoice) ttsManager.speakQueued(confirmMsg)
            }
            else -> _pendingAction.value = action
        }
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        viewModelScope.launch { taskPlanner.executeAction(action) }
    }

    fun cancelPendingAction() { _pendingAction.value = null }

    fun confirmPendingCall() {
        val call = _pendingCall.value ?: return
        _pendingCall.value = null
        viewModelScope.launch {
            contactsManager.makeCall(call.second)
            val msg = "Calling ${call.first}."
            conversationRepo.saveMessage("assistant", msg, sessionId)
            if (isVoiceSession) ttsManager.speak(msg)
        }
    }

    fun cancelPendingCall() {
        _pendingCall.value = null
        viewModelScope.launch {
            val msg = "Call cancelled."
            conversationRepo.saveMessage("assistant", msg, sessionId)
            if (isVoiceSession) ttsManager.speak(msg)
        }
    }

    fun confirmPendingWhatsApp() {
        val draft = _pendingWhatsApp.value ?: return
        _pendingWhatsApp.value = null
        viewModelScope.launch {
            val contact = contactsManager.findBestMatch(draft.contactName)
            val number = contact?.phoneNumbers?.firstOrNull() ?: draft.phoneNumber
            whatsApp.openWhatsAppChat(number, draft.message)
            val msg = "Opening WhatsApp."
            conversationRepo.saveMessage("assistant", msg, sessionId)
            if (isVoiceSession) ttsManager.speak(msg)
        }
    }

    fun cancelPendingWhatsApp() {
        _pendingWhatsApp.value = null
        viewModelScope.launch {
            val msg = "Message cancelled."
            conversationRepo.saveMessage("assistant", msg, sessionId)
            if (isVoiceSession) ttsManager.speak(msg)
        }
    }

    private fun buildEnhancedSystemPrompt(memoryContext: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
        val nameCtx = if (userName.isNotBlank()) "The user's name is $userName. " else ""
        val timeCtx = "It is currently $timeOfDay. "

        val actionGuide = """
When you need to trigger a device action, include action tags in your response:
- Call someone: [ACTION:CALL:ContactName:PhoneNumber]
- Launch app: [ACTION:LAUNCH:com.package.name:AppName]
- Search in app: [ACTION:SEARCH:AppName:search query]
- WhatsApp message: [ACTION:WHATSAPP:ContactName:PhoneNumber:message text]
- Set alarm: [ACTION:ALARM:Hour:Minute:Label]
- Set timer: [ACTION:TIMER:minutes]
- Volume up: [ACTION:VOLUME_UP:]
- Volume down: [ACTION:VOLUME_DOWN:]
- Mute: [ACTION:VOLUME_MUTE:]
- Flashlight on: [ACTION:FLASHLIGHT_ON:]
- Flashlight off: [ACTION:FLASHLIGHT_OFF:]
- Get weather: [ACTION:WEATHER:city name or auto]
- Battery status: [ACTION:BATTERY:]
Always ask for confirmation before calls and messages. Never send messages without user confirmation.
""".trimIndent()

        return JavisSystemPrompt.withMemory("$nameCtx$timeCtx$memoryContext") + "\n\n" + actionGuide
    }

    private fun extractContactName(text: String): String {
        val patterns = listOf(
            Regex("call\\s+([\\w\\s]+?)(?:\\s+(?:now|please|for me))?$", RegexOption.IGNORE_CASE),
            Regex("dial\\s+([\\w\\s]+?)(?:\\s+(?:now|please))?$", RegexOption.IGNORE_CASE),
            Regex("phone\\s+([\\w\\s]+?)(?:\\s+(?:now|please))?$", RegexOption.IGNORE_CASE),
            Regex("ring\\s+([\\w\\s]+?)(?:\\s+(?:now|please))?$", RegexOption.IGNORE_CASE)
        )
        patterns.forEach { pattern ->
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1].trim()
        }
        return ""
    }

    private suspend fun extractAndRemember(userText: String, aiResponse: String) {
        val lower = userText.lowercase()
        when {
            lower.contains("my name is") || lower.contains("i'm ") || lower.contains("i am ") -> {
                val name = extractNameFromText(userText)
                if (name.isNotBlank()) {
                    memoryRepo.remember("user_name", name, UserMemory.CATEGORY_NAME)
                    prefs.setUserName(name)
                }
            }
            lower.contains("i usually") || lower.contains("i always") || lower.contains("every morning") || lower.contains("every day") -> {
                memoryRepo.remember("habit_${System.currentTimeMillis()}", userText.take(200), UserMemory.CATEGORY_HABITS)
            }
            lower.contains("i like") || lower.contains("i love") || lower.contains("i prefer") || lower.contains("i enjoy") -> {
                memoryRepo.remember("pref_${System.currentTimeMillis()}", userText.take(200), UserMemory.CATEGORY_PREFERENCES)
            }
            lower.contains("wake me") || lower.contains("wake up") || lower.contains("sleep at") -> {
                memoryRepo.remember("routine_${System.currentTimeMillis()}", userText.take(200), UserMemory.CATEGORY_ROUTINES)
            }
        }
    }

    private fun extractNameFromText(text: String): String {
        listOf(
            Regex("my name is (\\w+)", RegexOption.IGNORE_CASE),
            Regex("i'?m (\\w+)", RegexOption.IGNORE_CASE),
            Regex("i am (\\w+)", RegexOption.IGNORE_CASE)
        ).forEach { pattern ->
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1]
        }
        return ""
    }

    fun greetUser() {
        viewModelScope.launch {
            val name = prefs.userName.first()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour in 5..11 -> if (name.isNotBlank()) "Good morning $name! Javis online. How can I help you?" else "Good morning! Javis online. How can I help?"
                hour in 12..16 -> if (name.isNotBlank()) "Good afternoon $name!" else "Good afternoon! What can I do for you?"
                hour in 17..21 -> if (name.isNotBlank()) "Good evening $name!" else "Good evening!"
                else -> if (name.isNotBlank()) "Hello $name! Javis online." else "Hello! Javis online."
            }
            ttsManager.speak(greeting)
        }
    }

    fun initializeVoice() {
        ttsManager.initialize()
        speechManager.initialize()
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationRepo.clearAll()
            sessionId = System.currentTimeMillis().toString()
        }
    }
}
