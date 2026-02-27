package com.honeypot.scamguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScamGuardDashboard()
                }
            }
        }
    }
}

@Composable
fun ScamGuardDashboard() {
    Text("ScamGuard: Interceptor Active")
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    MaterialTheme {
        ScamGuardDashboard()
    }
}
