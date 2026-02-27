package com.honeypot.scamguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.honeypot.scamguard.domain.model.DashboardStats
import com.honeypot.scamguard.domain.model.ScamEventUi
import com.honeypot.scamguard.domain.model.ServiceStatus
import com.honeypot.scamguard.domain.repository.MockScamRepository
import com.honeypot.scamguard.domain.repository.ScamRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val repository: ScamRepository = MockScamRepository()
) : ViewModel() {

    val serviceStatus: StateFlow<ServiceStatus> = repository.getServiceStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceStatus.ACTIVE)

    val stats: StateFlow<DashboardStats> = repository.getDashboardStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardStats(0, 0, 0, 0)
        )

    val recentEvents: StateFlow<List<ScamEventUi>> = repository.getRecentEvents(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
