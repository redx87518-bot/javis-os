package com.javis.os.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.os.service.JavisNotification
import com.javis.os.service.JavisNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {

    val notifications: StateFlow<List<JavisNotification>> = JavisNotificationListener.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
