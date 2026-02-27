package com.honeypot.scamguard.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.honeypot.scamguard.domain.model.ScamEventUi
import com.honeypot.scamguard.domain.model.ThreatLevel
import com.honeypot.scamguard.ui.components.RiskBadge
import com.honeypot.scamguard.ui.theme.CyberGreen
import com.honeypot.scamguard.ui.theme.Navy700
import com.honeypot.scamguard.ui.theme.SurfaceCard
import com.honeypot.scamguard.ui.theme.TextMuted
import com.honeypot.scamguard.ui.theme.TextSecondary
import com.honeypot.scamguard.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onProfileClick: (String) -> Unit = {}
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberGreen.copy(alpha = 0.15f),
                        selectedLabelColor = CyberGreen
                    )
                )
            }
            items(listOf(ThreatLevel.CRITICAL, ThreatLevel.HIGH, ThreatLevel.MEDIUM, ThreatLevel.LOW)) { level ->
                FilterChip(
                    selected = selectedFilter == level,
                    onClick = { viewModel.setFilter(level) },
                    label = { Text(level.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberGreen.copy(alpha = 0.15f),
                        selectedLabelColor = CyberGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timeline
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(events, key = { it.id }) { event ->
                TimelineEventCard(event = event, onProfileClick = { onProfileClick(event.scammerId) })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TimelineEventCard(event: ScamEventUi, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        // Timeline rail
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(Navy700)
            )
            Spacer(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CyberGreen)
                    .width(8.dp)
                    .height(8.dp)
            )
            Spacer(
                modifier = Modifier
                    .width(2.dp)
                    .height(80.dp)
                    .background(Navy700)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .clickable(onClick = onProfileClick)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.senderLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RiskBadge(level = event.threatLevel)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = event.originalMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (event.agentReply != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI Reply: ${event.agentReply}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${event.channel} • ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(event.timestamp))}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
