package com.honeypot.scamguard.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.honeypot.scamguard.ui.screens.DashboardScreen
import com.honeypot.scamguard.ui.screens.HistoryScreen
import com.honeypot.scamguard.ui.screens.IntelligenceDetailScreen
import com.honeypot.scamguard.ui.screens.ScammerProfileScreen
import com.honeypot.scamguard.ui.screens.SettingsScreen
import com.honeypot.scamguard.ui.theme.CyberGreen
import com.honeypot.scamguard.ui.theme.Navy800
import com.honeypot.scamguard.ui.theme.TextMuted

private data class BottomNavItem(val screen: Screen, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, Icons.Filled.Home),
    BottomNavItem(Screen.History, Icons.Filled.Timeline),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings)
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar on top-level screens
    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Navy800) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.screen.label
                                )
                            },
                            label = { Text(item.screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberGreen,
                                selectedTextColor = CyberGreen,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = CyberGreen.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onEventClick = { event ->
                        navController.navigate(Screen.ScammerProfile.createRoute(event.scammerId))
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onProfileClick = { scammerId ->
                        navController.navigate(Screen.ScammerProfile.createRoute(scammerId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.ScammerProfile.route,
                arguments = listOf(navArgument("scammerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scammerId = backStackEntry.arguments?.getString("scammerId") ?: return@composable
                ScammerProfileScreen(
                    scammerId = scammerId,
                    onIntelClick = { navController.navigate(Screen.IntelligenceDetail.createRoute(it)) }
                )
            }
            composable(
                route = Screen.IntelligenceDetail.route,
                arguments = listOf(navArgument("scammerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scammerId = backStackEntry.arguments?.getString("scammerId") ?: return@composable
                IntelligenceDetailScreen(scammerId = scammerId)
            }
        }
    }
}
