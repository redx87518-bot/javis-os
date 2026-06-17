package com.javis.os.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.os.data.repository.MemoryRepository
import com.javis.os.domain.model.UserMemory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryRepo: MemoryRepository
) : ViewModel() {

    val memories: StateFlow<List<UserMemory>> = memoryRepo.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun forgetMemory(key: String) {
        viewModelScope.launch { memoryRepo.forget(key) }
    }

    fun clearAllMemories() {
        viewModelScope.launch { memoryRepo.clearAll() }
    }
}
