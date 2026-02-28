package com.cipher.security.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.security.data.entity.ThreatEntity
import com.cipher.security.domain.model.ScamEventUi
import com.cipher.security.domain.model.ThreatLevel
import com.cipher.security.domain.repository.ThreatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val threatRepository = ThreatRepository(application)

    private val _selectedFilter = MutableStateFlow<ThreatLevel?>(null)
    val selectedFilter: StateFlow<ThreatLevel?> = _selectedFilter

    /**
     * Maps real ThreatEntity records from Room into UI-ready ScamEventUi objects.
     * Combines with user-selected filter for reactive updates.
     */
    val events: StateFlow<List<ScamEventUi>> = threatRepository.allThreats
        .map { threats -> threats.map { it.toScamEventUi() } }
        .combine(_selectedFilter) { events, filter ->
            if (filter == null) events
            else events.filter { it.threatLevel == filter }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(level: ThreatLevel?) {
        _selectedFilter.value = level
    }

    companion object {
        private fun ThreatEntity.toScamEventUi(): ScamEventUi {
            return ScamEventUi(
                id = id.toInt(),
                scammerId = sender,
                senderLabel = sender,
                originalMessage = body,
                agentReply = null,
                timestamp = timestamp,
                threatScore = backendScore ?: localScore,
                threatLevel = when (riskLevel.lowercase()) {
                    "critical" -> ThreatLevel.CRITICAL
                    "high" -> ThreatLevel.HIGH
                    "medium" -> ThreatLevel.MEDIUM
                    "low" -> ThreatLevel.LOW
                    else -> ThreatLevel.NONE
                },
                channel = channel
            )
        }
    }
}
