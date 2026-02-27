package com.honeypot.scamguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.honeypot.scamguard.ui.navigation.NavGraph
import com.honeypot.scamguard.ui.theme.ScamGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScamGuardTheme {
                NavGraph()
            }
        }
    }
}
