package com.cipher.security.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cipher.security.ui.components.IntelCard
import com.cipher.security.ui.components.RiskBadge
import com.cipher.security.ui.theme.CyberGreen
import com.cipher.security.ui.theme.Navy700
import com.cipher.security.ui.theme.SurfaceCard
import com.cipher.security.ui.theme.TextMuted
import com.cipher.security.ui.theme.TextSecondary
import com.cipher.security.ui.viewmodel.ScammerProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScammerProfileScreen(
    scammerId: String,
    viewModel: ScammerProfileViewModel = viewModel(),
    onIntelClick: (String) -> Unit = {}
) {
    LaunchedEffect(scammerId) { viewModel.loadProfile(scammerId) }

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val intelligence by viewModel.intelligence.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()

    val p = profile ?: return

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar placeholder from hash
                val initials = p.aliases.firstOrNull()?.take(2)?.uppercase() ?: "??"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Navy700)
                ) {
                    Text(
                        text = initials,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = p.aliases.firstOrNull() ?: "Unknown",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RiskBadge(level = p.threatLevel)
                        Text(
                            text = "${(p.confidenceScore * 100).toInt()}% confidence",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Meta row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetaStat("Events", "${p.totalEvents}")
                MetaStat("First Seen", dateFormat.format(Date(p.firstSeen)))
                MetaStat("Last Active", dateFormat.format(Date(p.lastEngaged)))
            }
        }

        // Aliases
        item {
            SectionTitle("Known Aliases")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.aliases.forEach { alias ->
                    Text(
                        text = alias,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Tactics
        item {
            SectionTitle("Tactics Detected")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.tacticsDetected.forEach { tactic ->
                    Text(
                        text = tactic,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Intelligence Cards
        item { SectionTitle("Extracted Intelligence") }
        item { IntelCard("UPI IDs", intelligence.upiIds, Icons.Outlined.Key) }
        item { IntelCard("Bank Accounts", intelligence.bankAccounts, Icons.Outlined.AccountBalance) }
        item { IntelCard("Suspicious Links", intelligence.suspiciousLinks, Icons.Outlined.Link) }
        item { IntelCard("Phone Numbers", intelligence.phoneNumbers, Icons.Outlined.Phone) }
        item { IntelCard("Keywords", intelligence.keywords, Icons.Outlined.Tag) }

        // Message History
        item { SectionTitle("Conversation Thread") }
        items(events, key = { it.id }) { event ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .padding(14.dp)
            ) {
                Text(
                    text = event.originalMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (event.agentReply != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "↳ ${event.agentReply}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun MetaStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
