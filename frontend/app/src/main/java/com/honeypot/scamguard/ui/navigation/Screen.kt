package com.honeypot.scamguard.ui.navigation

sealed class Screen(val route: String, val label: String) {
    data object Dashboard : Screen("dashboard", "Home")
    data object History : Screen("history", "History")
    data object Settings : Screen("settings", "Settings")
    data object ScammerProfile : Screen("profile/{scammerId}", "Profile") {
        fun createRoute(scammerId: String) = "profile/$scammerId"
    }
    data object IntelligenceDetail : Screen("intel/{scammerId}", "Intel") {
        fun createRoute(scammerId: String) = "intel/$scammerId"
    }
}
