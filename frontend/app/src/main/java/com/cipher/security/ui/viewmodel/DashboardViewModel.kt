package com.cipher.security.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.security.domain.model.DashboardStats
import com.cipher.security.domain.model.ScamEventUi
import com.cipher.security.domain.model.ServiceStatus
import com.cipher.security.domain.model.ThreatLevel
import com.cipher.security.domain.repository.ThreatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ThreatRepository(application)

    val serviceStatus: StateFlow<ServiceStatus> = kotlinx.coroutines.flow.MutableStateFlow(ServiceStatus.ACTIVE)

    // Map the real database count to the mock DashboardStats for now so we don't break the UI
    val stats: StateFlow<DashboardStats> = repository.totalScamsDetected.map { totalScams ->
        DashboardStats(
            totalScamsDetected = totalScams,
            activeEngagements = 0, // Placeholder until feature implemented
            intelligenceExtracted = 0, // Placeholder until feature implemented
            threatScore = if (totalScams > 0) 85 else 0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardStats(0, 0, 0, 0)
    )

    // Convert List<ThreatEntity> into List<ScamEventUi>
    val recentEvents: StateFlow<List<ScamEventUi>> = repository.getHighRiskThreats(20)
        .map { threats ->
            threats.map { entity ->
                val threatLevel = when (entity.riskLevel.lowercase()) {
                    "critical" -> ThreatLevel.CRITICAL
                    "high" -> ThreatLevel.HIGH
                    "medium" -> ThreatLevel.MEDIUM
                    else -> ThreatLevel.LOW
                }
                
                ScamEventUi(
                    id = entity.id.hashCode(),
                    scammerId = entity.id.toString(),
                    senderLabel = entity.sender,
                    originalMessage = entity.body,
                    agentReply = null, // Agent reply column pending in DB schema
                    timestamp = entity.timestamp,
                    threatScore = entity.backendScore ?: entity.localScore,
                    threatLevel = threatLevel,
                    channel = entity.channel
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
