package com.javis.os.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.os.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val aiProvider: String = "groq",
    val groqApiKey: String = "",
    val groqModel: String = "llama3-70b-8192",
    val deepSeekApiKey: String = "",
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "",
    val ttsProvider: String = "android",
    val ttsSpeed: Float = 1.0f,
    val offlineMode: Boolean = false,
    val savedMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            userName = prefs.getUserName(),
            aiProvider = prefs.getAiProvider(),
            groqApiKey = prefs.getGroqApiKey(),
            groqModel = prefs.getGroqModel(),
            deepSeekApiKey = prefs.getDeepSeekApiKey(),
            elevenLabsApiKey = prefs.getElevenLabsApiKey(),
            elevenLabsVoiceId = prefs.getElevenLabsVoiceId(),
            ttsProvider = prefs.getTtsProvider(),
            ttsSpeed = prefs.getTtsSpeed(),
            offlineMode = prefs.isOfflineMode()
        )
    }

    fun updateUserName(name: String) = _uiState.update { it.copy(userName = name) }
    fun updateAiProvider(p: String) = _uiState.update { it.copy(aiProvider = p) }
    fun updateGroqApiKey(k: String) = _uiState.update { it.copy(groqApiKey = k) }
    fun updateGroqModel(m: String) = _uiState.update { it.copy(groqModel = m) }
    fun updateDeepSeekApiKey(k: String) = _uiState.update { it.copy(deepSeekApiKey = k) }
    fun updateElevenLabsApiKey(k: String) = _uiState.update { it.copy(elevenLabsApiKey = k) }
    fun updateElevenLabsVoiceId(id: String) = _uiState.update { it.copy(elevenLabsVoiceId = id) }
    fun updateTtsProvider(p: String) = _uiState.update { it.copy(ttsProvider = p) }
    fun updateTtsSpeed(s: Float) = _uiState.update { it.copy(ttsSpeed = s) }
    fun updateOfflineMode(enabled: Boolean) = _uiState.update { it.copy(offlineMode = enabled) }

    fun saveSettings() {
        viewModelScope.launch {
            val s = _uiState.value
            prefs.setUserName(s.userName)
            prefs.setAiProvider(s.aiProvider)
            prefs.setGroqApiKey(s.groqApiKey)
            prefs.setGroqModel(s.groqModel)
            prefs.setDeepSeekApiKey(s.deepSeekApiKey)
            prefs.setElevenLabsApiKey(s.elevenLabsApiKey)
            prefs.setElevenLabsVoiceId(s.elevenLabsVoiceId)
            prefs.setTtsProvider(s.ttsProvider)
            prefs.setTtsSpeed(s.ttsSpeed)
            prefs.setOfflineMode(s.offlineMode)
            _uiState.update { it.copy(savedMessage = "Settings saved successfully!") }
        }
    }

    fun clearSavedMessage() = _uiState.update { it.copy(savedMessage = null) }
}
