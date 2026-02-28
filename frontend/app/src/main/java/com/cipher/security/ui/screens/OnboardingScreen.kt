package com.cipher.security.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cipher.security.interceptor.CipherNotificationListener
import com.cipher.security.managers.BatteryPermissionManager
import com.cipher.security.ui.theme.CyberGreen
import com.cipher.security.ui.theme.SurfaceCard
import com.cipher.security.ui.theme.TextMuted

/**
 * Single source of truth for all onboarding permission states.
 */
private data class OnboardingState(
    val smsGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryGranted: Boolean = false
) {
    val allGranted: Boolean
        get() = smsGranted && notificationGranted && batteryGranted
}

private fun resolveOnboardingState(
    context: Context,
    batteryManager: BatteryPermissionManager
): OnboardingState {
    return OnboardingState(
        smsGranted = checkSmsPermissions(context),
        notificationGranted = checkNotificationPermission(context),
        batteryGranted = batteryManager.isIgnoringBatteryOptimizations()
    )
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val batteryManager = remember { BatteryPermissionManager(context) }

    var state by remember { mutableStateOf(resolveOnboardingState(context, batteryManager)) }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsOk = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                permissions[Manifest.permission.READ_SMS] == true
        state = state.copy(smsGranted = smsOk)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = resolveOnboardingState(context, batteryManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        state = resolveOnboardingState(context, batteryManager)
    }

    // --- Stable Layout: Box with bottom-anchored button ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top content: header + permission items in a non-weighted Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = "CIPHER Security",
                tint = CyberGreen,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to CIPHER",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "CIPHER monitors suspicious communication patterns and autonomously engages high-risk threats to extract actionable intelligence.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Permission Items: fixed height rows ---
            PermissionItem(
                icon = Icons.Outlined.Message,
                title = "Read & Receive SMS",
                description = "Analyzes incoming messages to detect and intercept security threats.",
                granted = state.smsGranted,
                onClick = {
                    if (!state.smsGranted) {
                        smsLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                icon = Icons.Outlined.Notifications,
                title = "Notification Access",
                description = "Monitors connected messaging platforms for real-time risk evaluation.",
                granted = state.notificationGranted,
                onClick = {
                    if (!state.notificationGranted) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                icon = Icons.Outlined.BatteryAlert,
                title = "Continuous Monitoring",
                description = "Configures OS policies to guarantee uninterrupted background evaluation.",
                granted = state.batteryGranted,
                onClick = {
                    if (!state.batteryGranted) {
                        batteryManager.requestBatteryExemption()
                    }
                }
            )
        }

        // --- Bottom-anchored action button: never pushed off-screen ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                enabled = state.allGranted
            ) {
                Text(
                    if (state.allGranted) "Activate Protection" else "Grant All Permissions",
                    color = if (state.allGranted) MaterialTheme.colorScheme.background else TextMuted,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Fixed-height permission row composable.
 * Uses [requiredHeight] to prevent vertical expansion on state transitions.
 */
@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) CyberGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status indicator: always same width allocation via fixed-width Box
        Box(
            modifier = Modifier.width(90.dp),
            contentAlignment = Alignment.Center
        ) {
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Granted",
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Enabled",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Enable", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun checkSmsPermissions(context: Context): Boolean {
    val receive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
    val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
    return receive == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
}

private fun checkNotificationPermission(context: Context): Boolean {
    val cn = android.content.ComponentName(context, CipherNotificationListener::class.java)
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat?.contains(cn.flattenToString()) == true
}
