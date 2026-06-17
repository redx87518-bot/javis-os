package com.javis.os.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.javis.os.ui.screens.*
import com.javis.os.ui.theme.BackgroundDark
import com.javis.os.ui.theme.BorderDark
import com.javis.os.ui.theme.JavisCyan
import com.javis.os.ui.theme.TextSecondary
import com.javis.os.ui.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Voice : Screen("voice", "Voice", Icons.Default.Mic)
    object Memory : Screen("memory", "Memory", Icons.Default.Psychology)
    object Notifications : Screen("notifications", "Alerts", Icons.Default.Notifications)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.SmartToy)
}

val bottomNavItems = listOf(Screen.Chat, Screen.Voice, Screen.Memory, Screen.Notifications, Screen.Settings)

@Composable
fun JavisNavHost(
    startVoiceMode: Boolean,
    onVoiceModeHandled: () -> Unit
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isOnboarding = currentRoute == Screen.Onboarding.route

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val userName by settingsViewModel.userName.collectAsState()
    val isFirstLaunch = userName.isEmpty()

    LaunchedEffect(startVoiceMode) {
        if (startVoiceMode && !isOnboarding) {
            navController.navigate(Screen.Voice.route) { launchSingleTop = true }
            onVoiceModeHandled()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (!isOnboarding) {
                NavigationBar(
                    containerColor = BackgroundDark,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Chat.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = JavisCyan,
                                selectedTextColor = JavisCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = BorderDark
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (isFirstLaunch) Screen.Onboarding.route else Screen.Chat.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onFinished = { name ->
                    if (name.isNotBlank()) settingsViewModel.setUserName(name)
                    navController.navigate(Screen.Voice.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Voice.route) { VoiceModeScreen() }
            composable(Screen.Memory.route) { MemoryScreen() }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
