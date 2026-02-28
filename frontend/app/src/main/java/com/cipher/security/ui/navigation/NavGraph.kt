package com.cipher.security.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cipher.security.ui.screens.DashboardScreen
import com.cipher.security.ui.screens.HistoryScreen
import com.cipher.security.ui.screens.IntelligenceDetailScreen
import com.cipher.security.ui.screens.ScammerProfileScreen
import com.cipher.security.ui.screens.SettingsScreen
import com.cipher.security.ui.theme.CyberGreen
import com.cipher.security.ui.theme.Navy800
import com.cipher.security.ui.theme.Navy900
import com.cipher.security.ui.theme.TextMuted

private data class BottomNavItem(val screen: Screen, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, Icons.Filled.Home),
    BottomNavItem(Screen.History, Icons.Filled.Timeline),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val activity = LocalContext.current as? Activity

    // Check basic permissions to determine start destination dynamically
    val context = LocalContext.current
    val hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.RECEIVE_SMS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
    val startDest = if (hasSmsPermission) Screen.Dashboard.route else Screen.Onboarding.route

    // Determine navigation state
    val isTopLevel = Screen.isTopLevel(currentRoute)
    val showBottomBar = isTopLevel
    val title = Screen.labelFor(currentRoute)

    // On Dashboard, system back exits the app
    if (currentRoute == Screen.Dashboard.route) {
        BackHandler { activity?.finish() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900,
                    scrolledContainerColor = Navy800
                )
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Navy800) {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
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
                                    // Pop to start destination to avoid deep back-stack buildup
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(300)
                )
            }
        ) {
            // ── Top-level destinations ──

            composable(Screen.Onboarding.route) {
                com.cipher.security.ui.screens.OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

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

            // ── Detail destinations ──

            composable(
                route = Screen.ScammerProfile.route,
                arguments = listOf(navArgument("scammerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scammerId = backStackEntry.arguments?.getString("scammerId") ?: return@composable
                ScammerProfileScreen(
                    scammerId = scammerId,
                    onIntelClick = { id ->
                        navController.navigate(Screen.IntelligenceDetail.createRoute(id))
                    }
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
