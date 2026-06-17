package com.javis.os.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.os.apps.AppDiscoveryService
import com.javis.os.data.datastore.UserPreferencesDataStore
import com.javis.os.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val appDiscovery: AppDiscoveryService,
    private val memoryRepo: MemoryRepository
) : ViewModel() {

    private val _startVoiceMode = MutableStateFlow(false)
    val startVoiceMode: StateFlow<Boolean> = _startVoiceMode

    fun onAppStarted() {
        viewModelScope.launch {
            val isFirst = prefs.isFirstLaunch.first()
            if (isFirst) {
                prefs.setFirstLaunch(false)
            }
            runCatching { appDiscovery.scanInstalledApps() }
        }
    }

    fun activateVoiceMode() {
        _startVoiceMode.value = true
    }

    fun onVoiceModeHandled() {
        _startVoiceMode.value = false
    }
}
