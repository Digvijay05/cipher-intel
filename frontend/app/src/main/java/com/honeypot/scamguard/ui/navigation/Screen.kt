package com.honeypot.scamguard.ui.navigation

/**
 * Sealed route definitions for type-safe navigation.
 * Top-level destinations appear in the bottom bar.
 * Detail destinations show a back arrow in the TopAppBar.
 */
sealed class Screen(val route: String, val label: String) {
    // Top-level (bottom bar) destinations
    data object Dashboard : Screen("dashboard", "CIPHER")
    data object History : Screen("history", "Scam History")
    data object Settings : Screen("settings", "Settings")

    // Onboarding
    data object Onboarding : Screen("onboarding", "Setup CIPHER")

    // Detail destinations (back arrow shown)
    data object ScammerProfile : Screen("profile/{scammerId}", "Scammer Profile") {
        fun createRoute(scammerId: String) = "profile/$scammerId"
    }
    data object IntelligenceDetail : Screen("intel/{scammerId}", "Intelligence Report") {
        fun createRoute(scammerId: String) = "intel/$scammerId"
    }

    companion object {
        /** Routes that appear in the bottom navigation bar. */
        val topLevel = listOf(Dashboard, History, Settings)

        /** Resolve a route pattern to its display label. */
        fun labelFor(route: String?): String = when {
            route == null -> Dashboard.label
            route == Dashboard.route -> Dashboard.label
            route == History.route -> History.label
            route == Settings.route -> Settings.label
            route == Onboarding.route -> Onboarding.label
            route.startsWith("profile/") -> ScammerProfile.label
            route.startsWith("intel/") -> IntelligenceDetail.label
            else -> Dashboard.label
        }

        /** Returns true if the given route is a top-level destination (no back arrow). */
        fun isTopLevel(route: String?): Boolean =
            topLevel.any { it.route == route }
    }
}
