package com.cipher.security.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.security.domain.model.IntelligenceData
import com.cipher.security.domain.model.ScamEventUi
import com.cipher.security.domain.model.ScammerProfileUi
import com.cipher.security.domain.repository.MockScamRepository
import com.cipher.security.domain.repository.ScamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScammerProfileViewModel(
    private val repository: ScamRepository = MockScamRepository()
) : ViewModel() {

    private val _profile = MutableStateFlow<ScammerProfileUi?>(null)
    val profile: StateFlow<ScammerProfileUi?> = _profile

    private val _intelligence = MutableStateFlow(
        IntelligenceData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    )
    val intelligence: StateFlow<IntelligenceData> = _intelligence

    private val _events = MutableStateFlow<List<ScamEventUi>>(emptyList())
    val events: StateFlow<List<ScamEventUi>> = _events

    fun loadProfile(scammerId: String) {
        viewModelScope.launch {
            _profile.value = repository.getProfileById(scammerId)
            _intelligence.value = repository.getIntelligenceForScammer(scammerId)
        }
        // Collect events reactively
        viewModelScope.launch {
            repository.getEventsForScammer(scammerId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
                .collect { _events.value = it }
        }
    }
}
