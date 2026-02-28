package com.cipher.security.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cipher.security.ui.theme.CyberGreen
import com.cipher.security.ui.theme.Navy700
import com.cipher.security.ui.theme.SurfaceCard
import com.cipher.security.ui.theme.TextMuted
import com.cipher.security.ui.theme.WarningAmber
import com.cipher.security.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val serviceEnabled by viewModel.serviceEnabled.collectAsStateWithLifecycle()
    val activeInterception by viewModel.activeInterception.collectAsStateWithLifecycle()
    val notificationAccess by viewModel.notificationAccess.collectAsStateWithLifecycle()
    val batteryExempt by viewModel.batteryOptimizationExempt.collectAsStateWithLifecycle()
    val apiEndpoint by viewModel.apiEndpoint.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Service Controls Section
        SectionHeader("Service Controls")
        SettingsToggle(
            icon = Icons.Outlined.Security,
            title = "ScamGuard Service",
            subtitle = "Enable 24/7 background protection",
            checked = serviceEnabled,
            onCheckedChange = { viewModel.toggleService(it) }
        )
        SettingsToggle(
            icon = Icons.Outlined.Api,
            title = "Active Interception",
            subtitle = "Autonomously reply to suspected scammers",
            checked = activeInterception,
            onCheckedChange = { viewModel.toggleActiveInterception(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permissions Section
        SectionHeader("Permissions")
        PermissionRow(
            icon = Icons.Outlined.Notifications,
            title = "Notification Access",
            granted = notificationAccess,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        )
        PermissionRow(
            icon = Icons.Outlined.BatteryAlert,
            title = "Battery Optimization Exempt",
            granted = batteryExempt,
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(fallback)
                }
            }
        )
        
        if (viewModel.isXiaomiDevice && !batteryExempt) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Icon(Icons.Outlined.Api, contentDescription = "OEM Warning", tint = WarningAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MIUI Detected: Auto-start permission may also be required for 24/7 background interception. Check your device's security app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Config
        SectionHeader("Backend Configuration")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .padding(16.dp)
        ) {
            Text("Cloud API Endpoint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(apiEndpoint, style = MaterialTheme.typography.bodyMedium, color = CyberGreen)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Info
        SectionHeader("About")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .padding(16.dp)
        ) {
            InfoRow("Version", "1.0.0")
            Divider(color = Navy700, modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("Architecture", "MVVM + Compose")
            Divider(color = Navy700, modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("Inference", "Cloud-Based (FastAPI)")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = CyberGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = CyberGreen)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, granted: Boolean, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = CyberGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = if (granted) "GRANTED" else "DENIED",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (granted) CyberGreen else MaterialTheme.colorScheme.error
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
