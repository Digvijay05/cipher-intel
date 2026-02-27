package com.honeypot.scamguard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.honeypot.scamguard.domain.model.ServiceStatus
import com.honeypot.scamguard.ui.theme.CyberGreen
import com.honeypot.scamguard.ui.theme.ThreatRed
import com.honeypot.scamguard.ui.theme.WarningAmber

@Composable
fun StatusIndicator(status: ServiceStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        ServiceStatus.ACTIVE -> CyberGreen
        ServiceStatus.PAUSED -> WarningAmber
        ServiceStatus.STOPPED, ServiceStatus.ERROR -> ThreatRed
    }
    val label = when (status) {
        ServiceStatus.ACTIVE -> "System Active"
        ServiceStatus.PAUSED -> "Service Paused"
        ServiceStatus.STOPPED -> "Service Stopped"
        ServiceStatus.ERROR -> "Service Error"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == ServiceStatus.ACTIVE) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
