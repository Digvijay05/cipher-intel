package com.honeypot.scamguard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Enable Active Interception", fontWeight = FontWeight.SemiBold)
                Text("Allow Honeypot to reply autonomously", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = true, onCheckedChange = {})
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Cloud API Endpoint", fontWeight = FontWeight.SemiBold)
                Text("http://10.0.2.2:8000", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { /* Edit endpoint */ }) {
                Text("Edit")
            }
        }
    }
}
