package com.cipher.security.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.cipher.security.domain.model.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {

    private val _serviceEnabled = MutableStateFlow(true)
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled

    private val _activeInterception = MutableStateFlow(true)
    val activeInterception: StateFlow<Boolean> = _activeInterception

    private val _notificationAccess = MutableStateFlow(true)
    val notificationAccess: StateFlow<Boolean> = _notificationAccess

    private val _batteryOptimizationExempt = MutableStateFlow(false)
    val batteryOptimizationExempt: StateFlow<Boolean> = _batteryOptimizationExempt

    private val _apiEndpoint = MutableStateFlow("http://10.0.2.2:8000")
    val apiEndpoint: StateFlow<String> = _apiEndpoint

    fun toggleService(enabled: Boolean) {
        _serviceEnabled.value = enabled
    }

    fun toggleActiveInterception(enabled: Boolean) {
        _activeInterception.value = enabled
    }

    fun updateApiEndpoint(endpoint: String) {
        _apiEndpoint.value = endpoint
    }
}
