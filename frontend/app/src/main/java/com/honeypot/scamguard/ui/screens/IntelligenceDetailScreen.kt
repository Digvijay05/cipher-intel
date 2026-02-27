package com.honeypot.scamguard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.honeypot.scamguard.ui.components.IntelCard
import com.honeypot.scamguard.ui.viewmodel.ScammerProfileViewModel

@Composable
fun IntelligenceDetailScreen(
    scammerId: String,
    viewModel: ScammerProfileViewModel = viewModel()
) {
    LaunchedEffect(scammerId) { viewModel.loadProfile(scammerId) }

    val intelligence by viewModel.intelligence.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Intelligence Report",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        profile?.let {
            Text(
                text = "Subject: ${it.aliases.firstOrNull() ?: it.scammerId}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        IntelCard("UPI IDs", intelligence.upiIds, Icons.Outlined.Key)
        IntelCard("Bank Accounts", intelligence.bankAccounts, Icons.Outlined.AccountBalance)
        IntelCard("Suspicious Links", intelligence.suspiciousLinks, Icons.Outlined.Link)
        IntelCard("Phone Numbers", intelligence.phoneNumbers, Icons.Outlined.Phone)
        IntelCard("Keywords & Phrases", intelligence.keywords, Icons.Outlined.Tag)
        Spacer(modifier = Modifier.height(80.dp))
    }
}
