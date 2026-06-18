package com.javis.os.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.os.agent.AgentRouter
import com.javis.os.agent.GreetingManager
import com.javis.os.domain.model.Message
import com.javis.os.domain.repository.ConversationRepository
import com.javis.os.memory.MemoryEngine
import com.javis.os.service.JavisAssistantService
import com.javis.os.util.PreferencesManager
import com.javis.os.voice.SpeechRecognitionManager
import com.javis.os.voice.SpeechState
import com.javis.os.voice.TtsManager
import com.javis.os.voice.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantUiState(
    val messages: List<Message> = emptyList(),
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialText: String = "",
    val lastError: String? = null,
    val assistantStatus: AssistantStatus = AssistantStatus.IDLE,
    val autoListenEnabled: Boolean = true
)

enum class AssistantStatus { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

@HiltViewModel
class AssistantViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val agentRouter: AgentRouter,
    private val conversationRepository: ConversationRepository,
    private val speechManager: SpeechRecognitionManager,
    private val ttsManager: TtsManager,
    private val memoryEngine: MemoryEngine,
    private val greetingManager: GreetingManager,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null
    private var autoListenJob: Job? = null

    init {
        observeMessages()
        observeSpeechState()
        observeTtsState()
        startAssistantService()
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeMessages() {
        viewModelScope.launch {
            conversationRepository.getRecentMessages().collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    private fun observeSpeechState() {
        viewModelScope.launch {
            speechManager.state.collect { state ->
                when (state) {
                    is SpeechState.Listening -> _uiState.update {
                        it.copy(
                            isListening = true,
                            assistantStatus = AssistantStatus.LISTENING,
                            lastError = null
                        )
                    }
                    is SpeechState.Processing -> _uiState.update {
                        it.copy(
                            isListening = false,
                            isThinking = true,
                            assistantStatus = AssistantStatus.THINKING
                        )
                    }
                    is SpeechState.Result -> {
                        _uiState.update { it.copy(isListening = false, partialText = "") }
                        processUserInput(state.text)
                    }
                    is SpeechState.Error -> {
                        _uiState.update {
                            it.copy(
                                isListening = false,
                                isThinking = false,
                                lastError = state.message,
                                assistantStatus = AssistantStatus.ERROR
                            )
                        }
                        // Auto-retry listening after error (not too fast)
                        if (_uiState.value.autoListenEnabled) {
                            scheduleAutoListen(delayMs = 2000)
                        }
                    }
                    SpeechState.Idle -> _uiState.update {
                        it.copy(isListening = false, partialText = "")
                    }
                }
            }
        }
        viewModelScope.launch {
            speechManager.partialResult.collect { partial ->
                _uiState.update { it.copy(partialText = partial) }
            }
        }
    }

    private fun observeTtsState() {
        viewModelScope.launch {
            ttsManager.state.collect { state ->
                val isSpeaking = state is TtsState.Speaking || state is TtsState.Loading
                _uiState.update {
                    it.copy(
                        isSpeaking = isSpeaking,
                        assistantStatus = when (state) {
                            TtsState.Speaking -> AssistantStatus.SPEAKING
                            TtsState.Loading -> AssistantStatus.THINKING
                            TtsState.Idle -> AssistantStatus.IDLE
                        }
                    )
                }
                // When speaking finishes, auto-start listening again
                if (state == TtsState.Idle && _uiState.value.autoListenEnabled) {
                    scheduleAutoListen(delayMs = 600)
                }
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Greet the user with a contextual welcome, then start listening.
     * Called when VoiceSessionActivity opens.
     */
    fun greetAndListen() {
        viewModelScope.launch {
            val greeting = greetingManager.getWakeResponse()
            _uiState.update { it.copy(assistantStatus = AssistantStatus.SPEAKING) }
            conversationRepository.addMessage(Message.Role.ASSISTANT, greeting)
            ttsManager.speak(greeting)
            // TtsState observer will auto-start listening when done
        }
    }

    fun startListening() {
        autoListenJob?.cancel()
        val state = _uiState.value
        if (state.isListening || state.isThinking || state.isSpeaking) return
        ttsManager.stopSpeaking()
        speechManager.startListening { /* handled via state flow */ }
    }

    fun stopListening() {
        autoListenJob?.cancel()
        speechManager.stopListening()
        _uiState.update { it.copy(assistantStatus = AssistantStatus.IDLE) }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        processUserInput(text)
    }

    fun stopSpeaking() {
        ttsManager.stopSpeaking()
        _uiState.update { it.copy(assistantStatus = AssistantStatus.IDLE) }
    }

    fun clearError() {
        _uiState.update { it.copy(lastError = null, assistantStatus = AssistantStatus.IDLE) }
    }

    fun toggleAutoListen(enabled: Boolean) {
        _uiState.update { it.copy(autoListenEnabled = enabled) }
    }

    // ── Core processing ───────────────────────────────────────────────────────

    private fun processUserInput(input: String) {
        autoListenJob?.cancel()
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isThinking = true,
                    isListening = false,
                    assistantStatus = AssistantStatus.THINKING
                )
            }

            conversationRepository.addMessage(Message.Role.USER, input)

            val response = try {
                agentRouter.route(input)
            } catch (e: Exception) {
                "I hit a snag: ${e.message}. Let me try again."
            }

            conversationRepository.addMessage(Message.Role.ASSISTANT, response)

            // Learn from the exchange
            viewModelScope.launch {
                try { memoryEngine.learnFromConversation(input, response) } catch (_: Exception) {}
            }

            _uiState.update { it.copy(isThinking = false) }
            ttsManager.speak(response)
            // TtsState observer handles auto-listen after speech
        }
    }

    private fun scheduleAutoListen(delayMs: Long) {
        autoListenJob?.cancel()
        autoListenJob = viewModelScope.launch {
            delay(delayMs)
            val s = _uiState.value
            if (s.autoListenEnabled && !s.isListening && !s.isThinking && !s.isSpeaking) {
                startListening()
            }
        }
    }

    // ── Service ───────────────────────────────────────────────────────────────

    private fun startAssistantService() {
        val intent = Intent(context, JavisAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoListenJob?.cancel()
        processingJob?.cancel()
        speechManager.stopListening()
        ttsManager.destroy()
    }
}
