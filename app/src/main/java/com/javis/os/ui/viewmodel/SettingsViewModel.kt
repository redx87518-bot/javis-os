package com.javis.os.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.os.MainActivity
import com.javis.os.R
import com.javis.os.data.datastore.UserPreferencesDataStore
import com.javis.os.data.repository.ConversationRepository
import com.javis.os.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val conversationRepo: ConversationRepository,
    private val memoryRepo: MemoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userName = prefs.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val aiProvider = prefs.aiProvider.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "gemini")
    val aiApiKey = prefs.aiApiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val voiceSpeed = prefs.voiceSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.0f)
    val voicePitch = prefs.voicePitch.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 1.0f)
    val notificationsEnabled = prefs.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    fun setUserName(name: String) = viewModelScope.launch { prefs.setUserName(name) }
    fun setAiProvider(provider: String) = viewModelScope.launch { prefs.setAiProvider(provider) }
    fun setAiApiKey(key: String) = viewModelScope.launch { prefs.setAiApiKey(key) }
    fun setVoiceSpeed(speed: Float) = viewModelScope.launch { prefs.setVoiceSpeed(speed) }
    fun setVoicePitch(pitch: Float) = viewModelScope.launch { prefs.setVoicePitch(pitch) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }

    fun clearMemory() = viewModelScope.launch { memoryRepo.clearAll() }
    fun clearHistory() = viewModelScope.launch { conversationRepo.clearAll() }

    fun addHomeScreenShortcut() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager.isRequestPinShortcutSupported) {
            val shortcutIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.javis.os.ACTIVATE_VOICE"
            }
            val shortcutInfo = ShortcutInfo.Builder(context, "javis_voice_shortcut")
                .setShortLabel("JAVIS")
                .setLongLabel("Activate JAVIS")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_javis))
                .setIntent(shortcutIntent)
                .build()
            shortcutManager.requestPinShortcut(shortcutInfo, null)
        }
    }
}
