package com.cipher.security.ui.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.cipher.security.managers.BatteryPermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val batteryPermissionManager = BatteryPermissionManager(application)

    private val _serviceEnabled = MutableStateFlow(true)
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled

    private val _activeInterception = MutableStateFlow(true)
    val activeInterception: StateFlow<Boolean> = _activeInterception

    private val _notificationAccess = MutableStateFlow(false)
    val notificationAccess: StateFlow<Boolean> = _notificationAccess

    private val _batteryOptimizationExempt = MutableStateFlow(false)
    val batteryOptimizationExempt: StateFlow<Boolean> = _batteryOptimizationExempt

    private val _apiEndpoint = MutableStateFlow(com.cipher.security.BuildConfig.BASE_URL)
    val apiEndpoint: StateFlow<String> = _apiEndpoint

    val isXiaomiDevice = android.os.Build.MANUFACTURER.lowercase().let { 
        it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") 
    }

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _batteryOptimizationExempt.value = batteryPermissionManager.isIgnoringBatteryOptimizations()
        _notificationAccess.value = isNotificationListenerEnabled()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val app = getApplication<Application>()
        val pkgName = app.packageName
        val flat = Settings.Secure.getString(app.contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkgName) == true
    }

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
