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
    val assistantStatus: AssistantStatus = AssistantStatus.IDLE
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

    init {
        observeMessages()
        observeSpeechState()
        observeTtsState()
        startAssistantService()
    }

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
                        it.copy(isListening = true, assistantStatus = AssistantStatus.LISTENING, lastError = null)
                    }
                    is SpeechState.Processing -> _uiState.update {
                        it.copy(isListening = false, isThinking = true, assistantStatus = AssistantStatus.THINKING)
                    }
                    is SpeechState.Result -> {
                        _uiState.update { it.copy(isListening = false, partialText = "") }
                        processUserInput(state.text)
                    }
                    is SpeechState.Error -> _uiState.update {
                        it.copy(
                            isListening = false, isThinking = false,
                            lastError = state.message, assistantStatus = AssistantStatus.ERROR
                        )
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
                _uiState.update {
                    it.copy(
                        isSpeaking = state is TtsState.Speaking || state is TtsState.Loading,
                        assistantStatus = when (state) {
                            TtsState.Speaking -> AssistantStatus.SPEAKING
                            TtsState.Loading  -> AssistantStatus.THINKING
                            TtsState.Idle     -> AssistantStatus.IDLE
                        }
                    )
                }
            }
        }
    }

    fun startListening() {
        if (_uiState.value.isListening) return
        ttsManager.stopSpeaking()
        speechManager.startListening { /* state flow handles result */ }
    }

    fun stopListening() {
        speechManager.stopListening()
        _uiState.update { it.copy(assistantStatus = AssistantStatus.IDLE) }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        processUserInput(text)
    }

    private fun processUserInput(input: String) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _uiState.update { it.copy(isThinking = true, assistantStatus = AssistantStatus.THINKING) }

            conversationRepository.addMessage(Message.Role.USER, input)

            val response = try {
                agentRouter.route(input)
            } catch (e: Exception) {
                "I ran into an issue: ${e.message}. Please try again."
            }

            conversationRepository.addMessage(Message.Role.ASSISTANT, response)
            memoryEngine.learnFromConversation(input, response)

            _uiState.update { it.copy(isThinking = false) }
            ttsManager.speak(response)
        }
    }

    fun stopSpeaking() {
        ttsManager.stopSpeaking()
        _uiState.update { it.copy(assistantStatus = AssistantStatus.IDLE) }
    }

    fun clearError() {
        _uiState.update { it.copy(lastError = null, assistantStatus = AssistantStatus.IDLE) }
    }

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
        speechManager.stopListening()
        ttsManager.destroy()
    }
}
