package com.honeypot.scamguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.honeypot.scamguard.domain.model.ScamEventUi
import com.honeypot.scamguard.domain.model.ThreatLevel
import com.honeypot.scamguard.domain.repository.MockScamRepository
import com.honeypot.scamguard.domain.repository.ScamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    private val repository: ScamRepository = MockScamRepository()
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<ThreatLevel?>(null)
    val selectedFilter: StateFlow<ThreatLevel?> = _selectedFilter

    val events: StateFlow<List<ScamEventUi>> = repository.getAllEvents()
        .combine(_selectedFilter) { events, filter ->
            if (filter == null) events
            else events.filter { it.threatLevel == filter }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(level: ThreatLevel?) {
        _selectedFilter.value = level
    }
}
