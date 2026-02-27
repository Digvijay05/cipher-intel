package com.honeypot.scamguard.ui.screens

import androidx.compose.foundation.background
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
import com.honeypot.scamguard.ui.theme.CyberGreen
import com.honeypot.scamguard.ui.theme.Navy700
import com.honeypot.scamguard.ui.theme.SurfaceCard
import com.honeypot.scamguard.ui.theme.TextMuted
import com.honeypot.scamguard.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val serviceEnabled by viewModel.serviceEnabled.collectAsStateWithLifecycle()
    val activeInterception by viewModel.activeInterception.collectAsStateWithLifecycle()
    val notificationAccess by viewModel.notificationAccess.collectAsStateWithLifecycle()
    val batteryExempt by viewModel.batteryOptimizationExempt.collectAsStateWithLifecycle()
    val apiEndpoint by viewModel.apiEndpoint.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

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
            granted = notificationAccess
        )
        PermissionRow(
            icon = Icons.Outlined.BatteryAlert,
            title = "Battery Optimization Exempt",
            granted = batteryExempt
        )

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
private fun PermissionRow(icon: ImageVector, title: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
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
